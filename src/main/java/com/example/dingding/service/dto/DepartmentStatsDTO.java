package com.example.dingding.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 部门统计DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
public class DepartmentStatsDTO {

    /**
     * 部门统计列表
     */
    private List<DepartmentItem> departmentList;

    /**
     * 总改善项目数
     */
    private Long totalProjects;

    /**
     * 统计时间
     */
    private String statisticsTime;

    @Data
    public static class DepartmentItem {
        /**
         * 部门名称
         */
        private String departmentName;

        /**
         * 改善项目数
         */
        private Long projectCount;

        /**
         * A级项目数
         */
        private Long aLevelCount;

        /**
         * B级项目数
         */
        private Long bLevelCount;

        /**
         * C级项目数
         */
        private Long cLevelCount;

        /**
         * 实施中项目数
         */
        private Long inProgressCount;

        /**
         * 已完成项目数
         */
        private Long completedCount;

        /**
         * 结案率
         */
        private Double completionRate;

        /**
         * 人均件数
         */
        private Double averagePerPerson;

        /**
         * 部门参与人数
         */
        private Long participantCount;
    }
}