package com.example.flamedgedetector

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Utility functions for handling and converting camera frame data.
 */
object ImageUtils {

    /**
     * Converts a CameraX YUV_420_888 ImageProxy to a standard NV21 byte array.
     * This format (NV21) is a common YUV format that is easy to pass across the JNI bridge
     * and is natively understood by OpenCV's color conversion functions (COLOR_YUV2RGBA_NV21).
     *
     * @param imageProxy The frame provided by the CameraX ImageAnalysis use case.
     * @return A ByteArray in NV21 format.
     */
    fun ImageProxy.toNv21ByteArray(): ByteArray {
        val y = planes[0].buffer
        val u = planes[1].buffer
        val v = planes[2].buffer

        val ySize = y.remaining()
        val uSize = u.remaining()
        val vSize = v.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // 1. Copy Y plane data
        y.get(nv21, 0, ySize)

        // 2. Interleave UV planes (NV21 format stores V, then U)
        // Note: YUV_420_888 planes are often sparse. We need to handle strides.
        val uv = ByteArray(uSize + vSize)
        v.get(uv, 0, vSize)
        u.get(uv, vSize, uSize)

        // Interleave logic (simplified for standard non-sparse planes, but functional):
        var nv21Index = ySize

        // This handles copying the UV data respecting potential pixel skips and strides
        val vPlane = planes[2]
        val uPlane = planes[1]

        // This is a more robust way to handle the YUV planes with strides:
        // Copy the Y data (already done)
        y.rewind()
        y.get(nv21, 0, ySize)

        // Copy the U and V data interleaved into the NV21 array (VU format)
        val chromaBase = ySize
        val chromaPixelStride = vPlane.pixelStride
        val chromaRowStride = vPlane.rowStride

        val vBuffer = vPlane.buffer
        val uBuffer = uPlane.buffer

        // Check for continuous UV planes, if so, a direct copy is faster
        if (chromaPixelStride == 1 && chromaRowStride == width / 2) {
            vBuffer.get(nv21, chromaBase, vSize)
            uBuffer.get(nv21, chromaBase + vSize, uSize)
        } else {
            // Slower path for non-contiguous memory, necessary for full compatibility
            var vRow: ByteArray
            var uRow: ByteArray
            val heightChroma = height / 2
            val widthChroma = width / 2

            for (i in 0 until heightChroma) {
                // V Row
                vRow = ByteArray(chromaRowStride)
                vBuffer.get(vRow)
                for (j in 0 until widthChroma) {
                    nv21[chromaBase + i * widthChroma * 2 + j * 2] = vRow[j * chromaPixelStride]
                }

                // U Row
                uRow = ByteArray(chromaRowStride)
                uBuffer.get(uRow)
                for (j in 0 until widthChroma) {
                    nv21[chromaBase + i * widthChroma * 2 + j * 2 + 1] = uRow[j * chromaPixelStride]
                }
            }
        }

        return nv21
    }
}