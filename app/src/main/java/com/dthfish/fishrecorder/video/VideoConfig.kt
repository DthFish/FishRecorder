package com.dthfish.fishrecorder.video

import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface

class VideoConfig private constructor() {
    companion object {
        fun obtainGL(): VideoConfig {
            val videoConfig = VideoConfig()
            videoConfig.colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

            return videoConfig
        }

        fun obtain(): VideoConfig {
            val videoConfig = VideoConfig()
            videoConfig.colorFormat =
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar

            return videoConfig
        }
    }

    private var colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    /**
     * 帧率
     */
    private var fps = 25
    /**
     * Group Of Pictures,也可以理解为关键帧频率
     */
    private var gop = 1
    private var width = 720

    private var height = 1280
    /**
     * 屏幕方向
     */
    private var screenDegree = 0
    /**
     * 经过 Camera 和 [screenDegree] 计算得到展示的时候需要旋转的角度
     */
    private var displayDegree = 0

    private var bitRate = 1000 * 2000

    private var mime = "video/avc"

    private var defaultCamera = Camera.CameraInfo.CAMERA_FACING_FRONT

    /**
     * 预留给录屏使用
     */
    private var dpi = 1
    /**
     * 以下 camera 预览信息
     */
    private var previewWidth = 0

    private var previewHeight = 0

    private var previewFormat = ImageFormat.NV21

    private var previewMinFps = -1

    private var previewMaxFps = -1

    private var screenWidth = 0

    private var screenHeight = 0

    fun setColorFormat(colorFormat: Int) {
        this.colorFormat = colorFormat
    }

    fun getColorFormat(): Int {
        return colorFormat
    }

    fun setFps(fps: Int) {
        this.fps = fps
    }

    fun getFps(): Int {
        return fps
    }

    fun setGop(gop: Int) {
        this.gop = gop
    }

    fun getGop(): Int {
        return gop
    }

    fun setWidth(width: Int) {
        this.width = width
    }

    fun getWidth(): Int {
        return this.width
    }

    fun setHeight(height: Int) {
        this.height = height
    }

    fun getHeight(): Int {
        return height
    }

    fun setDpi(dpi: Int) {
        this.dpi = dpi
    }

    fun getDpi(): Int {
        return dpi
    }

    fun setBitRate(bitRate: Int) {
        this.bitRate = bitRate
    }

    fun getBitRate(): Int {
        return bitRate
    }

    fun setMime(mime: String) {
        this.mime = mime
    }

    fun getMime(): String {
        return mime
    }

    fun setDefaultCamera(camera: Int) {
        defaultCamera = camera
    }

    fun getDefaultCamera(): Int {
        return defaultCamera
    }

    fun setPreviewWidth(width: Int) {
        this.previewWidth = width
    }

    /**
     * 用来设置给相机预览
     */
    fun getPreviewWidth(): Int {
        return previewWidth
    }

    /**
     * 用来设置给 OpenGL 进行矩阵变换
     */
    fun getPreviewWidthForGL(): Int {
        return when (screenDegree) {
            0, 180 -> {
                previewHeight
            }
            else -> previewWidth
        }
    }

    fun setPreviewHeight(height: Int) {
        this.previewHeight = height
    }

    /**
     * 用来设置给相机预览
     */
    fun getPreviewHeight(): Int {
        return previewHeight
    }

    /**
     * 用来设置给 OpenGL 进行矩阵变换
     */
    fun getPreviewHeightForGL(): Int {

        return when (screenDegree) {
            0, 180 -> {
                previewWidth
            }
            else -> previewHeight
        }
    }

    fun setPreviewFormat(colorFormat: Int) {
        this.previewFormat = colorFormat
    }

    fun getPreviewFormat(): Int {
        return previewFormat
    }

    fun setPreviewMinFps(minFps: Int) {
        this.previewMinFps = minFps
    }

    fun getPreviewMinFps(): Int {
        return previewMinFps
    }

    fun setPreviewMaxFps(maxFps: Int) {
        this.previewMaxFps = maxFps
    }

    fun getPreviewMaxFps(): Int {
        return previewMaxFps
    }

    fun setScreenWidth(width: Int) {
        screenWidth = width
    }

    fun getScreenWidth(): Int {
        return screenWidth
    }

    fun setScreenHeight(height: Int) {
        screenHeight = height
    }

    fun getScreenHeight(): Int {
        return screenHeight
    }

    fun calculateScreenDegree(rotation: Int) {
        var screenDegree = 0
        when (rotation) {
            Surface.ROTATION_0 -> screenDegree = 0
            Surface.ROTATION_90 -> screenDegree = 90
            Surface.ROTATION_180 -> screenDegree = 180
            Surface.ROTATION_270 -> screenDegree = 270
        }
        setScreenDegree(screenDegree)
    }

    private fun setScreenDegree(screenDegree: Int) {
        this.screenDegree = screenDegree
    }

    fun getScreenDegree(): Int {
        return screenDegree
    }

    fun setDisplayDegree(displayDegree: Int) {
        this.displayDegree = displayDegree
    }

    fun getDisplayDegree(): Int {
        return displayDegree
    }

    fun createMediaCodec(): MediaCodec? {
        val mediaFormat = MediaFormat()
        mediaFormat.setString(MediaFormat.KEY_MIME, getMime())
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, getWidth())
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, getHeight())
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, getColorFormat())
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, getFps())
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, getGop())
        var mediaCodec: MediaCodec? = null
        try {
            mediaCodec = MediaCodec.createEncoderByType(getMime())
            mediaCodec.configure(
                mediaFormat,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaCodec
    }

}
