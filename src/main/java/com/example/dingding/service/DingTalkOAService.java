package com.example.dingding.service;

import com.example.dingding.dto.DepartmentDTO;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 获取钉钉所有部门的详细信息
     * 包含部门ID、名称、父部门ID等信息
     * 采用递归方式获取完整的组织架构
     *
     * @return 所有部门的详细信息列表
     */
    List<DepartmentDTO> getAllDepartmentsWithDetails();
}