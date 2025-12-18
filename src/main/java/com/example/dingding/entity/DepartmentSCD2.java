package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 部门维度表（SCD2模式）实体类
 * 用于保存部门的历史版本信息
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dim_department_jy")
public class DepartmentSCD2 implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SCD2 替代键（主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 钉钉部门ID（业务键）
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 父部门ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 部门名称
     */
    @TableField("name")
    private String name;

    /**
     * 部门下的员工数量
     */
    @TableField("num")
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
    public static DepartmentSCD2 createNewVersion(Long deptId, Long parentId, String name, Integer num, LocalDate effectiveDate) {
        DepartmentSCD2 dept = new DepartmentSCD2();
        dept.setDeptId(deptId);
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setNum(num);
        dept.setValidFrom(effectiveDate);
        // 使用一个足够远的未来日期表示永久有效
        dept.setValidTo(LocalDate.of(9999, 12, 31));
        dept.setCurrentVersion(true);
        return dept;
    }

    /**
     * 便利方法：关闭当前版本
     */
    public void closeVersion(LocalDate closeDate) {
        this.setValidTo(closeDate);
        this.setCurrentVersion(false);
    }
}