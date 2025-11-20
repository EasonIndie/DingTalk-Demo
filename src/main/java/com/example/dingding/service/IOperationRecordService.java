package com.example.dingding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dingding.entity.OperationRecord;
import com.example.dingding.mapper.OperationRecordMapper;

import java.util.List;

/**
 * 操作记录服务接口
 *
 * @author system
 * @version 1.0.0
 */
public interface IOperationRecordService extends IService<OperationRecord> {

    /**
     * 根据流程实例ID查询操作记录
     *
     * @param processInstanceId 流程实例ID
     * @return 操作记录列表
     */
    List<OperationRecord> listByProcessInstanceId(String processInstanceId);

    /**
     * 统计各审批人的审批数量
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 审批统计列表
     */
    List<OperationRecordMapper.ApprovalStats> getApprovalStats(String startTime, String endTime);

    /**
     * 统计各环节平均处理时间
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 流程效率统计列表
     */
    List<OperationRecordMapper.ProcessEfficiencyStats> getProcessEfficiencyStats(String startTime, String endTime);

    /**
     * 批量保存操作记录
     *
     * @param operationRecords 操作记录列表
     * @return 是否成功
     */
    boolean saveBatch(List<OperationRecord> operationRecords);
}