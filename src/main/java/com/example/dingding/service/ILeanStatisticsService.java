package com.example.dingding.service;

import com.example.dingding.service.dto.DepartmentStatsDTO;
import com.example.dingding.service.dto.OverallStatsDTO;
import com.example.dingding.service.dto.PersonRankingDTO;

import java.time.LocalDateTime;

/**
 * 精益统计服务接口
 *
 * @author system
 * @version 1.0.0
 */
public interface ILeanStatisticsService {

    /**
     * 获取全员统计指标 (Sheet 1)
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 全员统计DTO
     */
    OverallStatsDTO getOverallStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取部门管理统计 (Sheet 2)
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 部门统计列表
     */
    DepartmentStatsDTO getDepartmentStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取个人统计排行 (Sheet 3)
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 个人排行列表
     */
    PersonRankingDTO getPersonRanking(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 生成Excel报表
     *
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param outputPath  输出路径
     * @return 是否成功
     */
    boolean generateExcelReport(LocalDateTime startTime, LocalDateTime endTime, String outputPath);
}