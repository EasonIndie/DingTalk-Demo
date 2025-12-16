import com.example.dingding.dto.DepartmentDTO;
import com.example.dingding.dto.DepartmentSyncResultDTO;
import com.example.dingding.entity.DepartmentSCD2;
import com.example.dingding.service.IDepartmentSCD2Service;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 部门SCD2服务测试类
 *
 * @author system
 * @version 1.0.0
 */
@SpringBootTest(classes = com.example.dingding.DingdingDemoApplication.class)
@ActiveProfiles("test")
@Transactional
public class DepartmentSCD2ServiceTest {

    @Resource
    private IDepartmentSCD2Service departmentSCD2Service;

    @Test
    public void testSyncNewDepartment() {
        // 准备测试数据
        List<DepartmentDTO> departments = Arrays.asList(
            new DepartmentDTO()
                .setDeptId(100L)
                .setParentId(1L)
                .setName("测试部门1")
        );

        LocalDate syncDate = LocalDate.now();

        // 执行同步
        DepartmentSyncResultDTO result = departmentSCD2Service.syncDepartments(departments, syncDate);

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getNewCount());
        assertEquals(0, result.getChangedCount());
        assertEquals(0, result.getUnchangedCount());

        // 验证数据库中的数据
        DepartmentSCD2 saved = departmentSCD2Service.findCurrentByDeptId(100L);
        assertNotNull(saved);
        assertEquals(100L, saved.getDeptId());
        assertEquals(1L, saved.getParentId());
        assertEquals("测试部门1", saved.getName());
        assertTrue(saved.isCurrentVersion());
        assertEquals(syncDate, saved.getValidFrom());
        assertEquals(LocalDate.of(9999, 12, 31), saved.getValidTo());
    }

    @Test
    public void testSyncUnchangedDepartment() {
        LocalDate syncDate = LocalDate.now();

        // 第一次同步，创建部门
        DepartmentDTO dept = new DepartmentDTO()
            .setDeptId(200L)
            .setParentId(1L)
            .setName("测试部门2");

        departmentSCD2Service.insertNewVersion(dept, syncDate);

        // 第二次同步，相同数据
        List<DepartmentDTO> departments = Arrays.asList(dept);
        DepartmentSyncResultDTO result = departmentSCD2Service.syncDepartments(departments, syncDate);

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getNewCount());
        assertEquals(0, result.getChangedCount());
        assertEquals(1, result.getUnchangedCount());
    }

    @Test
    public void testSyncChangedDepartment() {
        LocalDate syncDate1 = LocalDate.now().minusDays(1);
        LocalDate syncDate2 = LocalDate.now();

        // 创建初始版本
        DepartmentDTO deptV1 = new DepartmentDTO()
            .setDeptId(300L)
            .setParentId(1L)
            .setName("原始部门名");

        departmentSCD2Service.insertNewVersion(deptV1, syncDate1);

        // 修改部门信息后再次同步
        DepartmentDTO deptV2 = new DepartmentDTO()
            .setDeptId(300L)
            .setParentId(2L)  // 父部门变更
            .setName("新部门名");  // 名称变更

        List<DepartmentDTO> departments = Arrays.asList(deptV2);
        DepartmentSyncResultDTO result = departmentSCD2Service.syncDepartments(departments, syncDate2);

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getNewCount());
        assertEquals(1, result.getChangedCount());
        assertEquals(0, result.getUnchangedCount());

        // 验证当前版本
        DepartmentSCD2 current = departmentSCD2Service.findCurrentByDeptId(300L);
        assertNotNull(current);
        assertEquals("新部门名", current.getName());
        assertEquals(2L, current.getParentId());
        assertEquals(syncDate2, current.getValidFrom());

        // 验证历史版本
        List<DepartmentSCD2> history = departmentSCD2Service.findAllVersionsByDeptId(300L);
        assertEquals(2, history.size());

        // 第一个版本应该已被关闭
        DepartmentSCD2 oldVersion = history.stream()
            .filter(d -> !d.isCurrentVersion())
            .findFirst()
            .orElse(null);
        assertNotNull(oldVersion);
        assertEquals("原始部门名", oldVersion.getName());
        assertEquals(1L, oldVersion.getParentId());
        assertEquals(syncDate1.minusDays(1), oldVersion.getValidTo()); // closed day before
    }

    @Test
    public void testHasChanged() {
        DepartmentSCD2 current = new DepartmentSCD2()
            .setDeptId(400L)
            .setParentId(1L)
            .setName("部门名");

        // 测试无变化
        DepartmentDTO unchanged = new DepartmentDTO()
            .setDeptId(400L)
            .setParentId(1L)
            .setName("部门名");
        assertFalse(departmentSCD2Service.hasChanged(current, unchanged));

        // 测试名称变化
        DepartmentDTO nameChanged = new DepartmentDTO()
            .setDeptId(400L)
            .setParentId(1L)
            .setName("新名称");
        assertTrue(departmentSCD2Service.hasChanged(current, nameChanged));

        // 测试父部门变化
        DepartmentDTO parentChanged = new DepartmentDTO()
            .setDeptId(400L)
            .setParentId(2L)
            .setName("部门名");
        assertTrue(departmentSCD2Service.hasChanged(current, parentChanged));

        // 测试null值处理
        DepartmentDTO nullParent = new DepartmentDTO()
            .setDeptId(400L)
            .setParentId(null)
            .setName("部门名");
        assertTrue(departmentSCD2Service.hasChanged(current, nullParent));
    }

    @Test
    public void testCanCreateVersionOnDate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 创建初始版本
        DepartmentDTO dept = new DepartmentDTO()
            .setDeptId(500L)
            .setParentId(1L)
            .setName("测试部门");

        departmentSCD2Service.insertNewVersion(dept, yesterday);

        // 同一天不能再次创建版本
        assertFalse(departmentSCD2Service.canCreateVersionOnDate(500L, yesterday));

        // 不同日期可以创建版本
        assertTrue(departmentSCD2Service.canCreateVersionOnDate(500L, today));
    }

    @Test
    public void testHandleDeletedDepartments() {
        LocalDate syncDate = LocalDate.now();

        // 创建几个部门
        departmentSCD2Service.insertNewVersion(
            new DepartmentDTO().setDeptId(600L).setName("部门1"),
            syncDate
        );
        departmentSCD2Service.insertNewVersion(
            new DepartmentDTO().setDeptId(601L).setName("部门2"),
            syncDate
        );
        departmentSCD2Service.insertNewVersion(
            new DepartmentDTO().setDeptId(602L).setName("部门3"),
            syncDate
        );

        // 模拟部门602被删除
        List<Long> activeDeptIds = Arrays.asList(600L, 601L);
        int closedCount = departmentSCD2Service.handleDeletedDepartments(activeDeptIds, syncDate.plusDays(1));

        // 验证只有一个部门被关闭
        assertEquals(1, closedCount);

        // 验证部门602的当前版本已被关闭
        DepartmentSCD2 deletedDept = departmentSCD2Service.findCurrentByDeptId(602L);
        assertNull(deletedDept); // 当前版本应该不存在
    }
}