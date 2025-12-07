#!/bin/bash

# Rule Engine Production Environment Docker Run Script
# This script helps you run the rule engine in production environment with external configuration

set -e

echo "🚀 Starting Rule Engine in PRODUCTION environment..."

# Required environment variables
MYSQL_HOST=${MYSQL_HOST:-"localhost"}
MYSQL_PORT=${MYSQL_PORT:-"3306"}
MYSQL_USER=${MYSQL_USER}
MYSQL_PASS=${MYSQL_PASS}

# Optional environment variables
REDIS_HOST=${REDIS_HOST:-"localhost"}
REDIS_PORT=${REDIS_PORT:-"6379"}
REDIS_PASSWORD=${REDIS_PASSWORD:-""}
REDIS_DB=${REDIS_DB:-"0"}

RABBITMQ_HOST=${RABBITMQ_HOST:-"localhost"}
RABBITMQ_PORT=${RABBITMQ_PORT:-"5672"}
RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-"guest"}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-"guest"}

# Check required environment variables
if [ -z "$MYSQL_USER" ] || [ -z "$MYSQL_PASS" ]; then
    echo "❌ Error: MYSQL_USER and MYSQL_PASS environment variables are required"
    echo "   Please set them with:"
    echo "   export MYSQL_USER=your_username"
    echo "   export MYSQL_PASS=your_password"
    exit 1
fi

# Create logs directory
mkdir -p logs/prod

echo "📋 Production Configuration:"
echo "   Environment: PRODUCTION"
echo "   MySQL: $MYSQL_HOST:$MYSQL_PORT (user: $MYSQL_USER)"
echo "   Redis: $REDIS_HOST:$REDIS_PORT"
echo "   RabbitMQ: $RABBITMQ_HOST:$RABBITMQ_PORT"
echo "   Logs: ./logs/prod/"
echo ""

# Stop existing container if running
if docker ps -q -f name=rule-engine-prod | grep -q .; then
    echo "🛑 Stopping existing rule-engine-prod container..."
    docker stop rule-engine-prod
    docker rm rule-engine-prod
fi

# Run Docker container with production settings
docker run -d \
  --name rule-engine-prod \
  -p 8080:8080 \
  --restart unless-stopped \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MYSQL_HOST="$MYSQL_HOST" \
  -e MYSQL_PORT="$MYSQL_PORT" \
  -e MYSQL_USER="$MYSQL_USER" \
  -e MYSQL_PASS="$MYSQL_PASS" \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT="$REDIS_PORT" \
  -e REDIS_PASSWORD="$REDIS_PASSWORD" \
  -e REDIS_DB="$REDIS_DB" \
  -e RABBITMQ_HOST="$RABBITMQ_HOST" \
  -e RABBITMQ_PORT="$RABBITMQ_PORT" \
  -e RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
  -e RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
  -v "$(pwd)/logs/prod:/var/log/rule-engine" \
  --memory="1.5g" \
  --cpus="1.0" \
  rule-engine:latest

echo "✅ Rule Engine (PRODUCTION) started successfully!"
echo "   Container: rule-engine-prod"
echo "   Port: 8080"
echo "   Auto-restart: enabled"
echo "   Memory limit: 1.5GB"
echo ""
echo "📊 View logs:"
echo "   docker logs -f rule-engine-prod"
echo "   or check ./logs/prod/ directory"
echo ""
echo "🔍 Health check:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo "🛑 To stop:"
echo "   docker stop rule-engine-prod"
