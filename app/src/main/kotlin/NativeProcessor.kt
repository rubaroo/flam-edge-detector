// src/main/kotlin/.../NativeProcessor.kt
package com.example.flamedgedetector

class NativeProcessor {

    // 1. Load the C++ shared library file
    companion object {
        init {
            // This name must match the 'TARGET_NAME' in your CMakeLists.txt (e.g., native-processor)
            System.loadLibrary("native-processor")
        }
    }

    // 2. Declare the native function
    // This function will be implemented in C++
    external fun processFrame(
        input: ByteArray, // Raw camera data (e.g., YUV)
        width: Int,
        height: Int,
        outputTextureId: Int // The ID of the OpenGL texture we want to update
    ): Long // Returns processing time or status
}