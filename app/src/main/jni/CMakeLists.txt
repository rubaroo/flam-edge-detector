# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)
project("native-processor")

# =========================================================================
# 1. SETUP OpenCV PATHS
# CORRECTED: Use '../../' to step up from 'jni' to 'main' and then to 'app'
# This path is relative to app/src/main/jni/
set(OPENCV_ROOT_DIR ${CMAKE_SOURCE_DIR}/../..)

# Add the OpenCV headers path (now points to app/opencv_modules/include)
include_directories(${OPENCV_ROOT_DIR}/opencv_modules/include)

# =========================================================================

# Creates and names a library (MUST match the name in System.loadLibrary)
add_library(native-processor SHARED
        # Renamed source file as per previous steps
        native-processor.cpp)

# =========================================================================
# 2. LINK OpenCV LIBRARIES
# Find the specific library path for the current target architecture (ABI)
# Path points to: app/opencv_modules/libs/arm64-v8a/
link_directories(${OPENCV_ROOT_DIR}/opencv_modules/libs/${ANDROID_ABI})

# Find the main OpenCV shared library file
find_library(OPENCV_LIB_NAME
        opencv_java4) # Name of the .so file is libopencv_java4.so

# =========================================================================

# Specifies libraries CMake should link to your target library.
target_link_libraries(native-processor # Target library name
        android
        log
        ${OPENCV_LIB_NAME} # Link the found OpenCV library
)