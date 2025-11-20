package com.example.dingding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dingding.entity.Task;
import com.example.dingding.mapper.TaskMapper;

import java.util.List;

/**
 * 任务服务接口
 *
 * @author system
 * @version 1.0.0
 */
public interface ITaskService extends IService<Task> {

    /**
     * 根据流程实例ID查询任务
     *
     * @param processInstanceId 流程实例ID
     * @return 任务列表
     */
    List<Task> listByProcessInstanceId(String processInstanceId);

    /**
     * 根据用户ID查询任务
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    List<Task> listByUserId(String userId);

    /**
     * 统计用户任务工作量
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 任务工作量统计列表
     */
    List<TaskMapper.TaskWorkloadStats> getUserWorkloadStats(String startTime, String endTime);

    /**
     * 批量保存任务
     *
     * @param tasks 任务列表
     * @return 是否成功
     */
    boolean saveBatch(List<Task> tasks);
}