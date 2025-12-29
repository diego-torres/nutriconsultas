#!/bin/bash

# Script to start the database container and run the Spring Boot application
# Uses podman-compose (or podman with docker-compose compatibility)

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo -e "${GREEN}✓ Loaded environment variables from .env${NC}"
else
    echo -e "${YELLOW}⚠ Warning: .env file not found. Using default values.${NC}"
fi

# Check if podman is installed
if ! command -v podman &> /dev/null; then
    echo -e "${RED}✗ Error: podman is not installed. Please install podman first.${NC}"
    exit 1
fi

# Check if podman-compose is available, otherwise use podman with docker-compose
USE_COMPOSE=false
if podman compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="podman compose"
    USE_COMPOSE=true
    echo -e "${GREEN}✓ Using podman compose${NC}"
elif command -v podman-compose &> /dev/null; then
    COMPOSE_CMD="podman-compose"
    USE_COMPOSE=true
    echo -e "${GREEN}✓ Using podman-compose${NC}"
else
    echo -e "${YELLOW}⚠ podman-compose not available. Will use podman directly${NC}"
    USE_COMPOSE=false
fi

CONTAINER_NAME="nutriconsultas-db"

# Function to check if container is running
is_container_running() {
    podman ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"
}

# Function to check if container exists (even if stopped)
container_exists() {
    podman ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"
}

# Start database container if not running
if is_container_running; then
    echo -e "${GREEN}✓ Database container '${CONTAINER_NAME}' is already running${NC}"
elif container_exists; then
    echo -e "${YELLOW}⚠ Database container exists but is not running. Starting it...${NC}"
    podman start ${CONTAINER_NAME}
    echo -e "${GREEN}✓ Started database container${NC}"
else
    echo -e "${YELLOW}⚠ Database container does not exist. Creating and starting it...${NC}"
    if [ "$USE_COMPOSE" = true ]; then
        # Use docker-compose/podman-compose
        ${COMPOSE_CMD} up -d postgres
    else
        # Fallback: use podman directly to create and run the container
        echo -e "${YELLOW}⚠ Creating container manually with podman...${NC}"
        podman run -d \
            --name ${CONTAINER_NAME} \
            -e POSTGRES_DB=${POSTGRES_DB:-nutriconsultas} \
            -e POSTGRES_USER=${POSTGRES_USER:-nutriconsultas} \
            -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-nutriconsultas} \
            -p ${POSTGRES_PORT:-5432}:5432 \
            -v nutriconsultas_postgres_data:/var/lib/postgresql/data \
            --health-cmd="pg_isready -U ${POSTGRES_USER:-nutriconsultas}" \
            --health-interval=10s \
            --health-timeout=5s \
            --health-retries=5 \
            postgres:16-alpine
    fi
    echo -e "${GREEN}✓ Created and started database container${NC}"
fi

# Wait for database to be ready
echo -e "${YELLOW}⏳ Waiting for database to be ready...${NC}"
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if podman exec ${CONTAINER_NAME} pg_isready -U ${POSTGRES_USER:-nutriconsultas} &> /dev/null; then
        echo -e "${GREEN}✓ Database is ready!${NC}"
        break
    fi
    attempt=$((attempt + 1))
    sleep 1
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}✗ Error: Database did not become ready in time${NC}"
    exit 1
fi

# Export environment variables for Spring Boot
export JDBC_DATABASE_URL=${JDBC_DATABASE_URL:-jdbc:postgresql://localhost:5432/nutriconsultas}
export JDBC_DATABASE_USERNAME=${JDBC_DATABASE_USERNAME:-nutriconsultas}
export JDBC_DATABASE_PASSWORD=${JDBC_DATABASE_PASSWORD:-nutriconsultas}

# Run Spring Boot application
echo -e "${GREEN}✓ Starting Spring Boot application...${NC}"
echo ""
mvn spring-boot:run

