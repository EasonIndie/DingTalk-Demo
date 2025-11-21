package com.example.dingding.service.impl;

import com.example.dingding.dto.DashboardStatsDTO;
import com.example.dingding.dto.ProposalListDTO;
import com.example.dingding.dto.ProposalQueryDTO;
import com.example.dingding.dto.ProposalListDTO.ProposalItem;
import com.example.dingding.entity.ProcessInstance;
import com.example.dingding.entity.FormComponentValue;
import com.example.dingding.service.IProcessInstanceService;
import com.example.dingding.service.IFormComponentValueService;
import com.example.dingding.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现类 - 合并指标看板和提案管理功能
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private IProcessInstanceService processInstanceService;

    @Autowired
    private IFormComponentValueService formComponentValueService;

    // ==================== 指标看板相关方法 ====================

    @Override
    public DashboardStatsDTO getDashboardStats(LocalDateTime startTime, LocalDateTime endTime,
                                                String area, String region) {
        log.info("获取指标看板统计数据: {} - {}, 院区: {}, 区域: {}", startTime, endTime, area, region);

        DashboardStatsDTO stats = new DashboardStatsDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        stats.setStatisticsTime(LocalDateTime.now().format(formatter));

        // 计算基础统计指标
        stats.setBasicStats(calculateBasicStats(startTime, endTime, area, region));

        // 计算项目管理指标
        stats.setProjectStats(calculateProjectStats(startTime, endTime, area, region));

        // 计算排名统计
        stats.setRankingStats(calculateRankingStats(startTime, endTime, area, region));

        return stats;
    }

    /**
     * 计算基础统计指标
     */
    private DashboardStatsDTO.BasicStats calculateBasicStats(LocalDateTime startTime, LocalDateTime endTime,
                                                           String area, String region) {
        DashboardStatsDTO.BasicStats basicStats = new DashboardStatsDTO.BasicStats();

        // 获取指定时间范围内的所有流程实例
        List<ProcessInstance> instances = getAllInstancesByTimeRange(startTime, endTime, area, region);

        // 提案总数
        Long totalCount = (long) instances.size();
        basicStats.setTotalCount(totalCount);

        if (totalCount == 0) {
            // 如果没有数据，返回默认值
            basicStats.setAverageCount(0.0);
            basicStats.setParticipationRate(0.0);
            basicStats.setPassRate(0.0);
            basicStats.setAdoptionRate(0.0);
            basicStats.setExcellentCount(0L);
            basicStats.setProjectCount(0L);
            basicStats.setTotalEmployees(0L);
            basicStats.setProposalEmployees(0L);
            return basicStats;
        }

        // 提案员工数（去重）
        Set<String> proposalEmployees = instances.stream()
            .map(ProcessInstance::getOriginatorUserid)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        basicStats.setProposalEmployees((long) proposalEmployees.size());

        // 总员工数（这里需要从其他数据源获取，暂时用提案员工数作为基准）
        // 实际应用中应该从员工管理系统获取
        basicStats.setTotalEmployees(basicStats.getProposalEmployees());

        // 平均提案数：提案总数 ÷ 提案的员工数
        if (basicStats.getProposalEmployees() > 0) {
            basicStats.setAverageCount((double) totalCount / basicStats.getProposalEmployees());
        } else {
            basicStats.setAverageCount(0.0);
        }

        // 提案参与率：提案的员工数 ÷ 总员工数
        if (basicStats.getTotalEmployees() > 0) {
            basicStats.setParticipationRate((double) basicStats.getProposalEmployees() / basicStats.getTotalEmployees() * 100);
        } else {
            basicStats.setParticipationRate(0.0);
        }

        // 提案通过率：通过数 ÷ 提案总数
        long passCount = instances.stream()
            .mapToLong(instance -> "agree".equals(instance.getResult()) ? 1 : 0)
            .sum();
        basicStats.setPassRate((double) passCount / totalCount * 100);

        // 提案采纳率：采纳数 ÷ 提案通过数（这里暂时用通过数作为采纳数）
        if (passCount > 0) {
            basicStats.setAdoptionRate(100.0); // 如果通过了就算采纳
        } else {
            basicStats.setAdoptionRate(0.0);
        }

        // 优秀提案数（预留字段，暂时设置为0）
        basicStats.setExcellentCount(0L);

        // 项目数：采纳的项目数（这里用通过的提案数）
        basicStats.setProjectCount(passCount);

        return basicStats;
    }

    /**
     * 计算项目管理指标
     */
    private DashboardStatsDTO.ProjectStats calculateProjectStats(LocalDateTime startTime, LocalDateTime endTime,
                                                              String area, String region) {
        DashboardStatsDTO.ProjectStats projectStats = new DashboardStatsDTO.ProjectStats();

        // 获取通过的提案（作为项目）
        List<ProcessInstance> projects = getInstancesByStatus("COMPLETED", startTime, endTime, area, region);
        Long totalProjects = (long) projects.size();
        projectStats.setTotalProjects(totalProjects);

        if (totalProjects == 0) {
            projectStats.setCloseRate30Days(0.0);
            projectStats.setOverdueRate30Days(0.0);
            projectStats.setClosedProjects30Days(0L);
            projectStats.setOverdueProjects30Days(0L);
            return projectStats;
        }

        // 计算30天内关闭的项目数
        LocalDateTime thirtyDaysAgo = endTime.minusDays(30);
        long closedProjects30Days = projects.stream()
            .filter(instance -> instance.getFinishTime() != null)
            .filter(instance -> instance.getFinishTime().isAfter(thirtyDaysAgo) &&
                               !instance.getFinishTime().isAfter(endTime))
            .count();
        projectStats.setClosedProjects30Days(closedProjects30Days);

        // 30天关闭率：结束采纳项目数 ÷ 项目总数（要求30天内结束）
        projectStats.setCloseRate30Days((double) closedProjects30Days / totalProjects * 100);

        // 30天超期率（创建时间超过30天但未完成的项目）
        long overdueProjects30Days = projects.stream()
            .filter(instance -> instance.getCreateTime().isBefore(thirtyDaysAgo))
            .count();
        projectStats.setOverdueProjects30Days(overdueProjects30Days);
        projectStats.setOverdueRate30Days((double) overdueProjects30Days / totalProjects * 100);

        return projectStats;
    }

    /**
     * 计算排名统计
     */
    private DashboardStatsDTO.RankingStats calculateRankingStats(LocalDateTime startTime, LocalDateTime endTime,
                                                              String area, String region) {
        DashboardStatsDTO.RankingStats rankingStats = new DashboardStatsDTO.RankingStats();

        // 各院区提案总数排名
        rankingStats.setAreaTotalRanking(calculateAreaRankingByTotal(startTime, endTime));

        // 各院区提案参与率排名
        rankingStats.setAreaParticipationRanking(calculateAreaRankingByParticipation(startTime, endTime));

        // 区域提案数排名
        rankingStats.setRegionRanking(calculateRegionRanking(startTime, endTime));

        return rankingStats;
    }

    /**
     * 计算各院区提案总数排名
     */
    private List<DashboardStatsDTO.AreaRanking> calculateAreaRankingByTotal(LocalDateTime startTime,
                                                                          LocalDateTime endTime) {
        Map<String, Long> areaCountMap = new HashMap<>();
        List<ProcessInstance> instances = getAllInstancesByTimeRange(startTime, endTime, null, null);

        // 按院区统计提案数
        instances.forEach(instance -> {
            String deptName = instance.getOriginatorDeptName();
            String area = extractAreaFromDepartment(deptName);
            if (StringUtils.hasText(area)) {
                areaCountMap.put(area, areaCountMap.getOrDefault(area, 0L) + 1);
            }
        });

        // 排名并转换为DTO
        List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(areaCountMap.entrySet());
        sortedEntries.sort(Map.Entry.<String, Long>comparingByValue().reversed());

        List<DashboardStatsDTO.AreaRanking> rankings = new ArrayList<>();
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<String, Long> entry = sortedEntries.get(i);
            DashboardStatsDTO.AreaRanking ranking = new DashboardStatsDTO.AreaRanking();
            ranking.setAreaName(entry.getKey());
            ranking.setValue(entry.getValue().doubleValue());
            ranking.setRanking(i + 1);
            ranking.setRankingType("total");
            rankings.add(ranking);
        }

        return rankings;
    }

    /**
     * 计算各院区提案参与率排名
     */
    private List<DashboardStatsDTO.AreaRanking> calculateAreaRankingByParticipation(LocalDateTime startTime,
                                                                                LocalDateTime endTime) {
        Map<String, Set<String>> areaEmployeesMap = new HashMap<>();
        List<ProcessInstance> instances = getAllInstancesByTimeRange(startTime, endTime, null, null);

        // 按院区统计员工参与情况
        instances.forEach(instance -> {
            String deptName = instance.getOriginatorDeptName();
            String area = extractAreaFromDepartment(deptName);
            String userId = instance.getOriginatorUserid();

            if (StringUtils.hasText(area) && StringUtils.hasText(userId)) {
                areaEmployeesMap.computeIfAbsent(area, k -> new HashSet<>()).add(userId);
            }
        });

        // 计算参与率并排名
        List<DashboardStatsDTO.AreaRanking> rankings = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : areaEmployeesMap.entrySet()) {
            double participationRate = (double) entry.getValue().size() / getTotalEmployeesByArea(entry.getKey()) * 100;
            DashboardStatsDTO.AreaRanking ranking = new DashboardStatsDTO.AreaRanking();
            ranking.setAreaName(entry.getKey());
            ranking.setValue(participationRate);
            ranking.setRankingType("participation");
            rankings.add(ranking);
        }

        // 按参与率降序排序
        rankings.sort((r1, r2) -> Double.compare(r2.getValue(), r1.getValue()));

        // 设置排名
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRanking(i + 1);
        }

        return rankings;
    }

    /**
     * 计算区域提案数排名
     */
    private List<DashboardStatsDTO.RegionRanking> calculateRegionRanking(LocalDateTime startTime,
                                                                      LocalDateTime endTime) {
        Map<String, Long> regionCountMap = new HashMap<>();
        List<ProcessInstance> instances = getAllInstancesByTimeRange(startTime, endTime, null, null);

        // 按区域统计提案数
        instances.forEach(instance -> {
            String deptName = instance.getOriginatorDeptName();
            String region = extractRegionFromDepartment(deptName);
            if (StringUtils.hasText(region)) {
                regionCountMap.put(region, regionCountMap.getOrDefault(region, 0L) + 1);
            }
        });

        // 排名并转换为DTO
        List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(regionCountMap.entrySet());
        sortedEntries.sort(Map.Entry.<String, Long>comparingByValue().reversed());

        List<DashboardStatsDTO.RegionRanking> rankings = new ArrayList<>();
        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<String, Long> entry = sortedEntries.get(i);
            DashboardStatsDTO.RegionRanking ranking = new DashboardStatsDTO.RegionRanking();
            ranking.setRegionName(entry.getKey());
            ranking.setProposalCount(entry.getValue());
            ranking.setRanking(i + 1);
            rankings.add(ranking);
        }

        return rankings;
    }

    // ==================== 提案管理相关方法 ====================

    @Override
    public ProposalListDTO getProposalList(ProposalQueryDTO queryDTO) {
        log.info("查询提案列表，页码: {}, 每页大小: {}, 条件: {}",
                queryDTO.getCurrent(), queryDTO.getSize(), queryDTO);

        ProposalListDTO result = new ProposalListDTO();
        result.setCurrent(queryDTO.getCurrent());
        result.setSize(queryDTO.getSize());

        try {
            // 获取所有符合条件的流程实例
            List<ProcessInstance> instances = getFilteredInstances(queryDTO);

            // 分页处理
            int total = instances.size();
            result.setTotal((long) total);

            int pages = (int) Math.ceil((double) total / queryDTO.getSize());
            result.setPages(pages);

            // 分页截取
            int startIndex = (queryDTO.getCurrent() - 1) * queryDTO.getSize();
            int endIndex = Math.min(startIndex + queryDTO.getSize(), total);

            List<ProcessInstance> pageInstances = instances.subList(startIndex, endIndex);

            // 转换为提案DTO
            List<ProposalItem> proposalItems = pageInstances.stream()
                .map(this::convertToProposalItem)
                .collect(Collectors.toList());

            result.setRecords(proposalItems);

        } catch (Exception e) {
            log.error("查询提案列表失败", e);
            result.setRecords(new ArrayList<>());
            result.setTotal(0L);
            result.setPages(0);
        }

        return result;
    }

    @Override
    public ProposalItem getProposalDetail(String processInstanceId) {
        log.info("获取提案详情: {}", processInstanceId);

        try {
            // 获取流程实例
            ProcessInstance instance = processInstanceService.getByProcessInstanceId(processInstanceId);
            if (instance == null) {
                log.warn("未找到流程实例: {}", processInstanceId);
                return null;
            }

            // 转换为提案详情
            return convertToProposalItem(instance);

        } catch (Exception e) {
            log.error("获取提案详情失败: {}", processInstanceId, e);
            return null;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据时间范围获取流程实例
     */
    private List<ProcessInstance> getAllInstancesByTimeRange(LocalDateTime startTime, LocalDateTime endTime,
                                                            String area, String region) {
        // 获取所有状态的实例
        List<ProcessInstance> allInstances = new ArrayList<>();
        String[] statuses = {"RUNNING", "COMPLETED", "TERMINATED", "CANCELED"};

        for (String status : statuses) {
            allInstances.addAll(processInstanceService.listByStatusAndTime(status, startTime, endTime));
        }

        // 如果指定了院区或区域，进行过滤
        if (StringUtils.hasText(area) || StringUtils.hasText(region)) {
            return allInstances.stream()
                .filter(instance -> {
                    String deptName = instance.getOriginatorDeptName();
                    String instanceArea = extractAreaFromDepartment(deptName);
                    String instanceRegion = extractRegionFromDepartment(deptName);

                    boolean areaMatch = !StringUtils.hasText(area) || area.equals(instanceArea);
                    boolean regionMatch = !StringUtils.hasText(region) || region.equals(instanceRegion);

                    return areaMatch && regionMatch;
                })
                .collect(Collectors.toList());
        }

        return allInstances;
    }

    /**
     * 根据状态获取流程实例
     */
    private List<ProcessInstance> getInstancesByStatus(String status, LocalDateTime startTime, LocalDateTime endTime,
                                                      String area, String region) {
        List<ProcessInstance> instances = processInstanceService.listByStatusAndTime(status, startTime, endTime);

        // 如果指定了院区或区域，进行过滤
        if (StringUtils.hasText(area) || StringUtils.hasText(region)) {
            return instances.stream()
                .filter(instance -> {
                    String deptName = instance.getOriginatorDeptName();
                    String instanceArea = extractAreaFromDepartment(deptName);
                    String instanceRegion = extractRegionFromDepartment(deptName);

                    boolean areaMatch = !StringUtils.hasText(area) || area.equals(instanceArea);
                    boolean regionMatch = !StringUtils.hasText(region) || region.equals(instanceRegion);

                    return areaMatch && regionMatch;
                })
                .collect(Collectors.toList());
        }

        return instances;
    }

    /**
     * 根据查询条件获取过滤后的流程实例
     */
    private List<ProcessInstance> getFilteredInstances(ProposalQueryDTO queryDTO) {
        List<ProcessInstance> allInstances = new ArrayList<>();

        // 根据时间范围获取数据
        LocalDateTime startTime = queryDTO.getStartTime();
        LocalDateTime endTime = queryDTO.getEndTime();

        if (startTime == null) {
            // 默认查询最近30天的数据
            startTime = LocalDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        // 获取所有状态的实例
        String[] statuses = {"RUNNING", "COMPLETED", "TERMINATED", "CANCELED"};
        for (String status : statuses) {
            // 如果指定了状态，只查询指定状态
            if (StringUtils.hasText(queryDTO.getStatus()) && !queryDTO.getStatus().equals(status)) {
                continue;
            }
            allInstances.addAll(processInstanceService.listByStatusAndTime(status, startTime, endTime));
        }

        // 应用过滤条件
        return allInstances.stream()
            .filter(instance -> applyFilters(instance, queryDTO))
            .sorted((i1, i2) -> compareBySort(i1, i2, queryDTO.getSortField(), queryDTO.getSortOrder()))
            .collect(Collectors.toList());
    }

    /**
     * 应用过滤条件
     */
    private boolean applyFilters(ProcessInstance instance, ProposalQueryDTO queryDTO) {
        // 提案人姓名过滤
        if (StringUtils.hasText(queryDTO.getProposerName())) {
            // 这里需要从表单组件值中获取真实姓名
            // 暂时使用用户ID匹配
            if (!queryDTO.getProposerName().equals(instance.getOriginatorUserid())) {
                return false;
            }
        }

        // 部门过滤
        if (StringUtils.hasText(queryDTO.getDepartment())) {
            if (!queryDTO.getDepartment().equals(instance.getOriginatorDeptName())) {
                return false;
            }
        }

        // 院区过滤
        if (StringUtils.hasText(queryDTO.getArea())) {
            String instanceArea = extractAreaFromDepartment(instance.getOriginatorDeptName());
            if (!queryDTO.getArea().equals(instanceArea)) {
                return false;
            }
        }

        // 区域过滤
        if (StringUtils.hasText(queryDTO.getRegion())) {
            String instanceRegion = extractRegionFromDepartment(instance.getOriginatorDeptName());
            if (!queryDTO.getRegion().equals(instanceRegion)) {
                return false;
            }
        }

        // 等级过滤（需要从表单组件值中获取）
        if (StringUtils.hasText(queryDTO.getLevel())) {
            // TODO: 从表单组件值中获取等级信息进行匹配
        }

        // 类别过滤（需要从表单组件值中获取）
        if (StringUtils.hasText(queryDTO.getCategory())) {
            // TODO: 从表单组件值中获取类别信息进行匹配
        }

        // 改善等级过滤（需要从表单组件值中获取）
        if (StringUtils.hasText(queryDTO.getImprovementLevel())) {
            // TODO: 从表单组件值中获取改善等级信息进行匹配
        }

        // 关键字搜索
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            // 搜索标题和业务ID
            boolean keywordMatch =
                (StringUtils.hasText(instance.getTitle()) &&
                 instance.getTitle().contains(queryDTO.getKeyword())) ||
                (StringUtils.hasText(instance.getBusinessId()) &&
                 instance.getBusinessId().contains(queryDTO.getKeyword()));

            // TODO: 还可以搜索表单组件值中的问题描述和解决方案

            if (!keywordMatch) {
                return false;
            }
        }

        return true;
    }

    /**
     * 根据排序字段和方向进行比较
     */
    private int compareBySort(ProcessInstance i1, ProcessInstance i2, String sortField, String sortOrder) {
        int comparison = 0;

        switch (sortField) {
            case "createTime":
                comparison = i1.getCreateTime().compareTo(i2.getCreateTime());
                break;
            case "finishTime":
                if (i1.getFinishTime() == null && i2.getFinishTime() == null) {
                    comparison = 0;
                } else if (i1.getFinishTime() == null) {
                    comparison = 1;
                } else if (i2.getFinishTime() == null) {
                    comparison = -1;
                } else {
                    comparison = i1.getFinishTime().compareTo(i2.getFinishTime());
                }
                break;
            case "expectedBenefit":
                // TODO: 需要从表单组件值中获取预期效益进行比较
                break;
            default:
                comparison = i1.getCreateTime().compareTo(i2.getCreateTime());
                break;
        }

        // 如果是升序，返回比较结果；如果是降序，返回相反结果
        return "asc".equals(sortOrder) ? comparison : -comparison;
    }

    /**
     * 转换流程实例为提案项目
     */
    private ProposalItem convertToProposalItem(ProcessInstance instance) {
        ProposalItem item = new ProposalItem();

        // 基本信息
        item.setProcessInstanceId(instance.getProcessInstanceId());
        item.setTitle(instance.getTitle());
        item.setBusinessId(instance.getBusinessId());
        item.setStatus(instance.getStatus());
        // item.setResult() - ProposalItem中没有这个方法，移除
        item.setCreateTime(instance.getCreateTime());
        item.setFinishTime(instance.getFinishTime());

        // 提案人信息
        item.setProposerId(instance.getOriginatorUserid());
        item.setProposerDepartment(instance.getOriginatorDeptName());

        // 提取院区和区域
        item.setArea(extractAreaFromDepartment(instance.getOriginatorDeptName()));
        item.setRegion(extractRegionFromDepartment(instance.getOriginatorDeptName()));

        // 从表单组件值中获取详细信息
        List<FormComponentValue> formValues = formComponentValueService.listByProcessInstanceId(instance.getProcessInstanceId());

        for (FormComponentValue fcv : formValues) {
            String componentName = fcv.getComponentName();
            String value = fcv.getValue();

            if (!StringUtils.hasText(value)) {
                continue;
            }

            switch (componentName) {
                case "姓名":
                case "提案人":
                    item.setProposerName(value);
                    break;
                case "问题描述":
                    item.setProblemDescription(value);
                    break;
                case "解决方案":
                case "改善措施":
                    item.setSolution(value);
                    break;
                case "等级":
                case "提案等级":
                    item.setLevel(value);
                    break;
                case "预计产生经济效益(元)":
                    try {
                        item.setExpectedBenefit(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        item.setExpectedBenefit(0.0);
                    }
                    break;
                case "提案类别":
                case "类别":
                    item.setCategory(value);
                    break;
                case "改善等级":
                    item.setImprovementLevel(value);
                    break;
                case "当前节点":
                    item.setCurrentNode(value);
                    break;
                case "点赞量":
                    try {
                        item.setLikeCount(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        item.setLikeCount(0);
                    }
                    break;
                case "评论":
                    item.setComments(value);
                    break;
                default:
                    // 记录未处理的字段，便于调试
                    log.debug("未处理的表单字段: {} = {}", componentName, value);
                    break;
            }
        }

        // 设置默认值
        if (item.getProposerName() == null) {
            item.setProposerName(item.getProposerId());
        }
        if (item.getCurrentNode() == null) {
            item.setCurrentNode(getCurrentNodeByStatus(item.getStatus()));
        }
        if (item.getLikeCount() == null) {
            item.setLikeCount(0);
        }

        // 设置关闭日期
        if ("COMPLETED".equals(item.getStatus()) && item.getFinishTime() != null) {
            item.setCloseDate(item.getFinishTime());
        }

        // 设置提交时间
        item.setSubmitTime(item.getCreateTime());

        return item;
    }

    /**
     * 根据状态获取当前节点
     */
    private String getCurrentNodeByStatus(String status) {
        switch (status) {
            case "RUNNING":
                return "审批中";
            case "COMPLETED":
                return "已完成";
            case "TERMINATED":
                return "已终止";
            case "CANCELED":
                return "已取消";
            default:
                return status;
        }
    }

    /**
     * 从部门名称中提取院区
     */
    private String extractAreaFromDepartment(String deptName) {
        if (!StringUtils.hasText(deptName)) {
            return "";
        }

        // 假设部门名称格式为 "院区-部门" 或类似格式
        String[] parts = deptName.split("-");
        return parts.length > 1 ? parts[0] : deptName;
    }

    /**
     * 从部门名称中提取区域
     */
    private String extractRegionFromDepartment(String deptName) {
        if (!StringUtils.hasText(deptName)) {
            return "";
        }

        // 这里可以根据实际的部门命名规则来提取区域
        // 暂时返回院区作为区域
        return extractAreaFromDepartment(deptName);
    }

    /**
     * 获取指定院区的总员工数（这里需要从实际员工管理系统获取）
     */
    private int getTotalEmployeesByArea(String area) {
        // 暂时返回提案员工数作为基准，实际应用中应该从员工管理系统获取
        return 100; // 示例值
    }
}