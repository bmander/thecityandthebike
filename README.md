# The City And The Bike (TCATB)

TCATB is a mobile application for discovering, photographing, and cataloguing graffiti tags on the rear fenders of rental bikes.

## Phase 1: Backend Setup & Core API

This repository contains the database schema, FastAPI API code, and Docker Compose configuration for Phase 1 of the project: setting up PostgreSQL, initializing the schema, and serving the backend API.

### Prerequisites
- Docker (>= 20.10)
- Docker Compose (>= 1.29)
- Git

### Project Structure
```
.
├── DESIGN.md                # Application design document
├── README.md                # Project README
├── docker-compose.yml       # Docker Compose config for DB and API
├── docker-compose.test.yml  # Test infrastructure (isolated DB + API)
├── scripts/
│   └── run-integration-tests.sh  # E2E test orchestration script
├── api/                     # FastAPI service
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py           # FastAPI app entry point
│   │   ├── config.py         # Pydantic BaseSettings
│   │   ├── database.py       # SQLAlchemy engine/session
│   │   ├── dependencies.py   # Auth & DB dependencies
│   │   ├── models/
│   │   │   └── orm.py        # SQLAlchemy models
│   │   ├── schemas/          # Pydantic schemas
│   │   │   ├── auth.py
│   │   │   ├── user.py
│   │   │   ├── submission.py
│   │   │   └── bike.py
│   │   └── routers/          # API route handlers
│   │       ├── auth.py
│   │       ├── users.py
│   │       ├── submissions.py
│   │       └── bikes.py
│   ├── requirements.txt
│   └── Dockerfile
├── android/                 # Android app (Jetpack Compose)
│   ├── app/
│   │   └── src/androidTest/kotlin/com/thecityandthebike/
│   │       └── integration/  # E2E integration tests
│   │           ├── IntegrationTestUtils.kt
│   │           ├── AuthIntegrationTest.kt
│   │           └── SubmissionsIntegrationTest.kt
│   └── gradlew
└── db/                      # Database initialization
    └── init/
        └── schema.sql       # SQL schema initialization
```

### Getting Started

1. Clone the repository and enter the project directory:
   ```bash
   git clone <repo-url>
   cd thecityandthebike
   ```

2. Start services (PostgreSQL database and API):
   ```bash
   docker-compose up -d --build
   ```

3. Verify the services are running:
   ```bash
   docker-compose ps
   ```

4. Connect to the database using `psql`:
   ```bash
   psql -h localhost -U tcatb -d tcatb_dev
   # Enter your database password when prompted
   ```
   You should see the tables: `users`, `bikes`, and `fender_submissions`.

5. To stop and remove the containers:
   ```bash
   docker-compose down
   ```

### Environment Variables

When implementing backend services, use the following environment variables for database connectivity:
```bash
PGHOST=localhost
PGPORT=5432
PGUSER=tcatb
PGPASSWORD=<your-password>
PGDATABASE=tcatb_dev
JWT_SECRET_KEY=<your-secret-key>
```

See `api/.env.example` for a template you can copy to `api/.env`.

### Next Steps
- Integrate object storage (e.g., AWS S3) for handling image uploads.
- Develop the Android application frontend (Phase 2).

## API (Local Development)

1. Change into the API directory:

   ```bash
   cd api
   ```

2. Create and activate a virtual environment:

   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

4. Set up environment variables:

   ```bash
   cp .env.example .env
   # Edit .env and fill in your database password and JWT secret key
   ```

5. Run the API server:

   ```bash
   uvicorn app.main:app --reload --port 5000
   ```

The API will be available at http://localhost:5000

Interactive API documentation (Swagger UI) is available at http://localhost:5000/docs

### Available Endpoints

Auth:
- POST /auth/register
- POST /auth/login

Users:
- GET /users/me
- GET /users/me/submissions

Submissions:
- GET /submissions
- POST /submissions

Bikes:
- GET /bikes/{bike_qr_id}/submissions

## Android App

The Android app is built with Jetpack Compose and targets SDK 34 (minSdk 26).

### Prerequisites
- Android SDK (typically installed via Android Studio)
- Java 17+ (Android Studio's bundled JDK works)
- A connected Android device with USB debugging enabled

### Project Structure
```
android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/thecityandthebike/
│       │   └── MainActivity.kt
│       └── res/values/
│           └── strings.xml
├── build.gradle
├── settings.gradle
├── gradle.properties
├── local.properties
└── gradlew
```

### Building and Running

1. Set up environment variables:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   export ANDROID_HOME=~/Library/Android/sdk
   export PATH="$ANDROID_HOME/platform-tools:$PATH"
   ```

2. Build the debug APK:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

3. Install on a connected device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. Launch the app:
   ```bash
   adb shell am start -n com.thecityandthebike/.MainActivity
   ```

## End-to-End Integration Tests

The project includes end-to-end integration tests that run the Android app against a real backend. These tests verify that the Android client can successfully communicate with the API for authentication and data operations.

### Prerequisites
- Docker and Docker Compose
- Android SDK with a connected device or running emulator
- Java 21+ (Android Studio's bundled JDK)

### Running Integration Tests

The easiest way to run integration tests is using the orchestration script:

```bash
./scripts/run-integration-tests.sh
```

This script will:
1. Start a test backend (PostgreSQL + API) using Docker Compose
2. Wait for the API to be healthy
3. Run the Android integration tests
4. Clean up containers when done (even on failure)

### Script Options

```bash
# Run full integration tests
./scripts/run-integration-tests.sh

# Start backend only (useful for manual testing or debugging)
./scripts/run-integration-tests.sh --skip-android

# Verbose output (shows container logs on failure)
./scripts/run-integration-tests.sh --verbose
```

### Running Specific Test Classes

You can run individual test classes directly with Gradle:

```bash
# Set up environment
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME=~/Library/Android/sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# Start test backend manually
docker compose -f docker-compose.test.yml up -d --build

# Wait for API to be ready
curl -f http://localhost:8000/docs

# Run auth tests only
cd android && ./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.thecityandthebike.integration.AuthIntegrationTest

# Run submission tests only
cd android && ./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.thecityandthebike.integration.SubmissionsIntegrationTest

# Run all integration tests
cd android && ./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.thecityandthebike.integration

# Clean up when done
docker compose -f docker-compose.test.yml down -v
```

### Physical Device vs Emulator

The integration tests automatically detect whether they're running on an emulator or physical device:

- **Emulator**: Tests connect to `http://10.0.2.2:8000/` (Android's special alias for host localhost)
- **Physical Device**: Tests connect to `http://10.0.0.17:8000/` (host machine's local IP)

If using a physical device on a different network, update the IP address in:
`android/app/src/androidTest/kotlin/com/thecityandthebike/integration/IntegrationTestUtils.kt`

### Test Coverage

The integration tests cover:

**AuthIntegrationTest** (6 tests):
- Register with valid credentials
- Register with duplicate username/email (conflict handling)
- Login after registration
- Login with invalid password
- Login with nonexistent user

**SubmissionsIntegrationTest** (6 tests):
- Get submissions with valid token
- Get submissions without token (auth required)
- Get user's own submissions
- Create submission with valid data
- Create submission without token (auth required)
- Verify created submission appears in user's list

---
_Phase 1: Database setup and schema initialization for The City And The Bike._
