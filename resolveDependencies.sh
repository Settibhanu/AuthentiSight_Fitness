#!/usr/bin/env bash
set -e

QAIRT_SDK_ROOT="${QAIRT_SDK_ROOT:-/opt/qairt/2.46.0.260424}"
OPENCV_VERSION="4.13.0"
OPENCV_ZIP="opencv-${OPENCV_VERSION}-android-sdk.zip"
OPENCV_URL="https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/${OPENCV_ZIP}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$SCRIPT_DIR/app/src/main"

echo "============================================="
echo " Snapdragon Fitness App — Dependency Setup"
echo "============================================="
echo " QAIRT_SDK_ROOT = $QAIRT_SDK_ROOT"
echo ""

# Validate QAIRT SDK
if [ ! -d "$QAIRT_SDK_ROOT" ]; then
    echo "❌ QAIRT SDK not found at $QAIRT_SDK_ROOT"
    echo "   Export QAIRT_SDK_ROOT=/path/to/qairt/2.46.0.260424 and retry."
    exit 1
fi

echo "[1/4] Downloading OpenCV ${OPENCV_VERSION} Android SDK..."
wget -q "$OPENCV_URL" -O "/tmp/$OPENCV_ZIP"
mkdir -p "$SCRIPT_DIR/sdk"
unzip -q "/tmp/$OPENCV_ZIP" -d "$SCRIPT_DIR/sdk/"
rm "/tmp/$OPENCV_ZIP"
echo "      ✓ OpenCV unpacked to sdk/"

echo "[2/4] Copying SNPE C++ headers..."
mkdir -p "$APP_DIR/cpp/inc/zdl"
cp -r "$QAIRT_SDK_ROOT/include/zdl/." "$APP_DIR/cpp/inc/zdl/"
echo "      ✓ Headers copied"

echo "[3/4] Copying snpe-release.aar..."
mkdir -p "$SCRIPT_DIR/app/libs"
# QAIRT SDK v2.11+ path
AAR_SRC="$QAIRT_SDK_ROOT/lib/android/snpe-release.aar"
if [ -f "$AAR_SRC" ]; then
    cp "$AAR_SRC" "$SCRIPT_DIR/app/libs/"
    echo "      ✓ AAR copied"
else
    echo "      ⚠ snpe-release.aar not found at $AAR_SRC — check SDK path"
fi

echo "[4/4] Copying .so libraries for NPU V79 (arm64-v8a)..."
JNILIBS="$APP_DIR/jniLibs/arm64-v8a"
mkdir -p "$JNILIBS"

for LIB in \
    "libSNPE.so" \
    "libSnpeHtpV79Skel.so" \
    "libSnpeHtpV79Stub.so" \
    "libSnpeHtp.so" \
    "libSnpeHtpPrepare.so"
do
    SRC="$QAIRT_SDK_ROOT/lib/aarch64-android/$LIB"
    if [ -f "$SRC" ]; then
        cp "$SRC" "$JNILIBS/"
        echo "      ✓ $LIB"
    else
        echo "      ⚠ $LIB not found (optional)"
    fi
done

echo ""
echo "✅ resolveDependencies complete."
echo "   Next: Open fitness-app/ in Android Studio and run Build → Make Project"
