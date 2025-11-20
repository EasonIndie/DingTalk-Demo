package com.example.dingding.service;

import java.time.LocalDateTime;

/**
 * 钉钉办公自动化服务接口
 * 提供钉钉数据同步相关功能
 *
 * @author system
 * @version 1.0.0
 */
public interface DingTalkOAService {

    /**
     * 同步所有用户ID到Redis
     * 包含完整流程：
     * 1. 获取有效的access_token
     * 2. 递归获取所有部门ID并缓存
     * 3. 遍历所有部门获取用户ID并缓存
     *
     * @return 同步的用户数量
     */
    int syncUserIds();

    void syncOALSS(LocalDateTime startTime);

}