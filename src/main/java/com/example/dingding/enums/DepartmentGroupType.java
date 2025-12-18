package com.example.dingding.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门统计类型枚举
 * 定义统计维度表的分组类型
 *
 * @author system
 * @version 1.0.0
 */
public enum DepartmentGroupType {

    /**
     * 区域类型
     * 对应区域管理部的直接子节点
     */
    REGION("REGION", "区域"),

    /**
     * 部门类型
     * 对应区域的子节点
     */
    DEPARTMENT("DEPARTMENT", "部门");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    DepartmentGroupType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }



    /**
     * 根据代码获取枚举值
     *
     * @param code 代码
     * @return 枚举值
     */
    public static DepartmentGroupType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (DepartmentGroupType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown DepartmentGroupType code: " + code);
    }


    public static List<String> getAllCode() {
        List<String> descriptions = new ArrayList<>();
        for (DepartmentGroupType type : values()) {
            descriptions.add(type.getCode());
        }
        return descriptions;
    }
}