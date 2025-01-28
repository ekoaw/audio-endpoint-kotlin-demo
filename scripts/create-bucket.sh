#!/bin/sh

# MinIO access details
MINIO_HOST="http://127.0.0.1:9000"
ACCESS_KEY="audiouser"
SECRET_KEY="audiopass"
BUCKET_STORAGE="audio-storage-bucket"
BUCKET_TEMP="audio-temp-bucket"

# Health check function to check if MinIO is alive
isAlive() {
    curl -sf $MINIO_HOST/minio/health/live > /dev/null
}

minio $0 "$@" --quiet & echo $! > /tmp/minio.pid

# Wait for MinIO to be ready
while ! isAlive; do
    sleep 0.1
done

# Set up MinIO client
mc alias set minio $MINIO_HOST $ACCESS_KEY $SECRET_KEY

# Function to check if a bucket exists and create it if it doesn't
create_bucket_if_not_exists() {
    BUCKET_NAME=$1
    # Check if the bucket already exists
    if mc ls minio/$BUCKET_NAME > /dev/null 2>&1; then
        echo "Bucket '$BUCKET_NAME' already exists, skipping creation."
    else
        mc mb minio/$BUCKET_NAME
        mc anonymous set public minio/$BUCKET_NAME
    fi
}

# Create the audio-storage-bucket
create_bucket_if_not_exists $BUCKET_STORAGE

# Create the audio-temp-bucket
create_bucket_if_not_exists $BUCKET_TEMP

kill -s INT $(cat /tmp/minio.pid) && rm /tmp/minio.pid

# Wait for MinIO to be stoped
while isAlive; do
    sleep 0.1
done

exec minio $0 "$@"
