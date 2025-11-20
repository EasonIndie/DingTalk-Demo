package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 根据流程实例ID查询任务
     */
    @Select("SELECT * FROM ding_tasks WHERE process_instance_id = #{processInstanceId} ORDER BY create_time")
    List<Task> selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    /**
     * 根据用户ID查询任务
     */
    @Select("SELECT * FROM ding_tasks WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Task> selectByUserId(@Param("userId") String userId);

    /**
     * 统计用户任务工作量
     */
    @Select("SELECT user_id, " +
            "COUNT(*) as totalTasks, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completedTasks, " +
            "SUM(CASE WHEN result = 'AGREE' THEN 1 ELSE 0 END) as agreeTasks, " +
            "SUM(CASE WHEN result = 'REFUSE' THEN 1 ELSE 0 END) as refuseTasks, " +
            "AVG(TIMESTAMPDIFF(HOUR, create_time, finish_time)) as avgProcessHours " +
            "FROM ding_tasks " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY user_id")
    List<TaskWorkloadStats> selectUserWorkloadStats(@Param("startTime") String startTime,
                                                   @Param("endTime") String endTime);

    /**
     * 任务工作量统计结果类
     */
    class TaskWorkloadStats {
        private String userId;
        private Long totalTasks;
        private Long completedTasks;
        private Long agreeTasks;
        private Long refuseTasks;
        private Double avgProcessHours;

        // getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Long getTotalTasks() { return totalTasks; }
        public void setTotalTasks(Long totalTasks) { this.totalTasks = totalTasks; }
        public Long getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(Long completedTasks) { this.completedTasks = completedTasks; }
        public Long getAgreeTasks() { return agreeTasks; }
        public void setAgreeTasks(Long agreeTasks) { this.agreeTasks = agreeTasks; }
        public Long getRefuseTasks() { return refuseTasks; }
        public void setRefuseTasks(Long refuseTasks) { this.refuseTasks = refuseTasks; }
        public Double getAvgProcessHours() { return avgProcessHours; }
        public void setAvgProcessHours(Double avgProcessHours) { this.avgProcessHours = avgProcessHours; }
    }
}