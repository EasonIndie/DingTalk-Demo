package com.example.dingding.controller;

import com.example.dingding.service.DingTalkOAService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉数据同步控制器
 * 提供钉钉数据同步相关的REST API接口
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/dingtalk")
@CrossOrigin
public class DingTalkController {

    @Autowired
    private DingTalkOAService dingTalkOAService;

    

    /**
     * 同步OA表单实例数据
     * 从部门SCD2表获取部门信息，实时从API获取用户数据
     *
     * @return 同步结果
     */
    @PostMapping("/sync/oaLSS")
    public ResponseEntity<Map<String, Object>> syncOALSS(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("收到同步OA数据的请求");

            // 如果没有指定开始时间，使用默认时间（30天前）
            LocalDateTime syncTime = startTime != null ? startTime : LocalDateTime.now().minusDays(30);

            long start = System.currentTimeMillis();
            dingTalkOAService.syncOALSS(syncTime);
            long end = System.currentTimeMillis();
            long costTime = end - start;

            result.put("success", true);
            result.put("message", "OA数据同步完成");
            result.put("syncStartTime", syncTime);
            result.put("costTime", costTime + "ms");

            log.info("OA数据同步完成，耗时{}ms", costTime);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("同步OA数据时发生异常", e);

            result.put("success", false);
            result.put("message", "同步失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

}