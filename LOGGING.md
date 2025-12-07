# 日志配置指南

本文档说明如何配置Rule Engine项目的日志系统。

## 日志框架

项目使用Spring Boot默认的Logback日志框架，支持以下配置方式：

1. **application.yml** - 基础配置
2. **logback-spring.xml** - 高级配置（推荐）
3. **环境特定配置** - 不同环境的日志配置

## 配置方式

### 1. 基础配置 (application.yml)

在 `src/main/resources/application.yml` 中已配置基础日志设置：

```yaml
logging:
  level:
    root: INFO
    lab.zhang.rule: DEBUG
  file:
    name: logs/rule-engine.log
```

### 2. 高级配置 (logback-spring.xml)

项目提供了 `src/main/resources/logback-spring.xml` 文件，支持：

- **多文件输出**：
  - 普通日志：`logs/rule-engine.log`
  - 错误日志：`logs/rule-engine-error.log`
  - 规则评估日志：`logs/rule-engine-eval.log`

- **按大小和时间滚动**：
  - 单个文件最大10MB
  - 保留30天历史
  - 总大小限制（普通日志1GB，错误日志500MB）

- **环境区分**：
  - `dev` 环境：详细日志输出到控制台和文件
  - `prod` 环境：INFO级别日志，只输出到文件

### 3. 环境配置

#### 开发环境 (application-dev.yml)
```yaml
spring:
  profiles:
    active: dev
logging:
  level:
    root: DEBUG
    lab.zhang.rule: DEBUG
```

#### 生产环境 (application-prod.yml)
```yaml
spring:
  profiles:
    active: prod
logging:
  level:
    root: INFO
    lab.zhang.rule: INFO
  file:
    name: /var/log/rule-engine/rule-engine.log
```

## 自定义配置

### 修改日志等级

在相应配置文件中修改：

```yaml
logging:
  level:
    root: INFO                    # 根日志等级
    lab.zhang.rule: DEBUG         # 项目包日志等级
    com.baomidou.mybatisplus: WARN # MyBatis Plus日志等级
```

可用等级：TRACE, DEBUG, INFO, WARN, ERROR, FATAL

### 修改日志路径

#### 方法1：在application.yml中设置
```yaml
logging:
  file:
    name: /your/custom/path/app.log
```

#### 方法2：在logback-spring.xml中修改
```xml
<property name="LOG_DIR" value="/your/custom/path"/>
```

### 添加新的日志文件

在 `logback-spring.xml` 中添加新的appender：

```xml
<!-- 自定义日志文件 -->
<appender name="CUSTOM_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_FILE}-custom.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_FILE}-custom.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
        <maxHistory>${MAX_HISTORY}</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

然后为特定logger添加该appender：

```xml
<logger name="your.package" level="DEBUG" additivity="false">
    <appender-ref ref="CUSTOM_FILE"/>
</logger>
```

## 启动参数

也可以通过JVM参数覆盖日志配置：

```bash
# 设置日志等级
-Dlogging.level.root=DEBUG
-Dlogging.level.lab.zhang.rule=TRACE

# 设置日志文件路径
-Dlogging.file.name=/tmp/myapp.log
```

## 监控和维护

### 日志轮转

系统会自动进行日志轮转，无需手动处理。

### 日志清理

- 设置了最大历史天数（30天）
- 设置了总大小限制，超出会删除旧日志

### 性能考虑

生产环境建议：
- 使用INFO或WARN级别
- 避免TRACE级别（性能影响大）
- 定期清理旧日志文件

## 故障排除

### 日志不输出到文件
1. 检查日志目录权限
2. 确认路径是否存在
3. 查看控制台错误信息

### 日志文件过大
1. 降低日志等级
2. 调整maxFileSize参数
3. 启用更多过滤器

### 特定包日志不生效
1. 检查包名是否正确
2. 确认继承关系（additivity属性）
