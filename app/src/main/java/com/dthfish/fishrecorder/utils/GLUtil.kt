package com.dthfish.fishrecorder.utils


import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.microedition.khronos.opengles.GL10

/**
 * Description
 * Author DthFish
 * Date  2019-12-02.
 */
object GLUtil {

    private fun String.toGLVShader(): Int {
        if (isBlank()) {
            return -1
        }
        var shader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(shader, this)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not compile shader:GL_VERTEX_SHADER")
            Log.e(TAG, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = -1
        }
        return shader
    }

    private fun String.toGLFShader(): Int {
        if (isBlank()) {
            return -1
        }
        var shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(shader, this)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not compile shader:GL_FRAGMENT_SHADER")
            Log.e(TAG, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = -1
        }
        return shader
    }

    fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertex = vertexCode.toGLVShader()
        if (vertex == -1) return -1
        val fragment = fragmentCode.toGLFShader()
        if (fragment == -1) return -1

        var program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.d(TAG, "Can not create program")
            return -1
        }
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program:" + GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            program = -1
        }
        return program
    }

    private fun loadShaderAssets(context: Context, name: String): String? {
        var result: String? = null
        try {
            val inputStream = context.assets.open(name)
            val baos = ByteArrayOutputStream()

            var ch = inputStream.read()
            while (ch != -1) {
                baos.write(ch)
                ch = inputStream.read()
            }
            val buff = baos.toByteArray()
            baos.close()
            inputStream.close()
            result = String(buff, Charset.forName("UTF-8"))
            result = result.replace("\\r\\n".toRegex(), "\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun createProgramAssets(context: Context, vertexPath: String, fragmentPath: String): Int {
        return createProgram(
            loadShaderAssets(context, vertexPath) ?: "",
            loadShaderAssets(context, fragmentPath) ?: ""
        )
    }

    fun createOESTextureID(): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S,
            GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T,
            GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return texture[0]
    }

    fun createFrameBuffer(
        frameBuffer: IntArray,
        frameBufferTex: IntArray,
        width: Int,
        height: Int
    ) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0)
        GLES20.glGenTextures(1, frameBufferTex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTex[0])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            frameBufferTex[0],
            0
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }


    fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException(
                msg + " : egl error: 0x" + Integer.toHexString(error)
            )
        }
    }


}