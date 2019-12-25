package com.dthfish.fishrecorder

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.dthfish.fishrecorder.utils.TAG
import com.dthfish.fishrecorder.utils.TestUtil
import com.dthfish.fishrecorder.video.GLVideoRecorder
import com.dthfish.fishrecorder.video.VideoConfig
import kotlinx.android.synthetic.main.activity_record_with_filter.*

/**
 * Description Camera 结合 opengl
 * Author DthFish
 * Date  2019-11-29.
 */
class RecordWidthFilterActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private var videoRecorder: GLVideoRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_with_filter)

        TestUtil.reset(this)

        val config = VideoConfig.obtainGL()
        val rotation = windowManager.defaultDisplay.rotation

        var screenDegree = 0
        when (rotation) {
            Surface.ROTATION_0 -> screenDegree = 0
            Surface.ROTATION_90 -> screenDegree = 90
            Surface.ROTATION_180 -> screenDegree = 180
            Surface.ROTATION_270 -> screenDegree = 270
        }
        Log.d("Camera1", "screen degree=$screenDegree")
        config.setScreenDegree(screenDegree)
        videoRecorder = GLVideoRecorder(config)

        textureView.keepScreenOn = true
        textureView.surfaceTextureListener = this

    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureAvailable")
        videoRecorder?.startPreview(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureSizeChanged")
        videoRecorder?.updatePreview(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.d(TAG, "onSurfaceTextureDestroyed")
        videoRecorder?.stopPreview()
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onDestroy() {
        super.onDestroy()
        TestUtil.clear()
    }
}