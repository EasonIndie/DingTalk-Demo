package com.example.dingding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 总部配置类
 * 用于定义需要重组的总部部门列表和目标结构
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "headquarter.department")
public class HeadquarterDepartmentConfig {

    /**
     * 总部根节点名称
     */
    private String groupName = "总部";

    /**
     * 需要包含在总部下的目标部门列表
     */
    private List<String> targetDepartments;
}