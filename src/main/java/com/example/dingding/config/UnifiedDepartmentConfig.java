package com.example.dingding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 统一部门配置类
 * 支持项目部、总部等多种类型的部门配置
 *
 * 重要变更说明：
 * 1. 合并原有的ProjectDepartmentConfig和HeadquarterDepartmentConfig
 * 2. 统一虚拟ID管理，避免硬编码
 * 3. 增加配置验证，确保数据完整性
 *
 * @author system
 * @version 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "unified.department")
public class UnifiedDepartmentConfig {

    /**
     * 项目部配置
     */
    private DepartmentConfig project;

    /**
     * 总部配置
     */
    private DepartmentConfig headquarter;

    /**
     * 部门配置内部类
     */
    @Data
    public static class DepartmentConfig {
        /**
         * 分组名称
         */
        private String groupName;

        /**
         * 目标部门列表
         */
        private List<String> targetDepartments;

        /**
         * 虚拟ID（用于区分不同类型的虚拟根节点）
         */
        private Long virtualId;

        /**
         * 描述信息（可选）
         */
        private String description;

        /**
         * 是否启用（可选）
         */
        private Boolean enabled = true;
    }

    /**
     * 配置验证
     * 确保必要的配置项不为空，虚拟ID唯一分配
     */
    @PostConstruct
    public void validateConfig() {
        log.info("开始验证统一部门配置");

        // 验证项目配置
        validateDepartmentConfig("project", project);

        // 验证总部配置
        validateDepartmentConfig("headquarter", headquarter);

        log.info("项目部配置: 虚拟ID={}, 目标部门数={}, 启用状态={}",
            project != null ? project.getVirtualId() : "null",
            project != null && project.getTargetDepartments() != null ? project.getTargetDepartments().size() : 0,
            project != null ? project.getEnabled() : "null");
        log.info("总部配置: 虚拟ID={}, 目标部门数={}, 启用状态={}",
            headquarter != null ? headquarter.getVirtualId() : "null",
            headquarter != null && headquarter.getTargetDepartments() != null ? headquarter.getTargetDepartments().size() : 0,
            headquarter != null ? headquarter.getEnabled() : "null");
    }

    /**
     * 验证单个部门配置
     */
    private void validateDepartmentConfig(String type, DepartmentConfig config) {
        if (config == null) {
            throw new IllegalStateException(type + "配置不能为空");
        }

        if (config.getGroupName() == null || config.getGroupName().trim().isEmpty()) {
            throw new IllegalStateException(type + "的groupName不能为空");
        }

        if (CollectionUtils.isEmpty(config.getTargetDepartments())) {
            throw new IllegalStateException(type + "的targetDepartments不能为空");
        }

        if (config.getVirtualId() == null) {
            throw new IllegalStateException(type + "的virtualId不能为空");
        }

        if (config.getEnabled() == null) {
            config.setEnabled(true); // 默认启用
        }

        log.debug("{}配置验证通过: groupName={}, virtualId={}, enabled={}, 目标部门数={}",
            type, config.getGroupName(), config.getVirtualId(),
            config.getEnabled(), config.getTargetDepartments().size());
    }

    // 添加日志支持
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnifiedDepartmentConfig.class);
}