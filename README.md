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
├── DESIGN.md            # Application design document
├── README.md            # Project README
├── docker-compose.yml   # Docker Compose config for DB and API
├── api/                 # FastAPI service
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
├── android/             # Android app (Jetpack Compose)
│   ├── app/
│   └── gradlew
└── db/                  # Database initialization
    └── init/
        └── schema.sql   # SQL schema initialization
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

---
_Phase 1: Database setup and schema initialization for The City And The Bike._
