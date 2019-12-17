package com.dthfish.fishrecorder.audio.packer

import android.media.MediaCodec
import android.util.Log
import com.dthfish.fishrecorder.audio.IAudioPacker
import com.dthfish.fishrecorder.utils.TAG
import java.lang.ref.WeakReference

/**
 * Description 打包线程，打包编码器传输来的数据，具体的操作放在 [IAudioPacker] 中实现
 * Author DthFish
 * Date  2019-12-05.
 */
class AudioPackThread(private val mediaCodec: MediaCodec, packer: IAudioPacker) :
    Thread("AudioPackThread") {

    private var quit = false

    private var weakPacker: WeakReference<IAudioPacker>? = WeakReference(packer)

    private var trackIndex = 0

    fun quit() {
        quit = true
        interrupt()
        try {
            weakPacker?.get()?.stopAfterCheck()
        } catch (e: Exception) {
            Log.d(TAG, "AudioPackThread fail stopAfterCheck packer")
        }
        Log.d(TAG, "AudioPackThread quit")

    }


    override fun run() {

        while (!quit) {

            val info = MediaCodec.BufferInfo()
            val bufferIndex = mediaCodec.dequeueOutputBuffer(info, 5000)

            when {
                bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // 忽略
                }
                bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = weakPacker?.get()?.formatChanged(mediaCodec.outputFormat) ?: 0
                    weakPacker?.get()?.startAfterCheck()

                }
                bufferIndex < 0 -> {
                    // 忽略
                }
                else -> {
                    val byteBuffer = mediaCodec.outputBuffers[bufferIndex]
                    weakPacker?.get()?.writeSampleData(trackIndex, byteBuffer, info)
                    mediaCodec.releaseOutputBuffer(bufferIndex, false)
                }
            }


        }
    }

}