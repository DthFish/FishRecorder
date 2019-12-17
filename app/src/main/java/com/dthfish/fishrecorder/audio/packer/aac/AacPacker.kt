package com.dthfish.fishrecorder.audio.packer.aac

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.dthfish.fishrecorder.audio.IAudioPacker
import com.dthfish.fishrecorder.audio.bean.AudioConfig
import com.dthfish.fishrecorder.utils.AudioUtil
import com.dthfish.fishrecorder.utils.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Description 保存 pcm 编码后的 aac 数据
 * Author DthFish
 * Date  2019-12-08.
 */
class AacPacker(config: AudioConfig, path: String) : IAudioPacker {

    private val file: File = File(path)
    private val tempFile: File = File(path + "temp")
    private var output: FileOutputStream? = null

    private val array = ByteArray(config.getMinBufferSize())
    private val channelCount = config.getChannelCount()
    private val profile = config.getCodecProfile()
    private val freqIdx = AudioUtil.getFreqIdx(config.getSampleRate())

    init {
        if (file.exists()) {
            file.delete()
        }
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    override fun startAfterCheck() {
        output = FileOutputStream(tempFile)
        Log.d(TAG, "AacPacker start")
    }

    override fun formatChanged(outputFormat: MediaFormat): Int {
        return 0
    }

    override fun writeSampleData(
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        try {
            // 写入 aac 数据头
            val dataLen = info.size
            val adtsHeader = AudioUtil.getADTSHeader(dataLen, profile, freqIdx, channelCount)
            output?.write(adtsHeader)

            byteBuffer.position(info.offset)
            byteBuffer.limit(info.offset + dataLen)
            byteBuffer.get(array, 0, dataLen)
            // 写入 aac 数据体
            output?.write(array, 0, dataLen)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun stopAfterCheck() {

        try {
            output?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        tempFile.renameTo(file)
        Log.d(TAG, "AacPacker stop")
    }
}