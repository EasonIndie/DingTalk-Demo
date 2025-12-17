package com.example.dingding.controller;

import com.example.dingding.entity.DepartmentGroup;
import com.example.dingding.service.DepartmentGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门统计维度控制器
 * 提供REST API接口进行部门统计数据的操作
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/department-group")
public class DepartmentGroupController {

    @Autowired
    private DepartmentGroupService departmentGroupService;

    /**
     * 生成项目部统计数据（全量生成，包含区域管理部和项目部）
     */
    @PostMapping("/generateProjectDepartments")
    public ResponseEntity<Map<String, Object>> generateProjectDepartments() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("接收到生成部门统计数据的请求");

            // 调用完整的重新生成方法（包含区域管理部和项目部）
            int count = departmentGroupService.truncateAndRegenerate();

            result.put("success", true);
            result.put("message", "统计数据生成成功（包含区域管理部和项目部）");
            result.put("count", count);

            log.info("成功生成 {} 条统计数据", count);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("生成统计数据失败", e);

            result.put("success", false);
            result.put("message", "生成失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取树形结构数据
     */
    @GetMapping("/tree")
    public ResponseEntity<List<Map<String, Object>>> getTreeStructure() {
        try {
            List<Map<String, Object>> treeStructure = departmentGroupService.getTreeStructure();
            log.info("获取到树形结构数据，共 {} 个根节点", treeStructure.size());
            return ResponseEntity.ok(treeStructure);

        } catch (Exception e) {
            log.error("获取树形结构数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}