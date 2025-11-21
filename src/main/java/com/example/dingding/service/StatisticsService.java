package com.example.dingding.service;

import com.example.dingding.dto.DashboardStatsDTO;
import com.example.dingding.dto.ProposalListDTO;
import com.example.dingding.dto.ProposalQueryDTO;
import com.example.dingding.dto.ProposalListDTO.ProposalItem;

import java.time.LocalDateTime;

/**
 * 统计服务接口 - 合并指标看板和提案管理功能
 *
 * @author system
 * @version 1.0.0
 */
public interface StatisticsService {

    /**
     * 获取指标看板统计数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param area      院区（可选）
     * @param region    区域（可选）
     * @return 指标看板统计数据
     */
    DashboardStatsDTO getDashboardStats(LocalDateTime startTime, LocalDateTime endTime,
                                        String area, String region);

    /**
     * 分页查询提案列表
     *
     * @param queryDTO 查询条件
     * @return 提案列表
     */
    ProposalListDTO getProposalList(ProposalQueryDTO queryDTO);

    /**
     * 获取提案详情
     *
     * @param processInstanceId 流程实例ID
     * @return 提案详情
     */
    ProposalItem getProposalDetail(String processInstanceId);
}