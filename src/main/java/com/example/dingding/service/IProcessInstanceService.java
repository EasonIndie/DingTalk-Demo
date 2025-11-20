package com.example.dingding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dingding.entity.ProcessInstance;
import com.example.dingding.mapper.ProcessInstanceMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例服务接口
 *
 * @author system
 * @version 1.0.0
 */
public interface IProcessInstanceService extends IService<ProcessInstance> {

    /**
     * 根据流程模板ID和时间范围查询流程实例
     *
     * @param processCode 流程模板ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 流程实例列表
     */
    List<ProcessInstance> listByProcessCodeAndTime(String processCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据部门名称和时间范围查询流程实例
     *
     * @param deptName  部门名称
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 流程实例列表
     */
    List<ProcessInstance> listByDeptNameAndTime(String deptName, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据状态和时间范围查询流程实例
     *
     * @param status    状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 流程实例列表
     */
    List<ProcessInstance> listByStatusAndTime(String status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计各部门的提案件数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 部门统计列表
     */
    List<ProcessInstanceMapper.DepartmentStats> getDepartmentProposalStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计月度提案数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 月度统计列表
     */
    List<ProcessInstanceMapper.MonthlyStats> getMonthlyStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 保存或更新流程实例
     *
     * @param processInstance 流程实例
     * @return 是否成功
     */
    boolean saveOrUpdate(ProcessInstance processInstance);

    /**
     * 根据流程实例ID查询
     *
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    ProcessInstance getByProcessInstanceId(String processInstanceId);
}