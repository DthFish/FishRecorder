package com.dthfish.fishrecorder.utils

import android.media.AudioFormat
import android.util.Log
import com.dthfish.fishrecorder.audio.bean.AudioConfig

/**
 * Description
 * Author DthFish
 * Date  2019-12-08.
 */
object AudioUtil {
    /**
     * 见 https://blog.csdn.net/tantion/article/details/82743942
     */
    private val AUDIO_SAMPLE_RATES = arrayListOf(
        96000,
        88200,
        64000,
        48000,
        44100,
        32000,
        24000,
        22050,
        16000,
        12000,
        11025,
        8000,
        7350
    )

    fun getFreqIdx(simpleRate: Int) = AUDIO_SAMPLE_RATES.indexOf(simpleRate)
    /**
     *
     * profile: AAC LC，MediaCodecInfo.CodecProfileLevel.AACObjectLC
     * freqIdx: 通过 [getFreqIdx] 获得
     * @return AAC 格式需要的头
     */
    fun getADTSHeader(realDataLen: Int, profile: Int, freqIdx: Int, channelCount: Int): ByteArray {
        // 需要添加头自身的长度
        val dataLen = realDataLen + 7
        val packet = ByteArray(7)
        packet[0] = (0xFF).toByte()
        packet[1] = (0xF1).toByte()
        packet[2] = (((profile - 1) shl 6) + ((freqIdx shl 2) + (channelCount shr 2))).toByte()
        packet[3] = (((channelCount and 3) shl 6) + (dataLen ushr 11)).toByte()
        packet[4] = ((dataLen and 0x7FF) shr 3).toByte()
        packet[5] = (((dataLen and 7) shl 5) + 0x1F).toByte()
        packet[6] = (0xFC).toByte()
        return packet
    }

    /**
     * @return Wave 格式需要的头
     */
    fun getWAVEHeader(
        realDataLen: Long, sampleRate: Int,
        channelCount: Int, byteRate: Int, bitsPerSample: Int
    ): ByteArray {
        val totalDataLen = realDataLen + 36

        val header = ByteArray(44)
        header[0] = 'R'.toByte() // 类似 java class 的魔数
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte() //具体 文件长度，不包括上边的魔数和这4个字节，所以前面只加了 36
        header[5] = ((totalDataLen ushr 8) and 0xff).toByte()
        header[6] = ((totalDataLen ushr 16) and 0xff).toByte()
        header[7] = ((totalDataLen ushr 24) and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()// 没有 4 字节用空格补齐
        header[16] = 16 // fmt 剩余的长度
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM 类型
        header[21] = 0
        header[22] = channelCount.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate ushr 8) and 0xff).toByte()
        header[26] = ((sampleRate ushr 16) and 0xff).toByte()
        header[27] = ((sampleRate ushr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate ushr 8) and 0xff).toByte()
        header[30] = ((byteRate ushr 16) and 0xff).toByte()
        header[31] = ((byteRate ushr 24) and 0xff).toByte()
        header[32] = (channelCount * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (realDataLen and 0xff).toByte()
        header[41] = ((realDataLen ushr 8) and 0xff).toByte()
        header[42] = ((realDataLen ushr 16) and 0xff).toByte()
        header[43] = ((realDataLen ushr 24) and 0xff).toByte()

        return header
    }

    /**
     * 并不是很标准的解析，但是在这里够用了
     */
    fun parseWAVEHeader(header: ByteArray): AudioConfig? {
        if (header.size < 44) {
            return null
        }
        val riff = ByteArray(4)
        riff[0] = header[0]
        riff[1] = header[1]
        riff[2] = header[2]
        riff[3] = header[3]
        val wave = ByteArray(4)
        wave[0] = header[8]
        wave[1] = header[9]
        wave[2] = header[10]
        wave[3] = header[11]
        if (String(riff) != "RIFF" && String(wave) != "WAVE") {
            return null
        }
        val config = AudioConfig()
        val channelCount = header[22].toInt()
        if (channelCount == 2) {
            config.setChannel(AudioFormat.CHANNEL_OUT_STEREO)
        } else {
            config.setChannel(AudioFormat.CHANNEL_IN_MONO)
        }
        var sampleRate = header[27].toInt() and 0xff shl 24
        sampleRate += header[26].toInt() and 0xff shl 16
        sampleRate += header[25].toInt() and 0xff shl 8
        sampleRate += header[24].toInt() and 0xff

        val format = if (header[34].toInt() == 8) {
            AudioFormat.ENCODING_PCM_8BIT
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
        config.setFormat(format)
        Log.d(TAG, "parse wave header:channelCount=$channelCount,sampleRate=$sampleRate")

        return config
    }
}