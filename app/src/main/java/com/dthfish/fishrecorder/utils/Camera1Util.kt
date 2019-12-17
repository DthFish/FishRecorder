package com.dthfish.fishrecorder.utils

import android.graphics.ImageFormat
import android.hardware.Camera
import com.dthfish.fishrecorder.video.VideoConfig
import kotlin.math.abs

/**
 * Description
 * Author DthFish
 * Date  2019-12-12.
 */
object Camera1Util {

    fun selectPreviewParams(camera: Camera, config: VideoConfig) {
        camera.apply {
            // 查找 size
            val previewsSizes = parameters.supportedPreviewSizes
            previewsSizes.sortWith(Comparator { size1, size2 ->
                size1.width * size1.height - size2.width * size2.height
            })
            previewsSizes.firstOrNull {
                it.width >= config.getWidth() && it.height >= config.getHeight()
            }?.apply {
                config.setPreviewWidth(width)
                config.setPreviewHeight(height)
            }

            // 查找 fps
            val previewFpsRange = parameters.supportedPreviewFpsRange
            val fps = config.getFps() * 1000
            previewFpsRange.sortWith(Comparator { range1, range2 ->
                val l = abs(range1[0] - fps) + abs(range1[1] - fps)
                val r = abs(range2[0] - fps) + abs(range2[1] - fps)
                l - r
            })
            previewFpsRange.firstOrNull()?.apply {
                config.setPreviewMinFps(this[0])
                config.setPreviewMaxFps(this[1])
            }

            if (config.getFps() > config.getPreviewMaxFps() / 1000) {
                config.setFps(config.getPreviewMaxFps() / 1000)
            }

            // 查找支持的 format
            val previewFormats = parameters.supportedPictureFormats
            previewFormats.firstOrNull {
                it == ImageFormat.NV21 || it == ImageFormat.YV12
            }?.apply {
                config.setPreviewFormat(this)
            }
        }
    }

    fun applyPreviewParams(camera: Camera, config: VideoConfig) {

        camera.apply {
            val parameters = this.parameters
            parameters.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
            val focusModes = parameters.supportedFocusModes
            if (focusModes != null) {
                when {
                    focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ->
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ->
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                    focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED) ->
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
                }
            }
            parameters.setPreviewSize(config.getPreviewWidth(), config.getPreviewHeight())
            parameters.setPreviewFpsRange(config.getPreviewMinFps(), config.getPreviewMaxFps())
            try {
                this.parameters = parameters
            } catch (e: Exception) {
                e.printStackTrace()
                this.release()
            }
        }


    }
}