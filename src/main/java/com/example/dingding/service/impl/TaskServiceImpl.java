package com.example.dingding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingding.entity.Task;
import com.example.dingding.mapper.TaskMapper;
import com.example.dingding.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务服务实现类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

    @Override
    public List<Task> listByProcessInstanceId(String processInstanceId) {
        return baseMapper.selectByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<Task> listByUserId(String userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<TaskMapper.TaskWorkloadStats> getUserWorkloadStats(String startTime, String endTime) {
        return baseMapper.selectUserWorkloadStats(startTime, endTime);
    }

    @Override
    public boolean saveBatch(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return true;
        }
        try {
            return saveBatch(tasks, 100); // 每批100条
        } catch (Exception e) {
            log.error("批量保存任务失败, 数量: {}", tasks.size(), e);
            return false;
        }
    }
}