# Rule Engine Docker 部署指南

本文档介绍如何使用Docker部署Rule Engine应用，支持gray和prod环境，并允许外部配置MySQL连接信息。

## 目录结构

```
.
├── Dockerfile                    # Docker镜像构建文件
├── docker-compose.gray.yml       # Gray环境完整部署配置
├── docker-compose.prod.yml       # Prod环境完整部署配置
├── docker-build.sh              # 构建脚本
├── docker-run-gray.sh           # Gray环境快速启动脚本
├── docker-run-prod.sh           # Prod环境快速启动脚本
└── DOCKER_README.md             # 本文档
```

## 快速开始

### 1. 构建Docker镜像

首先确保项目已构建：

```bash
# 构建应用
mvn clean package -DskipTests

# 构建Docker镜像
./docker-build.sh

# 或指定版本号
./docker-build.sh v1.0.0
```

### 2. 配置环境变量

#### Gray环境配置
```bash
export MYSQL_HOST=your-mysql-host
export MYSQL_PORT=3306
export MYSQL_USER=your-username
export MYSQL_PASS=your-password

# 可选配置
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
export REDIS_DB=0
```

#### Production环境配置
```bash
export MYSQL_HOST=prod-mysql-host
export MYSQL_PORT=3306
export MYSQL_USER=prod-username
export MYSQL_PASS=prod-password

# 可选配置
export REDIS_HOST=prod-redis-host
# ... 其他配置
```

### 3. 启动应用

#### 方式1：使用启动脚本（推荐）

```bash
# Gray环境
./docker-run-gray.sh

# Production环境
./docker-run-prod.sh
```

#### 方式2：使用docker-compose（完整环境）

```bash
# Gray环境（包含MySQL和Redis）
docker-compose -f docker-compose.gray.yml up -d

# Production环境（包含MySQL、Redis）
docker-compose -f docker-compose.prod.yml up -d
```

#### 方式3：直接使用docker run

```bash
# Gray环境
docker run -d \
  --name rule-engine-gray \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=gray \
  -e MYSQL_HOST=$MYSQL_HOST \
  -e MYSQL_PORT=$MYSQL_PORT \
  -e MYSQL_USER=$MYSQL_USER \
  -e MYSQL_PASS=$MYSQL_PASS \
  rule-engine:latest

# Production环境
docker run -d \
  --name rule-engine-prod \
  -p 8080:8080 \
  --restart unless-stopped \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MYSQL_HOST=$MYSQL_HOST \
  -e MYSQL_USER=$MYSQL_USER \
  -e MYSQL_PASS=$MYSQL_PASS \
  -v ./logs/prod:/var/log/rule-engine \
  --memory=1.5g \
  rule-engine:latest
```

## 环境说明

### Gray环境 (application-gray.yml)
- **用途**: 预发布/灰度测试环境
- **日志级别**: INFO级别
- **数据库**: 支持环境变量配置MySQL连接
- **缓存**: 支持Redis配置
- **日志路径**: `/var/log/rule-engine/gray/`

### Production环境 (application-prod.yml)
- **用途**: 生产环境
- **日志级别**: INFO级别，第三方库使用WARN
- **数据库**: 必须通过环境变量配置MySQL
- **缓存**: 支持Redis
- **日志路径**: `/var/log/rule-engine/`
- **资源限制**: 内存1.5GB，1个CPU核心

## 环境变量配置

### 必需的环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `MYSQL_HOST` | MySQL主机地址 | localhost |
| `MYSQL_PORT` | MySQL端口 | 3306 |
| `MYSQL_USER` | MySQL用户名 | (必需) |
| `MYSQL_PASS` | MySQL密码 | (必需) |

### 可选的环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `REDIS_HOST` | Redis主机地址 | localhost |
| `REDIS_PORT` | Redis端口 | 6379 |
| `REDIS_PASSWORD` | Redis密码 | (空) |
| `REDIS_DB` | Redis数据库编号 | 0 |

## 日志管理

### 日志文件位置

- **容器内**: `/var/log/rule-engine/`
- **宿主机**: `./logs/` 目录（通过volume挂载）

### 日志文件类型

- `rule-engine.log`: 主要应用日志
- `rule-engine-error.log`: 错误日志
- `rule-engine-evalRequest.log`: 规则评估专用日志

### 查看日志

```bash
# 查看容器日志
docker logs -f rule-engine-prod

# 查看文件日志
tail -f logs/prod/*.log
```

## 健康检查

应用启动后，可以通过以下方式检查健康状态：

```bash
# HTTP健康检查
curl http://localhost:8080/actuator/health

# 容器状态检查
docker ps | grep rule-engine
```

## 故障排除

### 常见问题

1. **MySQL连接失败**
   ```bash
   # 检查环境变量
   echo $MYSQL_HOST $MYSQL_USER

   # 检查MySQL服务
   docker exec -it mysql-prod mysql -u$MYSQL_USER -p$MYSQL_PASS -e "SELECT 1"
   ```

2. **端口占用**
   ```bash
   # 查找占用8080端口的进程
   lsof -i :8080

   # 使用不同端口
   docker run -p 8081:8080 ...
   ```

3. **内存不足**
   ```bash
   # 检查系统内存
   free -h

   # 调整容器内存限制
   docker run --memory=1g ...
   ```

### 调试模式

启用调试日志：

```bash
docker run -e JAVA_OPTS="-Xmx512m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" ...
```

## 生产部署建议

1. **使用外部数据库**: 不要使用docker-compose内置的MySQL
2. **配置监控**: 设置日志聚合和监控告警
3. **备份策略**: 定期备份数据库和日志
4. **安全加固**: 使用防火墙和安全组限制访问
5. **高可用**: 考虑使用负载均衡和多实例部署

## 更新部署

```bash
# 停止旧容器
docker stop rule-engine-prod

# 构建新镜像
./docker-build.sh v1.1.0

# 启动新容器
./docker-run-prod.sh

# 删除旧镜像（可选）
docker image prune -f
```
