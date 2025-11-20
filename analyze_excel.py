import pandas as pd
import json

# 读取Excel文件
excel_file = r'E:\wangchen\mytest\dingding-demo\精益数据统计.xlsx'

# 读取所有工作表
all_sheets = pd.read_excel(excel_file, sheet_name=None)

print("Excel文件工作表分析:")
print("=" * 50)

for sheet_name, df in all_sheets.items():
    print(f"\n工作表: {sheet_name}")
    print(f"行数: {len(df)}, 列数: {len(df.columns)}")
    print(f"列名: {list(df.columns)}")
    print("\n前5行数据:")
    print(df.head())
    print("\n数据类型:")
    print(df.dtypes)
    print("-" * 50)

# 分析JSON数据结构
print("\n\n钉钉表单JSON数据分析:")
print("=" * 50)

with open(r'E:\wangchen\mytest\dingding-demo\processInstanceResult.json', 'r', encoding='utf-8') as f:
    json_data = json.load(f)

print("主数据结构:")
print(f"- businessId: {json_data.get('businessId')}")
print(f"- title: {json_data.get('title')}")
print(f"- createTime: {json_data.get('createTime')}")
print(f"- status: {json_data.get('status')}")
print(f"- result: {json_data.get('result')}")

print(f"\n表单组件数量: {len(json_data.get('formComponentValues', []))}")
print("表单组件类型分析:")
component_types = {}
for component in json_data.get('formComponentValues', []):
    comp_type = component.get('componentType')
    component_types[comp_type] = component_types.get(comp_type, 0) + 1

for comp_type, count in component_types.items():
    print(f"- {comp_type}: {count}个")

print(f"\n操作记录数量: {len(json_data.get('operationRecords', []))}")
print(f"任务数量: {len(json_data.get('tasks', []))}")