package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.OperationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作记录Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface OperationRecordMapper extends BaseMapper<OperationRecord> {

    /**
     * 根据流程实例ID查询操作记录
     */
    @Select("SELECT * FROM ding_operation_records WHERE process_instance_id = #{processInstanceId} ORDER BY operation_date")
    List<OperationRecord> selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    /**
     * 统计各审批人的审批数量
     */
    @Select("SELECT user_id, show_name, " +
            "COUNT(*) as totalCount, " +
            "SUM(CASE WHEN result = 'AGREE' THEN 1 ELSE 0 END) as agreeCount, " +
            "SUM(CASE WHEN result = 'REFUSE' THEN 1 ELSE 0 END) as refuseCount " +
            "FROM ding_operation_records " +
            "WHERE operation_type = 'EXECUTE_TASK_NORMAL' " +
            "AND operation_date BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY user_id, show_name")
    List<ApprovalStats> selectApprovalStats(@Param("startTime") String startTime,
                                           @Param("endTime") String endTime);

    /**
     * 统计各环节平均处理时间
     */
    @Select("SELECT show_name, " +
            "COUNT(*) as count, " +
            "AVG(TIMESTAMPDIFF(HOUR, operation_date, " +
            "    (SELECT MIN(operation_date) FROM ding_operation_records o2 " +
            "     WHERE o2.process_instance_id = o1.process_instance_id " +
            "     AND o2.operation_date > o1.operation_date))) as avgProcessHours " +
            "FROM ding_operation_records o1 " +
            "WHERE operation_type = 'EXECUTE_TASK_NORMAL' " +
            "AND operation_date BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY show_name " +
            "HAVING count > 0")
    List<ProcessEfficiencyStats> selectProcessEfficiencyStats(@Param("startTime") String startTime,
                                                              @Param("endTime") String endTime);

    /**
     * 审批统计结果类
     */
    class ApprovalStats {
        private String userId;
        private String showName;
        private Long totalCount;
        private Long agreeCount;
        private Long refuseCount;

        // getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getShowName() { return showName; }
        public void setShowName(String showName) { this.showName = showName; }
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getAgreeCount() { return agreeCount; }
        public void setAgreeCount(Long agreeCount) { this.agreeCount = agreeCount; }
        public Long getRefuseCount() { return refuseCount; }
        public void setRefuseCount(Long refuseCount) { this.refuseCount = refuseCount; }
    }

    /**
     * 流程效率统计结果类
     */
    class ProcessEfficiencyStats {
        private String showName;
        private Long count;
        private Double avgProcessHours;

        // getters and setters
        public String getShowName() { return showName; }
        public void setShowName(String showName) { this.showName = showName; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public Double getAvgProcessHours() { return avgProcessHours; }
        public void setAvgProcessHours(Double avgProcessHours) { this.avgProcessHours = avgProcessHours; }
    }
}