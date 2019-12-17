package com.dthfish.fishrecorder.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Description
 * Author DthFish
 * Date  2019-12-02.
 */
const val TAG = "FishRecorder"

fun FloatArray.toFloatBuffer(): FloatBuffer {
    val bb = ByteBuffer.allocateDirect(size * 4)
    bb.order(ByteOrder.nativeOrder())
    val fb = bb.asFloatBuffer()
    fb.put(this)
    fb.position(0)
    return fb
}

fun ShortArray.toShortBuffer(): ShortBuffer {
    val bb = ByteBuffer.allocateDirect(size * 2)
    bb.order(ByteOrder.nativeOrder())
    val fb = bb.asShortBuffer()
    fb.put(this)
    fb.position(0)
    return fb
}