package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表单组件值实体类
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ding_form_component_values")
public class FormComponentValue {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程实例ID
     */
    @TableField("process_instance_id")
    private String processInstanceId;

    /**
     * 组件唯一ID
     */
    @TableField("component_id")
    private String componentId;

    /**
     * 组件显示名称
     */
    @TableField("component_name")
    private String componentName;

    /**
     * 组件类型(TextField/DDSelectField/DDDateField等)
     */
    @TableField("component_type")
    private String componentType;

    /**
     * 组件值
     */
    @TableField("value")
    private String value;

    /**
     * 扩展信息(JSON格式)
     */
    @TableField("ext_value")
    private String extValue;

    /**
     * 业务别名
     */
    @TableField("biz_alias")
    private String bizAlias;

    /**
     * 是否关键统计字段
     */
    @TableField("is_key_field")
    private Boolean isKeyField;

    /**
     * 字段分类(基础信息/项目详情/完成情况等)
     */
    @TableField("field_category")
    private String fieldCategory;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}