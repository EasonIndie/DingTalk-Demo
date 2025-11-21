package com.example.dingding.dto;

import lombok.Data;
import java.util.List;

/**
 * 指标看板统计数据DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class DashboardStatsDTO {

    /**
     * 统计时间
     */
    private String statisticsTime;

    /**
     * 基础统计指标
     */
    private BasicStats basicStats;

    /**
     * 项目管理指标
     */
    private ProjectStats projectStats;

    /**
     * 排名统计
     */
    private RankingStats rankingStats;

    @Data
    public static class BasicStats {
        /**
         * 提案总数
         */
        private Long totalCount;

        /**
         * 平均提案数：提案总数 ÷ 提案的员工数
         */
        private Double averageCount;

        /**
         * 提案参与率：提案的员工数 ÷ 总员工数
         */
        private Double participationRate;

        /**
         * 提案通过率：通过数 ÷ 提案总数
         */
        private Double passRate;

        /**
         * 提案采纳率：采纳数 ÷ 提案通过数
         */
        private Double adoptionRate;

        /**
         * 优秀提案数：预留字段
         */
        private Long excellentCount;

        /**
         * 项目数：采纳的项目数
         */
        private Long projectCount;

        /**
         * 总员工数
         */
        private Long totalEmployees;

        /**
         * 提案员工数
         */
        private Long proposalEmployees;
    }

    @Data
    public static class ProjectStats {
        /**
         * 30天关闭率：结束采纳项目数 ÷ 项目总数（要求30天内结束）
         */
        private Double closeRate30Days;

        /**
         * 30天超期率
         */
        private Double overdueRate30Days;

        /**
         * 项目总数
         */
        private Long totalProjects;

        /**
         * 30天内关闭的项目数
         */
        private Long closedProjects30Days;

        /**
         * 超期30天的项目数
         */
        private Long overdueProjects30Days;
    }

    @Data
    public static class RankingStats {
        /**
         * 各院区提案总数排名
         */
        private List<AreaRanking> areaTotalRanking;

        /**
         * 各院区提案参与率排名
         */
        private List<AreaRanking> areaParticipationRanking;

        /**
         * 区域提案数排名
         */
        private List<RegionRanking> regionRanking;
    }

    @Data
    public static class AreaRanking {
        /**
         * 院区名称
         */
        private String areaName;

        /**
         * 提案总数或参与率
         */
        private Double value;

        /**
         * 排名
         */
        private Integer ranking;

        /**
         * 排名类型（total/participation）
         */
        private String rankingType;
    }

    @Data
    public static class RegionRanking {
        /**
         * 区域名称
         */
        private String regionName;

        /**
         * 提案数
         */
        private Long proposalCount;

        /**
         * 排名
         */
        private Integer ranking;
    }
}