package com.example.dingding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingding.entity.SyncRecord;
import com.example.dingding.mapper.SyncRecordMapper;
import com.example.dingding.service.ISyncRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据同步记录服务实现类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class SyncRecordServiceImpl extends ServiceImpl<SyncRecordMapper, SyncRecord>
        implements ISyncRecordService {

    @Override
    public SyncRecord getLatestBySyncType(String syncType) {
        return baseMapper.selectLatestBySyncType(syncType);
    }

    @Override
    public List<SyncRecord> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public SyncRecord startSync(String syncType, Integer totalCount) {
        SyncRecord syncRecord = new SyncRecord();
        syncRecord.setSyncType(syncType);
        syncRecord.setSyncStartTime(LocalDateTime.now());
        syncRecord.setTotalCount(totalCount != null ? totalCount : 0);
        syncRecord.setSuccessCount(0);
        syncRecord.setFailedCount(0);
        syncRecord.setSyncStatus("RUNNING");

        save(syncRecord);
        log.info("开始同步: {}, 总数: {}", syncType, totalCount);
        return syncRecord;
    }

    @Override
    public void completeSync(SyncRecord syncRecord, Integer successCount) {
        syncRecord.setSyncEndTime(LocalDateTime.now());
        syncRecord.setSuccessCount(successCount != null ? successCount : 0);
        syncRecord.setFailedCount(syncRecord.getTotalCount() - syncRecord.getSuccessCount());
        syncRecord.setSyncStatus("SUCCESS");

        updateById(syncRecord);
        log.info("同步完成: {}, 成功: {}, 失败: {}",
                syncRecord.getSyncType(), syncRecord.getSuccessCount(), syncRecord.getFailedCount());
    }

    @Override
    public void failSync(SyncRecord syncRecord, Integer successCount, String errorMessage) {
        syncRecord.setSyncEndTime(LocalDateTime.now());
        syncRecord.setSuccessCount(successCount != null ? successCount : 0);
        syncRecord.setFailedCount(syncRecord.getTotalCount() - syncRecord.getSuccessCount());
        syncRecord.setSyncStatus("FAILED");
        syncRecord.setErrorMessage(errorMessage);

        updateById(syncRecord);
        log.error("同步失败: {}, 成功: {}, 失败: {}, 错误: {}",
                syncRecord.getSyncType(), syncRecord.getSuccessCount(), syncRecord.getFailedCount(), errorMessage);
    }

    @Override
    public List<SyncRecordMapper.SyncSummaryStats> getSyncSummaryStats(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectSyncSummaryStats(startTime, endTime);
    }
}