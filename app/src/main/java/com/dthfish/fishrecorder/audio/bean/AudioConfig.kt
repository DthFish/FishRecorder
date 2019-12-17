package com.dthfish.fishrecorder.audio.bean

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodecInfo
import android.media.MediaRecorder

/**
 * Description 录音需要的各种参数
 * Author DthFish
 * Date  2019-12-03.
 */
class AudioConfig {

    private var format = AudioFormat.ENCODING_PCM_16BIT
    /**
     * 声道
     * AudioFormat.CHANNEL_IN_MONO 单声道
     * AudioFormat.CHANNEL_IN_STEREO 双声道
     */
    private var channel = AudioFormat.CHANNEL_IN_MONO

    private var channelCount = 1

    /**
     * 采样率 常用采样频率：8000，12050，22050，44100等
     */
    private var sampleRate = 44100

    private var profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    /**
     * 音频码率=采样率*采样位数*通道数*压缩比例
     * 比如 44100 * 16 * 1 * 0.2 = 141120 b/s
     * 常用码率：64k，128k，192k，256k
     */
    private var bitRate = 128000

    private var audioSource = MediaRecorder.AudioSource.MIC

    private var mime = "audio/mp4a-latm"

    fun getFormat(): Int {
        return format
    }

    fun setFormat(format: Int) {
        this.format = format
    }

    fun getChannel(): Int {
        return channel
    }

    fun setChannel(channel: Int) {
        this.channel = channel

        if (AudioFormat.CHANNEL_IN_MONO == channel) {
            channelCount = 1
        } else if (AudioFormat.CHANNEL_OUT_STEREO == channel) {
            channelCount = 2
        }
    }

    fun getChannelCount(): Int {
        return channelCount
    }

    fun setSampleRate(sampleRate: Int) {
        this.sampleRate = sampleRate
    }

    fun getSampleRate(): Int {
        return sampleRate
    }

    fun getCodecProfile(): Int {
        return profile
    }

    fun setBitRate(bitRate: Int) {
        this.bitRate = bitRate
    }

    fun getBitRate(): Int {
        return bitRate
    }

    fun getMinBufferSize(): Int {
        return AudioRecord.getMinBufferSize(sampleRate, channel, format)
    }

    fun setAudioSource(audioSource: Int) {
        this.audioSource = audioSource
    }

    fun getAudioSource(): Int {
        return audioSource
    }

    fun setMime(mime: String) {
        this.mime = mime
    }

    fun getMime(): String {
        return mime
    }

    /**
     * 每秒钟采集的原始PCM数据大小
     */
    fun getSampleByteSizeInSec(): Int {
        return if (format == AudioFormat.ENCODING_PCM_16BIT) {
            2 * channelCount * sampleRate
        } else {
            1 * channelCount * sampleRate
        }
    }

    fun getBitsPerSample(): Int {
        return if (format == AudioFormat.ENCODING_PCM_16BIT) {
            16
        } else {
            8
        }
    }

}