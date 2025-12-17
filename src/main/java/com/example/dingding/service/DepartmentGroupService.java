package com.example.dingding.service;

import com.example.dingding.entity.DepartmentGroup;

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
     * 生成区域管理部统计数据
     * 从dim_department_jy表读取数据，按照层级规则整理并写入到dim_department_group_jy表
     *
     * @return 生成记录数
     */
    int generateDepartmentGroups();

    /**
     * 清理并重新生成数据
     * 先清空目标表，再重新生成数据（包含区域管理部和项目部）
     *
     * @return 生成记录数
     */
    int truncateAndRegenerate();

    /**
     * 获取树形结构数据
     * 用于前端展示层级结构
     *
     * @return 树形结构数据
     */
    List<Map<String, Object>> getTreeStructure();
}