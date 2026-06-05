#!/usr/bin/env bash
# Run inside the qairt_container to verify the environment is correctly set up.
set -e

echo "=== QAIRT Container Environment Verification ==="
echo ""

echo "[1] QAIRT_SDK_ROOT = $QAIRT_SDK_ROOT"
if [ -z "$QAIRT_SDK_ROOT" ]; then
    echo "    ❌ QAIRT_SDK_ROOT is not set!"
    exit 1
else
    echo "    ✓ Set correctly"
fi

echo ""
echo "[2] qnn-net-run version:"
if command -v qnn-net-run &>/dev/null; then
    qnn-net-run --version 2>&1 | head -3
else
    echo "    ❌ qnn-net-run not found on PATH"
fi

echo ""
echo "[3] snpe-onnx-to-dlc:"
DLC_TOOL="$QAIRT_SDK_ROOT/bin/x86_64-linux-clang/snpe-onnx-to-dlc"
if [ -f "$DLC_TOOL" ]; then
    echo "    ✓ Found at $DLC_TOOL"
else
    echo "    ❌ Not found at $DLC_TOOL"
fi

echo ""
echo "[4] Python / PyTorch:"
python3 -c "import torch; print('    torch =', torch.__version__)"
python3 -c "import onnx; print('    onnx  =', onnx.__version__)"
python3 -c "import cv2;  print('    cv2   =', cv2.__version__)"

echo ""
echo "[5] Android NDK:"
if [ -d "$ANDROID_NDK_ROOT" ]; then
    echo "    ✓ NDK found at $ANDROID_NDK_ROOT"
else
    echo "    ⚠ NDK not found at $ANDROID_NDK_ROOT"
fi

echo ""
echo "=== Verification complete ==="
