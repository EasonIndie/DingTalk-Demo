package com.example.dingding.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Excel报表服务
 *
 * @author system
 * @version 1.0.0
 */
@Service
public class ExcelService {

    /**
     * 生成精益数据统计Excel报表
     *
     * @param outputPath 输出路径
     * @return 是否成功
     */
    public boolean generateLeanReport(String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {

            // 创建三个Sheet页
            createOverallSheet(workbook);
            createDepartmentSheet(workbook);
            createPersonRankingSheet(workbook);

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建全员统计Sheet
     */
    private void createOverallSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("指标查看-全员");

        // 创建标题样式
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);

        // 创建标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("精益数据统计 - 全员指标");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

        // 创建表头
        Row headerRow = sheet.createRow(2);
        String[] headers = {"指标名称", "数值", "单位", "环比", "目标值", "达标率"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 创建数据行
        Object[][] data = {
                {"本月合理化建议提案数", "0", "个", "↑5%", "50", "100%"},
                {"参与提案人数", "0", "人", "↑3%", "30", "100%"},
                {"结案提案数", "0", "个", "↑8%", "40", "100%"},
                {"结案率", "0%", "%", "↑2%", "80%", "100%"},
                {"平均处理天数", "0", "天", "↓1%", "5", "100%"},
                {"产生经济效益", "0", "元", "↑15%", "10000", "100%"},
                {"节约金额", "0", "元", "↑12%", "8000", "100%"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 3);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue(String.valueOf(data[i][j]));
            }
        }

        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建部门管理Sheet
     */
    private void createDepartmentSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("项目管理-职能部门");

        // 创建样式
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);

        // 创建标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("精益数据统计 - 部门管理");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8));

        // 创建表头
        Row headerRow = sheet.createRow(2);
        String[] headers = {"部门名称", "改善项目数", "A级项目", "B级项目", "C级项目", "实施中", "已完成", "结案率", "人均件数"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 示例数据
        Object[][] data = {
                {"信息中心-开发部", "15", "3", "8", "4", "5", "10", "66.7%", "1.5"},
                {"精益部", "12", "2", "7", "3", "4", "8", "66.7%", "1.3"},
                {"生产部", "8", "1", "5", "2", "2", "6", "75.0%", "1.0"},
                {"质量部", "10", "2", "6", "2", "3", "7", "70.0%", "1.2"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 3);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue(String.valueOf(data[i][j]));
            }
        }

        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建个人排行Sheet
     */
    private void createPersonRankingSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("个人统计");

        // 创建样式
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);

        // 创建标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("精益数据统计 - 个人排行");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

        // 创建表头
        Row headerRow = sheet.createRow(2);
        String[] headers = {"排名", "姓名", "部门", "参与提案数", "采纳提案数", "采纳率", "经济效益(元)", "奖励积分"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 示例数据
        Object[][] data = {
                {"1", "张三", "信息中心-开发部", "5", "4", "80%", "5000", "45"},
                {"2", "李四", "精益部", "4", "3", "75%", "3000", "35"},
                {"3", "王五", "生产部", "3", "2", "67%", "2000", "25"},
                {"4", "赵六", "质量部", "3", "2", "67%", "1800", "23"},
                {"5", "钱七", "信息中心-开发部", "2", "2", "100%", "1500", "20"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 3);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue(String.valueOf(data[i][j]));
            }
        }

        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建标题样式
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}