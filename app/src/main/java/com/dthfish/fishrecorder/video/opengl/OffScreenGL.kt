package com.dthfish.fishrecorder.video.opengl

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.dthfish.fishrecorder.utils.EGLHelper
import com.dthfish.fishrecorder.utils.GLUtil
import com.dthfish.fishrecorder.utils.MatrixUtil
import com.dthfish.fishrecorder.utils.toFloatBuffer
import com.dthfish.fishrecorder.video.bean.VideoConfig

/**
 * Description
 * Author DthFish
 * Date  2019-12-13.
 */
class OffScreenGL(private val videoConfig: VideoConfig) {
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
                vec4  color = texture2D(uTexture, vTextureCoord);
                gl_FragColor = color;
            }
        """.trimIndent()
        private val VERTEX_SHADER_CAMERA = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            uniform mat4 uMatrix;
            uniform mat4 uTextureMatrix;
            varying vec2 vTextureCoord;
            void main(){
                gl_Position= uMatrix*aPosition;
                vTextureCoord = (uTextureMatrix*vec4(aTextureCoord,1,1)).xy;
            }
        """.trimIndent()
        private val FRAGMENT_SHADER_CAMERA = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform samplerExternalOES uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture,vTextureCoord);
            }            
        """.trimIndent()
    }

    /**
     * egl 环境
     */
    private val eglHelper = EGLHelper.obtain()
    private var inputTextureId = 0
    private var cameraTexture: SurfaceTexture? = null
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

    private val cameraMatrix = MatrixUtil.getOriginalMatrix()

    private val cameraTextureMatrix = MatrixUtil.getOriginalMatrix()

    private var cameraProgram = 0
    private var cameraPositionLoc = 0
    private var cameraTextureCoordLoc = 0
    private var cameraMatrixLoc = 0
    private var cameraTextureMatrixLoc = 0
    private var cameraTextureLoc = 0
    private var cameraFrameBuffer = 0
    /**
     * 与 [cameraFrameBuffer] 关联的纹理 id
     */
    private var cameraFrameBufferTexture = 0

    private var program = 0
    private var positionLoc = 0
    private var textureCoordLoc = 0
    private var textureLoc = 0
    private var frameBuffer = 0
    /**
     * 与 [frameBuffer] 关联的纹理 id
     */
    private var frameBufferTexture = 0
    private var videoWidth = 0
    private var videoHeight = 0
    private var previewWidth = 0
    private var previewHeight = 0
    private var rotateAngle = 0f

    init {
        videoWidth = videoConfig.getWidth()
        videoHeight = videoConfig.getHeight()
        previewWidth = videoConfig.getPreviewWidthForGL()
        previewHeight = videoConfig.getPreviewHeightForGL()
        rotateAngle = videoConfig.getDisplayDegree().toFloat()
        eglHelper.createOffScreenSurface(videoWidth, videoHeight)
        onCreate()
    }

    fun draw() {
        eglHelper.makeCurrent()
        onDraw()
    }

    fun destroy() {
        eglHelper.makeCurrent()
        onDestroy()
        eglHelper.destroy()
        cameraTexture?.release()
    }

    private fun onCreate() {
        cameraProgram = GLUtil.createProgram(
            VERTEX_SHADER_CAMERA,
            FRAGMENT_SHADER_CAMERA
        )
        GLES20.glUseProgram(cameraProgram)
        getCameraTexture()
        cameraPositionLoc = GLES20.glGetAttribLocation(cameraProgram, "aPosition")
        cameraTextureCoordLoc = GLES20.glGetAttribLocation(cameraProgram, "aTextureCoord")
        cameraMatrixLoc = GLES20.glGetUniformLocation(cameraProgram, "uMatrix")
        cameraTextureMatrixLoc = GLES20.glGetUniformLocation(cameraProgram, "uTextureMatrix")
        cameraTextureLoc = GLES20.glGetUniformLocation(cameraProgram, "uTexture")
        val fb = IntArray(1)
        val ft = IntArray(1)
        GLUtil.createFrameBuffer(fb, ft, videoWidth, videoHeight)
        cameraFrameBuffer = fb[0]
        cameraFrameBufferTexture = ft[0]

        GLUtil.createFrameBuffer(fb, ft, videoWidth, videoHeight)
        frameBuffer = fb[0]
        frameBufferTexture = ft[0]

        program = GLUtil.createProgram(
            VERTEX_SHADER,
            FRAGMENT_SHADER
        )
        GLES20.glUseProgram(program)
        positionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        textureCoordLoc = GLES20.glGetAttribLocation(program, "aTextureCoord")
        textureLoc = GLES20.glGetUniformLocation(program, "uTexture")
        GLUtil.checkEglError("onCreate")


        MatrixUtil.getMatrix(
            cameraMatrix,
            MatrixUtil.TYPE_CENTERCROP,
            this.previewWidth,
            this.previewHeight,
            this.videoWidth,
            this.videoHeight
        )
        Log.d(
            "Camera1",
            "previewWidthForGL=${this.previewWidth},previewHeightForGL=${this.previewHeight}"
        )
        Log.d("Camera1", "videoWidth=${this.videoWidth},videoHeight=${this.videoHeight}")

        MatrixUtil.rotate(cameraMatrix, rotateAngle)
        MatrixUtil.flip(cameraMatrix, x = false, y = true)

        /*MatrixUtil.getMatrix(
            cameraMatrix,
            MatrixUtil.TYPE_CENTERCROP,
            this.previewHeight,
            this.previewWidth,
            this.videoWidth,
            this.videoHeight
        )

        MatrixUtil.rotate(cameraMatrix, 90f)
        MatrixUtil.flip(cameraMatrix, x = true, y = false)*/

    }

    private fun onDraw() {
        onDrawCameraFrameBuffer(inputTextureId)
        onDrawFrameBuffer(cameraFrameBufferTexture)
    }

    private fun onDrawCameraFrameBuffer(inputTexture: Int) {
        // 绑定 frameBuffer 和 纹理
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, cameraFrameBuffer)
        GLES20.glUseProgram(cameraProgram)
        // 是 GLES20.GL_TEXTURE0 还是 GLES20.GL_TEXTURE1 主要看片元着色器里面有几个纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTexture)
        // TMD 千万千万别把这里 glUniform1i 写成 glUniform1f，不然会有 0x502 的错误
        // 花了我将近三天的时间排查对比代码找错，我想把自己的头打掉
        // 另外以后出现错误及时用 GLUtil.checkEglError() 排错，
        // opengl 的错误尽量及时抛异常查找，不断缩小范围，定位错误的代码。
        GLES20.glUniform1i(cameraTextureLoc, 0)

        //启用顶点坐标和纹理坐标
        GLES20.glEnableVertexAttribArray(cameraPositionLoc)
        GLES20.glVertexAttribPointer(
            cameraPositionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * 4,
            vertexPointBuffer
        )
        GLES20.glEnableVertexAttribArray(cameraTextureCoordLoc)
        GLES20.glVertexAttribPointer(
            cameraTextureCoordLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * 4,
            coordPointBuffer
        )

        GLES20.glUniformMatrix4fv(cameraMatrixLoc, 1, false, cameraMatrix, 0)

        cameraTexture?.getTransformMatrix(cameraTextureMatrix)
        GLES20.glUniformMatrix4fv(cameraTextureMatrixLoc, 1, false, cameraTextureMatrix, 0)
        GLES20.glViewport(0, 0, videoWidth, videoHeight)
        // draw
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        // 解绑
        GLES20.glFinish()
        GLES20.glDisableVertexAttribArray(cameraPositionLoc)
        GLES20.glDisableVertexAttribArray(cameraTextureLoc)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLUtil.checkEglError("onDrawCameraFrameBuffer")


    }

    private fun onDrawFrameBuffer(inputTexture: Int) {
        // 绑定 frameBuffer 和 纹理
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)

        GLES20.glUniform1i(textureLoc, 0)
        //启用顶点坐标和纹理坐标
        GLES20.glEnableVertexAttribArray(positionLoc)
        GLES20.glVertexAttribPointer(
            positionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * 4,
            vertexPointBuffer
        )
        GLES20.glEnableVertexAttribArray(textureCoordLoc)
        GLES20.glVertexAttribPointer(
            textureCoordLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * 4,
            coordPointBuffer
        )
        GLES20.glViewport(0, 0, videoWidth, videoHeight)
        // draw
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//        TestUtil.captureFrame(videoWidth, videoHeight)

        // 解绑
        GLES20.glFinish()
        GLES20.glDisableVertexAttribArray(positionLoc)
        GLES20.glDisableVertexAttribArray(textureLoc)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glUseProgram(0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLUtil.checkEglError("onDrawFrameBuffer")
    }

    private fun onDestroy() {
        GLES20.glDeleteProgram(cameraProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(cameraFrameBuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(cameraFrameBufferTexture), 0)
        GLES20.glDeleteProgram(program)
        GLES20.glDeleteFramebuffers(1, intArrayOf(frameBuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(frameBufferTexture), 0)
    }

    @Synchronized
    fun getCameraTexture(): SurfaceTexture {
        return cameraTexture ?: GLUtil.createOESTextureID().let {
            inputTextureId = it
            cameraTexture = SurfaceTexture(inputTextureId)
            cameraTexture!!
        }
    }

    fun makeCurrent() {
        eglHelper.makeCurrent()
    }

    fun getSharedContext(): EGLContext? {
        return eglHelper.getContext()
    }


    /**
     * 调用前需要调用 [makeCurrent]
     */
    fun updateTexImage() {
        cameraTexture?.updateTexImage()
    }

    fun getOutputTextureId(): Int {
        return frameBufferTexture
    }
}