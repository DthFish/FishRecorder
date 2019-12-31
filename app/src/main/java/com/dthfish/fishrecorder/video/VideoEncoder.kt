package com.dthfish.fishrecorder.video

import android.media.MediaCodec
import android.util.Log
import com.dthfish.fishrecorder.utils.TAG
import com.dthfish.fishrecorder.video.packer.VideoPackThread

/**
 * Description
 * Author DthFish
 * Date  2019-12-30.
 */
class VideoEncoder(private val mediaCodec: MediaCodec, private val packer: IVideoPacker) {
    private var videoPackThread: VideoPackThread? = null
    fun start() {
        Log.d(TAG, "VideoEncoder start")
        mediaCodec.start()
        videoPackThread = VideoPackThread(mediaCodec, packer)
        videoPackThread?.start()
    }


    fun stop() {
        Log.d(TAG, "VideoEncoder stop")
        try {
            videoPackThread?.quit()
            videoPackThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        mediaCodec.stop()
        mediaCodec.release()
    }
}