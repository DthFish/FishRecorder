package com.dthfish.fishrecorder.video.opengl.filter

import android.graphics.Bitmap
import android.opengl.GLES20
import com.dthfish.fishrecorder.utils.GLUtil

/**
 * Description 把原图的纹理绘制出来，把水印的纹理绘制出来，两者混合
 * Author DthFish
 * Date  2020-01-03.
 */
class WatermarkFilter(
    private val locationX: Int,
    private val locationY: Int,
    private val markWidth: Int,
    private val markHeight: Int,
    private val bitmap: Bitmap
) : AFilter() {

    private var watermarkTextureId = 0
    private var watermark = object : AFilter() {
        override fun viewportAndClearColor() {
            // 空实现为了实现两张纹理混合的效果，所以不需要 clear
            // 而且视图的位置大小需要重写
            GLES20.glViewport(locationX, locationY, markWidth, markHeight)

        }
    }

    override fun create() {
        super.create()
        watermark.create()
        watermarkTextureId = GLUtil.createBitmapTextureID(bitmap)
        watermark.setTextureId(watermarkTextureId)
        bitmap.recycle()


    }

    override fun draw() {
        super.draw()
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA)
        watermark.draw()
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glViewport(0, 0, width, height)//复原
    }

    override fun destroy() {
        super.destroy()
        watermark.destroy()
    }
}