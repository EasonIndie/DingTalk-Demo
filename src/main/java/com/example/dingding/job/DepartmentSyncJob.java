package com.example.dingding.job;

import com.example.dingding.dto.DepartmentDTO;
import com.example.dingding.dto.DepartmentSyncResultDTO;
import com.example.dingding.service.DingTalkOAService;
import com.example.dingding.service.IDepartmentSCD2Service;
import com.example.dingding.service.ISyncRecordService;
import com.example.dingding.entity.SyncRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门同步定时作业
 * 实现部门全量同步的SCD2模式，每日02:00执行
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "dingding.job.department-sync.enabled", havingValue = "true", matchIfMissing = true)
public class DepartmentSyncJob {

    @Autowired
    private DingTalkOAService dingTalkOAService;

    @Autowired
    private IDepartmentSCD2Service departmentSCD2Service;

    @Autowired
    private ISyncRecordService syncRecordService;

    /**
     * 定时执行部门全量同步
     * 每日02:00执行
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncDepartments() {
        log.info("=== 开始执行部门全量同步作业 [sync_ding_departments_scd2] ===");

        // 记录同步开始
        SyncRecord syncRecord = syncRecordService.startSync("DEPARTMENT_SCD2", 0);
        DepartmentSyncResultDTO result = null;

        try {
            // 1. 获取钉钉所有部门信息
            log.info("步骤1: 从钉钉获取所有部门信息");
            List<DepartmentDTO> departments = dingTalkOAService.getAllDepartmentsWithDetails();

            if (departments.isEmpty()) {
                log.warn("未获取到任何部门信息，可能是API调用失败或无数据");
                syncRecordService.failSync(syncRecord, 0, "未获取到部门信息");
                return;
            }

            // 2. 执行SCD2同步
            log.info("步骤2: 执行SCD2部门同步");
            LocalDate syncDate = LocalDate.now();
            result = departmentSCD2Service.syncDepartments(departments, syncDate);

            // 3. 记录同步结果
            if (result.isSuccess()) {
                log.info("部门同步成功完成 - 总数: {}, 新增: {}, 变更: {}, 未变化: {}, 耗时: {}ms",
                        result.getTotalCount(),
                        result.getNewCount(),
                        result.getChangedCount(),
                        result.getUnchangedCount(),
                        result.getDuration());

                // 更新同步记录为成功
                syncRecord.setTotalCount(result.getTotalCount());
                syncRecordService.completeSync(syncRecord, result.getTotalCount());

                // 如果有数据变化，记录额外信息
                if (result.hasChanges()) {
                    String extraData = String.format("{\"new\":%d,\"changed\":%d,\"unchanged\":%d}",
                            result.getNewCount(),
                            result.getChangedCount(),
                            result.getUnchangedCount());
                    syncRecord.setExtraData(extraData);
                    syncRecordService.updateById(syncRecord);
                }
            } else {
                log.error("部门同步失败: {}", result.getErrorMessage());
                syncRecordService.failSync(syncRecord,
                    result.getTotalCount(),
                    result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("部门同步作业执行异常", e);
            String errorMsg = String.format("同步执行异常: %s", e.getMessage());
            syncRecordService.failSync(syncRecord,
                result != null ? result.getTotalCount() : 0,
                errorMsg);
        } finally {
            log.info("=== 部门全量同步作业执行完成 ===");
        }
    }

    /**
     * 手动触发同步（用于测试）
     */
    public void manualSync() {
        log.info("手动触发部门同步");
        syncDepartments();
    }

    /**
     * 获取当前部门同步状态
     */
    public String getSyncStatus() {
        try {
            // 查询最近的一次同步记录
            List<SyncRecord> recentSyncs = syncRecordService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SyncRecord>()
                    .eq("sync_type", "DEPARTMENT_SCD2")
                    .orderByDesc("created_at")
                    .last("LIMIT 1")
            );

            if (recentSyncs.isEmpty()) {
                return "暂无同步记录";
            }

            SyncRecord lastSync = recentSyncs.get(0);
            return String.format("最近同步: %s, 状态: %s, 处理总数: %d, 成功: %d, 失败: %d",
                    lastSync.getCreatedAt(),
                    lastSync.getSyncStatus(),
                    lastSync.getTotalCount(),
                    lastSync.getSuccessCount(),
                    lastSync.getFailedCount());

        } catch (Exception e) {
            log.error("获取同步状态失败", e);
            return "获取同步状态失败: " + e.getMessage();
        }
    }
}