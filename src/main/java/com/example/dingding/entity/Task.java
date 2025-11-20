package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务实体类
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ding_tasks")
public class Task {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程实例ID
     */
    @TableField("process_instance_id")
    private String processInstanceId;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 节点ID
     */
    @TableField("activity_id")
    private String activityId;

    /**
     * 任务处理人
     */
    @TableField("user_id")
    private String userId;

    /**
     * 任务状态(RUNNING/TERMINATED/COMPLETED/CANCELED)
     */
    @TableField("status")
    private String status;

    /**
     * 任务结果(AGREE/REFUSE/REDIRECTED/NONE)
     */
    @TableField("result")
    private String result;

    /**
     * 任务创建时间戳
     */
    @TableField("create_timestamp")
    private Long createTimestamp;

    /**
     * 任务创建时间
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 任务结束时间戳
     */
    @TableField("finish_timestamp")
    private Long finishTimestamp;

    /**
     * 任务结束时间
     */
    @TableField("finish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /**
     * 移动端URL
     */
    @TableField("mobile_url")
    private String mobileUrl;

    /**
     * PC端URL
     */
    @TableField("pc_url")
    private String pcUrl;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}