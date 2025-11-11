#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <chrono>

// Define log tag for easy filtering in logcat
#define LOG_TAG "NativeProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global Mat to hold the RGBA converted image (for persistence/rendering)
// We declare this globally so it persists between frames, saving allocation time.
cv::Mat rgbaMat;

/**
 * JNI function implementation that receives the camera frame data (NV21 format).
 *
 * @param env JNI environment pointer.
 * @param thiz JNI reference to the NativeProcessor object.
 * @param input The NV21 byte array from the Kotlin side (Commit 5).
 * @param width Width of the image.
 * @param height Height of the image.
 * @param outputTextureId The OpenGL texture ID (ignored until Day 2).
 * @return long Processing time in milliseconds.
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_flamedgedetector_NativeProcessor_processFrame(
        JNIEnv *env,
        jobject thiz,
        jbyteArray input,
        jint width,
        jint height,
        jint outputTextureId) {

    auto start_time = std::chrono::high_resolution_clock::now();

    // 1. Get C-style pointer to the raw byte array data
    // JNI_ABORT tells JNI we won't be writing back to the buffer, only reading.
    jbyte* inputBuffer = env->GetByteArrayElements(input, nullptr);

    if (inputBuffer == nullptr) {
        LOGD("Error: Failed to get byte array elements.");
        return -1;
    }

    // 2. Wrap the NV21 raw data in an OpenCV Mat
    // The total height of the NV21 array (Y plane + UV plane) is height + height/2.
    // CV_8UC1 means 8-bit unsigned, 1 channel (since YUV is treated as a single data stream here).
    cv::Mat nv21Mat(height + height / 2, width, CV_8UC1, inputBuffer);

    // 3. Convert NV21 Mat to RGBA Mat (the format needed for OpenGL texture updates)
    // We only create the RGBA Mat once and reuse it to save memory allocation time.
    if (rgbaMat.empty() || rgbaMat.cols != width || rgbaMat.rows != height) {
        rgbaMat.create(height, width, CV_8UC4); // CV_8UC4 means 8-bit unsigned, 4 channels (RGBA)
    }

    // The core conversion step that uses the OpenCV library (Commit 2 integration)
    cv::cvtColor(nv21Mat, rgbaMat, cv::COLOR_YUV2RGBA_NV21);

    // --- DAY 1 DEBUG/PLACEHOLDER LOGIC ---
    // This log confirms the frame arrived, was converted, and is ready for processing.
    LOGD("Processed Frame: %dx%d. RGBA conversion complete. Output Texture ID: %d", width, height, outputTextureId);

    // 4. IMPORTANT: Release the array elements. This MUST be called after GetByteArrayElements.
    // JNI_ABORT ensures the copied changes are discarded (since we only read it).
    env->ReleaseByteArrayElements(input, inputBuffer, JNI_ABORT);

    // 5. Calculate and return processing time
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();

    return duration;
}