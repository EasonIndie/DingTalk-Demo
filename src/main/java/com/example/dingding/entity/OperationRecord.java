package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.dingding.handler.JsonListTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作记录实体类
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ding_operation_records")
public class OperationRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程实例ID
     */
    @TableField("process_instance_id")
    private String processInstanceId;

    /**
     * 活动ID
     */
    @TableField("activity_id")
    private String activityId;

    /**
     * 操作时间
     */
    @TableField("operation_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationDate;

    /**
     * 操作人ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 操作人显示名称
     */
    @TableField("show_name")
    private String showName;

    /**
     * 操作类型
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作结果(AGREE/REFUSE/NONE/REDIRECTED)
     */
    @TableField("result")
    private String result;

    /**
     * 评论内容
     */
    @TableField("remark")
    private String remark;

    /**
     * 操作图片
     */
    @TableField(value = "images", typeHandler = JsonListTypeHandler.class)
    private List<String> images;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}