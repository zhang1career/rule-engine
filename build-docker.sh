#!/bin/bash

# Docker build script for rule-engine
# Usage: ./build-docker.sh [tag]

set -e

# Default values
IMAGE_NAME="rule-engine"
DEFAULT_TAG="latest"
TAG=${1:-$DEFAULT_TAG}
FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"

echo "=========================================="
echo "Building Docker image: ${FULL_IMAGE_NAME}"
echo "=========================================="

# Build the Docker image
docker build -t ${FULL_IMAGE_NAME} .

# Also tag as latest if a specific tag was provided
if [ "$TAG" != "$DEFAULT_TAG" ]; then
    echo "Tagging as latest..."
    docker tag ${FULL_IMAGE_NAME} ${IMAGE_NAME}:latest
fi

echo "=========================================="
echo "Build completed successfully!"
echo "Image: ${FULL_IMAGE_NAME}"
echo ""
echo "To run the image:"
echo "  docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
echo ""
echo "To push to registry:"
echo "  docker tag ${FULL_IMAGE_NAME} <registry>/${FULL_IMAGE_NAME}"
echo "  docker push <registry>/${FULL_IMAGE_NAME}"
echo "=========================================="

