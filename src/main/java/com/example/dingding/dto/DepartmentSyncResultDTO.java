package com.example.dingding.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门同步结果DTO
 * 用于记录同步操作的结果统计
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Accessors(chain = true)
public class DepartmentSyncResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 同步开始时间
     */
    private LocalDateTime startTime;

    /**
     * 同步结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总部门数
     */
    private Integer totalCount = 0;

    /**
     * 新增部门数
     */
    private Integer newCount = 0;

    /**
     * 变更部门数
     */
    private Integer changedCount = 0;

    /**
     * 未变化部门数
     */
    private Integer unchangedCount = 0;

    /**
     * 失败部门数
     */
    private Integer failedCount = 0;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return failedCount == 0 && errorMessage == null;
    }

    /**
     * 是否有数据更新
     */
    public boolean hasChanges() {
        return newCount > 0 || changedCount > 0;
    }

    /**
     * 计算处理耗时（毫秒）
     */
    public Long getDuration() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
}