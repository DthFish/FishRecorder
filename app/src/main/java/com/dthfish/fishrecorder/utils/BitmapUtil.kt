package com.dthfish.fishrecorder.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes

/**
 * Description
 * Author zhaolizhi
 * Date  2020-01-22.
 */
object BitmapUtil {
    /**
     * OpenGL 创建 bitmap 纹理的时候要求 ARGB_8888 格式的，但是有些手机创建出来的不是这个格式，
     * 所以要用这个方法来创建
     */
    fun createBitmapForGL(context: Context, @DrawableRes resId: Int): Bitmap {

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        var bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            val tempBitmap = bitmap
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            tempBitmap.recycle()
        }
        return bitmap
    }
}