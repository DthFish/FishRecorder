package com.dthfish.fishrecorder

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dthfish.fishrecorder.audio.AudioRecorder
import com.dthfish.fishrecorder.audio.IAudioConsumer
import com.dthfish.fishrecorder.audio.IAudioConsumerFactory
import com.dthfish.fishrecorder.audio.bean.AudioConfig
import com.dthfish.fishrecorder.audio.consumer.AudioEncoder
import com.dthfish.fishrecorder.muxer.MediaMuxerPacker
import com.dthfish.fishrecorder.utils.TAG
import com.dthfish.fishrecorder.utils.TestUtil
import com.dthfish.fishrecorder.video.GLVideoRecorder
import com.dthfish.fishrecorder.video.IVideoPacker
import com.dthfish.fishrecorder.video.IVideoPackerFactory
import com.dthfish.fishrecorder.video.bean.VideoConfig
import com.dthfish.fishrecorder.video.opengl.filter.WatermarkFilter
import kotlinx.android.synthetic.main.activity_record_with_filter.*

/**
 * Description Camera 结合 opengl
 * Author DthFish
 * Date  2019-11-29.
 */
class RecordWidthFilterActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private var videoRecorder: GLVideoRecorder? = null
    private var isStart = false
    private var isPreview = true
    private var audioRecorder: AudioRecorder? = null
    // 用来测试预览和录制已经分离
    private val previewHolder = PreviewHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_with_filter)
        TestUtil.reset(this)

        // Video Audio Recorder
        initRecorder()
        initHandleView()
    }

    private fun initHandleView() {
        btnRecord.setOnClickListener {
            isStart = !isStart
            if (isStart) {
                videoRecorder?.startRecording()
                audioRecorder?.start()
                tvRecord.text = getString(R.string.str_record_stop)
            } else {
                videoRecorder?.stopRecording()
                audioRecorder?.stop()
                destroyMediaMuxerPacker()
                tvRecord.text = getString(R.string.str_record)
            }
        }

        btnPreview.setOnClickListener {
            isPreview = !isPreview
            if (isPreview) {
                videoRecorder?.startPreview(
                    previewHolder.surfaceTexture!!,
                    previewHolder.width,
                    previewHolder.height
                )
                btnPreview.setImageResource(R.drawable.icon_preview_open)
            } else {
                videoRecorder?.stopPreview()
                btnPreview.setImageResource(R.drawable.icon_preview_close)
            }
        }

        val function: (v: View) -> Unit = {
            val animation = RotateAnimation(
                0f,
                360f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            animation.duration = 500
            animation.interpolator = OvershootInterpolator()

            it.startAnimation(animation)
            videoRecorder?.swapCamera()
        }
        btnSwap.setOnClickListener(function)
    }

    private fun initRecorder() {
        val config = VideoConfig.obtainGL()
        val rotation = windowManager.defaultDisplay.rotation
        config.calculateScreenDegree(rotation)
        videoRecorder = GLVideoRecorder(config)

        videoRecorder?.setPackerFactory(object : IVideoPackerFactory {
            override fun createPacker(config: VideoConfig): IVideoPacker {
                return createMediaMuxerPacker()
            }
        })

        val loadBitmap = TestUtil.loadBitmap()!!
        val filter = WatermarkFilter(
            config.getWidth() - 40 - 100,
            40,
            100,
            75,
            loadBitmap
        )
        videoRecorder?.addFilter(filter)

        //        videoRecorder?.addFilter(GaryFilter())

        textureView.keepScreenOn = true
        textureView.surfaceTextureListener = this

        audioRecorder = AudioRecorder()
        audioRecorder?.setConsumerFactory(object : IAudioConsumerFactory {
            override fun createConsumer(config: AudioConfig): IAudioConsumer {
                return AudioEncoder(config, createMediaMuxerPacker())
            }
        })
    }

    private var mediaMuxerPacker: MediaMuxerPacker? = null

    @Synchronized
    private fun createMediaMuxerPacker(): MediaMuxerPacker {
        if (mediaMuxerPacker == null) {
            val path =
                getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.absolutePath + "/mp4_${System.currentTimeMillis()}.mp4"

            mediaMuxerPacker = MediaMuxerPacker(path, 2)
        }
        return mediaMuxerPacker!!
    }

    @Synchronized
    private fun destroyMediaMuxerPacker() {
        mediaMuxerPacker = null
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureAvailable")
        videoRecorder?.startPreview(surface, width, height)
        previewHolder.surfaceTexture = surface
        previewHolder.width = width
        previewHolder.height = height
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureSizeChanged")
        videoRecorder?.updatePreview(width, height)
        previewHolder.width = width
        previewHolder.height = height
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.d(TAG, "onSurfaceTextureDestroyed")
        videoRecorder?.stopPreview()
        previewHolder.surfaceTexture = null
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onDestroy() {
        videoRecorder?.destroy()
        super.onDestroy()
        TestUtil.clear()
    }

    override fun finish() {
        if (isStart) {
            Toast.makeText(this, getString(R.string.str_stop_before_finish), Toast.LENGTH_SHORT)
                .show()
            return
        }
        super.finish()
    }

    class PreviewHolder {
        var surfaceTexture: SurfaceTexture? = null
        var width = 0
        var height = 0
    }
}