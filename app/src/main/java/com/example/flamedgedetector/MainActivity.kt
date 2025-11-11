package com.example.flamedgedetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.flamedgedetector.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Primary activity responsible for handling camera permissions, CameraX setup,
 * and binding the image analysis pipeline to the JNI NativeProcessor.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private val nativeProcessor = NativeProcessor() // Instance of our JNI bridge class

    // Arbitrary request code for permissions
    private val REQUEST_CODE_PERMISSIONS = 10

    // List of permissions required for the camera
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize executor for camera analysis thread (needed for ImageAnalysis.Analyzer)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check for camera permissions and start camera or request permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    /**
     * Checks if all required permissions are granted.
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Binds CameraX use cases to the lifecycle.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 1. Preview Use Case: Displays the raw camera feed on the screen
            val preview = Preview.Builder()
                .build()
                .also {
                    // Assuming 'viewFinder' is the ID of your PreviewView in activity_main.xml
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // 2. Image Analysis Use Case: Provides frames for processing
            val imageAnalyzer = ImageAnalysis.Builder()
                // Use the latest frame and discard older ones if processing is slow
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    // Set up the analyzer thread using the custom ImageAnalyzer class
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any existing use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera lifecycle
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Custom Analyzer class to process the camera frames and call JNI.
     */
    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        // Placeholder for the OpenGL texture ID (will be set up later in Day 2)
        private val outputTextureId: Int = 0

        override fun analyze(imageProxy: ImageProxy) {

            // --- Placeholder for Commit 5: YUV to NV21 Conversion ---
            // The ImageProxy buffer is complex (YUV planes). We need to convert it
            // to a simple NV21 byte array before passing it to C++.
            val frameData = byteArrayOf(0)

            // Call the C++ native function with dummy data for now
            nativeProcessor.processFrame(
                frameData,
                imageProxy.width,
                imageProxy.height,
                outputTextureId
            )

            // CRITICAL: Close the image proxy to release the buffer and allow the next frame
            imageProxy.close()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Log.e("MainActivity", "Permissions not granted by the user.")
                finish() // Close the app if permissions are denied
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Important to shut down the executor thread
    }
}