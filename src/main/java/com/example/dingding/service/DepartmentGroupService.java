package com.example.dingding.service;

import com.example.dingding.entity.DepartmentGroup;
import com.example.dingding.enums.DepartmentGroupType;

import java.util.List;
import java.util.Map;

/**
 * 部门统计维度服务接口
 * 提供部门统计分组的业务操作
 *
 * @author system
 * @version 1.0.0
 */
public interface DepartmentGroupService {

    /**
     * 生成部门统计数据
     * 从dim_department_jy表读取数据，按照层级规则整理并写入到dim_department_group_jy表
     *
     * @return 生成记录数
     */
    int generateDepartmentGroups();

    /**
     * 清理并重新生成数据
     * 先清空目标表，再重新生成数据
     *
     * @return 生成记录数
     */
    int truncateAndRegenerate();

    /**
     * 验证和检查数据
     * 检查生成的数据是否正确
     *
     * @return 验证结果
     */
    Map<String, Object> validateAndCheck();

    /**
     * 获取当前版本的所有部门分组
     *
     * @return 部门分组列表
     */
    List<DepartmentGroup> getCurrentGroups();

    /**
     * 根据类型获取部门分组
     *
     * @param groupType 分组类型
     * @return 部门分组列表
     */
    List<DepartmentGroup> getGroupsByType(DepartmentGroupType groupType);

    /**
     * 获取树形结构数据
     * 用于前端展示层级结构
     *
     * @return 树形结构数据
     */
    List<Map<String, Object>> getTreeStructure();

    /**
     * 根据部门ID查找对应的分组
     *
     * @param deptId 部门ID
     * @return 部门分组
     */
    DepartmentGroup findByDeptId(Long deptId);

    /**
     * 获取数据分布统计
     *
     * @return 统计结果
     */
    Map<String, Object> getDistributionStats();

    /**
     * 检查根节点是否存在
     *
     * @param rootDeptName 根部门名称
     * @return 是否存在
     */
    boolean checkRootNodeExists(String rootDeptName);

    /**
     * 刷新指定部门的统计分组
     * 只更新单个部门及其子部门的数据
     *
     * @param deptId 部门ID
     * @return 更新记录数
     */
    int refreshDepartmentGroup(Long deptId);
}