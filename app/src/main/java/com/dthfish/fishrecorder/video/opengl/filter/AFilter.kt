package com.dthfish.fishrecorder.video.opengl.filter

import android.opengl.GLES20
import com.dthfish.fishrecorder.utils.GLUtil
import com.dthfish.fishrecorder.utils.MatrixUtil
import com.dthfish.fishrecorder.utils.toFloatBuffer

/**
 * Description
 * Author DthFish
 * Date  2020-01-02.
 */
open class AFilter {

    protected var vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            uniform mat4 uMatrix;
            varying vec2 vTextureCoord;
            void main(){
                gl_Position= uMatrix*aPosition;
                vTextureCoord = aTextureCoord;
            }        
    """.trimIndent()

    protected var fragmentShader = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D uTexture;
            void main(){
                vec4  color = texture2D(uTexture, vTextureCoord);
                gl_FragColor = color;
            }
    """.trimIndent()

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

    private var matrix = MatrixUtil.getOriginalMatrix()
    protected var width = 0
    protected var height = 0

    private var program = 0
    private var positionLoc = 0
    private var matrixLoc = 0
    private var textureCoordLoc = 0
    private var textureLoc = 0

    private var textureId = 0

    open fun create() {
        program = GLUtil.createProgram(vertexShader, fragmentShader)
        GLES20.glUseProgram(program)
        positionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        matrixLoc = GLES20.glGetUniformLocation(program, "uMatrix")
        textureCoordLoc = GLES20.glGetAttribLocation(program, "aTextureCoord")
        textureLoc = GLES20.glGetUniformLocation(program, "uTexture")

        MatrixUtil.flip(matrix, x = false, y = true)

    }

    open fun draw() {
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureLoc, 0)

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
        GLES20.glUniformMatrix4fv(matrixLoc, 1, false, matrix, 0)

        viewportAndClearColor()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glFinish()
        GLES20.glDisableVertexAttribArray(positionLoc)
        GLES20.glDisableVertexAttribArray(textureLoc)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glUseProgram(0)
    }

    open fun viewportAndClearColor() {
        GLES20.glViewport(0, 0, width, height)
        // draw
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    open fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    open fun destroy() {
        GLES20.glDeleteProgram(program)
    }

    fun setTextureId(inputTextureId: Int) {
        this.textureId = inputTextureId
    }

    fun getMatrix(): FloatArray {
        return matrix
    }

}