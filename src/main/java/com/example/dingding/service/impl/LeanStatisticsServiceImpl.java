package com.example.dingding.service.impl;

import com.example.dingding.entity.FormComponentValue;
import com.example.dingding.entity.ProcessInstance;
import com.example.dingding.mapper.ProcessInstanceMapper;
import com.example.dingding.mapper.FormComponentValueMapper;
import com.example.dingding.service.ExcelService;
import com.example.dingding.service.*;
import com.example.dingding.service.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 精益统计服务实现类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class LeanStatisticsServiceImpl implements ILeanStatisticsService {

    @Autowired
    private IProcessInstanceService processInstanceService;

    @Autowired
    private IFormComponentValueService formComponentValueService;

    @Autowired
    private ExcelService excelService;

    @Override
    public OverallStatsDTO getOverallStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("获取全员统计指标: {} - {}", startTime, endTime);

        OverallStatsDTO stats = new OverallStatsDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        stats.setStatisticsTime(LocalDateTime.now().format(formatter));
        stats.setMonthRange(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM")) + "月");

        // 获取月度统计数据
        List<ProcessInstanceMapper.MonthlyStats> monthlyStats =
            processInstanceService.getMonthlyStats(startTime, endTime);

        if (!monthlyStats.isEmpty()) {
            ProcessInstanceMapper.MonthlyStats monthStat = monthlyStats.get(0);
            stats.setMonthlyProposalCount(monthStat.getTotalCount());
            stats.setParticipantCount(monthStat.getParticipantCount());
            stats.setCompletedProposalCount(monthStat.getCompletedCount());

            // 计算结案率
            if (monthStat.getTotalCount() > 0) {
                stats.setCompletionRate((double) monthStat.getCompletedCount() / monthStat.getTotalCount() * 100);
            }
        }

        // 计算平均处理天数
        List<ProcessInstance> completedInstances =
            processInstanceService.listByStatusAndTime("COMPLETED", startTime, endTime);
        if (!completedInstances.isEmpty()) {
            double totalDays = completedInstances.stream()
                .filter(instance -> instance.getCreateTime() != null && instance.getFinishTime() != null)
                .mapToDouble(instance -> java.time.Duration.between(
                    instance.getCreateTime(), instance.getFinishTime()).toDays())
                .sum();
            stats.setAverageProcessDays(totalDays / completedInstances.size());
        }

        // 统计经济效益
        String startTimeStr = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<FormComponentValueMapper.EconomicBenefitStats> economicStats =
            formComponentValueService.getEconomicBenefitStats(startTimeStr, endTimeStr);

        stats.setTotalEconomicBenefit(economicStats.stream()
            .mapToDouble(FormComponentValueMapper.EconomicBenefitStats::getTotalBenefit)
            .sum());
        stats.setTotalSavings(stats.getTotalEconomicBenefit() * 0.8); // 假设节约金额为经济效益的80%

        return stats;
    }

    @Override
    public DepartmentStatsDTO getDepartmentStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("获取部门管理统计: {} - {}", startTime, endTime);

        DepartmentStatsDTO stats = new DepartmentStatsDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        stats.setStatisticsTime(LocalDateTime.now().format(formatter));

        // 获取部门提案件数统计
        List<ProcessInstanceMapper.DepartmentStats> departmentStats =
            processInstanceService.getDepartmentProposalStats(startTime, endTime);

        List<DepartmentStatsDTO.DepartmentItem> departmentList = new ArrayList<>();
        long totalProjects = 0;

        for (ProcessInstanceMapper.DepartmentStats deptStat : departmentStats) {
            DepartmentStatsDTO.DepartmentItem item = new DepartmentStatsDTO.DepartmentItem();
            item.setDepartmentName(deptStat.getDeptName());
            item.setProjectCount(deptStat.getProposalCount());
            totalProjects += deptStat.getProposalCount();

            // 获取该部门的等级分布
            String startTimeStr = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 获取该部门的所有流程实例ID
            List<ProcessInstance> deptInstances =
                processInstanceService.listByDeptNameAndTime(deptStat.getDeptName(), startTime, endTime);
            List<String> instanceIds = deptInstances.stream()
                .map(ProcessInstance::getProcessInstanceId)
                .collect(Collectors.toList());

            // 统计改善等级分布
            List<FormComponentValue> improvementLevels =
                formComponentValueService.listByProcessInstanceIdsAndComponentName(
                    instanceIds, "改善等级");

            long aLevel = improvementLevels.stream().mapToLong(fcv ->
                "A级".equals(fcv.getValue()) ? 1 : 0).sum();
            long bLevel = improvementLevels.stream().mapToLong(fcv ->
                "B级".equals(fcv.getValue()) ? 1 : 0).sum();
            long cLevel = improvementLevels.stream().mapToLong(fcv ->
                "C级".equals(fcv.getValue()) ? 1 : 0).sum();

            item.setALevelCount(aLevel);
            item.setBLevelCount(bLevel);
            item.setCLevelCount(cLevel);

            // 统计完成情况
            long completed = deptInstances.stream().mapToLong(instance ->
                "COMPLETED".equals(instance.getStatus()) ? 1 : 0).sum();
            long inProgress = deptInstances.stream().mapToLong(instance ->
                "RUNNING".equals(instance.getStatus()) ? 1 : 0).sum();

            item.setCompletedCount(completed);
            item.setInProgressCount(inProgress);

            // 计算结案率
            if (item.getProjectCount() > 0) {
                item.setCompletionRate((double) completed / item.getProjectCount() * 100);
            }

            // 统计参与人数
            Set<String> participants = deptInstances.stream()
                .map(ProcessInstance::getOriginatorUserid)
                .collect(Collectors.toSet());
            item.setParticipantCount((long) participants.size());

            // 计算人均件数
            if (item.getParticipantCount() > 0) {
                item.setAveragePerPerson((double) item.getProjectCount() / item.getParticipantCount());
            }

            departmentList.add(item);
        }

        stats.setDepartmentList(departmentList);
        stats.setTotalProjects(totalProjects);

        return stats;
    }

    @Override
    public PersonRankingDTO getPersonRanking(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("获取个人统计排行: {} - {}", startTime, endTime);

        PersonRankingDTO stats = new PersonRankingDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        stats.setStatisticsTime(LocalDateTime.now().format(formatter));

        // 获取所有时间范围内的流程实例（通过按状态查询所有类型）
        List<ProcessInstance> allInstances = new ArrayList<>();
        String[] statuses = {"RUNNING", "COMPLETED", "TERMINATED", "CANCELED"};
        for (String status : statuses) {
            allInstances.addAll(processInstanceService.listByStatusAndTime(status, startTime, endTime));
        }

        // 按发起人分组统计
        Map<String, List<ProcessInstance>> userInstances = allInstances.stream()
            .collect(Collectors.groupingBy(ProcessInstance::getOriginatorUserid));

        String startTimeStr = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<PersonRankingDTO.PersonItem> personList = new ArrayList<>();

        for (Map.Entry<String, List<ProcessInstance>> entry : userInstances.entrySet()) {
            String userId = entry.getKey();
            List<ProcessInstance> instances = entry.getValue();

            PersonRankingDTO.PersonItem item = new PersonRankingDTO.PersonItem();
            item.setUserId(userId);
            item.setProposalCount((long) instances.size());

            // 统计采纳数
            long adoptedCount = instances.stream().mapToLong(instance ->
                "agree".equals(instance.getResult()) ? 1 : 0).sum();
            item.setAdoptedCount(adoptedCount);

            // 计算采纳率
            if (item.getProposalCount() > 0) {
                item.setAdoptionRate((double) adoptedCount / item.getProposalCount() * 100);
            }

            // 统计经济效益
            List<String> instanceIds = instances.stream()
                .map(ProcessInstance::getProcessInstanceId)
                .collect(Collectors.toList());

            List<FormComponentValue> economicValues =
                formComponentValueService.listByProcessInstanceIdsAndComponentName(
                    instanceIds, "预计产生经济效益(元)");

            double totalBenefit = economicValues.stream()
                .filter(fcv -> StringUtils.hasText(fcv.getValue()))
                .mapToDouble(fcv -> {
                    try {
                        return Double.parseDouble(fcv.getValue());
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .sum();
            item.setEconomicBenefit(totalBenefit);

            // 计算奖励积分
            item.setRewardPoints(calculateRewardPoints(adoptedCount, totalBenefit));

            // 设置部门（取第一个实例的部门）
            item.setDepartment(instances.get(0).getOriginatorDeptName());
            // 这里可以设置真实姓名，目前使用userId
            item.setName(userId);

            personList.add(item);
        }

        // 按经济效益排序
        personList.sort((a, b) -> Double.compare(b.getEconomicBenefit(), a.getEconomicBenefit()));

        // 设置排名
        for (int i = 0; i < personList.size(); i++) {
            personList.get(i).setRanking(i + 1);
        }

        stats.setPersonList(personList);
        return stats;
    }

    @Override
    public boolean generateExcelReport(LocalDateTime startTime, LocalDateTime endTime, String outputPath) {
        try {
            log.info("生成Excel报表: {} - {}, 输出路径: {}", startTime, endTime, outputPath);

            // 获取统计数据
            OverallStatsDTO overallStats = getOverallStatistics(startTime, endTime);
            DepartmentStatsDTO departmentStats = getDepartmentStatistics(startTime, endTime);
            PersonRankingDTO personRanking = getPersonRanking(startTime, endTime);

            // 生成Excel报表
            boolean success = excelService.generateLeanReport(outputPath);

            if (success) {
                log.info("Excel报表生成成功: {}", outputPath);
            } else {
                log.error("Excel报表生成失败");
            }

            return success;

        } catch (Exception e) {
            log.error("生成Excel报表失败", e);
            return false;
        }
    }

    /**
     * 计算奖励积分
     */
    private Integer calculateRewardPoints(long adoptedCount, double economicBenefit) {
        // 基础积分：采纳数 * 10
        int basePoints = (int) (adoptedCount * 10);
        // 经济效益积分：每1000元奖励1分
        int benefitPoints = (int) (economicBenefit / 1000);
        return basePoints + benefitPoints;
    }
}