package com.dthfish.fishrecorder.audio.consumer.pcm

import android.util.Log
import com.dthfish.fishrecorder.audio.IAudioConsumer
import com.dthfish.fishrecorder.utils.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Description 保存 pcm 数据
 * Author DthFish
 * Date  2019-12-06.
 */
class PcmSaver(path: String) : IAudioConsumer {
    private val file: File = File(path)
    private val tempFile: File = File(path + "temp")
    private var output: FileOutputStream? = null

    init {
        if (file.exists()) {
            file.delete()
        }
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    override fun start() {
        output = FileOutputStream(tempFile)
        Log.d(TAG, "PcmSaver start")

    }

    override fun consume(audioFrame: ByteArray, size: Int, endOfStream: Boolean) {
        try {
            output?.write(audioFrame, 0, size)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun stop() {
        try {
            output?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        tempFile.renameTo(file)
        Log.d(TAG, "PcmSaver stop")
    }
}