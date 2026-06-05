# SnapFitness — Snapdragon NPU Fitness App

Fully offline Android fitness app with Snapdragon NPU-accelerated pose tracking.

## Quick Start (assuming `qairt_container` already built)

### Step 1 — Generate DLC Models
```bash
# Your container is already built. Start it:
docker start -ai qairt_container

# Inside the container:
cd /workspace/Generate_models
jupyter notebook --ip=0.0.0.0 --no-browser --allow-root --NotebookApp.token=''
# Open http://localhost:8888 and run all cells
# OR download pre-converted DLC from https://aihub.qualcomm.com (SM8750 target)
# and copy to app/src/main/assets/
```

### Step 2 — Resolve Android Dependencies
```bash
# On your host (Linux/WSL):
export QAIRT_SDK_ROOT=/opt/qairt/2.46.0.260424
chmod +x resolveDependencies.sh
./resolveDependencies.sh
```
This downloads OpenCV 4.13.0, copies SNPE headers, AAR, and `.so` files.

### Step 3 — Place Avatar GLBs
Copy `male.glb` and `female.glb` to `app/src/main/assets/avatars/`.

### Step 4 — Build & Deploy
```bash
# In Android Studio (Panda 4):
# File → Open → fitness-app/
# Build → Make Project

# Or via command line:
./gradlew assembleDebug

# Deploy (after each reboot of device):
adb root && adb shell setenforce 0
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

```
Thread 1  CameraX          → RGBA frames (never stored)
Thread 2  QAIRT Worker     → SNPE YOLO-NAS + HRNet inference (NPU)
Thread 3  Exercise Logic   → ExerciseCounter + PostureDetector (CPU)
Thread 4  Avatar Rendering → Filament / OpenGL ES 3.x (GPU)
Thread 5  Audio Engine     → AlertSoundPlayer TTS + MusicPlayer
Thread 6  Database         → Room DAO operations (summaries only)
```

## Privacy Guarantees
- Zero camera frames written to disk
- Zero pose history accumulated
- Zero video recording
- No network calls at runtime

## SDK Versions
- QAIRT SDK v2.46.0.260424
- Android NDK r26c
- OpenCV Android SDK 4.13.0
- compileSdk 35 / minSdk 31 / targetSdk 35
