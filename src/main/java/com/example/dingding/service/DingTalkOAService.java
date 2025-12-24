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
     * 同步OA表单实例数据
     * 从部门SCD2表获取部门信息，实时从API获取用户数据
     *
     * @param startTime 开始时间
     */
    void syncOALSS(LocalDateTime startTime);

    /**
     * 获取钉钉所有部门的详细信息
     * 包含部门ID、名称、父部门ID等信息
     * 采用递归方式获取完整的组织架构
     *
     * @return 所有部门的详细信息列表
     */
    List<DepartmentDTO> getAllDepartmentsWithDetails();

    /**
     * 根据流程发起人更新职位信息
     */
    void syncEmployeeTitle(LocalDateTime dateTime);

    /**
     * 缓存职位信息做字典
     */
    void cacheEmployeeTitle();
}