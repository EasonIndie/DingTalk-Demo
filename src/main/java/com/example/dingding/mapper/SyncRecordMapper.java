package com.example.dingding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dingding.entity.SyncRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据同步记录Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface SyncRecordMapper extends BaseMapper<SyncRecord> {

    /**
     * 根据同步类型查询最近的同步记录
     */
    @Select("SELECT * FROM ding_sync_records WHERE sync_type = #{syncType} ORDER BY sync_start_time DESC LIMIT 1")
    SyncRecord selectLatestBySyncType(@Param("syncType") String syncType);

    /**
     * 根据时间范围查询同步记录
     */
    @Select("SELECT * FROM ding_sync_records " +
            "WHERE sync_start_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY sync_start_time DESC")
    List<SyncRecord> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 统计同步情况
     */
    @Select("SELECT sync_type, " +
            "COUNT(*) as totalSyncs, " +
            "SUM(total_count) as totalProcessed, " +
            "SUM(success_count) as totalSuccess, " +
            "SUM(failed_count) as totalFailed " +
            "FROM ding_sync_records " +
            "WHERE sync_start_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY sync_type")
    List<SyncSummaryStats> selectSyncSummaryStats(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 同步汇总统计结果类
     */
    class SyncSummaryStats {
        private String syncType;
        private Long totalSyncs;
        private Long totalProcessed;
        private Long totalSuccess;
        private Long totalFailed;

        // getters and setters
        public String getSyncType() { return syncType; }
        public void setSyncType(String syncType) { this.syncType = syncType; }
        public Long getTotalSyncs() { return totalSyncs; }
        public void setTotalSyncs(Long totalSyncs) { this.totalSyncs = totalSyncs; }
        public Long getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(Long totalProcessed) { this.totalProcessed = totalProcessed; }
        public Long getTotalSuccess() { return totalSuccess; }
        public void setTotalSuccess(Long totalSuccess) { this.totalSuccess = totalSuccess; }
        public Long getTotalFailed() { return totalFailed; }
        public void setTotalFailed(Long totalFailed) { this.totalFailed = totalFailed; }
    }
}