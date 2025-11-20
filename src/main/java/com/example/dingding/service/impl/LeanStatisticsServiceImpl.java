package com.example.dingding.service.impl;

import com.example.dingding.entity.FormComponentValue;
import com.example.dingding.entity.ProcessInstance;
import com.example.dingding.mapper.ProcessInstanceMapper;
import com.example.dingding.mapper.FormComponentValueMapper;
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

    @Override
    public OverallStatsDTO getOverallStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("获取全员统计指标: {} - {}", startTime, endTime);

        OverallStatsDTO stats = new OverallStatsDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        stats.setStatisticsTime(LocalDateTime.now().format(formatter));

        List<OverallStatsDTO.IndicatorItem> indicatorList = new ArrayList<>();

        // 1. 本月合理化建议提案数
        Long monthlyProposalCount = processInstanceService.getMonthlyProposalCount(startTime, endTime);
        indicatorList.add(createIndicatorItem(
            "本月合理化建议提案数",
            monthlyProposalCount.toString(),
            "个",
            "↑5%",  // 示例环比，实际需要计算
            "50",
            monthlyProposalCount >= 50 ? "100%" : "80%"
        ));

        // 2. 参与提案人数
        Long participantCount = processInstanceService.getParticipantCount(startTime, endTime);
        indicatorList.add(createIndicatorItem(
            "参与提案人数",
            participantCount.toString(),
            "人",
            "↑3%",
            "30",
            participantCount >= 30 ? "100%" : "90%"
        ));

        // 3. 结案提案数
        Long completedProposalCount = processInstanceService.getCompletedProposalCount(startTime, endTime);
        indicatorList.add(createIndicatorItem(
            "结案提案数",
            completedProposalCount.toString(),
            "个",
            "↑8%",
            "40",
            completedProposalCount >= 40 ? "100%" : "85%"
        ));

        // 4. 结案率
        Double completionRate = monthlyProposalCount > 0 ?
            (double) completedProposalCount / monthlyProposalCount * 100 : 0.0;
        indicatorList.add(createIndicatorItem(
            "结案率",
            String.format("%.1f%%", completionRate),
            "%",
            "↑2%",
            "80%",
            completionRate >= 80.0 ? "100%" : "90%"
        ));

        // 5. 平均处理天数
        Double averageProcessDays = processInstanceService.getAverageProcessDays(startTime, endTime);
        indicatorList.add(createIndicatorItem(
            "平均处理天数",
            averageProcessDays != null ? String.format("%.1f", averageProcessDays) : "0",
            "天",
            "↓1%",
            "5",
            averageProcessDays != null && averageProcessDays <= 5.0 ? "100%" : "80%"
        ));

        // 6. 产生经济效益
        String startTimeStr = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<FormComponentValueMapper.EconomicBenefitStats> economicStats =
            formComponentValueService.getEconomicBenefitStats(startTimeStr, endTimeStr);
        Double totalEconomicBenefit = economicStats.stream()
            .mapToDouble(FormComponentValueMapper.EconomicBenefitStats::getTotalBenefit)
            .sum();
        indicatorList.add(createIndicatorItem(
            "产生经济效益",
            String.format("%.0f", totalEconomicBenefit),
            "元",
            "↑15%",
            "10000",
            totalEconomicBenefit >= 10000 ? "100%" : "70%"
        ));

        // 7. 节约金额
        Double totalSavings = totalEconomicBenefit * 0.8; // 假设节约金额为经济效益的80%
        indicatorList.add(createIndicatorItem(
            "节约金额",
            String.format("%.0f", totalSavings),
            "元",
            "↑12%",
            "8000",
            totalSavings >= 8000 ? "100%" : "75%"
        ));

        stats.setIndicatorList(indicatorList);
        return stats;
    }

    /**
     * 创建指标项
     */
    private OverallStatsDTO.IndicatorItem createIndicatorItem(String indicatorName, String value,
                                                              String unit, String monthOnMonth,
                                                              String targetValue, String complianceRate) {
        OverallStatsDTO.IndicatorItem item = new OverallStatsDTO.IndicatorItem();
        item.setIndicatorName(indicatorName);
        item.setValue(value);
        item.setUnit(unit);
        item.setMonthOnMonth(monthOnMonth);
        item.setTargetValue(targetValue);
        item.setComplianceRate(complianceRate);
        return item;
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