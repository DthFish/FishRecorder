package com.dthfish.fishrecorder.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.dthfish.fishrecorder.audio.IAudioPacker
import com.dthfish.fishrecorder.video.IVideoPacker
import com.dthfish.fishrecorder.utils.TAG
import java.nio.ByteBuffer

/**
 * Description
 * Author DthFish
 * Date  2019-12-05.
 */
class MediaMuxerPacker(path: String, private var trackCount: Int) :
    IAudioPacker, IVideoPacker {


    private val mediaMuxer: MediaMuxer =
        MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var trackPreparedCount = 0
    private var isStart = false

    override fun formatChanged(outputFormat: MediaFormat): Int {
        if (isStart) {
            throw IllegalStateException("muxer already started")
        }
        return mediaMuxer.addTrack(outputFormat)
    }

    @Synchronized
    override fun startAfterCheck() {
        trackPreparedCount++
        if (trackCount > 0 && trackPreparedCount == trackCount) {
            mediaMuxer.start()
            isStart = true
            Log.d(TAG, "MediaMuxerPacker started")
        }
    }

    @Synchronized
    override fun writeSampleData(
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {

        if (!isStart) {
            return
        }
        var tempBuffer: ByteBuffer? = byteBuffer
        // MediaCodec 初始化等信息，并非音频数据
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && info.size != 0) {
            tempBuffer = null
        }

        tempBuffer?.let {
            it.position(info.offset)
            it.limit(info.offset + info.size)
            try {
                Log.d(
                    TAG,
                    "muxer:trackIndex = $trackIndex,presentationTimeUs = ${info.presentationTimeUs}"
                )
                mediaMuxer.writeSampleData(trackIndex, it, info)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    override fun stopAfterCheck() {
        if (!isStart) {
            return
        }
        trackPreparedCount--
        if ((trackCount > 0) && trackPreparedCount <= 0) {
            mediaMuxer.stop()
            mediaMuxer.release()
            isStart = false
            Log.d(TAG, "MediaMuxerPacker stopped")
        }

    }
}