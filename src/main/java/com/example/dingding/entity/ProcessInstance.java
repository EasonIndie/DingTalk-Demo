package com.example.dingding.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.dingding.handler.JsonListTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例实体类
 *
 * @author system
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ding_process_instances")
public class ProcessInstance {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程实例ID
     */
    @TableField("process_instance_id")
    private String processInstanceId;

    /**
     * 流程模板ID
     */
    @TableField("process_code")
    private String processCode;

    /**
     * 审批单标题
     */
    @TableField("title")
    private String title;

    /**
     * 审批单编号
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 发起人用户ID
     */
    @TableField("originator_userid")
    private String originatorUserid;

    /**
     * 发起人用户ID
     */
    @TableField("employee_title")
    private String employeeTitle;

    /**
     * 发起人部门ID
     */
    @TableField("originator_dept_id")
    private String originatorDeptId;

    /**
     * 发起人部门名称
     */
    @TableField("originator_dept_name")
    private String originatorDeptName;

    /**
     * 状态(RUNNING/TERMINATED/COMPLETED/CANCELED)
     */
    @TableField("status")
    private String status;

    /**
     * 审批结果(agree/refuse)
     */
    @TableField("result")
    private String result;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    @TableField("finish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /**
     * 附属单信息
     */
    @TableField(value = "attached_process_instance_ids", typeHandler = JsonListTypeHandler.class)
    private List<String> attachedProcessInstanceIds;

    /**
     * 抄送用户ID列表
     */
    @TableField(value = "cc_userids", typeHandler = JsonListTypeHandler.class)
    private List<String> ccUserids;

    /**
     * 业务动作
     */
    @TableField("biz_action")
    private String bizAction;

    /**
     * 业务数据
     */
    @TableField("biz_data")
    private String bizData;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}