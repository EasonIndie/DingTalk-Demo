package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.dingding.enums.DepartmentGroupType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 部门统计维度表实体类
 * 用于保存区域和部门的统计分组信息
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dim_department_group_jy")
public class DepartmentGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 统计维度ID（主键），复用钉钉的dept_id
     */
    @TableId(value = "group_id", type = IdType.INPUT)
    private Long groupId;

    /**
     * 关联真实部门ID（钉钉dept_id）
     * 注意：group_id和dept_id存储相同的值
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 统计类型
     */
    @TableField("group_type")
    private DepartmentGroupType groupType;

    /**
     * 父统计节点ID
     */
    @TableField("parent_group_id")
    private Long parentGroupId;

    /**
     * 统计名称
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 简称
     */
    @TableField("short_name")
    private String shortName;

    private Integer num;

    /**
     * 版本生效日期
     */
    @TableField("valid_from")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;

    /**
     * 版本失效日期
     */
    @TableField("valid_to")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validTo;

    /**
     * 是否当前版本（1=是，0=否）
     */
    @TableField("is_current")
    private Boolean isCurrent;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 便利方法：检查是否为当前版本
     */
    public boolean isCurrentVersion() {
        return Boolean.TRUE.equals(isCurrent);
    }

    /**
     * 便利方法：设置当前版本
     */
    public void setCurrentVersion(boolean current) {
        this.isCurrent = current;
    }

    /**
     * 便利方法：创建新版本实例
     */
    public static DepartmentGroup createNewVersion(Long deptId, DepartmentGroupType groupType,
                                                   Long parentGroupId, String groupName, LocalDate effectiveDate) {
        DepartmentGroup group = new DepartmentGroup();
        group.setGroupId(deptId);  // group_id复用dept_id
        group.setDeptId(deptId);
        group.setGroupType(groupType);
        group.setParentGroupId(parentGroupId);
        group.setGroupName(groupName);
        group.setValidFrom(effectiveDate);
        // 使用一个足够远的未来日期表示永久有效
        group.setValidTo(LocalDate.of(9999, 12, 31));
        group.setCurrentVersion(true);
        return group;
    }

    /**
     * 便利方法：关闭当前版本
     */
    public void closeVersion(LocalDate closeDate) {
        this.setValidTo(closeDate);
        this.setCurrentVersion(false);
    }

    /**
     * 便利方法：检查是否为REGION类型
     */
    public boolean isRegion() {
        return DepartmentGroupType.REGION.equals(this.groupType);
    }

    /**
     * 便利方法：检查是否为DEPARTMENT类型
     */
    public boolean isDepartment() {
        return DepartmentGroupType.DEPARTMENT.equals(this.groupType);
    }

    /**
     * 便利方法：检查是否为根节点（parentGroupId为null）
     */
    public boolean isRoot() {
        return this.parentGroupId == null;
    }
}