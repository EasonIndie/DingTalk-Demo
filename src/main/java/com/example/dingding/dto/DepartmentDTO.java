package com.example.dingding.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 部门数据传输对象
 * 用于从钉钉API获取的部门信息和内部数据处理
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Accessors(chain = true)
public class DepartmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 从钉钉API响应创建DepartmentDTO
     */
    public static DepartmentDTO fromDingTalkResponse(com.dingtalk.api.response.OapiV2DepartmentListsubResponse.DeptBaseResponse response) {
        if (response == null) {
            return null;
        }

        return new DepartmentDTO()
                .setDeptId(response.getDeptId())
                .setParentId(response.getParentId())
                .setName(response.getName());
    }

    /**
     * 便利方法：检查是否为根部门
     */
    public boolean isRootDepartment() {
        // 钉钉中根部门的parent_id为1或null
        return parentId == null || parentId == 1L;
    }

    /**
     * 便利方法：规范化父部门ID
     * 将null转换为1（根部门标识）
     */
    public Long getNormalizedParentId() {
        return isRootDepartment() ? 1L : parentId;
    }
}