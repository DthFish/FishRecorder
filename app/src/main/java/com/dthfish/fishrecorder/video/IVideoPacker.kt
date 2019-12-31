package com.dthfish.fishrecorder.video

import android.media.MediaCodec
import android.media.MediaFormat
import androidx.annotation.WorkerThread
import java.nio.ByteBuffer

/**
 * Description 用于打包数据
 * Author DthFish
 * Date  2019-12-05.
 */
interface IVideoPacker {
    /**
     * 可能调用多次
     */
    fun startAfterCheck()
    @WorkerThread
    fun formatChanged(outputFormat: MediaFormat):Int
    @WorkerThread
    fun writeSampleData(trackIndex: Int, byteBuffer: ByteBuffer, info: MediaCodec.BufferInfo)
    /**
     * 可能调用多次
     */
    fun stopAfterCheck()
}