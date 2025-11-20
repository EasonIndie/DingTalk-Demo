package com.example.dingding.controller;

import com.example.dingding.service.DingTalkOAService;
import com.example.dingding.service.ILeanStatisticsService;
import com.example.dingding.service.dto.OverallStatsDTO;
import com.example.dingding.service.dto.DepartmentStatsDTO;
import com.example.dingding.service.dto.PersonRankingDTO;
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
    private ILeanStatisticsService leanStatisticsService;

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
     * 测试获取全员统计
     */
    @GetMapping("/overall-stats")
    public OverallStatsDTO getOverallStats(
            @RequestParam(defaultValue = "2025-11-01T00:00:00") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(defaultValue = "2025-11-30T23:59:59") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return leanStatisticsService.getOverallStatistics(startTime, endTime);
        } catch (Exception e) {
            log.error("获取全员统计失败", e);
            return new OverallStatsDTO();
        }
    }

    /**
     * 测试获取部门统计
     */
    @GetMapping("/department-stats")
    public DepartmentStatsDTO getDepartmentStats(
            @RequestParam(defaultValue = "2025-11-01T00:00:00") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(defaultValue = "2025-11-30T23:59:59") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return leanStatisticsService.getDepartmentStatistics(startTime, endTime);
        } catch (Exception e) {
            log.error("获取部门统计失败", e);
            return new DepartmentStatsDTO();
        }
    }

    /**
     * 测试获取个人排行
     */
    @GetMapping("/person-ranking")
    public PersonRankingDTO getPersonRanking(
            @RequestParam(defaultValue = "2025-11-01T00:00:00") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(defaultValue = "2025-11-30T23:59:59") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return leanStatisticsService.getPersonRanking(startTime, endTime);
        } catch (Exception e) {
            log.error("获取个人排行失败", e);
            return new PersonRankingDTO();
        }
    }

    /**
     * 测试生成Excel报表
     */
    @PostMapping("/generate-excel")
    public String generateExcel(
            @RequestParam(defaultValue = "2025-11-01T00:00:00") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(defaultValue = "2025-11-30T23:59:59") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "精益数据统计报告.xlsx") String fileName) {
        try {
            String outputPath = System.getProperty("user.home") + "/" + fileName;
            boolean success = leanStatisticsService.generateExcelReport(startTime, endTime, outputPath);
            return success ? "Excel报表生成成功: " + outputPath : "Excel报表生成失败";
        } catch (Exception e) {
            log.error("生成Excel报表失败", e);
            return "生成失败: " + e.getMessage();
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