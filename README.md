# The City And The Bike (TCATB)

TCATB is a mobile application for discovering, photographing, and cataloguing graffiti tags on the rear fenders of rental bikes.

## Phase 1: Backend Setup & Core API

This repository contains the database schema and Docker Compose configuration for Phase 1 of the project: setting up PostgreSQL and initializing the schema.

### Prerequisites
- Docker (>= 20.10)
- Docker Compose (>= 1.29)
- Git

### Project Structure
```
.
├── DESIGN.md            # Application design document
├── docker-compose.yml   # Docker Compose config for PostgreSQL
└── db/
    └── init/
        └── schema.sql   # SQL schema initialization
```

### Getting Started

1. Clone the repository and enter the project directory:
   ```bash
   git clone <repo-url>
   cd thecityandthebike
   ```

2. Start the PostgreSQL database:
   ```bash
   docker-compose up -d
   ```

3. Verify the database container is running:
   ```bash
   docker-compose ps
   ```

4. Connect to the database using `psql`:
   ```bash
   psql -h localhost -U tcatb -d tcatb_dev
   # Password: tcatbpass
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
PGPASSWORD=tcatbpass
PGDATABASE=tcatb_dev
```

### Next Steps
- Build the Flask API for user authentication, bike registration, and fender submissions.
- Integrate object storage (e.g., AWS S3) for handling image uploads.
- Develop the Android application frontend (Phase 2).

---
_Phase 1: Database setup and schema initialization for The City And The Bike._