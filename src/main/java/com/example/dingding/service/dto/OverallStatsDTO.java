package com.example.dingding.service.dto;

import lombok.Data;
import java.util.List;

/**
 * 全员统计DTO - 对应Excel Sheet1: 指标查看-全员
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class OverallStatsDTO {

    /**
     * 统计时间
     */
    private String statisticsTime;

    /**
     * 指标列表
     */
    private List<IndicatorItem> indicatorList;

    @Data
    public static class IndicatorItem {
        /**
         * 指标名称
         */
        private String indicatorName;

        /**
         * 数值
         */
        private String value;

        /**
         * 单位
         */
        private String unit;

        /**
         * 环比
         */
        private String monthOnMonth;

        /**
         * 目标值
         */
        private String targetValue;

        /**
         * 达标率
         */
        private String complianceRate;
    }
}