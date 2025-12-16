package com.example.dingding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingding.dto.DepartmentDTO;
import com.example.dingding.dto.DepartmentSyncResultDTO;
import com.example.dingding.entity.DepartmentSCD2;
import com.example.dingding.mapper.DepartmentSCD2Mapper;
import com.example.dingding.service.IDepartmentSCD2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 部门SCD2服务实现类
 * 提供部门维度的SCD2管理功能
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class DepartmentSCD2ServiceImpl
        extends ServiceImpl<DepartmentSCD2Mapper, DepartmentSCD2>
        implements IDepartmentSCD2Service {

    /**
     * 查找部门的当前版本
     */
    @Override
    public DepartmentSCD2 findCurrentByDeptId(Long deptId) {
        return baseMapper.findCurrentByDeptId(deptId);
    }

    /**
     * 获取所有当前版本的部门
     */
    @Override
    public List<DepartmentSCD2> findAllCurrent() {
        return baseMapper.findAllCurrent();
    }

    /**
     * 为新增部门创建新版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentSCD2 insertNewVersion(DepartmentDTO deptDto, LocalDate effectiveDate) {
        log.debug("为新增部门[{}] {} 创建新版本，生效日期: {}",
                 deptDto.getDeptId(), deptDto.getName(), effectiveDate);

        DepartmentSCD2 newDept = DepartmentSCD2.createNewVersion(
                deptDto.getDeptId(),
                deptDto.getParentId(),
                deptDto.getName(),
                effectiveDate
        );

        save(newDept);
        return newDept;
    }

    /**
     * 关闭部门的旧版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOldVersion(Long deptId, LocalDate closeDate) {
        log.debug("关闭部门[{}]的旧版本，失效日期: {}", deptId, closeDate);

        int updated = baseMapper.closeOldVersion(deptId, closeDate);
        if (updated > 0) {
            log.info("成功关闭部门[{}]的旧版本", deptId);
        } else {
            log.warn("未找到部门[{}]的当前版本", deptId);
        }

        return updated > 0;
    }

    /**
     * 为变更的部门创建新版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentSCD2 createNewVersionForChangedDept(DepartmentDTO deptDto, LocalDate effectiveDate) {
        log.info("部门[{}]发生变化，创建新版本", deptDto.getDeptId());

        // 1. 关闭旧版本（失效日期为生效日期的前一天）
        LocalDate closeDate = effectiveDate.minusDays(1);
        closeOldVersion(deptDto.getDeptId(), closeDate);

        // 2. 创建新版本
        return insertNewVersion(deptDto, effectiveDate);
    }

    /**
     * 检测部门是否发生变化
     */
    @Override
    public boolean hasChanged(DepartmentSCD2 current, DepartmentDTO newDept) {
        // 部门ID不可能变化，这是业务键
        assert Objects.equals(current.getDeptId(), newDept.getDeptId());

        // 检测部门名称变化（去除前后空格进行比较）
        String currentName = current.getName() == null ? "" : current.getName().trim();
        String newName = newDept.getName() == null ? "" : newDept.getName().trim();
        boolean nameChanged = !Objects.equals(currentName, newName);

        // 检测父部门ID变化（注意处理null值）
        boolean parentChanged = !Objects.equals(current.getParentId(), newDept.getParentId());

        // 如果有变化，记录详细日志
        if (nameChanged || parentChanged) {
            log.info("检测到部门[{}]发生变化:", current.getDeptId());

            if (nameChanged) {
                log.info("  - 部门名称: '{}' -> '{}'",
                        current.getName(), newDept.getName());
            }

            if (parentChanged) {
                log.info("  - 父部门ID: {} -> {}",
                        current.getParentId(), newDept.getParentId());
            }

            return true;
        }

        log.debug("部门[{}]无变化", current.getDeptId());
        return false;
    }

    /**
     * 执行部门全量同步
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentSyncResultDTO syncDepartments(List<DepartmentDTO> departments, LocalDate syncDate) {
        DepartmentSyncResultDTO result = new DepartmentSyncResultDTO()
                .setStartTime(java.time.LocalDateTime.now())
                .setTotalCount(departments.size());

        log.info("开始部门全量同步，总部门数: {}, 同步日期: {}", departments.size(), syncDate);

        try {
            if (CollectionUtils.isEmpty(departments)) {
                log.warn("部门列表为空，跳过同步");
                result.setEndTime(java.time.LocalDateTime.now());
                return result;
            }

            // 遍历所有部门进行同步
            for (DepartmentDTO dept : departments) {
                try {
                    processDepartment(dept, syncDate, result);
                } catch (Exception e) {
                    log.error("处理部门[{}]时发生异常: {}", dept.getDeptId(), e.getMessage(), e);
                    result.setFailedCount(result.getFailedCount() + 1);
                }
            }

            // 处理已删除的部门（在钉钉中不存在的部门）
            List<Long> activeDeptIds = departments.stream()
                    .map(DepartmentDTO::getDeptId)
                    .collect(Collectors.toList());

            int deletedCount = handleDeletedDepartments(activeDeptIds, syncDate.minusDays(1));
            result.setChangedCount(result.getChangedCount() + deletedCount);

            log.info("部门同步完成 - 总数: {}, 新增: {}, 变更: {}, 未变化: {}, 删除: {}, 失败: {}",
                    result.getTotalCount(),
                    result.getNewCount(),
                    result.getChangedCount(),
                    result.getUnchangedCount(),
                    deletedCount,
                    result.getFailedCount());

        } catch (Exception e) {
            log.error("部门同步过程中发生异常", e);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(java.time.LocalDateTime.now());
        }

        return result;
    }

    /**
     * 处理单个部门的同步逻辑
     */
    private void processDepartment(DepartmentDTO dept, LocalDate syncDate, DepartmentSyncResultDTO result) {
        // 查找当前版本
        DepartmentSCD2 current = findCurrentByDeptId(dept.getDeptId());

        if (current == null) {
            // 新增部门
            if (canCreateVersionOnDate(dept.getDeptId(), syncDate)) {
                insertNewVersion(dept, syncDate);
                result.setNewCount(result.getNewCount() + 1);
                log.info("新增部门: [{}] {}", dept.getDeptId(), dept.getName());
            } else {
                log.debug("部门[{}]在{}已有版本，跳过新增", dept.getDeptId(), syncDate);
                result.setUnchangedCount(result.getUnchangedCount() + 1);
            }
        } else if (hasChanged(current, dept)) {
            // 部门发生变化
            if (canCreateVersionOnDate(dept.getDeptId(), syncDate)) {
                createNewVersionForChangedDept(dept, syncDate);
                result.setChangedCount(result.getChangedCount() + 1);
            } else {
                log.debug("部门[{}]在{}已有版本，跳过变更", dept.getDeptId(), syncDate);
                result.setUnchangedCount(result.getUnchangedCount() + 1);
            }
        } else {
            // 未变化
            result.setUnchangedCount(result.getUnchangedCount() + 1);
            log.debug("部门[{}]无变化，跳过", dept.getDeptId());
        }
    }

    /**
     * 处理被删除的部门
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleDeletedDepartments(List<Long> activeDeptIds, LocalDate closeDate) {
        // 查找所有当前版本部门
        List<DepartmentSCD2> allCurrent = findAllCurrent();

        // 找出已删除的部门（在当前版本中但不在钉钉返回列表中的部门）
        List<Long> deletedDeptIds = allCurrent.stream()
                .map(DepartmentSCD2::getDeptId)
                .filter(deptId -> !activeDeptIds.contains(deptId))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(deletedDeptIds)) {
            log.info("发现{}个已删除的部门: {}", deletedDeptIds.size(), deletedDeptIds);
            int updated = baseMapper.batchCloseOldVersions(deletedDeptIds, closeDate);
            log.info("成功关闭{}个已删除部门的当前版本", updated);
            return updated;
        }

        return 0;
    }

    /**
     * 检查是否可以在指定日期创建新版本
     */
    @Override
    public boolean canCreateVersionOnDate(Long deptId, LocalDate effectiveDate) {
        return baseMapper.countVersionOnDate(deptId, effectiveDate) == 0;
    }

    /**
     * 获取部门的所有历史版本
     */
    @Override
    public List<DepartmentSCD2> findAllVersionsByDeptId(Long deptId) {
        return baseMapper.findAllVersionsByDeptId(deptId);
    }

    /**
     * 获取指定父部门下的所有当前子部门
     */
    @Override
    public List<DepartmentSCD2> findChildrenByParentId(Long parentId) {
        return baseMapper.findChildrenByParentId(parentId);
    }

    /**
     * 统计当前版本部门总数
     */
    @Override
    public Integer countCurrentVersions() {
        return baseMapper.countCurrentVersions();
    }
}