package com.dthfish.fishrecorder.video

import android.content.res.Configuration
import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodecInfo

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

    private var orientation = Configuration.ORIENTATION_PORTRAIT

    private var bitRate = 1000 * 2000

    private var mime = "video/avc"

    private var defaultCamera = Camera.CameraInfo.CAMERA_FACING_BACK

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

    fun getPreviewWidth(): Int {
        return previewWidth
    }

    fun setPreviewHeight(height: Int) {
        this.previewHeight = height
    }

    fun getPreviewHeight(): Int {
        return previewHeight
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

    fun setOriention(oriention: Int) {
        this.orientation = orientation
    }

    fun getOriention(): Int {
        return orientation
    }

}
