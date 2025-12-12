#!/bin/bash

# Rule Engine Docker Build Script
# This script builds the Docker image for the rule engine application

set -e

# Configuration
IMAGE_NAME="rule_engine"
IMAGE_TAG="latest"

echo "🏗️  Building Rule Engine Docker Image..."

# Check if JAR file exists
if ! ls target/rule_engine-*.jar 1> /dev/null 2>&1; then
    echo "❌ Error: JAR file not found in target/ directory"
    echo "   Please build the application first:"
    echo "   mvn clean package -DskipTests"
    exit 1
fi

# Get the JAR file name
JAR_FILE=$(ls target/rule_engine-*.jar | head -n1)
echo "📦 Using JAR file: $JAR_FILE"

# Build Docker image
echo "🐳 Building Docker image: $IMAGE_NAME:$IMAGE_TAG"
docker build -t "$IMAGE_NAME:$IMAGE_TAG" .

# Tag with version if provided
if [ ! -z "$1" ]; then
    VERSION_TAG="$1"
    docker tag "$IMAGE_NAME:$IMAGE_TAG" "$IMAGE_NAME:$VERSION_TAG"
    echo "🏷️  Tagged image as: $IMAGE_NAME:$VERSION_TAG"
fi

echo "✅ Docker image built successfully!"
echo "   Image: $IMAGE_NAME:$IMAGE_TAG"
echo ""
echo "🚀 Run with:"
echo "   docker-compose -f docker-compose.yml up -d"
