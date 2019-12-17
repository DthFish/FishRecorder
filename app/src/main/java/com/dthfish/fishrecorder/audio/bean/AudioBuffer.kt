package com.dthfish.fishrecorder.audio.bean

/**
 * Description 录音 pcm 原数据 buffer
 * Author DthFish
 * Date  2019-12-04.
 */
class AudioBuffer(val audioFormat: Int, size: Int) {
    var isReadyToFill: Boolean = true
        set(value) {
            if (value) {
                fillSize = 0
                endOfStream = false
            }
            field = value
        }
    val buffer: ByteArray = ByteArray(size)
    var fillSize = 0
    var endOfStream = false
}