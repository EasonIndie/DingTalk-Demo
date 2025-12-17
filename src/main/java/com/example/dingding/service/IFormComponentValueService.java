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
     * 根据流程实例ID查询表单组件值
     *
     * @param processInstanceId 流程实例ID
     * @return 表单组件值列表
     */
    List<FormComponentValue> listByProcessInstanceId(String processInstanceId);

    /**
     * 批量保存表单组件值
     *
     * @param formComponentValues 表单组件值列表
     * @return 是否成功
     */
    boolean saveBatch(List<FormComponentValue> formComponentValues);
}