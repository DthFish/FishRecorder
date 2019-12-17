package com.dthfish.fishrecorder.audio

import androidx.annotation.WorkerThread

/**
 * Description 用于直接接收 PCM 数据
 * Author DthFish
 * Date  2019-12-04.
 */
interface IAudioConsumer {
    @WorkerThread
    fun start()

    @WorkerThread
    fun consume(audioFrame: ByteArray, size: Int, endOfStream: Boolean)

    @WorkerThread
    fun stop()
}