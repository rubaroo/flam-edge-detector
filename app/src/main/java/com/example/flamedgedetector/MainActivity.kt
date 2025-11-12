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
 * and binding the image analysis pipeline to the JNI NativeProcessor + OpenGL renderer.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var glCameraView: GLCameraView

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private val nativeProcessor = NativeProcessor() // JNI bridge

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    // Will hold the OpenGL texture ID from GLCameraView
    private var textureId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Initialize non-null reference safely
        glCameraView = binding.viewFinder
            ?: throw IllegalStateException("GLCameraView not found in layout")

        cameraExecutor = Executors.newSingleThreadExecutor()

        // ✅ Retrieve OpenGL texture ID
        textureId = glCameraView.getTextureId()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Configures CameraX — ImageAnalysis only (no PreviewView now).
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // ❌ Remove Preview use case (we’ll render through OpenGL now)
            // val preview = Preview.Builder().build().also {
            //     it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            // }

            // ✅ Keep ImageAnalysis for frame-by-frame processing
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Custom Analyzer class to process camera frames through JNI + OpenGL.
     */
    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            val frameData = imageProxy.toNv21ByteArray()

            // 🔥 Send frame to JNI and render to OpenGL texture
            nativeProcessor.processFrame(
                frameData,
                imageProxy.width,
                imageProxy.height,
                textureId
            )

            // Request redraw on GLSurfaceView after new texture data
            runOnUiThread {
                glCameraView.refreshFrame()
            }

            imageProxy.close()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Log.e("MainActivity", "Permissions not granted by the user.")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
