package com.example.dingding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingding.entity.ProcessInstance;
import com.example.dingding.mapper.ProcessInstanceMapper;
import com.example.dingding.service.IProcessInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例服务实现类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class ProcessInstanceServiceImpl extends ServiceImpl<ProcessInstanceMapper, ProcessInstance>
        implements IProcessInstanceService {

    @Override
    public List<ProcessInstance> listByProcessCodeAndTime(String processCode, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectByProcessCodeAndTime(processCode, startTime, endTime);
    }

    @Override
    public List<ProcessInstance> listByDeptNameAndTime(String deptName, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectByDeptNameAndTime(deptName, startTime, endTime);
    }

    @Override
    public List<ProcessInstance> listByStatusAndTime(String status, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectByStatusAndTime(status, startTime, endTime);
    }

    @Override
    public List<ProcessInstanceMapper.DepartmentStats> getDepartmentProposalStats(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectDepartmentProposalStats(startTime, endTime);
    }

    @Override
    public List<ProcessInstanceMapper.MonthlyStats> getMonthlyStats(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectMonthlyStats(startTime, endTime);
    }

    @Override
    public boolean saveOrUpdate(ProcessInstance entity) {
        try {
            return super.saveOrUpdate(entity);
        } catch (Exception e) {
            log.error("保存或更新流程实例失败: {}", entity.getProcessInstanceId(), e);
            return false;
        }
    }

    @Override
    public ProcessInstance getByProcessInstanceId(String processInstanceId) {
        return getOne(new LambdaQueryWrapper<ProcessInstance>()
                .eq(ProcessInstance::getProcessInstanceId, processInstanceId));
    }
}