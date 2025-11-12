package com.example.flamedgedetector

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Implements the OpenGL ES 2.0 renderer.
 *
 * Responsibilities:
 * 1. Initialize OpenGL environment and shaders.
 * 2. Generate a shared texture for the C++ code to write into.
 * 3. Draw the texture onto the screen.
 */
class GLRenderer : GLSurfaceView.Renderer {

    // --- OpenGL Handles ---
    private var textureId: Int = 0
    private var shaderProgram: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordsHandle: Int = 0
    private var textureSamplerHandle: Int = 0

    // --- Geometry Data ---
    private val vertices = floatArrayOf(
        // X, Y, Z
        -1.0f, -1.0f, 0.0f, // Bottom Left
        1.0f, -1.0f, 0.0f,  // Bottom Right
        -1.0f,  1.0f, 0.0f, // Top Left
        1.0f,  1.0f, 0.0f   // Top Right
    )

    private val textureCoords = floatArrayOf(
        // U, V
        0.0f, 1.0f, // Bottom Left
        1.0f, 1.0f, // Bottom Right
        0.0f, 0.0f, // Top Left
        1.0f, 0.0f  // Top Right
    )

    // --- Buffers ---
    private val vertexBuffer: FloatBuffer
    private val textureCoordsBuffer: FloatBuffer

    // Constants
    private val vertexStride = 3 * 4 // 3 floats per vertex * 4 bytes/float

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        textureCoordsBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureCoords)
        textureCoordsBuffer.position(0)
    }

    // --- Public Getter ---
    fun getTextureId(): Int = textureId

    // --- Shaders ---
    private val vertexShaderCode = """
        attribute vec4 a_Position;
        attribute vec2 a_TextureCoords;
        varying vec2 v_TextureCoords;
        void main() {
            v_TextureCoords = a_TextureCoords;
            gl_Position = a_Position;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D u_TextureSampler;
        varying vec2 v_TextureCoords;
        void main() {
            gl_FragColor = texture2D(u_TextureSampler, v_TextureCoords);
        }
    """.trimIndent()

    // --- Renderer Lifecycle ---

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // 1. Compile shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 2. Link program
        shaderProgram = createProgram(vertexShader, fragmentShader)

        // 3. Get attribute/uniform handles
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position")
        textureCoordsHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TextureCoords")
        textureSamplerHandle = GLES20.glGetUniformLocation(shaderProgram, "u_TextureSampler")

        // 4. Generate texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        if (textureId == 0) {
            Log.e("GLRenderer", "Failed to generate texture ID.")
            return
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        Log.d("GLRenderer", "onSurfaceCreated: Texture ID = $textureId")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Log.d("GLRenderer", "Viewport set to ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawQuad()
    }

    // --- Core Drawing ---

    private fun drawQuad() {
        if (textureId == 0) return

        GLES20.glUseProgram(shaderProgram)

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureSamplerHandle, 0)

        // Vertex data
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )

        // Texture data
        GLES20.glEnableVertexAttribArray(textureCoordsHandle)
        GLES20.glVertexAttribPointer(
            textureCoordsHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * 4,
            textureCoordsBuffer
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordsHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glUseProgram(0)
    }

    // --- Utility Functions ---

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("GLRenderer", "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
        }
        return shader
    }

    private fun createProgram(vShader: Int, fShader: Int): Int {
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vShader)
        GLES20.glAttachShader(program, fShader)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("GLRenderer", "Link error: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
        }
        return program
    }

    private fun checkGLError(op: String) {
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e("GLRenderer", "$op: glError $error")
        }
    }
}
