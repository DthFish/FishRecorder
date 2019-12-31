package com.dthfish.fishrecorder.audio.consumer

import android.media.MediaCodec
import android.util.Log
import com.dthfish.fishrecorder.audio.IAudioConsumer
import com.dthfish.fishrecorder.audio.IAudioPacker
import com.dthfish.fishrecorder.audio.bean.AudioBuffer
import com.dthfish.fishrecorder.audio.bean.AudioConfig
import com.dthfish.fishrecorder.audio.packer.AudioPackThread
import com.dthfish.fishrecorder.utils.TAG

/**
 * Description 录音编码器，消费产生的 pcm 数据
 * Author DthFish
 * Date  2019-12-04.
 */
class AudioEncoder(config: AudioConfig, private val packer: IAudioPacker) :
    IAudioConsumer {

    private var mediaCodec: MediaCodec

    private val originBuffers: Array<AudioBuffer>

    private var lastAudioBufferIndex = 0//用于判断 originBuffers 中用来接收数据的索引

    private var lastPresentationTimeUs: Long = 0//输入时候的时间

    private val sampleByteSizeInSec: Int

    init {
        val size = config.getMinBufferSize()
        mediaCodec = config.createMediaCodec()!!

        originBuffers = Array(5) {
            AudioBuffer(size)
        }

        sampleByteSizeInSec = config.getSampleByteSizeInSec()

    }

    private var audioPackThread: AudioPackThread? = null
    override fun start() {
        Log.d(TAG, "AudioEncoder start")
        lastAudioBufferIndex = 0
        mediaCodec.start()
        audioPackThread = AudioPackThread(mediaCodec, packer)
        audioPackThread?.start()
    }


    override fun consume(audioFrame: ByteArray, size: Int, endOfStream: Boolean) {
        val targetIndex = lastAudioBufferIndex % originBuffers.size
        lastAudioBufferIndex++
        val audioBuffer = originBuffers[targetIndex]
        if (audioBuffer.isReadyToFill) {
            Log.d(TAG, "AudioEncoder consume audio frame size:$size")

            audioBuffer.isReadyToFill = false
            // 不直接从 mediaCodec 获取 ByteBuffer 的目的是可以在这里进一步对 pcm 数据做处理,例如音量
            // 但是目前没做
            System.arraycopy(audioFrame, 0, audioBuffer.buffer, 0, size)
            audioBuffer.fillSize = size
            audioBuffer.endOfStream = endOfStream
            // 如果希望自己进一步处理 pcm 数据，这里应该使用 HandleThread 发送到另外一个线程处理后再 enqueue
            enqueue(targetIndex)
        } else {
            Log.d(TAG, "AudioEncoder drop one audio frame targetIndex:$targetIndex")
        }

    }

    private fun enqueue(targetIndex: Int) {
        val audioBuffer = originBuffers[targetIndex]
        // 这里可以先对 pcm 处理，但是暂时没有做

        val bufferIndex = mediaCodec.dequeueInputBuffer(0)
        if (bufferIndex >= 0) {
            val buffer = mediaCodec.inputBuffers[bufferIndex]
            buffer.clear()
            buffer.put(audioBuffer.buffer, 0, audioBuffer.fillSize)

            val flag = if (audioBuffer.endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0

            mediaCodec.queueInputBuffer(
                bufferIndex,
                0,
                audioBuffer.fillSize,
                lastPresentationTimeUs,
                flag
            )
            // 根据采集数据的大小确定实际经过的时间,1000_000 1秒等于1000_000 微妙
            val interval = (audioBuffer.fillSize.toLong() * 1000_000 / sampleByteSizeInSec).toLong()
            lastPresentationTimeUs += interval
        }
        audioBuffer.isReadyToFill = true

    }

    override fun stop() {
        Log.d(TAG, "AudioEncoder stop")
        try {
            audioPackThread?.quit()
            audioPackThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        mediaCodec.stop()
        mediaCodec.release()

    }

}