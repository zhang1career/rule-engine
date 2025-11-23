# 规则引擎系统

基于SpringBoot的规则计算服务，支持多种规则类型和灵活的规则执行流程。

## 技术栈

- **JDK**: 1.8
- **框架**: SpringBoot 2.7.18
- **构建工具**: Maven 3.6
- **脚本引擎**: Groovy 3.0.17
- **表达式引擎**: MVEL 2.4.14

## 项目结构

```
rule-engine/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ruleengine/
│   │   │       ├── RuleEngineApplication.java      # 主启动类
│   │   │       ├── common/                         # 通用类
│   │   │       │   └── TypedValue.java             # 类型化值封装
│   │   │       ├── config/                         # 配置类
│   │   │       │   └── RuleEngineConfig.java       # 规则引擎配置
│   │   │       ├── controller/                     # 控制器层
│   │   │       │   └── RuleController.java         # 规则计算接口
│   │   │       ├── dto/                            # 数据传输对象
│   │   │       │   ├── EvalRequest.java            # 计算请求
│   │   │       │   └── EvalResponse.java           # 计算响应
│   │   │       ├── engine/                         # 规则引擎
│   │   │       │   └── RuleExecutionEngine.java    # 规则执行引擎
│   │   │       ├── enums/                          # 枚举类
│   │   │       │   ├── Environment.java            # 环境类型
│   │   │       │   ├── RuleStatus.java             # 规则状态
│   │   │       │   └── RuleType.java               # 规则类型
│   │   │       ├── model/                          # 实体模型
│   │   │       │   ├── ABTestRecord.java           # A/B测试记录
│   │   │       │   ├── ExecutionSequence.java      # 执行序列
│   │   │       │   ├── Rule.java                   # 规则实体
│   │   │       │   └── RuleExecutionContext.java   # 执行上下文
│   │   │       ├── rule/                           # 规则相关
│   │   │       │   └── executor/                   # 规则执行器
│   │   │       │       ├── RuleExecutor.java       # 执行器接口
│   │   │       │       └── impl/                   # 执行器实现
│   │   │       │           ├── ApiQueryRuleExecutor.java    # 接口查询执行器
│   │   │       │           ├── ExpressionRuleExecutor.java  # 表达式执行器
│   │   │       │           ├── ScriptRuleExecutor.java      # 脚本执行器
│   │   │       │           └── SqlQueryRuleExecutor.java    # SQL查询执行器
│   │   │       └── service/                        # 服务层
│   │   │           ├── ABTestService.java          # A/B测试服务
│   │   │           ├── EvalService.java            # 计算服务
│   │   │           ├── MessageQueueService.java   # 消息队列服务
│   │   │           ├── RuleService.java            # 规则服务
│   │   │           └── impl/                       # 服务实现
│   │   │               ├── ABTestServiceImpl.java
│   │   │               ├── EvalServiceImpl.java
│   │   │               ├── MessageQueueServiceImpl.java
│   │   │               └── RuleServiceImpl.java
│   │   └── resources/
│   │       └── application.yml                      # 配置文件
│   └── test/                                        # 测试代码
├── pom.xml                                          # Maven配置
├── PRD.md                                           # 产品需求文档
└── README.md                                        # 项目说明
```

## 核心功能

### 1. 规则计算接口

**接口路径**: `POST /rule/eval`

**请求示例**:
```json
{
  "userId": 123456789,
  "eventId": 1001,
  "traceId": 987654321,
  "dataMap": {
    "amount": {
      "value": 1000.00,
      "type": "DECIMAL"
    },
    "age": {
      "value": 25,
      "type": "INTEGER"
    }
  }
}
```

**响应示例**:
```json
{
  "result": {
    "value": true,
    "type": "BOOLEAN"
  },
  "success": true
}
```

### 2. 规则类型

- **逻辑表达式 (EXPRESSION)**: 使用MVEL表达式引擎执行逻辑表达式
- **接口查询 (API_QUERY)**: 调用外部HTTP接口获取数据
- **SQL查询 (SQL_QUERY)**: 执行数据库查询操作
- **脚本执行 (SCRIPT)**: 执行Groovy脚本

### 3. 规则状态

- **下线 (OFFLINE)**: 规则不可用，不允许执行
- **测试 (TEST)**: 仅测试环境可执行
- **A/B测试 (AB_TEST)**: 生产环境按比例执行
- **全量 (FULL)**: 生产环境全量执行

### 4. 执行流程

1. 根据`eventId`获取执行序列
2. 按顺序执行序列中的规则
3. 根据规则状态判断是否执行
4. 支持根据规则结果提前跳出
5. 支持规则间数据传递
6. 支持循环和递归调用（通过执行深度控制）

## 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+ (可选，仅当使用SQL查询功能时需要)
- RabbitMQ (可选，仅当需要消息队列功能时需要)

### 2. 配置说明

编辑 `src/main/resources/application.yml`:

```yaml
# 规则引擎环境配置
rule:
  engine:
    environment: TEST  # 或 PRODUCTION

# RabbitMQ配置（可选）
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# 数据库配置（可选）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rule_engine
    username: root
    password: root
```

### 3. 编译运行

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/rule-engine-1.0.0-SNAPSHOT.jar
```

### 4. 测试接口

```bash
curl -X POST http://localhost:8080/rule/eval \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456789,
    "eventId": 1001,
    "traceId": 987654321,
    "dataMap": {
      "amount": {"value": 1000.00, "type": "DECIMAL"},
      "age": {"value": 25, "type": "INTEGER"}
    }
  }'
```

## 扩展说明

### 1. 添加新的规则类型

1. 在`RuleType`枚举中添加新类型
2. 实现`RuleExecutor`接口
3. 在`RuleEngineConfig`中注册执行器

### 2. 支持RPC调用

预留了RPC接口扩展点，可以通过以下方式扩展：

- 创建`RpcRuleController`实现RPC接口
- 在`EvalService`中支持RPC调用方式

### 3. 支持异步非阻塞调用

- 使用Spring WebFlux实现响应式编程
- 在Controller层添加异步接口

### 4. 数据持久化

当前使用内存存储，可以替换为：

- 数据库存储（MySQL/PostgreSQL等）
- 配置中心（Nacos/Apollo等）
- Redis缓存

## 注意事项

1. **规则配置**: 当前使用内存存储，生产环境需要实现持久化存储
2. **A/B测试记录**: 当前使用内存存储，生产环境需要持久化
3. **执行深度限制**: 默认最大执行深度为100，防止无限递归
4. **消息队列**: RabbitMQ为可选配置，未配置时消息发送会跳过
5. **数据库**: 数据库为可选配置，未配置时SQL查询功能不可用

## 开发计划

- [x] 基础架构搭建
- [x] HTTP同步阻塞接口
- [x] 基础规则执行引擎
- [x] 规则状态控制
- [x] RabbitMQ消息发送
- [ ] A/B测试功能完善
- [ ] 规则配置持久化
- [ ] RPC接口实现
- [ ] 异步非阻塞调用
- [ ] 规则执行监控和日志

## 许可证

MIT License

