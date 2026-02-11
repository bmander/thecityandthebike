# TCATB API

FastAPI backend for The City And The Bike.

## Local Development

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

## Environment Variables

When implementing backend services, use the following environment variables for database connectivity:
```bash
PGHOST=localhost
PGPORT=5432
PGUSER=tcatb
PGPASSWORD=<your-password>
PGDATABASE=tcatb_dev
JWT_SECRET_KEY=<your-secret-key>
```

See `.env.example` for a template you can copy to `.env`.

**Production deployments must use unique, strong secrets.** The defaults in `docker-compose.yml` are for local development only and must not be reused in production. Copy `.env.example` to `.env` and set `POSTGRES_PASSWORD` and `JWT_SECRET_KEY` to secure values.

## Available Endpoints

Health:
- GET /health

Auth:
- POST /auth/register
- POST /auth/login
- POST /auth/refresh
- POST /auth/logout

Users:
- GET /users/me
- GET /users/me/submissions
- GET /users/{user_id}
- GET /users/{user_id}/submissions

Submissions:
- GET /submissions
- GET /submissions/{submission_id}
- POST /submissions
- DELETE /submissions/{submission_id}

Bikes:
- GET /bikes/{bike_qr_id}
- GET /bikes/{bike_qr_id}/submissions

Uploads:
- POST /uploads/images
- GET /uploads/images/{filename}

An admin panel is also available at /admin.

## CI/CD Configuration

The Android build needs the staging API URL to be set as a GitHub Actions repository variable. To find the current Cloud Run service URL:

```bash
gcloud run services describe tcatb-api-staging --region=us-central1 --format='value(status.url)'
```

Then set it in the repo:

```bash
gh variable set API_BASE_URL --body "<service-url>"
```
