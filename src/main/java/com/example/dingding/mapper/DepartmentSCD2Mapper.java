package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.DepartmentSCD2;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
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
     * 查找部门的当前版本
     *
     * @param deptId 部门ID
     * @return 当前版本记录，如果不存在返回null
     */
    default DepartmentSCD2 findCurrentByDeptId(Long deptId) {
        DepartmentSCD2 query = new DepartmentSCD2();
        query.setDeptId(deptId);
        query.setIsCurrent(true);
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(query));
    }

    /**
     * 查找所有当前版本的部门
     *
     * @return 当前版本部门列表
     */
    default List<DepartmentSCD2> findAllCurrent() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DepartmentSCD2>()
                .eq("is_current", 1)
                .orderByAsc("dept_id"));
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
        return Math.toIntExact(selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DepartmentSCD2>()
                .eq("dept_id", deptId)
                .eq("valid_from", effectiveDate)));
    }

    /**
     * 获取部门的所有历史版本
     *
     * @param deptId 部门ID
     * @return 历史版本列表（按生效日期降序）
     */
    default List<DepartmentSCD2> findAllVersionsByDeptId(Long deptId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DepartmentSCD2>()
                .eq("dept_id", deptId)
                .orderByDesc("valid_from"));
    }

    /**
     * 获取指定父部门下的所有当前子部门
     *
     * @param parentId 父部门ID，null表示查询根部门
     * @return 子部门列表
     */
    default List<DepartmentSCD2> findChildrenByParentId(Long parentId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DepartmentSCD2> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DepartmentSCD2>()
                .eq("is_current", 1);

        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }

        return selectList(wrapper.orderByAsc("dept_id"));
    }

    /**
     * 统计当前版本部门总数
     *
     * @return 当前版本部门总数
     */
    default Integer countCurrentVersions() {
        return Math.toIntExact(selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DepartmentSCD2>()
                .eq("is_current", 1)));
    }
}