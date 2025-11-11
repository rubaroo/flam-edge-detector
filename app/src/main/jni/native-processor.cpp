// jni/native-processor.cpp
#include <jni.h>
#include <string>
// Include OpenCV headers
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
// Include OpenGL headers (will be used later in Day 2)
#include <GLES2/gl2.h>

using namespace cv;

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_flamedgedetector_NativeProcessor_processFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input,
        jint width,
        jint height,
        jint outputTextureId) {

    // --- Day 1 Test: Basic JNI Functionality ---
    // Get the input buffer
    jbyte* inputBuffer = env->GetByteArrayElements(input, 0);

    // TODO: Placeholder for OpenCV processing (Day 2)
    // For now, just a basic check and return.

    // Release the buffer (VERY IMPORTANT!)
    env->ReleaseByteArrayElements(input, inputBuffer, JNI_ABORT);

    // Return a dummy value (e.g., 0)
    return 0;
}