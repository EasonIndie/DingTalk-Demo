package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据同步记录实体类
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ding_sync_records")
public class SyncRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 同步类型(INSTANCE_IDS/PROCESS_DETAILS)
     */
    @TableField("sync_type")
    private String syncType;

    /**
     * 同步开始时间
     */
    @TableField("sync_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncStartTime;

    /**
     * 同步结束时间
     */
    @TableField("sync_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncEndTime;

    /**
     * 同步状态(SUCCESS/FAILED/PARTIAL)
     */
    @TableField("sync_status")
    private String syncStatus;

    /**
     * 处理总数
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 成功数量
     */
    @TableField("success_count")
    private Integer successCount;

    /**
     * 失败数量
     */
    @TableField("failed_count")
    private Integer failedCount;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 额外数据
     */
    @TableField("extra_data")
    private String extraData;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}