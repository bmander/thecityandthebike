#!/bin/bash

set -e

USERNAME="$1"

if [ -z "$USERNAME" ]; then
    echo "Usage: $0 <username>"
    exit 1
fi

REGION="us-central1"
PROJECT="tcatb-app"
SQL_INSTANCE="$PROJECT:$REGION:tcatb-staging-db"
JOB_NAME="promote-admin-staging"

# Get the latest image from the staging Cloud Run service
IMAGE=$(gcloud run services describe tcatb-api-staging \
    --region "$REGION" \
    --format 'value(spec.template.spec.containers[0].image)')

echo "Using image: $IMAGE"
echo "Promoting '$USERNAME' to admin on staging..."

# Delete existing job if present (args may differ)
gcloud run jobs delete "$JOB_NAME" --region "$REGION" --quiet 2>/dev/null || true

# Create and run the job
gcloud run jobs create "$JOB_NAME" \
    --region "$REGION" \
    --image "$IMAGE" \
    --command python \
    --args="-m,app.promote_admin,$USERNAME" \
    --set-cloudsql-instances "$SQL_INSTANCE" \
    --set-env-vars "PGHOST=/cloudsql/$SQL_INSTANCE,PGPORT=5432,PGUSER=tcatb,PGDATABASE=tcatb" \
    --set-secrets "PGPASSWORD=db-password-staging:latest,JWT_SECRET_KEY=jwt-secret-staging:latest" \
    --memory 512Mi \
    --cpu 1 \
    --max-retries 0 \
    --task-timeout 60 \
    --execute-now \
    --wait

echo "Done."
