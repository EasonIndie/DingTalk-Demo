package com.example.dingding.controller;

import com.example.dingding.service.DingTalkOAService;
import com.example.dingding.service.StatisticsService;
import com.example.dingding.dto.DashboardStatsDTO;
import com.example.dingding.dto.ProposalListDTO;
import com.example.dingding.dto.ProposalQueryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 测试控制器
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private DingTalkOAService dingTalkOAService;

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 测试同步用户ID
     */
    @PostMapping("/sync-users")
    public String syncUsers() {
        try {
            int count = dingTalkOAService.syncUserIds();
            return "同步完成，共同步 " + count + " 个用户ID";
        } catch (Exception e) {
            log.error("同步用户ID失败", e);
            return "同步失败: " + e.getMessage();
        }
    }

    /**
     * 测试同步表单数据
     */
    @PostMapping("/sync-forms")
    public String syncForms(@RequestParam(defaultValue = "30") int days) {
        try {
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            dingTalkOAService.syncOALSS(startTime);
            return "表单数据同步完成";
        } catch (Exception e) {
            log.error("同步表单数据失败", e);
            return "同步失败: " + e.getMessage();
        }
    }

    /**
     * 测试获取指标看板统计
     */
    @GetMapping("/dashboard-stats")
    public DashboardStatsDTO getDashboardStats(
            @RequestParam(defaultValue = "2025-11-01T00:00:00") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(defaultValue = "2025-11-30T23:59:59") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String region) {
        try {
            return statisticsService.getDashboardStats(startTime, endTime, area, region);
        } catch (Exception e) {
            log.error("获取指标看板统计失败", e);
            return new DashboardStatsDTO();
        }
    }

    /**
     * 测试获取提案列表
     */
    @GetMapping("/proposal-list")
    public ProposalListDTO getProposalList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String proposerName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        try {
            ProposalQueryDTO queryDTO = new ProposalQueryDTO();
            queryDTO.setCurrent(current);
            queryDTO.setSize(size);
            queryDTO.setProposerName(proposerName);
            queryDTO.setDepartment(department);
            queryDTO.setStatus(status);

            return statisticsService.getProposalList(queryDTO);
        } catch (Exception e) {
            log.error("获取提案列表失败", e);
            return new ProposalListDTO();
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "服务运行正常，当前时间: " + LocalDateTime.now();
    }
}