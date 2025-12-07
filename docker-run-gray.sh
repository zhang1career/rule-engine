#!/bin/bash

# Rule Engine Gray Environment Docker Run Script
# This script helps you run the rule engine in gray environment with external MySQL configuration

set -e

echo "🚀 Starting Rule Engine in GRAY environment..."

# Default values (can be overridden by environment variables)
MYSQL_HOST=${MYSQL_HOST:-"localhost"}
MYSQL_PORT=${MYSQL_PORT:-"3306"}
MYSQL_USER=${MYSQL_USER:-"rule_engine"}
MYSQL_PASS=${MYSQL_PASS:-""}

REDIS_HOST=${REDIS_HOST:-"localhost"}
REDIS_PORT=${REDIS_PORT:-"6379"}
REDIS_PASSWORD=${REDIS_PASSWORD:-""}
REDIS_DB=${REDIS_DB:-"0"}

# Check required environment variables
if [ -z "$MYSQL_PASS" ]; then
    echo "❌ Error: MYSQL_PASS environment variable is required"
    echo "   Please set it with: export MYSQL_PASS=your_password"
    exit 1
fi

# Create logs directory
mkdir -p logs/gray

echo "📋 Configuration:"
echo "   Environment: GRAY"
echo "   MySQL Host: $MYSQL_HOST:$MYSQL_PORT"
echo "   MySQL User: $MYSQL_USER"
echo "   Redis Host: $REDIS_HOST:$REDIS_PORT"
echo "   Logs: ./logs/gray/"
echo ""

# Run Docker container
docker run -d \
  --name rule-engine-gray \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=gray \
  -e MYSQL_HOST="$MYSQL_HOST" \
  -e MYSQL_PORT="$MYSQL_PORT" \
  -e MYSQL_USER="$MYSQL_USER" \
  -e MYSQL_PASS="$MYSQL_PASS" \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT="$REDIS_PORT" \
  -e REDIS_PASSWORD="$REDIS_PASSWORD" \
  -e REDIS_DB="$REDIS_DB" \
  -v "$(pwd)/logs/gray:/var/log/rule-engine/gray" \
  rule-engine:latest

echo "✅ Rule Engine started successfully!"
echo "   Container: rule-engine-gray"
echo "   Port: 8080"
echo "   Health check: curl http://localhost:8080/actuator/health"
echo ""
echo "📊 View logs:"
echo "   docker logs -f rule-engine-gray"
echo "   or check ./logs/gray/ directory"
