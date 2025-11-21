package com.example.dingding.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提案列表DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class ProposalListDTO {

    /**
     * 当前页码
     */
    private Integer current;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 提案列表
     */
    private List<ProposalItem> records;

    @Data
    public static class ProposalItem {
        /**
         * 流程实例ID
         */
        private String processInstanceId;

        /**
         * 提案人
         */
        private String proposerName;

        /**
         * 提案人工号
         */
        private String proposerId;

        /**
         * 提案人部门
         */
        private String proposerDepartment;

        /**
         * 问题描述
         */
        private String problemDescription;

        /**
         * 解决方案
         */
        private String solution;

        /**
         * 提案标题
         */
        private String title;

        /**
         * 提案编号
         */
        private String businessId;

        /**
         * 状态
         * RUNNING-运行中
         * COMPLETED-已完成
         * TERMINATED-已终止
         * CANCELED-已取消
         */
        private String status;

        /**
         * 当前节点
         */
        private String currentNode;

        /**
         * 等级
         */
        private String level;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        /**
         * 完成时间
         */
        private LocalDateTime finishTime;

        /**
         * 关闭日期
         */
        private LocalDateTime closeDate;

        /**
         * 点赞量
         */
        private Integer likeCount;

        /**
         * 评论
         */
        private String comments;

        /**
         * 预计经济效益
         */
        private Double expectedBenefit;

        /**
         * 提案类别
         */
        private String category;

        /**
         * 改善等级
         */
        private String improvementLevel;

        /**
         * 发起时间
         */
        private LocalDateTime submitTime;

        /**
         * 院区
         */
        private String area;

        /**
         * 区域
         */
        private String region;
    }
}