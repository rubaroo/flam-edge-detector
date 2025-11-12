package com.example.flamedgedetector

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

fun ImageProxy.toNv21ByteArray(): ByteArray {
    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    // NV21 layout: all Y bytes first, then interleaved VU bytes
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)

    val chromaRowStride = planes[1].rowStride
    val chromaPixelStride = planes[1].pixelStride

    var offset = ySize
    for (row in 0 until height / 2) {
        var col = 0
        while (col < width / 2) {
            val uIndex = row * chromaRowStride + col * chromaPixelStride
            val vIndex = row * chromaRowStride + col * chromaPixelStride

            nv21[offset++] = vBuffer.get(vIndex) // V
            nv21[offset++] = uBuffer.get(uIndex) // U
            col++
        }
    }

    return nv21
}
