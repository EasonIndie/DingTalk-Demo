package com.example.dingding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingding.entity.OperationRecord;
import com.example.dingding.mapper.OperationRecordMapper;
import com.example.dingding.service.IOperationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作记录服务实现类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class OperationRecordServiceImpl extends ServiceImpl<OperationRecordMapper, OperationRecord>
        implements IOperationRecordService {

    @Override
    public List<OperationRecord> listByProcessInstanceId(String processInstanceId) {
        return baseMapper.selectByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<OperationRecordMapper.ApprovalStats> getApprovalStats(String startTime, String endTime) {
        return baseMapper.selectApprovalStats(startTime, endTime);
    }

    @Override
    public List<OperationRecordMapper.ProcessEfficiencyStats> getProcessEfficiencyStats(String startTime, String endTime) {
        return baseMapper.selectProcessEfficiencyStats(startTime, endTime);
    }

    @Override
    public boolean saveBatch(List<OperationRecord> operationRecords) {
        if (operationRecords == null || operationRecords.isEmpty()) {
            return true;
        }
        try {
            return saveBatch(operationRecords, 100); // 每批100条
        } catch (Exception e) {
            log.error("批量保存操作记录失败, 数量: {}", operationRecords.size(), e);
            return false;
        }
    }
}