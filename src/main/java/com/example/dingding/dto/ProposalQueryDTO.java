package com.example.dingding.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 提案查询参数DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class ProposalQueryDTO {

    /**
     * 当前页码，默认1
     */
    private Integer current = 1;

    /**
     * 每页大小，默认10
     */
    private Integer size = 10;

    /**
     * 提案人姓名
     */
    private String proposerName;

    /**
     * 提案人部门
     */
    private String department;

    /**
     * 提案状态
     * RUNNING-运行中
     * COMPLETED-已完成
     * TERMINATED-已终止
     * CANCELED-已取消
     */
    private String status;

    /**
     * 提案等级
     */
    private String level;

    /**
     * 提案类别
     */
    private String category;

    /**
     * 改善等级
     */
    private String improvementLevel;

    /**
     * 院区
     */
    private String area;

    /**
     * 区域
     */
    private String region;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 关键字搜索（问题描述、解决方案等）
     */
    private String keyword;

    /**
     * 排序字段
     * createTime-创建时间
     * finishTime-完成时间
     * expectedBenefit-预期效益
     */
    private String sortField = "createTime";

    /**
     * 排序方向
     * asc-升序
     * desc-降序
     */
    private String sortOrder = "desc";
}