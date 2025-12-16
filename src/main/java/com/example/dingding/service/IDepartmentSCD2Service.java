package com.example.dingding.service;

import com.example.dingding.dto.DepartmentDTO;
import com.example.dingding.dto.DepartmentSyncResultDTO;
import com.example.dingding.entity.DepartmentSCD2;

import java.time.LocalDate;
import java.util.List;

/**
 * 部门SCD2服务接口
 * 提供部门维度的SCD2管理功能
 *
 * @author system
 * @version 1.0.0
 */
public interface IDepartmentSCD2Service {

    /**
     * 查找部门的当前版本
     *
     * @param deptId 部门ID
     * @return 当前版本记录，如果不存在返回null
     */
    DepartmentSCD2 findCurrentByDeptId(Long deptId);

    /**
     * 获取所有当前版本的部门
     *
     * @return 当前版本部门列表
     */
    List<DepartmentSCD2> findAllCurrent();

    /**
     * 为新增部门创建新版本
     *
     * @param deptDto 部门信息
     * @param effectiveDate 生效日期
     * @return 创建的部门记录
     */
    DepartmentSCD2 insertNewVersion(DepartmentDTO deptDto, LocalDate effectiveDate);

    /**
     * 关闭部门的旧版本
     *
     * @param deptId 部门ID
     * @param closeDate 关闭日期（valid_to）
     * @return 是否成功关闭
     */
    boolean closeOldVersion(Long deptId, LocalDate closeDate);

    /**
     * 为变更的部门创建新版本
     * 1. 关闭当前版本
     * 2. 创建新版本
     *
     * @param deptDto 新的部门信息
     * @param effectiveDate 新版本生效日期
     * @return 创建的新版本记录
     */
    DepartmentSCD2 createNewVersionForChangedDept(DepartmentDTO deptDto, LocalDate effectiveDate);

    /**
     * 检测部门是否发生变化
     * 对比维度：dept_id（业务键）、name、parent_id
     *
     * @param current 当前数据库中的部门版本
     * @param newDept 从钉钉API获取的新部门数据
     * @return true 表示有变化，false 表示无变化
     */
    boolean hasChanged(DepartmentSCD2 current, DepartmentDTO newDept);

    /**
     * 执行部门全量同步
     * 实现SCD2变更检测和版本管理
     *
     * @param departments 从钉钉获取的所有部门列表
     * @param syncDate 同步日期
     * @return 同步结果
     */
    DepartmentSyncResultDTO syncDepartments(List<DepartmentDTO> departments, LocalDate syncDate);

    /**
     * 获取部门的所有历史版本
     *
     * @param deptId 部门ID
     * @return 历史版本列表（按生效日期降序）
     */
    List<DepartmentSCD2> findAllVersionsByDeptId(Long deptId);

    /**
     * 处理被删除的部门
     * 当部门在钉钉中不再存在时，关闭其在数据库中的当前版本
     *
     * @param activeDeptIds 钉钉中仍然存在的部门ID列表
     * @param closeDate 关闭日期
     * @return 关闭的部门数量
     */
    int handleDeletedDepartments(List<Long> activeDeptIds, LocalDate closeDate);

    /**
     * 检查是否可以在指定日期创建新版本
     * 用于幂等性检查，避免重复创建版本
     *
     * @param deptId 部门ID
     * @param effectiveDate 生效日期
     * @return true 表示可以创建，false 表示该日期已有版本
     */
    boolean canCreateVersionOnDate(Long deptId, LocalDate effectiveDate);

    /**
     * 获取指定父部门下的所有当前子部门
     *
     * @param parentId 父部门ID，null表示查询根部门
     * @return 子部门列表
     */
    List<DepartmentSCD2> findChildrenByParentId(Long parentId);

    /**
     * 统计当前版本部门总数
     *
     * @return 当前版本部门总数
     */
    Integer countCurrentVersions();
}