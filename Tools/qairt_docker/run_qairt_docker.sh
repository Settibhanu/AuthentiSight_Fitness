#!/usr/bin/env bash
set -e

IMAGE_NAME="qairt"
CONTAINER_NAME="qairt_container"
MOUNT_HOST="/local/"
MOUNT_CONTAINER="/local/"

# Parse flags
while [[ $# -gt 0 ]]; do
  case $1 in
    -i|--image)     IMAGE_NAME="$2";     shift 2 ;;
    -c|--container) CONTAINER_NAME="$2"; shift 2 ;;
    -m|--mount)     MOUNT_HOST="$2";     shift 2 ;;
    -t|--target)    MOUNT_CONTAINER="$2";shift 2 ;;
    --rebuild)      FORCE_REBUILD=1;     shift ;;
    --no-cache)     NO_CACHE="--no-cache"; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Build image if missing or --rebuild requested
if [[ -n "$FORCE_REBUILD" ]] || ! docker image inspect "$IMAGE_NAME" &>/dev/null; then
    echo "[qairt-docker] Building Docker image '$IMAGE_NAME'..."
    docker build $NO_CACHE -t "$IMAGE_NAME" "$SCRIPT_DIR"
fi

# Start or reuse container
if docker container inspect "$CONTAINER_NAME" &>/dev/null; then
    echo "[qairt-docker] Restarting existing container '$CONTAINER_NAME'..."
    docker start -ai "$CONTAINER_NAME"
else
    echo "[qairt-docker] Creating new container '$CONTAINER_NAME'..."
    docker run -it \
        --name "$CONTAINER_NAME" \
        -v "$MOUNT_HOST:$MOUNT_CONTAINER" \
        -v "$(pwd):/workspace" \
        -p 8888:8888 \
        "$IMAGE_NAME" \
        /bin/bash
fi
