package com.dthfish.fishrecorder.video.opengl.filter

import android.opengl.GLES20
import com.dthfish.fishrecorder.utils.GLUtil
import com.dthfish.fishrecorder.utils.MatrixUtil

/**
 * Description
 * Author DthFish
 * Date  2020-01-02.
 */
class GroupFilter(width: Int, height: Int) : AFilter() {
    private var inputTextureId = 0
    private val filters = ArrayList<AFilter>()

    /**
     * 直接创建两个 buffer 两个相互交替使用
     */
    private val frameBuffers = IntArray(2)
    private val frameBufferTextures = IntArray(2)
    private var textureIndex = 0

    init {
        setSize(width, height)
    }

    override fun create() {

        val fb = IntArray(1)
        val ft = IntArray(1)
        GLUtil.createFrameBuffer(fb, ft, width, height)
        frameBuffers[0] = fb[0]
        frameBufferTextures[0] = ft[0]

        GLUtil.createFrameBuffer(fb, ft, width, height)
        frameBuffers[1] = fb[0]
        frameBufferTextures[1] = ft[0]
    }

    override fun draw() {

        textureIndex = 0
        for (filter in filters) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[textureIndex % 2])
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, frameBufferTextures[textureIndex % 2], 0
            )
            GLES20.glViewport(0, 0, width, height)
            if (textureIndex == 0) {
                filter.setTextureId(inputTextureId)
            } else {
                filter.setTextureId(frameBufferTextures[(textureIndex - 1) % 2])
            }
            filter.draw()

            // 取消绑定
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            textureIndex++
        }

    }

    override fun destroy() {
        GLES20.glDeleteFramebuffers(2, frameBuffers, 0)
        GLES20.glDeleteTextures(2, frameBufferTextures, 0)
        filters.forEach { filter ->
            filter.destroy()
        }
        filters.clear()
    }

    fun addFilter(filter: AFilter) {
        MatrixUtil.flip(filter.getMatrix(), x = false, y = true)
        filters.add(filter)
        filter.create()
        filter.setSize(width, height)
    }

    fun removeFilter(filter: AFilter) {
        filters.remove(filter)
        MatrixUtil.flip(filter.getMatrix(), x = false, y = true)
        filter.destroy()
    }

    fun setInputTextureId(inputTextureId: Int) {
        this.inputTextureId = inputTextureId
    }

    fun getOutputTextureId(): Int {
        return if (filters.isEmpty())
            inputTextureId
        else
            frameBufferTextures[(textureIndex - 1) % 2]
    }
}