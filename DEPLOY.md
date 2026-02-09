# Deployment Guide

## Architecture

```
GCP Project: tcatb-app
Region: us-central1

Backend:  Cloud Run  ->  Cloud SQL (PostgreSQL 14)  +  Cloud Storage (GCS)
Android:  Firebase App Distribution (staging)  /  Play Store (production)
CI/CD:    GitHub Actions
Secrets:  GCP Secret Manager
Images:   GCP Artifact Registry
```

## Environments

| | Staging | Production |
|---|---|---|
| **Branch** | `develop` | `main` |
| **API URL** | https://tcatb-api-staging-821600862601.us-central1.run.app | TBD (after first production deploy) |
| **Cloud Run Service** | `tcatb-api-staging` | `tcatb-api-production` |
| **Cloud SQL Instance** | `tcatb-staging-db` (db-f1-micro) | `tcatb-prod-db` (db-g1-small) |
| **Storage Bucket** | `tcatb-staging-uploads` | `tcatb-production-uploads` |
| **Android Package** | `com.thecityandthebike.staging` | `com.thecityandthebike` |
| **Android Distribution** | Firebase App Distribution | Play Store internal track |

## Automated Deployment (CI/CD)

### Backend

Workflow: `.github/workflows/deploy-backend.yml`

Triggers on push to `develop` (staging) or `main` (production) when files in `api/` change.

Steps: run tests -> build Docker image -> push to Artifact Registry -> deploy to Cloud Run -> verify health check.

### Android

Workflow: `.github/workflows/deploy-android.yml`

Triggers on push to `develop` (staging) or `main` (production) when files in `android/` change.

Steps: run unit tests -> build APK/AAB -> upload to Firebase (staging) or Play Store (production).

## Manual Deployment

### Backend

```bash
# 1. Build
docker build -t us-central1-docker.pkg.dev/tcatb-app/tcatb-api/tcatb-api:TAG ./api

# 2. Push
docker push us-central1-docker.pkg.dev/tcatb-app/tcatb-api/tcatb-api:TAG

# 3. Deploy (staging)
gcloud run deploy tcatb-api-staging \
  --image=us-central1-docker.pkg.dev/tcatb-app/tcatb-api/tcatb-api:TAG \
  --region=us-central1 \
  --platform=managed \
  --allow-unauthenticated \
  --set-env-vars="PGHOST=/cloudsql/tcatb-app:us-central1:tcatb-staging-db" \
  --set-env-vars="PGPORT=5432" \
  --set-env-vars="PGUSER=tcatb" \
  --set-env-vars="PGDATABASE=tcatb" \
  --set-env-vars="STORAGE_BUCKET=tcatb-staging-uploads" \
  --set-secrets="JWT_SECRET_KEY=jwt-secret-staging:latest" \
  --set-secrets="PGPASSWORD=db-password-staging:latest" \
  --add-cloudsql-instances=tcatb-app:us-central1:tcatb-staging-db \
  --min-instances=0 \
  --max-instances=10 \
  --memory=512Mi \
  --cpu=1 \
  --project=tcatb-app

# 4. Verify
curl https://tcatb-api-staging-821600862601.us-central1.run.app/health
```

For production, replace `staging` with `production` and `tcatb-staging-db` with `tcatb-prod-db` throughout.

### Android

```bash
cd android

# Staging APK
./gradlew assembleStagingRelease

# Production AAB
./gradlew bundleProductionRelease
```

Release builds require signing environment variables: `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

## GCP Resources

### Secrets (Secret Manager)

| Secret | Used By |
|--------|---------|
| `jwt-secret-staging` | Staging Cloud Run |
| `jwt-secret-production` | Production Cloud Run |
| `db-password-staging` | Staging Cloud Run |
| `db-password-production` | Production Cloud Run |

Retrieve a secret value:
```bash
gcloud secrets versions access latest --secret=SECRET_NAME --project=tcatb-app
```

### Service Accounts

| Account | Purpose |
|---------|---------|
| `github-deployer@tcatb-app.iam.gserviceaccount.com` | GitHub Actions deploys (via Workload Identity Federation) |
| `821600862601-compute@developer.gserviceaccount.com` | Cloud Run runtime (default) |

### GitHub Repository Secrets

| Secret | Description |
|--------|-------------|
| `GCP_PROJECT_ID` | `tcatb-app` |
| `WIF_PROVIDER` | Workload Identity Federation provider (`projects/821600862601/locations/global/workloadIdentityPools/github-actions/providers/github`) |
| `WIF_SERVICE_ACCOUNT` | Service account for WIF (`github-deployer@tcatb-app.iam.gserviceaccount.com`) |
| `SIGNING_KEYSTORE_BASE64` | Android release keystore (base64) |
| `SIGNING_KEY_ALIAS` | Keystore key alias |
| `SIGNING_KEY_PASSWORD` | Keystore key password |
| `SIGNING_STORE_PASSWORD` | Keystore store password |
| `FIREBASE_APP_ID_STAGING` | Firebase App ID for staging |
| `FIREBASE_SERVICE_ACCOUNT` | Firebase service account JSON |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Play Store service account JSON |

All secrets above are configured except `PLAY_STORE_SERVICE_ACCOUNT_JSON` (pending Play Store setup).

## Rollback

### Backend

Cloud Run keeps a revision history. Roll back by routing traffic to a previous revision:

```bash
# List revisions
gcloud run revisions list --service=tcatb-api-staging --region=us-central1 --project=tcatb-app

# Route traffic to a previous revision
gcloud run services update-traffic tcatb-api-staging \
  --region=us-central1 \
  --to-revisions=REVISION_NAME=100 \
  --project=tcatb-app
```

### Android

- **Staging:** Upload previous APK to Firebase App Distribution
- **Production:** Halt rollout in Play Console, upload previous AAB

## Database Access

Connect to Cloud SQL via the proxy:

```bash
# Install proxy
gcloud components install cloud-sql-proxy

# Connect (staging)
cloud-sql-proxy tcatb-app:us-central1:tcatb-staging-db &
psql "host=127.0.0.1 port=5432 user=tcatb dbname=tcatb"
```

## Firebase App Distribution

- **Firebase Project:** `tcatb-app` (linked to GCP project)
- **Staging App ID:** `1:821600862601:android:53799c0cc0b1dfb7c4a158`
- **Package:** `com.thecityandthebike.staging`
- **Tester Group:** `internal-testers`
- **Console:** https://console.firebase.google.com/project/tcatb-app/appdistribution

### Managing Testers

```bash
# Add a tester
firebase appdistribution:testers:add EMAIL --project=tcatb-app --group-alias internal-testers

# List testers
firebase appdistribution:testers:list --project=tcatb-app
```

## IAM Notes

### Workload Identity Federation

GitHub Actions authenticates to GCP using OIDC-based Workload Identity Federation (no static keys). The setup:

- **Workload Identity Pool:** `github-actions` (global)
- **OIDC Provider:** `github` (issuer: `https://token.actions.githubusercontent.com`)
- **Attribute condition:** `assertion.repository=='bmander/thecityandthebike'` — only this repo can authenticate
- **IAM binding:** `github-deployer` SA has `roles/iam.workloadIdentityUser` for the pool

The workflow uses `google-github-actions/auth@v2` with `workload_identity_provider` and `service_account` parameters. GitHub's OIDC token is exchanged for a short-lived GCP access token on each run.

### Other IAM Notes

The Cloud Run runtime service account (`821600862601-compute@developer.gserviceaccount.com`) needs secret-level `roles/secretmanager.secretAccessor` bindings on each secret it accesses. Project-level binding alone is insufficient.

The deployer service account (`github-deployer`) needs `roles/iam.serviceAccountUser` on the runtime service account to deploy Cloud Run revisions.

## Remaining Setup

- [x] Generate Android release keystore and add signing secrets to GitHub
- [x] Set up Firebase project and App Distribution
- [ ] Create Google Play developer account and app listing
- [ ] First production deploy (then update production `BASE_URL` in `android/app/build.gradle`)
