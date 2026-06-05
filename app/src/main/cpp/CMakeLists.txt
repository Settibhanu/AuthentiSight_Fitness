cmake_minimum_required(VERSION 3.22.1)
project(snpe_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── OpenCV ────────────────────────────────────────────────────────────────────
# Unpacked by resolveDependencies.sh into <root>/sdk/OpenCV-android-sdk/
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../sdk/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED core imgproc)

# ── SNPE / QAIRT headers ──────────────────────────────────────────────────────
# Copied by resolveDependencies.sh from QAIRT_SDK_ROOT/include/zdl/
include_directories(${CMAKE_SOURCE_DIR}/inc)

# ── Shared JNI library ────────────────────────────────────────────────────────
add_library(snpe_jni SHARED
    inference.cpp
    inference_helper.cpp
    snpe_helper.cpp
)

# ── Pre-built SNPE runtime library ───────────────────────────────────────────
# Copied by resolveDependencies.sh into jniLibs/arm64-v8a/
add_library(SNPE SHARED IMPORTED)
set_target_properties(SNPE PROPERTIES
    IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../jniLibs/arm64-v8a/libSNPE.so"
)

# ── Link ──────────────────────────────────────────────────────────────────────
target_link_libraries(snpe_jni
    SNPE
    ${OpenCV_LIBS}
    android
    log
    EGL
    GLESv3
)

# Propagate include paths for OpenCV to all sources
target_include_directories(snpe_jni PRIVATE
    ${OpenCV_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/inc
)
