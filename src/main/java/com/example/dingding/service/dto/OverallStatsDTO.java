package com.example.dingding.service.dto;

import lombok.Data;

/**
 * 全员统计DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class OverallStatsDTO {

    /**
     * 本月合理化建议提案数
     */
    private Long monthlyProposalCount;

    /**
     * 参与提案人数
     */
    private Long participantCount;

    /**
     * 结案提案数
     */
    private Long completedProposalCount;

    /**
     * 结案率
     */
    private Double completionRate;

    /**
     * 平均处理天数
     */
    private Double averageProcessDays;

    /**
     * 产生经济效益
     */
    private Double totalEconomicBenefit;

    /**
     * 节约金额
     */
    private Double totalSavings;

    /**
     * 本月时间范围
     */
    private String monthRange;

    /**
     * 统计时间
     */
    private String statisticsTime;
}