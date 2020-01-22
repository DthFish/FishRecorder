package com.dthfish.fishrecorder.utils

import android.graphics.ImageFormat
import android.hardware.Camera
import android.util.Log
import com.dthfish.fishrecorder.video.bean.VideoConfig
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

            // 方案1 直接选取宽高略大于目标的预览宽高
            /*previewsSizes.firstOrNull {
                it.width >= config.getWidth() && it.height >= config.getHeight()
            }?.apply {
                Log.d("Camera1", "Camera preview selected width=$width, height=$height")
                config.setPreviewWidth(width)
                config.setPreviewHeight(height)
            }*/

            // 方案2 直接选取宽高最大的预览宽高
            /*previewsSizes.lastOrNull()?.apply {
                Log.d(TAG, "Camera preview selected width=$width, height=$height")
                config.setPreviewWidth(width)
                config.setPreviewHeight(height)
            }*/

            // 方案3 这个才是正解
            val targetWidth: Int
            val targetHeight: Int
            when (config.getScreenDegree()) {
                //竖屏
                0, 180 -> {
                    targetWidth = config.getHeight()
                    targetHeight = config.getWidth()
                }
                else -> {
                    targetWidth = config.getWidth()
                    targetHeight = config.getHeight()
                }
            }

            previewsSizes.firstOrNull {
                it.width >= targetWidth && it.height >= targetHeight
            }?.apply {
                Log.d("Camera1", "Camera preview selected width=$width, height=$height")
                config.setPreviewWidth(width)
                config.setPreviewHeight(height)
            }

            // 计算给 OpenGL 矩阵旋转的角度
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(config.getDefaultCamera(), cameraInfo)
            val displayDegree = if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                (cameraInfo.orientation + config.getScreenDegree()) % 360
            } else {
                (360 - cameraInfo.orientation + config.getScreenDegree()) % 360
            }
            Log.d(
                "Camera1", "Screen degree=${config.getScreenDegree()}" +
                        ",Camera orientation=${cameraInfo.orientation}"
            )
            Log.d("Camera1", "Display degree=$displayDegree")
            config.setDisplayDegree(displayDegree)


            // 查找 fps
            val previewFpsRange = parameters.supportedPreviewFpsRange
            val fps = config.getFps() * 1000
            previewFpsRange.sortWith(Comparator { range1, range2 ->
                val l = abs(range1[0] - fps) + abs(range1[1] - fps)
                val r = abs(range2[0] - fps) + abs(range2[1] - fps)
                l - r
            })
            previewFpsRange.firstOrNull()?.apply {
                Log.d("Camera1", "Camera preview selected minFps=${this[0]}, maxFps=$${this[1]}")
                config.setPreviewMinFps(this[0])
                config.setPreviewMaxFps(this[1])
            }

            if (config.getFps() > config.getPreviewMaxFps() / 1000) {
                config.setFps(config.getPreviewMaxFps() / 1000)
            }

            // 查找支持的 format
            val previewFormats = parameters.supportedPictureFormats

            previewFormats?.forEach { it ->
                Log.d("Camera1", "format =$it")
            }
            // 目前测试的手机获得的都是 jpeg，所以不会走到下面的逻辑，如果发生错误可能要调整
            previewFormats.firstOrNull {
                it == ImageFormat.NV21 || it == ImageFormat.YV12
            }?.apply {
                Log.d(
                    "Camera1",
                    "Camera preview selected previewFormat=${if (this == ImageFormat.NV21) "NV21" else "YV12"}"
                )
                config.setPreviewFormat(this)
            }
        }
    }

    fun applyPreviewParams(camera: Camera, config: VideoConfig) {

        camera.apply {
            // 这里不进行方向调整，方向调整放到 OffScreenGL,见 VideoConfig displayDegree 字段
            /*
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(config.getDefaultCamera(), cameraInfo)

            Log.d("Camera1", "camera orientation=" + cameraInfo.orientation)
            var displayOrientation: Int
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                displayOrientation = (cameraInfo.orientation + config.getScreenDegree()) % 360
                displayOrientation =
                    (360 - displayOrientation) % 360          // compensate the mirror
            } else {
                displayOrientation = (cameraInfo.orientation - config.getScreenDegree() + 360) % 360
            }
            camera.setDisplayOrientation(displayOrientation)
            */


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