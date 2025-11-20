package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.ProcessInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface ProcessInstanceMapper extends BaseMapper<ProcessInstance> {

    /**
     * 根据流程模板ID和时间范围查询流程实例
     */
    @Select("SELECT * FROM ding_process_instances WHERE process_code = #{processCode} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime}")
    List<ProcessInstance> selectByProcessCodeAndTime(@Param("processCode") String processCode,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 根据部门名称和时间范围查询流程实例
     */
    @Select("SELECT * FROM ding_process_instances WHERE originator_dept_name = #{deptName} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime}")
    List<ProcessInstance> selectByDeptNameAndTime(@Param("deptName") String deptName,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 根据状态和时间范围查询流程实例
     */
    @Select("SELECT * FROM ding_process_instances WHERE status = #{status} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime}")
    List<ProcessInstance> selectByStatusAndTime(@Param("status") String status,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各部门的提案件数
     */
    @Select("SELECT originator_dept_name as deptName, COUNT(*) as proposalCount " +
            "FROM ding_process_instances " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY originator_dept_name")
    List<DepartmentStats> selectDepartmentProposalStats(@Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各流程模板的提案件数
     */
    @Select("SELECT process_code, COUNT(*) as count " +
            "FROM ding_process_instances " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY process_code")
    List<ProcessCodeStats> selectProcessCodeStats(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计月度提案数据
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') as month, " +
            "COUNT(*) as totalCount, " +
            "COUNT(DISTINCT originator_userid) as participantCount, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount " +
            "FROM ding_process_instances " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m')")
    List<MonthlyStats> selectMonthlyStats(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 部门统计结果类
     */
    class DepartmentStats {
        private String deptName;
        private Long proposalCount;

        // getters and setters
        public String getDeptName() { return deptName; }
        public void setDeptName(String deptName) { this.deptName = deptName; }
        public Long getProposalCount() { return proposalCount; }
        public void setProposalCount(Long proposalCount) { this.proposalCount = proposalCount; }
    }

    /**
     * 流程模板统计结果类
     */
    class ProcessCodeStats {
        private String processCode;
        private Long count;

        // getters and setters
        public String getProcessCode() { return processCode; }
        public void setProcessCode(String processCode) { this.processCode = processCode; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 月度统计结果类
     */
    class MonthlyStats {
        private String month;
        private Long totalCount;
        private Long participantCount;
        private Long completedCount;

        // getters and setters
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getParticipantCount() { return participantCount; }
        public void setParticipantCount(Long participantCount) { this.participantCount = participantCount; }
        public Long getCompletedCount() { return completedCount; }
        public void setCompletedCount(Long completedCount) { this.completedCount = completedCount; }
    }
}