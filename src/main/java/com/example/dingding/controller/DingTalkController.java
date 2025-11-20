package com.example.dingding.controller;

import com.example.dingding.service.DingTalkOAService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     * 同步所有用户ID到Redis
     *
     * @return 同步结果
     */
    @PostMapping("/sync/users")
    public ResponseEntity<Map<String, Object>> syncUsers() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("收到同步用户ID的请求");

            long startTime = System.currentTimeMillis();
            int userCount = dingTalkOAService.syncUserIds();
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            result.put("success", true);
            result.put("message", "同步完成");
            result.put("userCount", userCount);
            result.put("costTime", costTime + "ms");

            log.info("同步用户ID完成，共{}个用户，耗时{}ms", userCount, costTime);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("同步用户ID时发生异常", e);

            result.put("success", false);
            result.put("message", "同步失败：" + e.getMessage());
            result.put("userCount", 0);

            return ResponseEntity.internalServerError().body(result);
        }
    }


    @PostMapping("/sync/oaLSS")
    public ResponseEntity<Map<String, Object>> syncOALSS() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("收到从缓存获取用户ID列表的请求");

            long startTime = System.currentTimeMillis();
            LocalDateTime start = LocalDateTime.now().minusDays(120);
            dingTalkOAService.syncOALSS(start);
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            result.put("success", true);
            result.put("costTime", costTime + "ms");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("从缓存获取用户ID列表时发生异常", e);

            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

}