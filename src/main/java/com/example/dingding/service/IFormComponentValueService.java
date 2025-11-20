package com.example.dingding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dingding.entity.FormComponentValue;
import com.example.dingding.mapper.FormComponentValueMapper;

import java.util.List;

/**
 * 表单组件值服务接口
 *
 * @author system
 * @version 1.0.0
 */
public interface IFormComponentValueService extends IService<FormComponentValue> {

    /**
     * 根据流程实例ID和组件名称查询
     *
     * @param processInstanceId 流程实例ID
     * @param componentName     组件名称
     * @return 表单组件值
     */
    FormComponentValue getByProcessInstanceAndComponentName(String processInstanceId, String componentName);

    /**
     * 根据流程实例ID列表批量查询表单组件值
     *
     * @param processInstanceIds 流程实例ID列表
     * @param componentName      组件名称
     * @return 表单组件值列表
     */
    List<FormComponentValue> listByProcessInstanceIdsAndComponentName(List<String> processInstanceIds, String componentName);

    /**
     * 统计改善等级分布
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 改善等级统计列表
     */
    List<FormComponentValueMapper.ImprovementLevelStats> getImprovementLevelStats(String startTime, String endTime);

    /**
     * 统计提案类别分布
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 提案类别统计列表
     */
    List<FormComponentValueMapper.ProposalCategoryStats> getProposalCategoryStats(String startTime, String endTime);

    /**
     * 统计经济效益
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 经济效益统计列表
     */
    List<FormComponentValueMapper.EconomicBenefitStats> getEconomicBenefitStats(String startTime, String endTime);

    /**
     * 批量保存表单组件值
     *
     * @param formComponentValues 表单组件值列表
     * @return 是否成功
     */
    boolean saveBatch(List<FormComponentValue> formComponentValues);
}