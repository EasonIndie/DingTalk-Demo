# 钉钉表单实例数据获取 Demo

这是一个基于Spring Boot 2.x + JDK8的钉钉表单实例数据获取Demo，演示了如何集成钉钉开放平台API来获取表单实例列表和详细信息。支持分页查询、条件筛选等完整功能。

## 功能特性

- ✅ **合理的Token管理**：自动获取、缓存和刷新钉钉access_token
- ✅ **表单实例列表查询**：支持分页、筛选的表单实例列表获取
- ✅ **单个表单实例详情**：根据表单实例ID获取详细信息
- ✅ **智能分页机制**：支持钉钉API游标分页，转换为前端友好格式
- ✅ **多种查询方式**：支持按状态、发起人等条件筛选
- ✅ **优雅的架构设计**：分层架构，职责清晰
- ✅ **完善的异常处理**：统一的异常处理和错误响应
- ✅ **详细的日志记录**：便于调试和问题排查
- ✅ **标准化API响应**：统一的API响应格式
- ✅ **参数校验**：完善的参数校验机制
- ✅ **配置外部化**：支持环境变量和配置文件

## 技术栈

- **Spring Boot 2.7.18**
- **JDK 8**
- **Maven**
- **OkHttp3** (HTTP客户端)
- **Lombok** (代码简化)
- **Jackson** (JSON序列化)

## 项目结构

```
dingding-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── dingding/
│   │   │               ├── DingdingDemoApplication.java      # 应用启动类
│   │   │               ├── config/
│   │   │               │   ├── DingdingConfig.java          # 钉钉配置类
│   │   │               │   └── WebConfig.java               # Web配置类
│   │   │               ├── controller/
│   │   │               │   └── FormInstanceController.java  # 表单实例控制器
│   │   │               ├── service/
│   │   │               │   ├── DingdingApiClient.java       # 钉钉API客户端
│   │   │               │   ├── DingdingTokenManager.java    # Token管理器
│   │   │               │   └── FormInstanceService.java     # 表单实例服务
│   │   │               ├── model/
│   │   │               │   ├── ApiReponse.java              # API响应模型
│   │   │               │   ├── FormInstance.java            # 表单实例模型
│   │   │               │   └── TokenResponse.java           # Token响应模型
│   │   │               └── exception/
│   │   │                   └── GlobalExceptionHandler.java   # 全局异常处理器
│   │   └── resources/
│   │       └── application.yml                              # 应用配置文件
│   └── test/
│       └── java/
├── pom.xml
└── README.md
```

## 快速开始

### 1. 环境准备

- JDK 8+
- Maven 3.6+
- 钉钉开发者账号和权限

### 2. 配置钉钉应用

1. 登录[钉钉开放平台](https://open-dev.dingtalk.com/)
2. 创建应用并获取 **AppKey** 和 **AppSecret**
3. 配置应用权限，确保有访问表单数据的权限

### 3. 配置应用参数

修改 `src/main/resources/application.yml` 文件中的钉钉应用配置：

```yaml
dingding:
  app:
    app-key: ${DINGDING_APP_KEY:your_app_key_here}        # 替换为你的AppKey
    app-secret: ${DINGDING_APP_SECRET:your_app_secret_here} # 替换为你的AppSecret
    corp-id: ${DINGDING_CORP_ID:your_corp_id_here}       # 替换为你的企业ID
```

**推荐方式：使用环境变量**
```bash
export DINGDING_APP_KEY=your_actual_app_key
export DINGDING_APP_SECRET=your_actual_app_secret
export DINGDING_CORP_ID=your_actual_corp_id
```

### 4. 启动应用

```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

或者直接运行 `DingdingDemoApplication` 的 main 方法。

应用启动后会在 http://localhost:8080 可用。

## API接口说明

### 1. 获取表单实例详情

**接口地址：** `GET /api/form-instances/{formInstanceId}`

**请求参数：**
- `formInstanceId`：表单实例ID（路径参数）

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "formInstanceId": "xxxxx",
    "formName": "请假申请单",
    "formStatus": "RUNNING",
    "originatorUserId": "manager123",
    "createTime": 1698765432000,
    "formContent": {
      "请假类型": "事假",
      "请假天数": "2",
      "请假原因": "家中有事"
    },
    "formComponents": [...],
    "tasks": [...],
    "operationRecords": [...]
  },
  "timestamp": "2024-01-01T10:00:00",
  "requestId": "REQ-1704110400000-123"
}
```

### 2. 获取表单实例详情（查询参数）

**接口地址：** `GET /api/form-instances?formInstanceId={formInstanceId}`

### 3. 获取表单实例列表

**接口地址：** `GET /api/form-instances/list`

**请求参数：**
- `page`：页码（从1开始，默认1）
- `size`：每页数量（默认20，最大100）
- `processStatus`：流程状态（可选，RUNNING运行中, COMPLETED已完成, TERMINATED已终止）
- `originatorUserId`：发起人用户ID（可选）

**请求示例：**
```bash
# 获取第一页，每页10条记录
curl "http://localhost:8080/api/form-instances/list?page=1&size=10"

# 获取运行中的表单实例
curl "http://localhost:8080/api/form-instances/list?processStatus=RUNNING"

# 获取指定发起人的表单实例
curl "http://localhost:8080/api/form-instances/list?originatorUserId=user123"
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "hasMore": true,
    "nextToken": "10",
    "list": [
      {
        "formInstanceId": "xxxxx",
        "formName": "请假申请单",
        "formStatus": "RUNNING",
        "originatorUserId": "manager123",
        "createTime": 1698765432000,
        "formContent": {
          "请假类型": "事假",
          "请假天数": "2"
        }
      },
      {
        "formInstanceId": "yyyyy",
        "formName": "报销申请单",
        "formStatus": "COMPLETED",
        "originatorUserId": "user456",
        "createTime": 1698765432000,
        "formContent": {
          "报销类型": "差旅费",
          "报销金额": "1000"
        }
      }
    ],
    "totalCount": 100,
    "currentPage": 1,
    "pageSize": 10,
    "totalPages": 10
  },
  "timestamp": "2024-01-01T10:00:00",
  "requestId": "REQ-1704110400000-123"
}
```

### 4. 获取表单实例列表（简化版本）

**接口地址：** `GET /api/form-instances/list/simple`

**请求参数：**
- `page`：页码（从1开始，默认1）
- `size`：每页数量（默认20，最大100）

**使用场景：** 当只需要基本的分页查询，不需要其他筛选条件时使用。

### 5. 获取表单实例摘要

**接口地址：** `GET /api/form-instances/{formInstanceId}/summary`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "表单名称: 请假申请单, 状态: RUNNING, 创建时间: Mon Oct 30 14:30:32 CST 2023, 审批任务数: 3",
  "timestamp": "2024-01-01T10:00:00",
  "requestId": "REQ-1704110400000-123"
}
```

### 6. 健康检查

**接口地址：** `GET /api/form-instances/health`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "表单实例服务运行正常",
  "timestamp": "2024-01-01T10:00:00"
}
```

## Token管理机制

本项目实现了一个合理的Token管理机制：

### 1. 自动获取Token
- 应用启动时自动获取第一个access_token
- 后续请求优先使用缓存中的token

### 2. 智能缓存
- Token缓存时间默认为7000秒（钉钉token有效期7200秒）
- 提前5分钟刷新，避免token在调用时过期
- 使用双重检查锁定，防止并发环境下重复获取token

### 3. 自动刷新
- 当检测到token过期时，自动获取新token
- 支持强制刷新机制
- 重试机制：获取失败时自动重试3次

### 4. 线程安全
- 使用ReentrantLock确保多线程环境下的线程安全
- 避免token获取的竞态条件

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 参数错误 |
| 500 | 服务器内部错误 |

### 钉钉API错误码
当钉钉API返回错误时，错误信息会直接透传给客户端，常见的钉钉错误码包括：

- `0`：成功
- `40001`：access_token无效
- `40013`：不合法的CorpID
- `40014`：不合法的access_token
- `45009`：接口调用超过限制

## 日志配置

应用配置了详细的日志记录：

```yaml
logging:
  level:
    com.example.dingding: DEBUG
    org.springframework.web: DEBUG
```

关键日志级别：
- **INFO**：重要的业务操作记录
- **DEBUG**：详细的调试信息
- **WARN**：警告信息
- **ERROR**：错误信息

## 开发建议

### 1. 配置管理
- 生产环境建议使用环境变量管理敏感配置
- 可以考虑使用配置中心（如Nacos、Apollo）进行配置管理

### 2. 监控告警
- 建议添加Prometheus或Micrometer进行指标监控
- 配置钉钉机器人或邮件告警

### 3. 安全加固
- 添加API访问认证和权限控制
- 配置HTTPS
- 添加请求频率限制

### 4. 性能优化
- 考虑使用连接池管理HTTP连接
- 添加Redis缓存token
- 异步处理非关键业务

## 常见问题

### Q1: 如何获取表单实例ID？
A1: 表单实例ID可以通过钉钉的审批流程API或回调消息获取，也可以在钉钉管理后台查看具体的审批记录。

### Q2: Token获取失败怎么办？
A2: 检查以下几点：
- AppKey和AppSecret是否正确
- 网络是否能访问钉钉API服务器
- 应用是否有足够的权限

### Q3: 支持哪些类型的表单？
A3: 支持钉钉OA审批中的所有表单类型，包括但不限于请假、报销、采购等各类审批表单。

### Q4: 如何处理大文件表单数据？
A4: 对于包含大文件的表单，建议：
- 增加HTTP读取超时时间
- 考虑异步处理方式
- 实现流式处理避免内存溢出

## 许可证

本项目仅用于学习和演示目的，请勿用于生产环境。

## 联系方式

如有问题或建议，请通过以下方式联系：
- 邮箱：your-email@example.com
- GitHub Issues：[项目地址]

---

**注意：** 本Demo仅展示钉钉API的基本集成方式，实际生产使用时请根据具体需求进行完善和优化。