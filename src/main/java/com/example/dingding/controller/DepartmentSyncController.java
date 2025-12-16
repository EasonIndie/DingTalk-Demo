package com.example.dingding.controller;

import com.example.dingding.dto.DepartmentSyncResultDTO;
import com.example.dingding.dto.DepartmentDTO;
import com.example.dingding.entity.DepartmentSCD2;
import com.example.dingding.job.DepartmentSyncJob;
import com.example.dingding.service.IDepartmentSCD2Service;
import com.example.dingding.service.DingTalkOAService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门同步控制器
 * 提供部门同步相关的API接口
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/department-sync")
public class DepartmentSyncController {

    @Autowired
    private DepartmentSyncJob departmentSyncJob;

    @Autowired
    private DingTalkOAService dingTalkOAService;

    @Autowired
    private IDepartmentSCD2Service departmentSCD2Service;

    /**
     * 手动触发部门同步
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> manualSync() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("手动触发部门同步请求");
            departmentSyncJob.manualSync();

            response.put("success", true);
            response.put("message", "部门同步已触发");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("手动触发部门同步失败", e);

            response.put("success", false);
            response.put("message", "触发失败: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取部门同步状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            String status = departmentSyncJob.getSyncStatus();
            Integer currentDeptCount = departmentSCD2Service.countCurrentVersions();

            response.put("success", true);
            response.put("status", status);
            response.put("currentDepartmentCount", currentDeptCount);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取部门同步状态失败", e);

            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    
    /**
     * 获取部门的历史版本信息
     */
    @GetMapping("/history/{deptId}")
    public ResponseEntity<Map<String, Object>> getDepartmentHistory(@PathVariable Long deptId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<DepartmentSCD2> history = departmentSCD2Service.findAllVersionsByDeptId(deptId);

            response.put("success", true);
            response.put("deptId", deptId);
            response.put("history", history);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取部门历史版本失败", e);

            response.put("success", false);
            response.put("message", "获取历史版本失败: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}