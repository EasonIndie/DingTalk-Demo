package com.example.dingding.service.impl;

import com.example.dingding.config.UnifiedDepartmentConfig;
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
    private UnifiedDepartmentConfig unifiedDepartmentConfig;

    private static final String ROOT_DEPT_NAME = "区域管理部";


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

        //处理总部数据
        int headquarterCount = syncHeadquarterDepartmentGroups();
        log.info("生成总部数据完成，共 {} 条", headquarterCount);

        int totalCount = regionCount + projectCount + headquarterCount;
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
     * 同步总部数据
     * 将配置的目标部门重新组织为总部的树形结构
     *
     * @return 生成的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncHeadquarterDepartmentGroups() {
        log.info("开始生成总部统计分组数据");

        // 1. 查找所有总部相关部门（包含所有子级）
        List<DepartmentSCD2> headquarterDepts = findHeadquarterDepartments();

        if (CollectionUtils.isEmpty(headquarterDepts)) {
            log.warn("未找到任何总部相关部门数据");
            return 0;
        }

        log.info("找到 {} 个总部相关部门", headquarterDepts.size());

        // 2. 构建树形结构
        List<DepartmentGroup> groupList = buildHeadquarterDepartmentTree(headquarterDepts);

        // 3. 批量插入数据
        int insertedCount = departmentGroupMapper.batchInsert(groupList);

        log.info("成功生成 {} 条总部统计数据", insertedCount);
        return insertedCount;
    }

    /**
     * 查找所有项目相关部门（包含所有子级）
     *
     * @return 部门列表
     */
    private List<DepartmentSCD2> findProjectDepartments() {
        // 从统一配置中获取项目部目标部门列表，避免硬编码
        List<String> targetDeptNames = unifiedDepartmentConfig.getProject().getTargetDepartments();

        if (CollectionUtils.isEmpty(targetDeptNames)) {
            log.error("项目部门配置为空，请检查配置文件");
            return Collections.emptyList();
        }

        log.info("目标部门列表：{}", targetDeptNames);

        // 1. 查找一级部门
        List<DepartmentSCD2> firstLevel = departmentSCD2Mapper.findByNameIn(targetDeptNames);
        log.info("找到 {} 个一级部门", firstLevel.size());

        // 打印一级部门信息
        firstLevel.forEach(dept ->
            log.info("  一级部门: {} (ID: {})", dept.getName(), dept.getDeptId()));

        // 2. 查找所有子部门（SQL已经使用递归CTE，不需要Java再递归）
        List<DepartmentSCD2> allDepts = new ArrayList<>(firstLevel);
        Set<Long> existingIds = new HashSet<>();

        // 先添加一级部门的ID到已存在集合
        firstLevel.forEach(dept -> existingIds.add(dept.getDeptId()));

        for (DepartmentSCD2 dept : firstLevel) {
            List<DepartmentSCD2> children = departmentSCD2Mapper.findAllChildren(dept.getDeptId());
            log.info("部门 {} ({}) 有 {} 个子部门", dept.getName(), dept.getDeptId(), children.size());

            for (DepartmentSCD2 child : children) {
                // 跳过已存在的部门（防止重复）
                if (!existingIds.contains(child.getDeptId())) {
                    allDepts.add(child);
                    existingIds.add(child.getDeptId());
                    log.debug("添加子部门: {} (ID: {}, 父ID: {})",
                        child.getName(), child.getDeptId(), child.getParentId());
                } else {
                    log.warn("部门 {} (ID: {}) 已存在，跳过",
                        child.getName(), child.getDeptId());
                }
            }
        }

        log.info("总共找到 {} 个项目相关部门（包含子部门）", allDepts.size());

        // 打印所有收集到的部门，帮助调试
        log.info("收集到的部门列表：");
        allDepts.forEach(dept ->
            log.info("  ID: {}, 名称: {}, 父ID: {}",
                dept.getDeptId(), dept.getName(), dept.getParentId()));

        return allDepts;
    }

    /**
     * 查找所有总部相关部门（包含所有子级）
     *
     * @return 部门列表
     */
    private List<DepartmentSCD2> findHeadquarterDepartments() {
        // 从统一配置中获取总部目标部门列表
        List<String> targetDeptNames = unifiedDepartmentConfig.getHeadquarter().getTargetDepartments();

        if (CollectionUtils.isEmpty(targetDeptNames)) {
            log.error("总部部门配置为空，请检查配置文件");
            return Collections.emptyList();
        }

        log.info("总部目标部门列表：{}", targetDeptNames);

        // 1. 查找一级部门
        List<DepartmentSCD2> firstLevel = departmentSCD2Mapper.findByNameIn(targetDeptNames);
        log.info("找到 {} 个总部一级部门", firstLevel.size());

        // 打印一级部门信息
        firstLevel.forEach(dept ->
            log.info("  一级部门: {} (ID: {})", dept.getName(), dept.getDeptId()));

        // 2. 查找所有子部门（重要：SQL已经使用递归CTE，返回所有层级子部门）
        List<DepartmentSCD2> allDepts = new ArrayList<>(firstLevel);
        Set<Long> existingIds = new HashSet<>();

        // 先添加一级部门的ID到已存在集合
        firstLevel.forEach(dept -> existingIds.add(dept.getDeptId()));

        for (DepartmentSCD2 dept : firstLevel) {
            // SQL查询已通过递归CTE返回所有层级的子部门
            List<DepartmentSCD2> children = departmentSCD2Mapper.findAllChildren(dept.getDeptId());
            log.info("部门 {} ({}) 有 {} 个子部门", dept.getName(), dept.getDeptId(), children.size());

            for (DepartmentSCD2 child : children) {
                // 使用existingIds去重，避免重复添加
                if (!existingIds.contains(child.getDeptId())) {
                    allDepts.add(child);
                    existingIds.add(child.getDeptId());
                    log.debug("添加子部门: {} (ID: {}, 父ID: {})",
                        child.getName(), child.getDeptId(), child.getParentId());
                } else {
                    log.warn("部门 {} (ID: {}) 已存在，跳过",
                        child.getName(), child.getDeptId());
                }
            }
        }

        log.info("总共找到 {} 个总部相关部门（包含子部门）", allDepts.size());

        // 打印所有收集到的部门，帮助调试
        log.info("收集到的部门列表：");
        allDepts.forEach(dept ->
            log.info("  ID: {}, 名称: {}, 父ID: {}",
                dept.getDeptId(), dept.getName(), dept.getParentId()));

        return allDepts;
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
        // 注意：使用配置中的虚拟ID，避免硬编码
        Long projectVirtualId = unifiedDepartmentConfig.getProject().getVirtualId();
        DepartmentGroup projectRoot = new DepartmentGroup();
        projectRoot.setGroupId(projectVirtualId);
        projectRoot.setDeptId(projectVirtualId);
        projectRoot.setGroupName(unifiedDepartmentConfig.getProject().getGroupName());
        projectRoot.setGroupType(DepartmentGroupType.REGION);
        projectRoot.setParentGroupId(null);  // REGION的父节点为null
        projectRoot.setCurrentVersion(true);
        projectRoot.setValidFrom(LocalDate.now());
        projectRoot.setValidTo(LocalDate.of(9999, 12, 31));
        result.add(projectRoot);

        // 2. 构建部门映射
        log.info("开始构建部门映射，共 {} 个部门", departments.size());
        Map<Long, DepartmentSCD2> deptMap;
        try {
            deptMap = departments.stream()
                    .collect(Collectors.toMap(DepartmentSCD2::getDeptId, d -> d));
            log.info("部门映射构建成功，共 {} 个唯一部门", deptMap.size());
        } catch (IllegalStateException e) {
            log.error("构建部门映射时出现重复键异常！", e);

            // 找出重复的部门
            Map<Long, Integer> countMap = new HashMap<>();
            Set<Long> duplicateIds = new HashSet<>();

            for (DepartmentSCD2 dept : departments) {
                long count = countMap.merge(dept.getDeptId(), 1, Integer::sum);
                if (count > 1) {
                    duplicateIds.add(dept.getDeptId());
                }
            }

            log.error("发现 {} 个重复的部门ID", duplicateIds.size());

            // 对每个重复的部门ID，打印详细信息
            for (Long duplicateId : duplicateIds) {
                log.error("部门ID {} 重复出现:", duplicateId);
                departments.stream()
                    .filter(d -> d.getDeptId().equals(duplicateId))
                    .forEach(d -> {
                        log.error("  - ID: {}, 名称: {}, 父ID: {}",
                            d.getDeptId(), d.getName(), d.getParentId());
                    });
            }

            throw e;
        }

        // 3. 从统一配置中获取项目部目标部门列表（避免硬编码）
        List<String> targetDeptNames = unifiedDepartmentConfig.getProject().getTargetDepartments();
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
            group.setParentGroupId(projectVirtualId);  // 父节点指向项目部（使用配置的虚拟ID）
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
     * 构建总部的树形结构
     *
     * @param departments 部门列表
     * @return 部门分组列表
     */
    private List<DepartmentGroup> buildHeadquarterDepartmentTree(List<DepartmentSCD2> departments) {
        List<DepartmentGroup> result = new ArrayList<>();

        // 1. 创建总部根节点（REGION类型，parent_group_id=null）
        // 使用配置中的虚拟ID，确保不与其他节点冲突
        Long headquarterVirtualId = unifiedDepartmentConfig.getHeadquarter().getVirtualId();
        DepartmentGroup headquarterRoot = new DepartmentGroup();
        headquarterRoot.setGroupId(headquarterVirtualId);
        headquarterRoot.setDeptId(headquarterVirtualId);
        headquarterRoot.setGroupName(unifiedDepartmentConfig.getHeadquarter().getGroupName());
        headquarterRoot.setGroupType(DepartmentGroupType.REGION);
        headquarterRoot.setParentGroupId(null);  // REGION的父节点为null
        headquarterRoot.setCurrentVersion(true);
        headquarterRoot.setValidFrom(LocalDate.now());
        headquarterRoot.setValidTo(LocalDate.of(9999, 12, 31));
        result.add(headquarterRoot);

        // 2. 构建部门映射（重要：使用LinkedHashMap避免重复键异常）
        Map<Long, DepartmentSCD2> deptMap;
        try {
            // 使用LinkedHashMap保持顺序，同时避免重复键
            deptMap = departments.stream()
                    .collect(Collectors.toMap(
                        DepartmentSCD2::getDeptId,
                        d -> d,
                        (existing, replacement) -> existing,  // 遇到重复保留现有的
                        LinkedHashMap::new
                    ));
        } catch (Exception e) {
            log.error("构建部门映射时出现异常", e);
            // 降级处理：手动去重
            deptMap = new LinkedHashMap<>();
            for (DepartmentSCD2 dept : departments) {
                if (!deptMap.containsKey(dept.getDeptId())) {
                    deptMap.put(dept.getDeptId(), dept);
                }
            }
        }

        log.info("开始构建总部部门映射，共 {} 个部门", departments.size());

        // 3. 从统一配置中获取总部目标部门列表
        List<String> targetDeptNames = unifiedDepartmentConfig.getHeadquarter().getTargetDepartments();
        List<DepartmentSCD2> firstLevel = departments.stream()
                .filter(d -> targetDeptNames.contains(d.getName()))
                .collect(Collectors.toList());

        // 4. 处理一级部门（DEPARTMENT类型，parent为总部）
        for (DepartmentSCD2 dept : firstLevel) {
            DepartmentGroup group = new DepartmentGroup();
            group.setGroupId(dept.getDeptId());      // 使用真实的dept_id
            group.setDeptId(dept.getDeptId());       // 使用真实的dept_id
            group.setGroupName(dept.getName());
            group.setGroupType(DepartmentGroupType.DEPARTMENT);
            group.setParentGroupId(headquarterVirtualId);  // 父节点指向总部（使用配置的虚拟ID）
            group.setCurrentVersion(true);
            group.setValidFrom(LocalDate.now());
            group.setValidTo(LocalDate.of(9999, 12, 31));
            result.add(group);

            // 5. 递归处理子部门（group_type=null，层级不限）
            buildHeadquarterChildGroups(dept.getDeptId(), deptMap, result);
        }

        return result;
    }

    /**
     * 递归构建总部子部门树
     *
     * @param parentId 父部门ID
     * @param deptMap 所有部门映射
     * @param result 结果列表
     */
    private void buildHeadquarterChildGroups(Long parentId, Map<Long, DepartmentSCD2> deptMap,
                                            List<DepartmentGroup> result) {
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
                buildHeadquarterChildGroups(dept.getDeptId(), deptMap, result);
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

    /**
     * 查找部门在查找路径中的位置
     * 返回格式：目标部门名称 > ... > 当前部门名称
     *
     * @param departments 所有部门列表
     * @param deptId 要查找的部门ID
     * @return 部门的查找路径
     */
    private String findParentPath(List<DepartmentSCD2> departments, Long deptId) {
        // 查找所有项目目标部门作为可能的起点
        List<String> targetDeptNames = unifiedDepartmentConfig.getProject().getTargetDepartments();

        // 创建部门ID到部门的映射
        Map<Long, DepartmentSCD2> deptMap = departments.stream()
            .collect(Collectors.toMap(DepartmentSCD2::getDeptId, d -> d));

        List<String> path = new ArrayList<>();
        Long currentId = deptId;

        // 向上追溯，直到找到目标部门或根节点
        while (currentId != null) {
            DepartmentSCD2 currentDept = deptMap.get(currentId);
            if (currentDept == null) {
                break;
            }

            path.add(0, currentDept.getName());

            // 如果是目标部门，停止查找
            if (targetDeptNames.contains(currentDept.getName())) {
                break;
            }

            currentId = currentDept.getParentId();
        }

        return String.join(" > ", path);
    }
}