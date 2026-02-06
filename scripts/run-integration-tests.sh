#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

SKIP_ANDROID=false
VERBOSE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-android)
            SKIP_ANDROID=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--skip-android] [--verbose]"
            exit 1
            ;;
    esac
done

log() {
    echo "[integration-tests] $1"
}

cleanup() {
    log "Cleaning up test infrastructure..."
    cd "$PROJECT_DIR"
    docker compose -f docker-compose.test.yml down -v --remove-orphans 2>/dev/null || true
}

# Trap EXIT for cleanup
trap cleanup EXIT

log "Starting test infrastructure..."
cd "$PROJECT_DIR"

# Build and start containers
if [ "$VERBOSE" = true ]; then
    docker compose -f docker-compose.test.yml up -d --build
else
    docker compose -f docker-compose.test.yml up -d --build 2>&1 | grep -v "^$" || true
fi

log "Waiting for API to be healthy..."

# Poll for API health
MAX_ATTEMPTS=60
ATTEMPT=0
API_URL="http://localhost:8000/docs"

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s -f "$API_URL" > /dev/null 2>&1; then
        log "API is healthy!"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    if [ "$VERBOSE" = true ]; then
        echo "Waiting for API... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
    fi
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    log "ERROR: API failed to become healthy after $MAX_ATTEMPTS attempts"
    if [ "$VERBOSE" = true ]; then
        log "Container logs:"
        docker compose -f docker-compose.test.yml logs
    fi
    exit 1
fi

if [ "$SKIP_ANDROID" = true ]; then
    log "Skipping Android tests (--skip-android flag set)"
    log "Test infrastructure is running. Press Ctrl+C to stop."
    # Keep running until interrupted
    while true; do
        sleep 1
    done
else
    log "Running Android integration tests..."
    cd "$PROJECT_DIR/android"

    # Set required environment variables for Android builds
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    export ANDROID_HOME=~/Library/Android/sdk
    export PATH="$ANDROID_HOME/platform-tools:$PATH"

    # Run only integration tests (not UI component tests)
    if [ "$VERBOSE" = true ]; then
        ./gradlew connectedLocalDebugAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.package=com.thecityandthebike.integration \
            --info
    else
        ./gradlew connectedLocalDebugAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.package=com.thecityandthebike.integration
    fi

    log "Integration tests completed successfully!"
fi
