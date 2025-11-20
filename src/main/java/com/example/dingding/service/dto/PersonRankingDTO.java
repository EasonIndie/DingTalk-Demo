package com.example.dingding.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 个人统计排行DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class PersonRankingDTO {

    /**
     * 个人排行列表
     */
    private List<PersonItem> personList;

    /**
     * 统计时间
     */
    private String statisticsTime;

    @Data
    public static class PersonItem {
        /**
         * 排名
         */
        private Integer ranking;

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 姓名
         */
        private String name;

        /**
         * 参与提案数
         */
        private Long proposalCount;

        /**
         * 采纳提案数
         */
        private Long adoptedCount;

        /**
         * 采纳率
         */
        private Double adoptionRate;

        /**
         * 产生经济效益
         */
        private Double economicBenefit;

        /**
         * 奖励积分
         */
        private Integer rewardPoints;

        /**
         * 部门
         */
        private String department;
    }
}