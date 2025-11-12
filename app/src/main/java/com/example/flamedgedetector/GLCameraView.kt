package com.example.flamedgedetector

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 * Custom GLSurfaceView that hosts the GLRenderer.
 *
 * Responsibilities:
 * - Set EGL context for OpenGL ES 2.0.
 * - Attach the GLRenderer to handle all drawing.
 * - Provide access to the OpenGL texture ID so the native (C++) side can update it.
 */
class GLCameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: GLRenderer

    init {
        // 1. Use OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // 2. Create and attach the renderer
        renderer = GLRenderer()
        setRenderer(renderer)

        // 3. Control how frames are drawn
        // RENDERMODE_WHEN_DIRTY means it only redraws when explicitly requested.
        // For camera apps, you can also use RENDERMODE_CONTINUOUSLY for live updates.
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /**
     * Returns the OpenGL texture ID.
     * This ID is passed to the C++ code, which updates the texture each frame.
     */
    fun getTextureId(): Int = renderer.getTextureId()

    /**
     * Request a redraw from outside (e.g., after the native side updates the texture).
     */
    fun refreshFrame() {
        requestRender()
    }
}
