package com.example.dingding.controller;

import com.example.dingding.dto.DashboardStatsDTO;
import com.example.dingding.dto.ProposalListDTO;
import com.example.dingding.dto.ProposalQueryDTO;
import com.example.dingding.dto.ProposalListDTO.ProposalItem;
import com.example.dingding.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 统计控制器 - 合并指标看板和提案管理功能
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    // ==================== 指标看板相关接口 ====================

    /**
     * 获取指标看板统计数据
     *
     * @param startTime 开始时间 (格式: yyyy-MM-dd HH:mm:ss)
     * @param endTime   结束时间 (格式: yyyy-MM-dd HH:mm:ss)
     * @param area      院区 (可选)
     * @param region    区域 (可选)
     * @return 指标看板统计数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String region) {

        log.info("获取指标看板统计数据: {} - {}, 院区: {}, 区域: {}", startTime, endTime, area, region);

        try {
            // 设置默认时间范围
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            DashboardStatsDTO stats = statisticsService.getDashboardStats(startTime, endTime, area, region);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取指标看板统计数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== 提案管理相关接口 ====================d'd

    /**
     * 分页查询提案列表（POST方式）
     *
     * @param queryDTO 查询条件
     * @return 提案列表
     */
    @PostMapping("/proposals")
    public ResponseEntity<ProposalListDTO> getProposalList(@RequestBody ProposalQueryDTO queryDTO) {
        log.info("查询提案列表: {}", queryDTO);

        try {
            // 设置默认值
            if (queryDTO.getCurrent() == null || queryDTO.getCurrent() < 1) {
                queryDTO.setCurrent(1);
            }
            if (queryDTO.getSize() == null || queryDTO.getSize() < 1) {
                queryDTO.setSize(10);
            }
            if (queryDTO.getSize() > 100) {
                queryDTO.setSize(100); // 限制最大每页数量
            }

            ProposalListDTO result = statisticsService.getProposalList(queryDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询提案列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取提案列表（GET方式，简单查询）
     *
     * @param current       当前页码，默认1
     * @param size          每页大小，默认10
     * @param proposerName  提案人姓名
     * @param department    部门
     * @param status        状态
     * @param level         等级
     * @param area          院区
     * @param region        区域
     * @param keyword       关键字
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 提案列表
     */
    @GetMapping("/proposals")
    public ResponseEntity<ProposalListDTO> getProposalListByGet(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String proposerName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        log.info("GET查询提案列表: current={}, size={}, proposerName={}, department={}, status={}, area={}, region={}",
                current, size, proposerName, department, status, area, region);

        try {
            ProposalQueryDTO queryDTO = new ProposalQueryDTO();
            queryDTO.setCurrent(current);
            queryDTO.setSize(size);
            queryDTO.setProposerName(proposerName);
            queryDTO.setDepartment(department);
            queryDTO.setStatus(status);
            queryDTO.setLevel(level);
            queryDTO.setArea(area);
            queryDTO.setRegion(region);
            queryDTO.setKeyword(keyword);
            queryDTO.setStartTime(startTime);
            queryDTO.setEndTime(endTime);

            ProposalListDTO result = statisticsService.getProposalList(queryDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GET查询提案列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取提案详情
     *
     * @param processInstanceId 流程实例ID
     * @return 提案详情
     */
    @GetMapping("/proposals/{processInstanceId}")
    public ResponseEntity<ProposalItem> getProposalDetail(@PathVariable String processInstanceId) {
        log.info("获取提案详情: {}", processInstanceId);

        try {
            ProposalItem proposalItem = statisticsService.getProposalDetail(processInstanceId);
            if (proposalItem != null) {
                return ResponseEntity.ok(proposalItem);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取提案详情失败: {}", processInstanceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("统计服务运行正常，当前时间: " + LocalDateTime.now());
    }

    /**
     * API信息接口
     *
     * @return API信息
     */
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("钉钉精益数据统计API v1.0.0");
    }
}