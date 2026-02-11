# The City And The Bike (TCATB)

A mobile app for discovering, photographing, and cataloging graffiti tags on the rear fenders of rental bikes.

## Quick Start

```bash
docker-compose up -d --build    # start PostgreSQL + API
curl http://localhost:5000/health  # verify API is running
```

Optionally configure secrets first (`cp .env.example .env`), otherwise dev defaults are used.

To stop: `docker-compose down`

## Component Documentation

- [API](api/README.md) -- local development, environment variables, and endpoints
- [Android App](android/README.md) -- building and running

## Integration Tests

End-to-end tests run the Android app against a real backend (Docker + connected device/emulator).

### Quick Run

```bash
./scripts/run-integration-tests.sh
```

This starts a test backend, runs the Android integration tests, and cleans up containers on exit.

Options: `--skip-android` (start backend only), `--verbose` (show container logs on failure).

### Running Specific Tests

```bash
# Start test backend
docker compose -f docker-compose.test.yml up -d --build

# Wait for API
curl -f http://localhost:8000/docs

# Run a specific test class
cd android && ./gradlew connectedLocalDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.thecityandthebike.integration.AuthIntegrationTest

# Clean up
docker compose -f docker-compose.test.yml down -v
```

Requires `JAVA_HOME`, `ANDROID_HOME`, and `PATH` to be set (see [android/README.md](android/README.md)).

### Device Connectivity

Tests auto-detect emulator vs physical device:

- **Emulator**: connects to `10.0.2.2:8000`
- **Physical device**: connects to `10.0.0.17:8000`

To change the physical device IP, edit `android/app/src/androidTest/kotlin/com/thecityandthebike/integration/IntegrationTestUtils.kt`.
