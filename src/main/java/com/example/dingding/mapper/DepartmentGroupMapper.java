package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.DepartmentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 部门统计维度表Mapper接口
 * 提供部门统计分组的数据访问操作
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface DepartmentGroupMapper extends BaseMapper<DepartmentGroup> {

    /**
     * 清理表数据
     * 使用TRUNCATE TABLE快速清空所有数据
     *
     * @return 执行结果
     */
    int truncateTable();

    /**
     * 递归查询部门层级数据
     * 从区域管理部的子节点开始，递归查询所有子部门
     *
     * @param rootDeptName 根部门名称（如："区域管理部"）
     * @return 层级部门数据列表
     */
    List<DepartmentGroup> selectDepartmentHierarchy(@Param("rootDeptName") String rootDeptName);

    /**
     * 批量插入部门统计数据
     *
     * @param groups 部门统计分组列表
     * @return 插入的记录数
     */
    int batchInsert(@Param("groups") List<DepartmentGroup> groups);

    /**
     * 获取树形结构数据（扁平化）
     * Service层会将其构建成真正的树形结构
     *
     * @return 扁平化的层级数据
     */
    List<Map<String, Object>> selectTreeStructure();

    /**
     * 检查根节点是否存在
     *
     * @param rootDeptName 根部门名称
     * @return 是否存在
     */
    boolean checkRootNodeExists(@Param("rootDeptName") String rootDeptName);
}