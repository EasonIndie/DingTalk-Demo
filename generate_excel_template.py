import pandas as pd
import xlsxwriter
from datetime import datetime, date, timedelta
import json

class LeanStatsExcelGenerator:
    def __init__(self):
        self.workbook = None
        self.formats = None

    def generate_monthly_report(self, year: int, month: int, output_path: str):
        """生成精益数据统计月度报告模板"""

        # 创建工作簿
        self.workbook = xlsxwriter.Workbook(output_path)
        self._setup_formats()

        # 计算月份的起止日期
        start_date = date(year, month, 1)
        if month == 12:
            end_date = date(year + 1, 1, 1) - timedelta(days=1)
        else:
            end_date = date(year, month + 1, 1) - timedelta(days=1)

        month_name = f"{year}年{month:02d}月"

        # 创建各个工作表
        self._create_overview_sheet(month_name)
        self._create_dept_stats_sheet(month_name)
        self._create_project_management_sheet(month_name)
        self._create_personal_stats_sheet(month_name)

        self.workbook.close()
        print(f"Excel模板已生成: {output_path}")

    def _setup_formats(self):
        """设置格式"""
        self.formats = {
            'title': self.workbook.add_format({
                'bold': True,
                'font_size': 16,
                'align': 'center',
                'valign': 'vcenter',
                'text_wrap': True
            }),
            'subtitle': self.workbook.add_format({
                'bold': True,
                'font_size': 14,
                'align': 'center',
                'valign': 'vcenter'
            }),
            'header': self.workbook.add_format({
                'bold': True,
                'bg_color': '#D9E1F2',
                'border': 1,
                'align': 'center',
                'valign': 'vcenter',
                'text_wrap': True
            }),
            'number': self.workbook.add_format({
                'align': 'center',
                'valign': 'vcenter',
                'border': 1
            }),
            'text': self.workbook.add_format({
                'align': 'left',
                'valign': 'vcenter',
                'border': 1,
                'text_wrap': True
            }),
            'percentage': self.workbook.add_format({
                'align': 'center',
                'valign': 'vcenter',
                'border': 1,
                'num_format': '0.00%'
            }),
            'highlight': self.workbook.add_format({
                'bg_color': '#FFFF00',
                'border': 1,
                'align': 'center',
                'valign': 'vcenter'
            })
        }

    def _create_overview_sheet(self, month_name: str):
        """创建指标查看-全员工作表"""
        worksheet = self.workbook.add_worksheet('指标查看-全员')

        # 设置列宽
        column_widths = [25, 15, 15, 15, 15, 15, 20, 15, 15, 15, 20, 30, 25]
        for i, width in enumerate(column_widths):
            worksheet.set_column(i, i, width)

        # 标题
        worksheet.merge_range('A1:M1', f'精益数据统计 - {month_name}', self.formats['title'])

        # 第二行标题
        worksheet.merge_range('A2:M2', '按部门/职级/职级（默认显示全部数据，数据可切换）', self.formats['subtitle'])

        # 第三行：数据说明
        worksheet.merge_range('A3:M3', '提案列表展示/提报/通过/标准/同频次/首页展示/数据可切换', self.formats['subtitle'])

        # 主要指标展示区域 (第4-6行)
        headers_row4 = [
            '提案展示\n/培训/标准\n同频次\n首页展示',
            '提案总数\n4000',
            '平均提案数\n4',
            '提案通过率\n50%',
            '提案采纳率\n73%',
            '提案采纳率\n28%',
            '有效提案数817',
            '项目数\n30',
            '30天关闭率\n70%',
            '30天超期率\n10%',
            None,
            '提案展示数=提案图记录',
            '员工提报4000条'
        ]

        for col, header in enumerate(headers_row4):
            if header:
                worksheet.write(3, col, header, self.formats['header'])

        # 指标说明区域 (第7-11行)
        descriptions = [
            ('分院提案数据\n各科室数据统计看图', '分院提案数据\n各科室数据统计看图', None, None, None, None, None, None, None, None, '提案展示数=提案图记录', '员工提报4000条'),
            (None, None, None, None, None, None, None, None, None, None, '平均提案数=提案总数/提案参与人数', '1000个提案参与人对应4000条，平均提案数4条'),
            (None, None, None, None, None, None, None, None, None, None, '提案参与人数=提案参与人数/总人数', '提案参与人数1000人，总人数1000/2000=50%'),
            (None, None, None, None, None, None, None, None, None, None, '提案采纳率=采纳数/提案通过数', '采纳2920条，采纳数采纳2920/4000=73%'),
            (None, '分院采纳数据\n各科室采纳数据看图', None, '分院采纳数据\n各科室采纳数据看图', None, None, '有效采纳数\n各科室采纳数据看图', None, None, None, None, '采纳数=采纳', '采纳817条，采纳采纳817/2920=28%')
        ]

        for row, desc_row in enumerate(descriptions, start=6):
            for col, desc in enumerate(desc_row):
                if desc:
                    worksheet.write(row, col, desc, self.formats['text'])

        # 底部说明区域 (第12-13行)
        bottom_descriptions = [
            ('30天关闭率=项目立项后需要在30天内进行关闭', '30天关闭率=项目立项后需要在30天内进行关闭'),
            ('30天超期率=项目立项后（未完成提案作废）超过30天未关闭', '30天超期率=项目立项后（未完成提案作废）超过30天未关闭'),
            ('有效提案数=奖励', '奖励是否达到积分？奖励积分转换标准'),
            ('项目数=立项项目数', None)
        ]

        for row, (left_desc, right_desc) in enumerate(bottom_descriptions, start=11):
            worksheet.write(row, 0, left_desc, self.formats['text'])
            if right_desc:
                worksheet.write(row, 11, right_desc, self.formats['text'])

    def _create_dept_stats_sheet(self, month_name: str):
        """创建项目管理-职能部门工作表"""
        worksheet = self.workbook.add_worksheet('项目管理-职能部门')

        # 设置列宽
        column_widths = [15, 15, 15, 15, 25, 15, 15, 15, 25, 15, 15, 15, 20, 15, 15, 15, 15, 15, 15, 15]
        for i, width in enumerate(column_widths):
            worksheet.set_column(i, i, width)

        # 标题
        worksheet.merge_range('A1:T1', f'项目管理统计 - {month_name}', self.formats['title'])

        # 主要统计数据行 (第2行)
        stats_headers = [
            '提案总数\n50',
            '通过数\n15',
            '否决数\n3',
            '立项数\n30',
            None,
            None,
            '关闭项目数\n21',
            None,
            '待关闭项目数\n3',
            None,
            None,
            None,
            '30天关闭率\n70%',
            None,
            '30天超期率\n10%',
            None,
            None,
            None
        ]

        for col, header in enumerate(stats_headers):
            if header:
                worksheet.write(1, col, header, self.formats['header'])

        # 数据标题行 (第5-6行)
        data_headers = [
            'P', '提案编号', '提案主题', '状态', None, None, None, None, None, None,
            '提案等级', '提案等级', None, None, None, None, None, None, None, None
        ]

        for col, header in enumerate(data_headers):
            if header:
                worksheet.write(4, col, header, self.formats['header'])

        # 部门列标题 (第10行)
        dept_names = ['提案人', '提案编号', '提案主题', '状态', '信息部', '大外科', None, None, '护理部', '手术室', '药剂科', '急诊科', '骨科', '市场部', '泌尿科', '内科', '办公室', None, '说明']

        for col, dept_name in enumerate(dept_names):
            if dept_name:
                worksheet.write(9, col, dept_name, self.formats['header'])

        # 提案等级说明 (第11行)
        grade_explanation = [
            'P', '提案等级', '提案等级', None, None, None, None, None, None, None,
            '提案等级', '提案等级', None, None, None, None, None, None, '1.提案等级为PQC的大类；\n2.提案级别位置分类进行颜色；\n3.使用位置颜色进行形成看板；\n4.位置数据需要负责人点击可开查看项目详情；\n5.关闭记录的项目自动成绿色；\n6.提案时间在点的默认值；\n7.使用状态，不作筛选', None
        ]

        for col, explanation in enumerate(grade_explanation):
            if explanation:
                worksheet.write(10, col, explanation, self.formats['text'])

    def _create_project_management_sheet(self, month_name: str):
        """创建项目管理工作表"""
        worksheet = self.workbook.add_worksheet('项目管理')

        # 设置列宽
        column_widths = [15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15]
        for i, width in enumerate(column_widths):
            worksheet.set_column(i, i, width)

        # 标题
        worksheet.merge_range('A1:S1', f'项目管理详情 - {month_name}', self.formats['title'])

        # 主要统计数据 (第2行)
        stats_data = [
            '提案总数\n50',
            '通过数\n15',
            '否决数\n3',
            '立项数\n30',
            None,
            None,
            '关闭项目数\n21',
            None,
            '待关闭项目数\n3',
            None,
            None,
            None,
            '30天关闭率\n70%',
            None,
            '30天超期率\n10%',
            None,
            None,
            None,
            '重要提案'
        ]

        for col, data in enumerate(stats_data):
            if data:
                worksheet.write(1, col, data, self.formats['header'])

        # 提案列表标题 (第5-6行)
        list_headers = [
            '提案列表',
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None
        ]

        for col, header in enumerate(list_headers):
            if header:
                worksheet.write(4, col, header, self.formats['header'])

        # 等级说明 (第11行)
        grade_explanation = [
            'P', '提案等级', '提案等级', None, None, None, None, None, None, None,
            '提案等级', '提案等级', None, None, None, None, None, None, None, None
        ]

        for col, explanation in enumerate(grade_explanation):
            if explanation:
                worksheet.write(10, col, explanation, self.formats['text'])

    def _create_personal_stats_sheet(self, month_name: str):
        """创建个人统计工作表"""
        worksheet = self.workbook.add_worksheet('个人统计')

        # 设置列宽
        column_widths = [15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15]
        for i, width in enumerate(column_widths):
            worksheet.set_column(i, i, width)

        # 标题
        worksheet.merge_range('A1:S1', f'个人提案统计 - {month_name}', self.formats['title'])

        # 主要统计数据 (第2行)
        personal_stats = [
            '提案总数\n50',
            None,
            '通过数\n15',
            None,
            '否决数\n3',
            None,
            '立项数\n30',
            None,
            '关闭项目数\n21',
            None,
            '待关闭项目数\n3',
            None,
            '奖励\n300',
            None,
            '企业贡献',
            None,
            '企业贡献值',
            None,
            '重要提案'
        ]

        for col, stat in enumerate(personal_stats):
            if stat:
                worksheet.write(1, col, stat, self.formats['header'])

        # 个人排行榜标题 (第6行)
        ranking_headers = [
            '员工提案top10\n（第一是本人）',
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            '员工达标top10\n（第一是本人）',
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None
        ]

        for col, header in enumerate(ranking_headers):
            if header:
                worksheet.write(5, col, header, self.formats['header'])

def main():
    # 创建Excel生成器
    generator = LeanStatsExcelGenerator()

    # 生成当前月份的模板
    current_date = datetime.now()
    output_path = f"E:\\wangchen\\mytest\\dingding-demo\\精益数据统计模板_{current_date.strftime('%Y%m')}.xlsx"

    generator.generate_monthly_report(
        year=current_date.year,
        month=current_date.month,
        output_path=output_path
    )

if __name__ == "__main__":
    main()