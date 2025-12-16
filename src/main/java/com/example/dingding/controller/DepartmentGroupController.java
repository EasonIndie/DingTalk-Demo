package com.example.dingding.controller;

import com.example.dingding.entity.DepartmentGroup;
import com.example.dingding.enums.DepartmentGroupType;
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
     * 生成部门统计数据
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateDepartmentGroups() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("接收到生成部门统计数据的请求");
            int count = departmentGroupService.generateDepartmentGroups();

            result.put("success", true);
            result.put("message", "部门统计数据生成成功");
            result.put("count", count);

            log.info("成功生成 {} 条部门统计数据", count);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("生成部门统计数据失败", e);

            result.put("success", false);
            result.put("message", "生成失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 清理并重新生成数据
     */
    @PostMapping("/truncate-and-regenerate")
    public ResponseEntity<Map<String, Object>> truncateAndRegenerate() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("接收到清理并重新生成部门统计数据的请求");
            int count = departmentGroupService.truncateAndRegenerate();

            result.put("success", true);
            result.put("message", "清理并重新生成成功");
            result.put("count", count);

            log.info("成功清理并重新生成 {} 条部门统计数据", count);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("清理并重新生成部门统计数据失败", e);

            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 验证和检查数据
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateData() {
        try {
            log.info("接收到验证部门统计数据的请求");
            Map<String, Object> validationResult = departmentGroupService.validateAndCheck();

            return ResponseEntity.ok(validationResult);

        } catch (Exception e) {
            log.error("验证部门统计数据失败", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "验证失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 获取当前版本的所有部门分组
     */
    @GetMapping("/current")
    public ResponseEntity<List<DepartmentGroup>> getCurrentGroups() {
        try {
            List<DepartmentGroup> groups = departmentGroupService.getCurrentGroups();
            log.info("获取到 {} 条当前部门分组数据", groups.size());
            return ResponseEntity.ok(groups);

        } catch (Exception e) {
            log.error("获取当前部门分组失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据类型获取部门分组
     */
    @GetMapping("/type/{groupType}")
    public ResponseEntity<List<DepartmentGroup>> getGroupsByType(@PathVariable DepartmentGroupType groupType) {
        try {
            List<DepartmentGroup> groups = departmentGroupService.getGroupsByType(groupType);
            log.info("获取到 {} 条 {} 类型的部门分组数据", groups.size(), groupType);
            return ResponseEntity.ok(groups);

        } catch (Exception e) {
            log.error("根据类型获取部门分组失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取树形结构数据
     */
    @GetMapping("/tree")
    public ResponseEntity<List<Map<String, Object>>> getTreeStructure() {
        try {
            List<Map<String, Object>> treeStructure = departmentGroupService.getTreeStructure();
            log.info("获取到树形结构数据，共 {} 层", treeStructure.size());
            return ResponseEntity.ok(treeStructure);

        } catch (Exception e) {
            log.error("获取树形结构数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据部门ID查找对应的分组
     */
    @GetMapping("/dept/{deptId}")
    public ResponseEntity<DepartmentGroup> findByDeptId(@PathVariable Long deptId) {
        try {
            DepartmentGroup group = departmentGroupService.findByDeptId(deptId);

            if (group != null) {
                log.info("找到部门 {} 的分组信息", deptId);
                return ResponseEntity.ok(group);
            } else {
                log.warn("未找到部门 {} 的分组信息", deptId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("查找部门分组失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取数据分布统计
     */
    @GetMapping("/stats/distribution")
    public ResponseEntity<Map<String, Object>> getDistributionStats() {
        try {
            Map<String, Object> stats = departmentGroupService.getDistributionStats();
            log.info("获取数据分布统计成功");
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("获取数据分布统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 检查根节点是否存在
     */
    @GetMapping("/check-root/{rootDeptName}")
    public ResponseEntity<Map<String, Object>> checkRootNodeExists(@PathVariable String rootDeptName) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean exists = departmentGroupService.checkRootNodeExists(rootDeptName);

            result.put("rootDeptName", rootDeptName);
            result.put("exists", exists);
            result.put("message", exists ? "根节点存在" : "根节点不存在");

            log.info("检查根节点 '{}'，结果：{}", rootDeptName, exists ? "存在" : "不存在");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("检查根节点失败", e);

            result.put("exists", false);
            result.put("message", "检查失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 刷新指定部门的统计分组
     */
    @PostMapping("/refresh/{deptId}")
    public ResponseEntity<Map<String, Object>> refreshDepartmentGroup(@PathVariable Long deptId) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("接收到刷新部门 {} 统计分组的请求", deptId);
            int count = departmentGroupService.refreshDepartmentGroup(deptId);

            result.put("success", true);
            result.put("message", "刷新成功");
            result.put("count", count);

            log.info("成功刷新 {} 条部门统计分组数据", count);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("刷新部门统计分组失败", e);

            result.put("success", false);
            result.put("message", "刷新失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 执行完整的操作：清理 -> 生成 -> 验证
     */
    @PostMapping("/full-process")
    public ResponseEntity<Map<String, Object>> fullProcess() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始执行完整的部门统计数据处理流程");

            // 1. 清理并重新生成
            int generateCount = departmentGroupService.truncateAndRegenerate();
            result.put("generateCount", generateCount);

            // 2. 验证数据
            Map<String, Object> validationResult = departmentGroupService.validateAndCheck();
            result.put("validation", validationResult);

            // 3. 获取统计信息
            Map<String, Object> stats = departmentGroupService.getDistributionStats();
            result.put("stats", stats);

            result.put("success", true);
            result.put("message", "完整流程执行成功");

            log.info("完整流程执行成功，生成 {} 条数据", generateCount);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("执行完整流程失败", e);

            result.put("success", false);
            result.put("message", "流程执行失败：" + e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }
}