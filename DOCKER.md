# Docker Build and Deployment Guide

This document explains how to build the rule engine project as a Docker image and run it.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+ (optional, for local development)

## Quick Start

### 1. Build Docker Image

#### Method 1: Using Build Script (Recommended)

```bash
# Build latest version
./build-docker.sh

# Build specified version
./build-docker.sh v1.0.0
```

#### Method 2: Using Docker Command

```bash
# Build image
docker build -t rule-engine:latest .

# Build specified version
docker build -t rule-engine:v1.0.0 .
```

### 2. Run Docker Image

#### Method 1: Using Docker Compose (Recommended, includes all dependencies)

```bash
# Start all services (MySQL, Redis, RabbitMQ, Rule Engine)
docker-compose up -d

# View logs
docker-compose logs -f rule-engine

# Stop all services
docker-compose down

# Stop and delete volumes
docker-compose down -v
```

#### Method 2: Run Application Container Separately

**Note**: Need to start MySQL, Redis and other services first, or use external services.

```bash
# Run container
docker run -d \
  --name rule-engine \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/rule_engine \
  -e SPRING_DATASOURCE_USERNAME=zhang \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  rule-engine:latest
```

### 3. Development Environment (Start Dependency Services Only)

```bash
# Start MySQL and Redis (do not start application)
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop services
docker-compose -f docker-compose.dev.yml down
```

## Docker Compose Configuration Description

### docker-compose.yml (Production Environment)

Contains the following services:
- **mysql**: MySQL 8.0 database
- **redis**: Redis 7 cache
- **rabbitmq**: RabbitMQ 3.12 message queue (optional)
- **rule-engine**: Rule engine application

### docker-compose.dev.yml (Development Environment)

Only contains dependency services:
- **mysql**: MySQL 8.0 database
- **redis**: Redis 7 cache

## Environment Variable Configuration

### Required Configuration

| Variable Name | Description | Default Value |
|--------------|-------------|--------------|
| `SPRING_DATASOURCE_URL` | MySQL database connection URL | - |
| `SPRING_DATASOURCE_USERNAME` | Database username | - |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - |

### Optional Configuration

| Variable Name | Description | Default Value |
|--------------|-------------|--------------|
| `SPRING_PROFILES_ACTIVE` | Spring configuration file | `prod` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host | `rabbitmq` |
| `SPRING_RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RULE_ENGINE_ENVIRONMENT` | Rule engine environment (TEST/GRAY/PRODUCTION) | `PRODUCTION` |
| `JAVA_OPTS` | JVM parameters | `-Xms512m -Xmx1024m -XX:+UseG1GC` |

## Health Check

Health check is configured in Dockerfile using the following command:

```bash
curl -f http://localhost:8080/actuator/health
```

**Note**: If Spring Boot Actuator dependency is not added to the project, health check will fail. You can:

1. **Add Actuator dependency** (recommended):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. **Modify health check command**:
Modify `HEALTHCHECK` in Dockerfile to:
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/events || exit 1
```

## Build Optimization

### Multi-stage Build

Dockerfile uses multi-stage build:
1. **Build stage**: Uses Maven image to compile and package application
2. **Run stage**: Uses lightweight JRE image to run application

This reduces the final image size.

### Build Cache

Dockerfile optimizes build cache:
- First copy `pom.xml` and download dependencies (if `pom.xml` doesn't change, this layer will be cached)
- Then copy source code and compile

### Image Size Optimization

- Use `openjdk:8-jre-slim` instead of full JDK
- Run application with non-root user
- Exclude unnecessary files (via `.dockerignore`)

## Common Commands

### View Images

```bash
docker images | grep rule-engine
```

### View Containers

```bash
docker ps -a | grep rule-engine
```

### View Logs

```bash
# Docker Compose
docker-compose logs -f rule-engine

# Docker
docker logs -f rule-engine
```

### Enter Container

```bash
docker exec -it rule-engine bash
```

### Stop and Delete

```bash
# Stop container
docker stop rule-engine

# Delete container
docker rm rule-engine

# Delete image
docker rmi rule-engine:latest
```

## Push to Image Registry

### Docker Hub

```bash
# Login
docker login

# Tag image
docker tag rule-engine:latest <username>/rule-engine:latest

# Push
docker push <username>/rule-engine:latest
```

### Private Registry

```bash
# Tag image
docker tag rule-engine:latest <registry-host>:<port>/rule-engine:latest

# Push
docker push <registry-host>:<port>/rule-engine:latest
```

## Production Deployment Recommendations

1. **Use specific version tags**: Do not use `latest` tag
   ```bash
   docker build -t rule-engine:v1.0.0 .
   ```

2. **Configure resource limits**:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 2G
       reservations:
         cpus: '1'
         memory: 1G
   ```

3. **Configure log driver**:
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

4. **Use secrets to manage sensitive information**:
   ```yaml
   secrets:
     db_password:
       external: true
   ```

5. **Configure network**: Use custom network to isolate services

6. **Regularly update base images**: Keep security patches updated

## Troubleshooting

### 1. Container Cannot Start

```bash
# View container logs
docker logs rule-engine

# View container status
docker inspect rule-engine
```

### 2. Database Connection Failed

```bash
# Check if MySQL container is running
docker ps | grep mysql

# Check network connection
docker exec -it rule-engine ping mysql
```

### 3. Port Already in Use

```bash
# Check port usage
lsof -i :8080

# Modify port mapping in docker-compose.yml
ports:
  - "8081:8080"  # Change host port to 8081
```

### 4. Insufficient Memory

```bash
# Adjust JVM parameters
docker run -e JAVA_OPTS="-Xms256m -Xmx512m" rule-engine:latest
```

## Notes

1. **Database Initialization**: On first startup, MySQL will automatically execute `schema.sql` to initialize database
2. **Data Persistence**: Use Docker volumes to persist MySQL, Redis, RabbitMQ data
3. **Timezone Setting**: All containers are set to `Asia/Shanghai` timezone
4. **Health Check**: Ensure application starts before health check (start-period=40s)
5. **Log Directory**: Application logs will be mounted to `./logs` directory

## Reference Documentation

- [Docker Official Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
