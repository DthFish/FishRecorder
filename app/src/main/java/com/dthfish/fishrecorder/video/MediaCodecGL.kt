package com.dthfish.fishrecorder.video

import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.dthfish.fishrecorder.utils.EGLHelper
import com.dthfish.fishrecorder.utils.GLUtil
import com.dthfish.fishrecorder.utils.toFloatBuffer

/**
 * Description
 * Author DthFish
 * Date  2019-12-19.
 */
class MediaCodecGL(
    sharedContext: EGLContext?,
    videoConfig: VideoConfig,
    private var inputTextureId: Int
) {
    companion object {
        private val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            void main(){
                gl_Position= aPosition;
                vTextureCoord = aTextureCoord;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D uTexture;
            void main(){
                gl_FragColor = texture2D(uTexture, vTextureCoord);
            }
        """.trimIndent()

    }

    /**
     * egl 环境
     */
    private val eglHelper = EGLHelper.obtain(sharedContext, true)

    //顶点坐标
    private val pos = floatArrayOf(
        -1.0f, 1.0f,
        -1.0f, -1.0f,
        1.0f, 1.0f,
        1.0f, -1.0f
    )

    //纹理坐标
    private val coord = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    private val vertexPointBuffer = pos.toFloatBuffer()
    private val coordPointBuffer = coord.toFloatBuffer()

    private var program = 0
    private var positionLoc = 0
    private var textureCoordLoc = 0
    private var textureLoc = 0

    init {

        val mediaCodec = videoConfig.createMediaCodec()!!
        eglHelper.createMediaCodecSurface(mediaCodec.createInputSurface())
        onCreate()
    }

    fun draw() {
        eglHelper.makeCurrent()
        onDraw()
        //TODO
        eglHelper.setPresentationTime(0)
        eglHelper.swapBuffers()
    }

    fun destroy() {
        eglHelper.makeCurrent()
        onDestroy()
        eglHelper.destroy()
    }

    private fun onCreate() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        program = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        GLES20.glUseProgram(program)
        positionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        textureCoordLoc = GLES20.glGetAttribLocation(program, "aTextureCoord")
        textureLoc = GLES20.glGetUniformLocation(program, "uTexture")
    }

    private fun onDraw() {

        // 绑定纹理
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureId)
        GLES20.glUniform1i(textureLoc, 0)
        //启用顶点坐标和纹理坐标
        GLES20.glEnableVertexAttribArray(positionLoc)
        GLES20.glVertexAttribPointer(
            positionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexPointBuffer
        )
        GLES20.glEnableVertexAttribArray(textureCoordLoc)
        GLES20.glVertexAttribPointer(
            textureCoordLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            coordPointBuffer
        )

//        GLES20.glUniformMatrix4fv(matrixLoc, 1, false, matrix, 0)

//        GLES20.glViewport(0, 0, screenWidth, screenHeight)
        // draw
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 解绑
        GLES20.glFinish()
        GLES20.glDisableVertexAttribArray(positionLoc)
        GLES20.glDisableVertexAttribArray(textureLoc)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glUseProgram(0)
        GLUtil.checkEglError("MediaCodecGL onDraw")

    }

    private fun onDestroy() {
        GLES20.glDeleteProgram(program)
    }
}