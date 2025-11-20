# 钉钉精益数据统计系统 - 实现指南

## 1. 数据同步方案

### 1.1 数据同步架构

```
钉钉API → 数据采集服务 → 消息队列 → 数据处理服务 → MySQL数据库
                                        ↓
                                  统计计算引擎
                                        ↓
                                  前端展示系统
```

### 1.2 数据采集服务实现

#### 核心同步代码示例

```python
import requests
import json
import pymysql
from datetime import datetime
import logging
from typing import Dict, List, Optional

class DingTalkDataSync:
    def __init__(self, config: Dict):
        self.config = config
        self.db_config = config['database']
        self.dingtalk_config = config['dingtalk']
        self.logger = self._setup_logger()

    def _setup_logger(self):
        """设置日志记录"""
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s'
        )
        return logging.getLogger(__name__)

    def get_process_instances(self, start_time: str, end_time: str) -> List[Dict]:
        """
        获取指定时间范围内的流程实例

        Args:
            start_time: 开始时间 (格式: 2025-11-01 00:00:00)
            end_time: 结束时间 (格式: 2025-11-30 23:59:59)

        Returns:
            流程实例列表
        """
        url = f"{self.dingtalk_config['api_url']}/topapi/processinstance/list"

        params = {
            'access_token': self._get_access_token(),
            'start_time': start_time,
            'end_time': end_time,
            'size': 100,
            'cursor': 0
        }

        all_instances = []

        while True:
            try:
                response = requests.post(url, params=params)
                result = response.json()

                if result.get('errcode') == 0:
                    instances = result.get('result', {}).get('result', [])
                    all_instances.extend(instances)

                    # 检查是否还有更多数据
                    if not result.get('result', {}).get('has_more'):
                        break

                    params['cursor'] = result.get('result', {}).get('next_cursor')
                else:
                    self.logger.error(f"获取流程实例失败: {result}")
                    break

            except Exception as e:
                self.logger.error(f"请求异常: {e}")
                break

        return all_instances

    def sync_single_process(self, business_id: str) -> bool:
        """
        同步单个流程实例的详细数据

        Args:
            business_id: 业务ID

        Returns:
            同步是否成功
        """
        try:
            # 获取流程实例详情
            process_detail = self._get_process_detail(business_id)
            if not process_detail:
                return False

            # 保存到数据库
            return self._save_process_data(process_detail)

        except Exception as e:
            self.logger.error(f"同步流程 {business_id} 失败: {e}")
            return False

    def _get_process_detail(self, business_id: str) -> Optional[Dict]:
        """获取流程实例详情"""
        url = f"{self.dingtalk_config['api_url']}/topapi/processinstance/get"

        params = {
            'access_token': self._get_access_token(),
            'process_instance_id': business_id
        }

        try:
            response = requests.post(url, params=params)
            result = response.json()

            if result.get('errcode') == 0:
                return result.get('result')
            else:
                self.logger.error(f"获取流程详情失败: {result}")
                return None

        except Exception as e:
            self.logger.error(f"请求流程详情异常: {e}")
            return None

    def _save_process_data(self, process_data: Dict) -> bool:
        """保存流程数据到数据库"""
        connection = None
        try:
            connection = pymysql.connect(**self.db_config)
            cursor = connection.cursor()

            # 调用存储过程同步数据
            cursor.callproc('sync_ding_process_data', [json.dumps(process_data)])

            # 获取存储过程返回结果
            cursor.execute("SELECT @_sync_ding_process_data_0, @_sync_ding_process_data_1")
            result, message = cursor.fetchone()

            connection.commit()

            if result > 0:
                self.logger.info(f"数据同步成功: {message}")
                return True
            else:
                self.logger.error(f"数据同步失败: {message}")
                return False

        except Exception as e:
            if connection:
                connection.rollback()
            self.logger.error(f"数据库操作异常: {e}")
            return False
        finally:
            if connection:
                connection.close()

    def batch_sync_processes(self, business_ids: List[str]) -> Dict[str, int]:
        """
        批量同步流程实例

        Args:
            business_ids: 业务ID列表

        Returns:
            同步结果统计 {'success': 成功数量, 'failed': 失败数量}
        """
        success_count = 0
        failed_count = 0

        for business_id in business_ids:
            if self.sync_single_process(business_id):
                success_count += 1
            else:
                failed_count += 1

        return {
            'success': success_count,
            'failed': failed_count
        }
```

### 1.3 定时同步任务

```python
import schedule
import time
from datetime import datetime, timedelta

class SyncScheduler:
    def __init__(self, sync_service: DingTalkDataSync):
        self.sync_service = sync_service

    def daily_sync(self):
        """每日定时同步任务"""
        print(f"开始执行每日同步任务: {datetime.now()}")

        # 同步昨天的新数据
        yesterday = datetime.now() - timedelta(days=1)
        start_time = yesterday.strftime('%Y-%m-%d 00:00:00')
        end_time = yesterday.strftime('%Y-%m-%d 23:59:59')

        # 获取流程实例列表
        instances = self.sync_service.get_process_instances(start_time, end_time)

        # 批量同步
        business_ids = [instance['business_id'] for instance in instances]
        result = self.sync_service.batch_sync_processes(business_ids)

        print(f"同步完成 - 成功: {result['success']}, 失败: {result['failed']}")

    def full_sync(self):
        """全量数据同步（通常在夜间执行）"""
        print(f"开始执行全量同步任务: {datetime.now()}")

        # 获取过去30天的数据
        start_date = datetime.now() - timedelta(days=30)
        end_date = datetime.now()

        start_time = start_date.strftime('%Y-%m-%d 00:00:00')
        end_time = end_date.strftime('%Y-%m-%d 23:59:59')

        instances = self.sync_service.get_process_instances(start_time, end_time)
        business_ids = [instance['business_id'] for instance in instances]
        result = self.sync_service.batch_sync_processes(business_ids)

        print(f"全量同步完成 - 成功: {result['success']}, 失败: {result['failed']}")

    def start_scheduler(self):
        """启动定时任务"""
        # 每天凌晨2点执行同步
        schedule.every().day.at("02:00").do(self.daily_sync)

        # 每周日凌晨3点执行全量同步
        schedule.every().sunday.at("03:00").do(self.full_sync)

        while True:
            schedule.run_pending()
            time.sleep(60)  # 每分钟检查一次
```

## 2. 统计分析引擎

### 2.1 统计服务核心类

```python
from typing import Dict, List, Any, Optional
from datetime import datetime, date
import pymysql

class ProposalStatisticsEngine:
    def __init__(self, db_config: Dict):
        self.db_config = db_config

    def get_overview_stats(self, start_date: date, end_date: date) -> Dict[str, Any]:
        """
        获取总体统计概览

        Args:
            start_date: 开始日期
            end_date: 结束日期

        Returns:
            统计结果字典
        """
        connection = pymysql.connect(**self.db_config)

        try:
            cursor = connection.cursor(pymysql.cursors.DictCursor)

            # 1. 总提案数
            cursor.execute("""
                SELECT COUNT(*) as total_proposals
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
            """, (start_date, end_date))
            total_proposals = cursor.fetchone()['total_proposals']

            # 2. 通过提案数
            cursor.execute("""
                SELECT COUNT(*) as passed_proposals
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
                AND result = 'agree'
            """, (start_date, end_date))
            passed_proposals = cursor.fetchone()['passed_proposals']

            # 3. 通过率
            pass_rate = (passed_proposals / total_proposals * 100) if total_proposals > 0 else 0

            # 4. 参与人数
            cursor.execute("""
                SELECT COUNT(DISTINCT originator_user_id) as participant_count
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
            """, (start_date, end_date))
            participant_count = cursor.fetchone()['participant_count']

            # 5. 参与部门数
            cursor.execute("""
                SELECT COUNT(DISTINCT originator_dept_id) as dept_count
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
            """, (start_date, end_date))
            dept_count = cursor.fetchone()['dept_count']

            return {
                'total_proposals': total_proposals,
                'passed_proposals': passed_proposals,
                'pass_rate': round(pass_rate, 2),
                'participant_count': participant_count,
                'dept_count': dept_count
            }

        finally:
            connection.close()

    def get_dept_stats(self, start_date: date, end_date: date) -> List[Dict[str, Any]]:
        """按部门统计"""
        connection = pymysql.connect(**self.db_config)

        try:
            cursor = connection.cursor(pymysql.cursors.DictCursor)

            cursor.execute("""
                SELECT
                    originator_dept_name as dept_name,
                    COUNT(*) as proposal_count,
                    COUNT(CASE WHEN result = 'agree' THEN 1 END) as passed_count,
                    ROUND(COUNT(CASE WHEN result = 'agree' THEN 1 END) * 100.0 / COUNT(*), 2) as pass_rate,
                    COUNT(DISTINCT originator_user_id) as participant_count
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
                    AND originator_dept_name IS NOT NULL
                GROUP BY originator_dept_name
                ORDER BY proposal_count DESC
            """, (start_date, end_date))

            return cursor.fetchall()

        finally:
            connection.close()

    def get_category_stats(self, start_date: date, end_date: date) -> List[Dict[str, Any]]:
        """按提案类别统计"""
        connection = pymysql.connect(**self.db_config)

        try:
            cursor = connection.cursor(pymysql.cursors.DictCursor)

            cursor.execute("""
                SELECT
                    fc.value_text as category,
                    COUNT(*) as proposal_count,
                    COUNT(CASE WHEN pi.result = 'agree' THEN 1 END) as passed_count,
                    ROUND(COUNT(CASE WHEN pi.result = 'agree' THEN 1 END) * 100.0 / COUNT(*), 2) as pass_rate
                FROM ding_process_instances pi
                JOIN ding_form_component_values fc ON pi.business_id = fc.business_id
                WHERE pi.create_time BETWEEN %s AND %s
                    AND fc.component_name = '提案类别'
                GROUP BY fc.value_text
                ORDER BY proposal_count DESC
            """, (start_date, end_date))

            return cursor.fetchall()

        finally:
            connection.close()

    def get_monthly_trend(self, start_date: date, end_date: date) -> List[Dict[str, Any]]:
        """获取月度趋势数据"""
        connection = pymysql.connect(**self.db_config)

        try:
            cursor = connection.cursor(pymysql.cursors.DictCursor)

            cursor.execute("""
                SELECT
                    DATE_FORMAT(create_time, '%Y-%m') as month,
                    COUNT(*) as proposal_count,
                    COUNT(CASE WHEN result = 'agree' THEN 1 END) as passed_count,
                    COUNT(DISTINCT originator_user_id) as participant_count
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
                GROUP BY DATE_FORMAT(create_time, '%Y-%m')
                ORDER BY month
            """, (start_date, end_date))

            return cursor.fetchall()

        finally:
            connection.close()

    def get_top_proposers(self, start_date: date, end_date: date, limit: int = 10) -> List[Dict[str, Any]]:
        """获取提案排行榜"""
        connection = pymysql.connect(**self.db_config)

        try:
            cursor = connection.cursor(pymysql.cursors.DictCursor)

            cursor.execute("""
                SELECT
                    originator_user_name as proposer_name,
                    originator_dept_name as dept_name,
                    COUNT(*) as proposal_count,
                    COUNT(CASE WHEN result = 'agree' THEN 1 END) as passed_count,
                    ROUND(COUNT(CASE WHEN result = 'agree' THEN 1 END) * 100.0 / COUNT(*), 2) as pass_rate
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
                    AND originator_user_name IS NOT NULL
                GROUP BY originator_user_id, originator_dept_name
                ORDER BY proposal_count DESC
                LIMIT %s
            """, (start_date, end_date, limit))

            return cursor.fetchall()

        finally:
            connection.close()

    def get_processing_efficiency(self, start_date: date, end_date: date) -> Dict[str, Any]:
        """获取处理效率统计"""
        connection = pymysql.connect(**self.db_config)

        try:
            cursor = connection.cursor(pymysql.cursors.DictCursor)

            # 平均处理时长
            cursor.execute("""
                SELECT
                    AVG(DATEDIFF(finish_time, create_time)) as avg_days,
                    MIN(DATEDIFF(finish_time, create_time)) as min_days,
                    MAX(DATEDIFF(finish_time, create_time)) as max_days,
                    COUNT(*) as completed_count
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
                    AND status = 'COMPLETED'
                    AND finish_time IS NOT NULL
            """, (start_date, end_date))

            efficiency_stats = cursor.fetchone()

            # 30天内完成率
            cursor.execute("""
                SELECT
                    COUNT(*) as total,
                    COUNT(CASE WHEN DATEDIFF(finish_time, create_time) <= 30 THEN 1 END) as within_30_days
                FROM ding_process_instances
                WHERE create_time BETWEEN %s AND %s
                    AND status = 'COMPLETED'
                    AND finish_time IS NOT NULL
            """, (start_date, end_date))

            completion_stats = cursor.fetchone()

            within_30_rate = (completion_stats['within_30_days'] / completion_stats['total'] * 100) if completion_stats['total'] > 0 else 0

            return {
                'avg_processing_days': round(efficiency_stats['avg_days'] or 0, 1),
                'min_processing_days': efficiency_stats['min_days'] or 0,
                'max_processing_days': efficiency_stats['max_days'] or 0,
                'completed_count': efficiency_stats['completed_count'],
                'within_30_days_rate': round(within_30_rate, 2)
            }

        finally:
            connection.close()
```

### 2.2 Excel报表生成

```python
import pandas as pd
from datetime import datetime, date
import xlsxwriter
from typing import Dict, List

class ExcelReportGenerator:
    def __init__(self, stats_engine: ProposalStatisticsEngine):
        self.stats_engine = stats_engine

    def generate_monthly_report(self, report_date: date, output_path: str):
        """生成月度统计报告"""

        # 计算报告月份的起止日期
        start_date = report_date.replace(day=1)
        if report_date.month == 12:
            end_date = report_date.replace(year=report_date.year + 1, month=1, day=1) - datetime.timedelta(days=1)
        else:
            end_date = report_date.replace(month=report_date.month + 1, day=1) - datetime.timedelta(days=1)

        # 创建Excel工作簿
        workbook = xlsxwriter.Workbook(output_path)

        # 1. 总体概览
        self._create_overview_sheet(workbook, start_date, end_date)

        # 2. 部门统计
        self._create_dept_stats_sheet(workbook, start_date, end_date)

        # 3. 提案类别统计
        self._create_category_stats_sheet(workbook, start_date, end_date)

        # 4. 个人排行榜
        self._create_top_proposers_sheet(workbook, start_date, end_date)

        # 5. 处理效率分析
        self._create_efficiency_sheet(workbook, start_date, end_date)

        workbook.close()

    def _create_overview_sheet(self, workbook, start_date: date, end_date: date):
        """创建总体概览工作表"""
        worksheet = workbook.add_worksheet('总体概览')

        # 设置标题格式
        title_format = workbook.add_format({
            'bold': True,
            'font_size': 16,
            'align': 'center',
            'valign': 'vcenter'
        })

        # 设置数据格式
        data_format = workbook.add_format({
            'align': 'center',
            'valign': 'vcenter'
        })

        # 标题
        worksheet.merge_range('A1:E1', f'精益数据统计月报 - {start_date.strftime("%Y年%m月")}', title_format)

        # 获取统计数据
        stats = self.stats_engine.get_overview_stats(start_date, end_date)

        # 表头
        headers = ['统计项目', '数值', '说明']
        worksheet.write_row(3, 0, headers, data_format)

        # 数据
        data = [
            ['总提案数', stats['total_proposals'], '期间内所有提案数量'],
            ['通过提案数', stats['passed_proposals'], '审核通过的提案数量'],
            ['通过率', f"{stats['pass_rate']}%", '提案审核通过百分比'],
            ['参与人数', stats['participant_count'], '参与提案的总人数'],
            ['参与部门数', stats['dept_count'], '参与提案的部门数量']
        ]

        for i, row in enumerate(data, start=4):
            worksheet.write_row(i, 0, row, data_format)

        # 设置列宽
        worksheet.set_column('A:A', 20)
        worksheet.set_column('B:B', 15)
        worksheet.set_column('C:C', 30)

    def _create_dept_stats_sheet(self, workbook, start_date: date, end_date: date):
        """创建部门统计工作表"""
        worksheet = workbook.add_worksheet('部门统计')

        # 格式设置
        title_format = workbook.add_format({'bold': True, 'font_size': 14})
        header_format = workbook.add_format({'bold': True, 'bg_color': '#E6E6FA'})

        # 标题
        worksheet.merge_range('A1:F1', '部门提案统计', title_format)

        # 表头
        headers = ['部门名称', '提案数量', '通过数量', '通过率', '参与人数', '排名']
        worksheet.write_row(3, 0, headers, header_format)

        # 获取数据
        dept_stats = self.stats_engine.get_dept_stats(start_date, end_date)

        # 写入数据
        for i, dept in enumerate(dept_stats, start=4):
            row_data = [
                dept['dept_name'],
                dept['proposal_count'],
                dept['passed_count'],
                f"{dept['pass_rate']}%",
                dept['participant_count'],
                i - 3  # 排名
            ]
            worksheet.write_row(i, 0, row_data)

        # 设置列宽
        for i in range(6):
            worksheet.set_column(i, i, 15)

    def _create_category_stats_sheet(self, workbook, start_date: date, end_date: date):
        """创建提案类别统计工作表"""
        worksheet = workbook.add_worksheet('提案类别统计')

        # 格式设置
        title_format = workbook.add_format({'bold': True, 'font_size': 14})
        header_format = workbook.add_format({'bold': True, 'bg_color': '#E6E6FA'})

        # 标题
        worksheet.merge_range('A1:D1', '提案类别分布', title_format)

        # 表头
        headers = ['提案类别', '提案数量', '通过数量', '通过率']
        worksheet.write_row(3, 0, headers, header_format)

        # 获取数据
        category_stats = self.stats_engine.get_category_stats(start_date, end_date)

        # 写入数据
        for i, category in enumerate(category_stats, start=4):
            row_data = [
                category['category'],
                category['proposal_count'],
                category['passed_count'],
                f"{category['pass_rate']}%"
            ]
            worksheet.write_row(i, 0, row_data)

        # 设置列宽
        worksheet.set_column('A:A', 20)
        worksheet.set_column('B:B', 12)
        worksheet.set_column('C:C', 12)
        worksheet.set_column('D:D', 12)

    def _create_top_proposers_sheet(self, workbook, start_date: date, end_date: date):
        """创建个人排行榜工作表"""
        worksheet = workbook.add_worksheet('个人排行榜')

        # 格式设置
        title_format = workbook.add_format({'bold': True, 'font_size': 14})
        header_format = workbook.add_format({'bold': True, 'bg_color': '#E6E6FA'})

        # 标题
        worksheet.merge_range('A1:E1', '提案个人TOP10', title_format)

        # 表头
        headers = ['排名', '姓名', '部门', '提案数量', '通过率']
        worksheet.write_row(3, 0, headers, header_format)

        # 获取数据
        top_proposers = self.stats_engine.get_top_proposers(start_date, end_date, 10)

        # 写入数据
        for i, proposer in enumerate(top_proposers, start=4):
            row_data = [
                i - 3,  # 排名
                proposer['proposer_name'],
                proposer['dept_name'],
                proposer['proposal_count'],
                f"{proposer['pass_rate']}%"
            ]
            worksheet.write_row(i, 0, row_data)

        # 设置列宽
        worksheet.set_column('A:A', 8)
        worksheet.set_column('B:B', 15)
        worksheet.set_column('C:C', 20)
        worksheet.set_column('D:D', 12)
        worksheet.set_column('E:E', 12)

    def _create_efficiency_sheet(self, workbook, start_date: date, end_date: date):
        """创建处理效率分析工作表"""
        worksheet = workbook.add_worksheet('处理效率分析')

        # 格式设置
        title_format = workbook.add_format({'bold': True, 'font_size': 14})
        header_format = workbook.add_format({'bold': True, 'bg_color': '#E6E6FA'})

        # 标题
        worksheet.merge_range('A1:B1', '提案处理效率统计', title_format)

        # 获取数据
        efficiency = self.stats_engine.get_processing_efficiency(start_date, end_date)

        # 表头
        headers = ['统计项目', '数值']
        worksheet.write_row(3, 0, headers, header_format)

        # 数据
        data = [
            ['平均处理天数(天)', f"{efficiency['avg_processing_days']}"],
            ['最短处理天数(天)', f"{efficiency['min_processing_days']}"],
            ['最长处理天数(天)', f"{efficiency['max_processing_days']}"],
            ['已完成提案数', f"{efficiency['completed_count']}"],
            ['30天内完成率', f"{efficiency['within_30_days_rate']}%"]
        ]

        for i, row in enumerate(data, start=4):
            worksheet.write_row(i, 0, row)

        # 设置列宽
        worksheet.set_column('A:A', 20)
        worksheet.set_column('B:B', 20)
```

## 3. 配置文件示例

### 3.1 数据库配置

```python
# config.py
DATABASE_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'dingtalk_user',
    'password': 'your_password',
    'database': 'dingtalk_stats',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

DINGTALK_CONFIG = {
    'app_key': 'your_app_key',
    'app_secret': 'your_app_secret',
    'api_url': 'https://oapi.dingtalk.com'
}

SYNC_CONFIG = {
    'sync_interval': 3600,  # 同步间隔（秒）
    'batch_size': 100,      # 批处理大小
    'retry_times': 3,       # 重试次数
    'timeout': 30           # 请求超时时间
}
```

## 4. 部署和运维

### 4.1 Docker部署配置

```dockerfile
# Dockerfile
FROM python:3.9-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install -r requirements.txt

# 复制代码
COPY . .

# 设置环境变量
ENV PYTHONPATH=/app

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["python", "main.py"]
```

### 4.2 监控和告警

```python
import smtplib
from email.mime.text import MIMEText
from datetime import datetime

class AlertManager:
    def __init__(self, config: Dict):
        self.config = config

    def send_sync_alert(self, success_count: int, failed_count: int):
        """发送同步异常告警"""
        if failed_count > 0:
            subject = "钉钉数据同步异常告警"
            content = f"""
            同步时间: {datetime.now()}
            成功数量: {success_count}
            失败数量: {failed_count}
            请及时检查系统状态！
            """
            self._send_email(subject, content)

    def send_performance_alert(self, metric_name: str, value: float, threshold: float):
        """发送性能告警"""
        if value > threshold:
            subject = f"系统性能告警 - {metric_name}"
            content = f"""
            告警时间: {datetime.now()}
            指标名称: {metric_name}
            当前值: {value}
            阈值: {threshold}
            请及时处理！
            """
            self._send_email(subject, content)

    def _send_email(self, subject: str, content: str):
        """发送邮件"""
        msg = MIMEText(content, 'plain', 'utf-8')
        msg['Subject'] = subject
        msg['From'] = self.config['sender']
        msg['To'] = self.config['recipients']

        try:
            with smtplib.SMTP(self.config['smtp_server'], self.config['smtp_port']) as server:
                server.starttls()
                server.login(self.config['username'], self.config['password'])
                server.send_message(msg)
        except Exception as e:
            print(f"发送邮件失败: {e}")
```

## 5. 总结

本实现方案提供了：

1. **完整的数据同步方案** - 支持增量同步和全量同步
2. **强大的统计分析引擎** - 支持多维度统计分析
3. **Excel报表自动生成** - 定期生成统计报告
4. **监控和告警机制** - 确保系统稳定运行
5. **可扩展的架构设计** - 支持后续功能扩展

该方案充分考虑了实际业务需求，具有良好的性能和可维护性。