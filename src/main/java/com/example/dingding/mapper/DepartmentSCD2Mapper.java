package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.DepartmentSCD2;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 部门维度表（SCD2）Mapper接口
 * 提供SCD2特定的数据库操作方法
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface DepartmentSCD2Mapper extends BaseMapper<DepartmentSCD2> {

    /**
     * 根据部门名称列表查找当前版本部门
     *
     * @param names 部门名称列表
     * @return 部门列表
     */
    default List<DepartmentSCD2> findByNameIn(@Param("names") List<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<DepartmentSCD2>()
                .in(DepartmentSCD2::getName, names)
                .eq(DepartmentSCD2::getIsCurrent, true)
                .orderByAsc(DepartmentSCD2::getDeptId));
    }

    /**
     * 递归查找指定部门的所有子部门（当前版本）
     *
     * @param parentId 父部门ID
     * @return 所有子部门列表
     */
    @Select("WITH RECURSIVE dept_tree AS (" +
            "    SELECT dept_id, name, parent_id, 1 AS level " +
            "    FROM dim_department_jy " +
            "    WHERE dept_id = #{parentId} AND is_current = 1 " +
            "    " +
            "    UNION ALL " +
            "    " +
            "    SELECT d.dept_id, d.name, d.parent_id, dt.level + 1 " +
            "    FROM dim_department_jy d " +
            "    INNER JOIN dept_tree dt ON d.parent_id = dt.dept_id " +
            "    WHERE d.is_current = 1 " +
            ") " +
            "SELECT d.* FROM dim_department_jy d " +
            "INNER JOIN dept_tree t ON d.dept_id = t.dept_id " +
            "WHERE d.dept_id != #{parentId} AND d.is_current = 1 " +
            "ORDER BY d.dept_id")
    List<DepartmentSCD2> findAllChildren(@Param("parentId") Long parentId);

    /**
     * 统计当前版本部门总数
     *
     * @return 当前版本部门总数
     */
    default Integer countCurrentVersions() {
        return Math.toIntExact(selectCount(new LambdaQueryWrapper<DepartmentSCD2>()
                .eq(DepartmentSCD2::getIsCurrent, true)));
    }

    /**
     * 查找部门的当前版本
     *
     * @param deptId 部门ID
     * @return 当前版本记录，如果不存在返回null
     */
    default DepartmentSCD2 findCurrentByDeptId(Long deptId) {
        return selectOne(new LambdaQueryWrapper<DepartmentSCD2>()
                .eq(DepartmentSCD2::getDeptId, deptId)
                .eq(DepartmentSCD2::getIsCurrent, true));
    }

    /**
     * 查找所有当前版本的部门
     *
     * @return 当前版本部门列表
     */
    default List<DepartmentSCD2> findAllCurrent() {
        return selectList(new LambdaQueryWrapper<DepartmentSCD2>()
                .eq(DepartmentSCD2::getIsCurrent, true)
                .orderByAsc(DepartmentSCD2::getDeptId));
    }

    /**
     * 关闭部门的旧版本
     * 将指定部门的当前版本的valid_to设置为指定日期，is_current设置为false
     *
     * @param deptId 部门ID
     * @param validTo 失效日期
     * @return 更新的记录数
     */
    @Update("UPDATE dim_department_jy " +
            "SET valid_to = #{validTo}, " +
            "    is_current = 0, " +
            "    updated_at = NOW() " +
            "WHERE dept_id = #{deptId} " +
            "  AND is_current = 1")
    int closeOldVersion(@Param("deptId") Long deptId, @Param("validTo") LocalDate validTo);

    /**
     * 批量关闭多个部门的旧版本
     *
     * @param deptIds 部门ID列表
     * @param validTo 失效日期
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE dim_department_jy " +
            "SET valid_to = #{validTo}, " +
            "    is_current = 0, " +
            "    updated_at = NOW() " +
            "WHERE dept_id IN " +
            "    <foreach collection='deptIds' item='id' open='(' separator=',' close=')'>" +
            "        #{id}" +
            "    </foreach>" +
            "  AND is_current = 1" +
            "</script>")
    int batchCloseOldVersions(@Param("deptIds") List<Long> deptIds, @Param("validTo") LocalDate validTo);

    /**
     * 检查部门在指定日期是否已有版本
     * 用于幂等性检查，避免同一天重复创建版本
     *
     * @param deptId 部门ID
     * @param effectiveDate 生效日期
     * @return 记录数
     */
    default int countVersionOnDate(Long deptId, LocalDate effectiveDate) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapper<DepartmentSCD2>()
                .eq(DepartmentSCD2::getDeptId, deptId)
                .eq(DepartmentSCD2::getValidFrom, effectiveDate)));
    }

    /**
     * 获取部门的所有历史版本
     *
     * @param deptId 部门ID
     * @return 历史版本列表（按生效日期降序）
     */
    default List<DepartmentSCD2> findAllVersionsByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapper<DepartmentSCD2>()
                .eq(DepartmentSCD2::getDeptId, deptId)
                .orderByDesc(DepartmentSCD2::getValidFrom));
    }

    /**
     * 获取指定父部门下的所有当前子部门
     *
     * @param parentId 父部门ID，null表示查询根部门
     * @return 子部门列表
     */
    default List<DepartmentSCD2> findChildrenByParentId(Long parentId) {
        LambdaQueryWrapper<DepartmentSCD2> wrapper = new LambdaQueryWrapper<DepartmentSCD2>()
                .eq(DepartmentSCD2::getIsCurrent, true);

        if (parentId == null) {
            wrapper.isNull(DepartmentSCD2::getParentId);
        } else {
            wrapper.eq(DepartmentSCD2::getParentId, parentId);
        }

        return selectList(wrapper.orderByAsc(DepartmentSCD2::getDeptId));
    }
}