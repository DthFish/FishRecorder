package com.dthfish.fishrecorder.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dthfish.fishrecorder.R
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Description
 * Author DthFish
 * Date  2019-12-19.
 */
object TestUtil {

    private var activity: AppCompatActivity? = null

    private var callTimes = 0L

    fun reset(activity: AppCompatActivity) {
        this.activity = activity
        callTimes = 0L
    }

    fun clear() {
        activity = null
    }

    /**
     * 用于测试离屏缓冲
     */
    fun captureFrame(
        width: Int,
        height: Int
    ) {

        if (callTimes != 100L) {
            callTimes++
            return
        }
        callTimes++

        val bytes = ByteBuffer.allocate(
            width *
                    height * 4
        )
        GLES20.glReadPixels(
            0, 0, width, height,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bytes
        )

        Thread(Runnable {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(bytes)
            saveBitmap(activity!!, bitmap)
            bitmap.recycle()
        }).start()

    }

    private fun saveBitmap(activity: AppCompatActivity, bitmap: Bitmap) {

        val folder =
            File(
                activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath,
                "glPic"
            )
        if (!folder.exists() && !folder.mkdirs()) {
            activity.runOnUiThread { Toast.makeText(activity, "无法保存照片", Toast.LENGTH_SHORT).show() }
            return
        }
        val dataTake = System.currentTimeMillis()
        val jpegName = folder.absolutePath + "/" + dataTake + ".jpg"
        try {
            val fout = FileOutputStream(jpegName)
            val bos = BufferedOutputStream(fout)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        activity.runOnUiThread {
            Toast.makeText(activity, "保存成功->$jpegName", Toast.LENGTH_SHORT).show()
        }

    }

    fun loadBitmap(): Bitmap? {
        if (activity == null) {
            return null
        }
        return BitmapFactory.decodeResource(activity!!.resources, R.drawable.icon_spider, null)
    }
}