FlameEdgeDetector

Real-time flame and edge detection using Android + OpenCV + OpenGL (NDK integration).

---

Features Implemented

Android (Kotlin)

* CameraX live frame capture
* JNI bridge to native C++
* Real-time frame analysis via OpenCV (Canny edge detection)
* GLSurfaceView-based GPU rendering
* Texture sharing between Kotlin and C++ for smooth preview


Architecture Overview

| Layer                              | Description                                                                    |
| ---------------------------------- | ------------------------------------------------------------------------------ |
| **CameraX**                        | Captures frames from the device camera (YUV format)                            |
| **JNI Bridge (`NativeProcessor`)** | Transfers raw NV21 frames to C++ layer                                         |
| **OpenCV (C++)**                   | Performs NV21 → RGBA conversion, grayscale filtering, and Canny edge detection |
| **OpenGL ES (C++)**                | Uploads final `rgbaMat` to GPU texture for live preview                        |
| **GLCameraView**                   | Displays processed texture in Android UI                                       |

Frame Flow:

```
CameraX → ImageProxy → NV21 ByteArray → JNI (NativeProcessor.cpp)
→ OpenCV → RGBA (Canny) → OpenGL → GLSurfaceView (Preview)
```

---

Setup Instructions

1️⃣ Install Dependencies

* Android Studio Flamingo or newer
* Android NDK (v23 or later)
* OpenCV Android SDK
* Gradle plugin for NDK (`externalNativeBuild.cmake` enabled)

2️⃣ Clone Repo

```bash
git clone https://github.com/<yourusername>/FlameEdgeDetector.git
cd FlameEdgeDetector
```

3️⃣ Configure OpenCV

In `app/build.gradle`:

```gradle
externalNativeBuild {
    cmake {
        cppFlags ""
    }
}
```

Make sure you add the correct OpenCV `.so` path under `jniLibs/`.

4️⃣ Build and Run

* Connect your Android device
* Allow camera permissions
* Run → App
* You should see the live edge-detected preview.

---
