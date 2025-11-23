# 快速启动指南

## 前置条件

- JDK 1.8+
- Maven 3.6+

## 快速启动（无需外部依赖）

项目已经配置为可以在没有RabbitMQ和数据库的情况下运行（这些是可选的）。

### 1. 编译项目

```bash
cd /Users/mini/Projects/startups/rule-engine
mvn clean compile
```

### 2. 运行项目

```bash
mvn spring-boot:run
```

或者打包后运行：

```bash
mvn clean package
java -jar target/rule-engine-1.0.0-SNAPSHOT.jar
```

### 3. 测试接口

项目启动后，会自动初始化一些示例规则。可以使用以下命令测试：

```bash
curl -X POST http://localhost:8080/rule/eval \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456789,
    "eventId": 1001,
    "traceId": 987654321,
    "dataMap": {
      "amount": {"value": 2000.00, "type": "DECIMAL"},
      "age": {"value": 25, "type": "INTEGER"}
    }
  }'
```

**预期响应**:
```json
{
  "result": {
    "value": 0.05,
    "type": "DECIMAL"
  },
  "success": true
}
```

## 配置说明

### 环境配置

编辑 `src/main/resources/application.yml` 中的 `rule.engine.environment`:

- `TEST`: 测试环境（默认）
- `PRODUCTION`: 生产环境

### RabbitMQ配置（可选）

如果需要使用RabbitMQ消息队列功能，取消注释 `application.yml` 中的RabbitMQ配置：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 数据库配置（可选）

如果需要使用SQL查询功能，取消注释 `application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rule_engine
    username: root
    password: root
```

## 示例规则说明

项目启动时会自动创建以下示例规则：

1. **rule_001**: 金额检查规则（表达式）
   - 检查金额是否大于1000且年龄大于等于18
   - 状态：全量

2. **rule_002**: 计算折扣规则（Groovy脚本）
   - 根据金额计算折扣（>5000: 10%, >2000: 5%）
   - 状态：全量

3. **rule_003**: A/B测试规则（表达式）
   - 检查金额是否大于500
   - 状态：A/B测试（50%比例）

执行序列：
- eventId=1001: 执行 rule_001 -> rule_002
- eventId=1002: 执行 rule_001 -> rule_003

## 常见问题

### 1. 端口被占用

修改 `application.yml` 中的 `server.port` 配置。

### 2. RabbitMQ连接失败

如果未配置RabbitMQ，消息发送功能会自动跳过，不影响主流程。

### 3. 数据库连接失败

如果未配置数据库，SQL查询功能不可用，其他功能正常。

## 下一步

- 查看 [README.md](README.md) 了解详细功能
- 查看 [PRD.md](PRD.md) 了解产品需求
- 根据需要扩展规则类型和执行器

