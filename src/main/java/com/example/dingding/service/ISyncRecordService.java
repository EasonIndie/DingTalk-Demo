package com.example.dingding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dingding.entity.SyncRecord;
import com.example.dingding.mapper.SyncRecordMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据同步记录服务接口
 *
 * @author system
 * @version 1.0.0
 */
public interface ISyncRecordService extends IService<SyncRecord> {

    /**
     * 根据同步类型查询最近的同步记录
     *
     * @param syncType 同步类型
     * @return 同步记录
     */
    SyncRecord getLatestBySyncType(String syncType);

    /**
     * 根据时间范围查询同步记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 同步记录列表
     */
    List<SyncRecord> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 记录同步开始
     *
     * @param syncType  同步类型
     * @param totalCount 处理总数
     * @return 同步记录
     */
    SyncRecord startSync(String syncType, Integer totalCount);

    /**
     * 记录同步成功
     *
     * @param syncRecord    同步记录
     * @param successCount  成功数量
     */
    void completeSync(SyncRecord syncRecord, Integer successCount);

    /**
     * 记录同步失败
     *
     * @param syncRecord   同步记录
     * @param successCount 成功数量
     * @param errorMessage 错误信息
     */
    void failSync(SyncRecord syncRecord, Integer successCount, String errorMessage);

    /**
     * 统计同步情况
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 同步汇总统计列表
     */
    List<SyncRecordMapper.SyncSummaryStats> getSyncSummaryStats(LocalDateTime startTime, LocalDateTime endTime);
}