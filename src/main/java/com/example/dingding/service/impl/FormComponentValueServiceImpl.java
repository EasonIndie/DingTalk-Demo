package com.example.dingding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dingding.entity.FormComponentValue;
import com.example.dingding.mapper.FormComponentValueMapper;
import com.example.dingding.service.IFormComponentValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单组件值服务实现类
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
public class FormComponentValueServiceImpl extends ServiceImpl<FormComponentValueMapper, FormComponentValue>
        implements IFormComponentValueService {

    @Override
    public FormComponentValue getByProcessInstanceAndComponentName(String processInstanceId, String componentName) {
        return baseMapper.selectByProcessInstanceAndComponentName(processInstanceId, componentName);
    }

    @Override
    public List<FormComponentValue> listByProcessInstanceIdsAndComponentName(List<String> processInstanceIds, String componentName) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return new ArrayList<>();
        }
        return baseMapper.selectByProcessInstanceIdsAndComponentName(processInstanceIds, componentName);
    }

    @Override
    public List<FormComponentValueMapper.ImprovementLevelStats> getImprovementLevelStats(String startTime, String endTime) {
        return baseMapper.selectImprovementLevelStats(startTime, endTime);
    }

    @Override
    public List<FormComponentValueMapper.ProposalCategoryStats> getProposalCategoryStats(String startTime, String endTime) {
        return baseMapper.selectProposalCategoryStats(startTime, endTime);
    }

    @Override
    public List<FormComponentValueMapper.EconomicBenefitStats> getEconomicBenefitStats(String startTime, String endTime) {
        return baseMapper.selectEconomicBenefitStats(startTime, endTime);
    }

    @Override
    public boolean saveBatch(List<FormComponentValue> formComponentValues) {
        if (formComponentValues == null || formComponentValues.isEmpty()) {
            return true;
        }
        try {
            // 先删除已存在的记录，避免唯一键冲突
            List<String> processInstanceIds = formComponentValues.stream()
                .map(FormComponentValue::getProcessInstanceId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

            if (!processInstanceIds.isEmpty()) {
                baseMapper.deleteByProcessInstanceIds(processInstanceIds);
            }

            // 再批量插入新记录
            return saveBatch(formComponentValues, 100); // 每批100条
        } catch (Exception e) {
            log.error("批量保存表单组件值失败, 数量: {}", formComponentValues.size(), e);
            return false;
        }
    }
}