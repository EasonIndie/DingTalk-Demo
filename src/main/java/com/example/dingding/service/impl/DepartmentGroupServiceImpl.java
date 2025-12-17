package com.example.dingding.service.impl;

import com.example.dingding.config.ProjectDepartmentConfig;
import com.example.dingding.entity.DepartmentGroup;
import com.example.dingding.entity.DepartmentSCD2;
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

    @Autowired
    private ProjectDepartmentConfig projectDepartmentConfig;

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

        // 处理区域管理部数据
        int regionCount = generateDepartmentGroups();
        log.info("生成区域管理部数据完成，共 {} 条", regionCount);

        // 处理项目部数据
        int projectCount = syncProjectDepartmentGroups();
        log.info("生成项目部数据完成，共 {} 条", projectCount);

        int totalCount = regionCount + projectCount;
        log.info("总共生成 {} 条部门统计数据", totalCount);

        return totalCount;
    }

    @Override
    public List<Map<String, Object>> getTreeStructure() {
        // 获取扁平化的数据
        List<Map<String, Object>> flatList = departmentGroupMapper.selectTreeStructure();

        // 构建树形结构
        return buildTree(flatList);
    }

    /**
     * 同步项目部数据
     * 将配置的目标部门重新组织为项目部的树形结构
     *
     * @return 生成的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncProjectDepartmentGroups() {
        log.info("开始生成项目部统计分组数据");

        // 1. 查找所有项目相关部门（包含所有子级）
        List<DepartmentSCD2> projectDepts = findProjectDepartments();

        if (CollectionUtils.isEmpty(projectDepts)) {
            log.warn("未找到任何项目相关部门数据");
            return 0;
        }

        log.info("找到 {} 个项目相关部门", projectDepts.size());

        // 2. 构建树形结构
        List<DepartmentGroup> groupList = buildProjectDepartmentTree(projectDepts);

        // 3. 批量插入数据
        int insertedCount = departmentGroupMapper.batchInsert(groupList);

        log.info("成功生成 {} 条项目部统计数据", insertedCount);
        return insertedCount;
    }

    /**
     * 查找所有项目相关部门（包含所有子级）
     *
     * @return 部门列表
     */
    private List<DepartmentSCD2> findProjectDepartments() {
        // 从配置中获取目标部门列表，避免硬编码
        List<String> targetDeptNames = projectDepartmentConfig.getTargetDepartments();

        if (CollectionUtils.isEmpty(targetDeptNames)) {
            log.error("项目部门配置为空，请检查配置文件");
            return Collections.emptyList();
        }

        log.info("目标部门列表：{}", targetDeptNames);

        // 1. 查找一级部门
        List<DepartmentSCD2> firstLevel = departmentSCD2Mapper.findByNameIn(targetDeptNames);
        log.info("找到 {} 个一级部门", firstLevel.size());

        // 2. 递归查找所有子部门
        List<DepartmentSCD2> allDepts = new ArrayList<>(firstLevel);
        Set<Long> visited = new HashSet<>();

        for (DepartmentSCD2 dept : firstLevel) {
            findAllChildren(dept.getDeptId(), allDepts, visited);
        }

        log.info("总共找到 {} 个项目相关部门（包含子部门）", allDepts.size());
        return allDepts;
    }

    /**
     * 递归查找所有子部门
     *
     * @param parentId 父部门ID
     * @param allDepts 所有部门列表
     * @param visited 已访问的部门ID（防止循环）
     */
    private void findAllChildren(Long parentId, List<DepartmentSCD2> allDepts, Set<Long> visited) {
        if (visited.contains(parentId)) {
            return;
        }
        visited.add(parentId);

        List<DepartmentSCD2> children = departmentSCD2Mapper.findAllChildren(parentId);

        for (DepartmentSCD2 child : children) {
            allDepts.add(child);
            findAllChildren(child.getDeptId(), allDepts, visited);
        }
    }

    /**
     * 构建项目部的树形结构
     *
     * @param departments 部门列表
     * @return 部门分组列表
     */
    private List<DepartmentGroup> buildProjectDepartmentTree(List<DepartmentSCD2> departments) {
        List<DepartmentGroup> result = new ArrayList<>();

        // 1. 创建项目部根节点（REGION类型，parent_group_id=null）
        // 注意：项目部使用虚拟ID -1，因为不是真实部门
        DepartmentGroup projectRoot = new DepartmentGroup();
        projectRoot.setGroupId(-1L);
        projectRoot.setDeptId(-1L);
        projectRoot.setGroupName(projectDepartmentConfig.getGroupName());
        projectRoot.setGroupType(DepartmentGroupType.REGION);
        projectRoot.setParentGroupId(null);  // REGION的父节点为null
        projectRoot.setCurrentVersion(true);
        projectRoot.setValidFrom(LocalDate.now());
        projectRoot.setValidTo(LocalDate.of(9999, 12, 31));
        result.add(projectRoot);

        // 2. 构建部门映射
        Map<Long, DepartmentSCD2> deptMap = departments.stream()
                .collect(Collectors.toMap(DepartmentSCD2::getDeptId, d -> d));

        // 3. 从配置中获取目标部门列表（避免硬编码）
        List<String> targetDeptNames = projectDepartmentConfig.getTargetDepartments();
        List<DepartmentSCD2> firstLevel = departments.stream()
                .filter(d -> targetDeptNames.contains(d.getName()))
                .collect(Collectors.toList());

        // 4. 处理一级部门（DEPARTMENT类型，parent为项目部）
        for (DepartmentSCD2 dept : firstLevel) {
            DepartmentGroup group = new DepartmentGroup();
            group.setGroupId(dept.getDeptId());      // 使用真实的dept_id
            group.setDeptId(dept.getDeptId());       // 使用真实的dept_id
            group.setGroupName(dept.getName());
            group.setGroupType(DepartmentGroupType.DEPARTMENT);
            group.setParentGroupId(-1L);             // 父节点指向项目部（虚拟ID -1）
            group.setCurrentVersion(true);
            group.setValidFrom(LocalDate.now());
            group.setValidTo(LocalDate.of(9999, 12, 31));
            result.add(group);

            // 5. 递归处理子部门（group_type=null，层级不限）
            // 从DEPARTMENT级别开始递归查找所有子级
            buildChildGroups(dept.getDeptId(), deptMap, result);
        }

        return result;
    }

    /**
     * 递归构建子部门树
     *
     * @param parentId 父部门ID
     * @param deptMap 所有部门映射
     * @param result 结果列表
     */
    private void buildChildGroups(Long parentId, Map<Long, DepartmentSCD2> deptMap,
                                List<DepartmentGroup> result) {
        // parentId是真实的父部门ID，从dim_department_jy表获取
        for (DepartmentSCD2 dept : deptMap.values()) {
            if (parentId.equals(dept.getParentId())) {
                // 子部门的group_type为null
                DepartmentGroup group = new DepartmentGroup();
                group.setGroupId(dept.getDeptId());  // 使用真实的dept_id
                group.setDeptId(dept.getDeptId());   // 使用真实的dept_id
                group.setGroupName(dept.getName());
                group.setGroupType(null);            // 子部门group_type为null
                group.setParentGroupId(dept.getParentId());  // 指向真实的父部门ID
                group.setCurrentVersion(true);
                group.setValidFrom(LocalDate.now());
                group.setValidTo(LocalDate.of(9999, 12, 31));
                result.add(group);

                // 继续递归处理下级
                buildChildGroups(dept.getDeptId(), deptMap, result);
            }
        }
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

    /**
     * 检查根节点是否存在
     *
     * @param rootDeptName 根部门名称
     * @return 是否存在
     */
    private boolean checkRootNodeExists(String rootDeptName) {
        return departmentGroupMapper.checkRootNodeExists(rootDeptName);
    }
}