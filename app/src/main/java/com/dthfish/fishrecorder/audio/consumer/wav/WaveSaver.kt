package com.dthfish.fishrecorder.audio.consumer.wav

import android.util.Log
import com.dthfish.fishrecorder.audio.IAudioConsumer
import com.dthfish.fishrecorder.audio.bean.AudioConfig
import com.dthfish.fishrecorder.utils.AudioUtil
import com.dthfish.fishrecorder.utils.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Description 保存 Wave 文件
 * Author DthFish
 * Date  2019-12-08.
 */
class WaveSaver(config: AudioConfig, path: String) : IAudioConsumer {
    private val file: File = File(path)
    private val tempFile: File = File(path + "temp")
    private var output: FileOutputStream? = null
    private var totalAudioLen: Long = 0
    private val channelCount = config.getChannelCount()
    private val sampleRate = config.getSampleRate()
    private val byteRate = config.getBitRate()
    private val bitsPerSample = config.getBitsPerSample()

    init {
        if (file.exists()) {
            file.delete()
        }
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    override fun start() {
        Log.d(TAG, "WaveSaver start")
        output = FileOutputStream(tempFile)
        // 随便写给占位的长度
        val tempLength = 1000L
        val waveHeader =
            AudioUtil.getWAVEHeader(tempLength, sampleRate, channelCount, byteRate, bitsPerSample)
        try {
            output?.write(waveHeader)
        } catch (e: IOException) {
            e.printStackTrace()
        }


    }

    override fun consume(audioFrame: ByteArray, size: Int, endOfStream: Boolean) {

        try {
            output?.write(audioFrame, 0, size)
            totalAudioLen += size
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
        var randomAccessFile: RandomAccessFile? = null
        try {
            randomAccessFile = RandomAccessFile(tempFile, "rw")
            randomAccessFile.seek(4)
            randomAccessFile.writeByte(((totalAudioLen + 36).ushr(0) and 0xFF).toInt())
            randomAccessFile.writeByte(((totalAudioLen + 36).ushr(8) and 0xFF).toInt())
            randomAccessFile.writeByte(((totalAudioLen + 36).ushr(16) and 0xFF).toInt())
            randomAccessFile.writeByte(((totalAudioLen + 36).ushr(24) and 0xFF).toInt())

            randomAccessFile.seek(40)
            randomAccessFile.writeByte((totalAudioLen.ushr(0) and 0xFF).toInt())
            randomAccessFile.writeByte((totalAudioLen.ushr(8) and 0xFF).toInt())
            randomAccessFile.writeByte((totalAudioLen.ushr(16) and 0xFF).toInt())
            randomAccessFile.writeByte((totalAudioLen.ushr(24) and 0xFF).toInt())

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            randomAccessFile?.close()
        }
        tempFile.renameTo(file)
        Log.d(TAG, "WaveSaver stop")

    }
}