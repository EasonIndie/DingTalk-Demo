package com.example.dingding.service.impl;

import com.example.dingding.entity.DepartmentGroup;
import com.example.dingding.enums.DepartmentGroupType;
import com.example.dingding.mapper.DepartmentGroupMapper;
import com.example.dingding.mapper.DepartmentSCD2Mapper;
import com.example.dingding.service.DepartmentGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门统计维度服务实现类
 * 核心业务逻辑：将部门层级数据按照规则整理成统计维度数据
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class DepartmentGroupServiceImpl implements DepartmentGroupService {

    @Autowired
    private DepartmentGroupMapper departmentGroupMapper;

    @Autowired
    private DepartmentSCD2Mapper departmentSCD2Mapper;

    private static final String ROOT_DEPT_NAME = "区域管理部";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateDepartmentGroups() {
        log.info("开始生成部门统计数据，根部门：{}", ROOT_DEPT_NAME);

        // 检查根节点是否存在
        if (!checkRootNodeExists(ROOT_DEPT_NAME)) {
            log.error("根节点 '{}' 不存在或不是当前版本", ROOT_DEPT_NAME);
            throw new RuntimeException("根节点 '" + ROOT_DEPT_NAME + "' 不存在或不是当前版本");
        }

        // 执行递归查询
        List<DepartmentGroup> groups = departmentGroupMapper.selectDepartmentHierarchy(ROOT_DEPT_NAME);

        if (CollectionUtils.isEmpty(groups)) {
            log.warn("未找到任何部门数据");
            return 0;
        }

        // 批量插入数据
        int insertedCount = departmentGroupMapper.batchInsert(groups);

        log.info("成功生成 {} 条部门统计数据", insertedCount);
        return insertedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int truncateAndRegenerate() {
        log.info("清理并重新生成部门统计数据");

        // 先清理数据
        departmentGroupMapper.truncateTable();
        log.info("已清理原有数据");

        // 重新生成
        return generateDepartmentGroups();
    }

    @Override
    public Map<String, Object> validateAndCheck() {
        log.info("开始验证部门统计数据");

        Map<String, Object> validationResult = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. 检查根节点
        if (!checkRootNodeExists(ROOT_DEPT_NAME)) {
            errors.add("根节点 '" + ROOT_DEPT_NAME + "' 不存在");
        } else {
            log.info("根节点检查通过");
        }

        // 2. 获取数据分布
        List<Map<String, Object>> distributionStats = departmentGroupMapper.selectDistributionStats();
        validationResult.put("distribution", distributionStats);

        // 统计各类别数量
        Map<String, Integer> typeCount = new HashMap<>();
        int totalCount = 0;
        for (Map<String, Object> stat : distributionStats) {
            String type = (String) stat.get("group_type");
            Long count = (Long) stat.get("count");
            typeCount.put(type == null ? "NULL" : type, count.intValue());
            totalCount += count.intValue();
        }

        // 验证数据分布合理性
        if (typeCount.get("REGION") == null || typeCount.get("REGION") == 0) {
            warnings.add("没有找到REGION类型的数据");
        }

        validationResult.put("typeCount", typeCount);
        validationResult.put("totalCount", totalCount);

        // 3. 验证层级关系
        List<Map<String, Object>> hierarchyValidation = departmentGroupMapper.selectHierarchyValidation();
        validationResult.put("hierarchy", hierarchyValidation);

        // 检查REGION节点的parent_group_id是否为NULL
        long regionWithParentCount = hierarchyValidation.stream()
            .filter(m -> "REGION".equals(m.get("group_type")) && m.get("parent_group_name") != null)
            .count();

        if (regionWithParentCount > 0) {
            errors.add("发现 " + regionWithParentCount + " 个REGION节点的parent_group_id不为NULL");
        }

        // 4. 数据完整性检查
        int sourceCount = departmentSCD2Mapper.countCurrentVersions();
        int targetCount = totalCount;
        validationResult.put("sourceCount", sourceCount);
        validationResult.put("targetCount", targetCount);

        // 注意：targetCount应该小于sourceCount，因为不包括根节点
        if (targetCount >= sourceCount) {
            warnings.add("目标表记录数大于等于源表，可能包含重复数据");
        }

        // 5. 获取树形结构
        List<Map<String, Object>> treeStructure = departmentGroupMapper.selectTreeStructure();
        validationResult.put("treeStructure", treeStructure);

        // 汇总验证结果
        validationResult.put("errors", errors);
        validationResult.put("warnings", warnings);
        validationResult.put("success", errors.isEmpty());

        if (errors.isEmpty()) {
            log.info("数据验证通过，共 {} 条记录", totalCount);
        } else {
            log.error("数据验证失败，错误数：{}，警告数：{}", errors.size(), warnings.size());
        }

        return validationResult;
    }

    @Override
    public List<DepartmentGroup> getCurrentGroups() {
        return departmentGroupMapper.findAllCurrent();
    }

    @Override
    public List<DepartmentGroup> getGroupsByType(DepartmentGroupType groupType) {
        String typeValue = groupType != null ? groupType.getCode() : null;
        return departmentGroupMapper.selectByGroupType(typeValue);
    }

    @Override
    public List<Map<String, Object>> getTreeStructure() {
        // 获取扁平化的数据
        List<Map<String, Object>> flatList = departmentGroupMapper.selectTreeStructure();

        // 构建树形结构
        return buildTree(flatList);
    }

    /**
     * 将扁平化的数据构建成树形结构
     * 简化版本：因为group_id和dept_id相同，parent_group_id直接就是父节点的ID
     *
     * @param flatList 扁平化的数据列表
     * @return 树形结构数据
     */
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> flatList) {
        // 构建节点映射：groupId -> node
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();
        List<Map<String, Object>> rootNodes = new ArrayList<>();

        // 第一遍：创建所有节点
        for (Map<String, Object> item : flatList) {
            Map<String, Object> node = new HashMap<>();

            // 基本信息
            Long groupId = ((Number) item.get("group_id")).longValue();
            Long deptId = ((Number) item.get("dept_id")).longValue();
            String groupName = (String) item.get("group_name");
            String groupType = (String) item.get("group_type");
            Long parentGroupId = item.get("parent_group_id") != null ?
                ((Number) item.get("parent_group_id")).longValue() : null;

            // 构建节点
            node.put("groupId", groupId);
            node.put("deptId", deptId);
            node.put("groupName", groupName);
            node.put("groupType", groupType);
            node.put("parentGroupId", parentGroupId);

            // 添加额外属性
            node.put("isRoot", parentGroupId == null);
            node.put("label", groupName);
            node.put("value", groupId);

            // 子节点列表初始化
            node.put("children", new ArrayList<Map<String, Object>>());

            // 建立映射关系
            nodeMap.put(groupId, node);
        }

        // 第二遍：建立父子关系
        // 简化：因为group_id和dept_id相同，parent_group_id直接就是父节点的ID
        for (Map<String, Object> node : nodeMap.values()) {
            Long parentId = (Long) node.get("parentGroupId");

            if (parentId == null) {
                // 根节点
                rootNodes.add(node);
            } else {
                // 直接通过parent_group_id找到父节点
                Map<String, Object> parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children =
                        (List<Map<String, Object>>) parentNode.get("children");
                    children.add(node);
                } else {
                    // 如果找不到父节点，作为根节点处理
                    rootNodes.add(node);
                }
            }
        }

        // 对每个节点的子节点进行排序
        sortTreeNodes(rootNodes);

        return rootNodes;
    }

    /**
     * 递归排序树节点
     *
     * @param nodes 节点列表
     */
    private void sortTreeNodes(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 按type和name排序
        nodes.sort((a, b) -> {
            String typeA = (String) a.get("groupType");
            String typeB = (String) b.get("groupType");
            String nameA = (String) a.get("groupName");
            String nameB = (String) b.get("groupName");

            // REGION -> DEPARTMENT -> NULL
            int typeOrderA = getTypeOrder(typeA);
            int typeOrderB = getTypeOrder(typeB);

            if (typeOrderA != typeOrderB) {
                return Integer.compare(typeOrderA, typeOrderB);
            }

            return nameA.compareTo(nameB);
        });

        // 递归排序子节点
        for (Map<String, Object> node : nodes) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children =
                (List<Map<String, Object>>) node.get("children");
            sortTreeNodes(children);
        }
    }

    /**
     * 获取类型的排序值
     *
     * @param type 类型
     * @return 排序值
     */
    private int getTypeOrder(String type) {
        if ("REGION".equals(type)) {
            return 1;
        } else if ("DEPARTMENT".equals(type)) {
            return 2;
        } else {
            return 3; // NULL or others
        }
    }

    @Override
    public DepartmentGroup findByDeptId(Long deptId) {
        return departmentGroupMapper.findCurrentByDeptId(deptId);
    }

    @Override
    public Map<String, Object> getDistributionStats() {
        List<Map<String, Object>> distribution = departmentGroupMapper.selectDistributionStats();

        Map<String, Object> result = new HashMap<>();
        result.put("distribution", distribution);

        // 按类型统计
        Map<String, Long> typeStats = distribution.stream()
            .collect(Collectors.toMap(
                m -> (String) m.get("group_type"),
                m -> (Long) m.get("count")
            ));

        result.put("typeStats", typeStats);
        result.put("totalCount", typeStats.values().stream().mapToLong(Long::longValue).sum());

        return result;
    }

    @Override
    public boolean checkRootNodeExists(String rootDeptName) {
        return departmentGroupMapper.checkRootNodeExists(rootDeptName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshDepartmentGroup(Long deptId) {
        log.info("刷新部门 {} 的统计分组数据", deptId);

        // 这里可以实现增量更新逻辑
        // 当前实现是先清理再生成，后续可以优化为只更新受影响的分支

        // 1. 删除该部门及其所有子部门的数据
        // 2. 重新生成这些部门的数据

        log.info("当前使用全量刷新策略");
        return truncateAndRegenerate();
    }
}