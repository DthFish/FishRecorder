package com.dthfish.fishrecorder.video

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.dthfish.fishrecorder.utils.Camera1Util
import com.dthfish.fishrecorder.utils.TAG
import java.io.IOException
import kotlin.system.measureTimeMillis

/**
 * Description
 * Author DthFish
 * Date  2019-12-10.
 */
class GLVideoRecorder(private val config: VideoConfig = VideoConfig.obtain()) {
    private var camera: Camera? = null
    private var isStreaming = false
    private var isRecording = false
    @Volatile
    private var isPreviewing = false

    private val openGLThread = HandlerThread("OpenGLThread")
    private val openGLHandler: OpenGLHandler

    private var drawInterval: Int

    init {
        //初始化一些 camera 的信息
        try {
            camera = Camera.open(config.getDefaultCamera())
            camera?.setDisplayOrientation(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        camera?.apply {
            Camera1Util.selectPreviewParams(this, config)
            Camera1Util.applyPreviewParams(this, config)
        }

        openGLThread.start()
        openGLHandler = OpenGLHandler(openGLThread.looper)
        //初始化一些配置
        drawInterval = 1000 / config.getFps()
    }


    @Synchronized
    fun startPreview(surface: SurfaceTexture, width: Int, height: Int) {
        config.setScreenWidth(width)
        config.setScreenHeight(height)

        if (!(isRecording || isStreaming || isPreviewing)) {

            try {
                val cameraTexture = openGLHandler.getCameraTexture()
                camera?.setPreviewTexture(cameraTexture)
            } catch (e: IOException) {
                e.printStackTrace()
                camera?.release()
                return
            }
            camera?.startPreview()
        }

        openGLHandler.sendMessage(openGLHandler.obtainMessage(START_PREVIEW, surface))
        openGLHandler.sendMessage(openGLHandler.obtainMessage(ON_DRAW, System.currentTimeMillis()))
        isPreviewing = true

    }

    fun updatePreview(width: Int, height: Int) {
        config.setScreenWidth(width)
        config.setScreenHeight(height)
    }

    @Synchronized
    fun stopPreview() {
        if (isPreviewing) {
            camera?.stopPreview()
            camera?.release()
            openGLHandler.sendMessage(openGLHandler.obtainMessage(STOP_PREVIEW))
            isPreviewing = false
        }

    }

    fun startRecording() {

    }

    fun stopRecording() {

    }

    companion object {
        const val START_PREVIEW = 1
        const val STOP_PREVIEW = 2
        const val ON_FRAME = 3
        const val ON_DRAW = 4

    }

    inner class OpenGLHandler(looper: Looper) : Handler(looper) {
        private var frameCount = 0L
        private val frameCountLock = Any()

        /**
         * 初始为 0，每当绘制的时间超过 fps 计算出的时间就会累加
         * 直接把它除以 [drawInterval] 就相当于丢了多少帧
         */
        private var skipFrameTimeMillis = 0L

        /**
         * 这里直接创建的离屏的 egl 环境，是因为马上需要构造一个 SurfaceTexture
         * 用来获取相机的预览数据 [getCameraTexture]
         */
        private val offScreenGL = OffScreenGL(
            config.getWidth(), config.getHeight(),
            config.getPreviewWidth(), config.getPreviewHeight()
        )

        private var previewGL: PreviewGL? = null

        @Volatile
        private var hasFrame = false

        fun getCameraTexture(): SurfaceTexture {
            val cameraTexture = offScreenGL.getCameraTexture()
            cameraTexture.setOnFrameAvailableListener {
                onFrameAvailable()
            }
            return cameraTexture
        }

        private fun onFrameAvailable() {
            synchronized(frameCountLock) {
                frameCount++
                removeMessages(ON_FRAME)
                sendMessageAtFrontOfQueue(obtainMessage(ON_FRAME))
            }
        }

        override fun handleMessage(msg: Message) {

            when (msg.what) {
                START_PREVIEW -> {
                    skipFrameTimeMillis = 0
                    if (previewGL == null) {
                        previewGL = PreviewGL(
                            msg.obj as SurfaceTexture,
                            config.getWidth(), config.getHeight(),
                            config.getScreenWidth(), config.getScreenHeight()
                        )
                        previewGL?.setInputTextureId(offScreenGL.getOutputTextureId())
                    }
                }

                STOP_PREVIEW -> {
                    previewGL?.destroy()
                    previewGL = null
                }

                ON_FRAME -> {
                    Log.d(TAG, "ON_FRAME")
                    synchronized(frameCountLock) {
                        while (frameCount != 0L) {
                            offScreenGL.updateTexImage()
                            Log.d(TAG, "ON_FRAME after")
                            frameCount--
                        }
                    }
                    hasFrame = true

                }
                ON_DRAW -> {
                    if (!hasFrame) {
                        sendMessageDelayed(
                            obtainMessage(
                                ON_DRAW,
                                System.currentTimeMillis() + 20
                            ), 20
                        )
                        return
                    }
                    Log.d(TAG, "ON_DRAW")
                    // 延迟消息的时间 = 每一帧的时间间隔 - 在 Handler 调度中消耗的时间 - 绘制消耗的时间
                    val sendTimeMillis = msg.obj as Long
                    val spendTimeMillisInLoop = System.currentTimeMillis() - sendTimeMillis

                    val spendTimeMillisInDraw = measureTimeMillis {
                        Log.d(TAG, "ON_DRAW before offScreen")
                        offScreenGL.draw()
                        Log.d(TAG, "ON_DRAW before previewGL")
                        previewGL?.draw()
                    }

                    val delayTimeMillis =
                        drawInterval - spendTimeMillisInDraw - spendTimeMillisInLoop
                    Log.d(TAG, "ON_DRAW delayTimeMillis=$delayTimeMillis")

                    if (delayTimeMillis <= 0) {
                        skipFrameTimeMillis -= delayTimeMillis
                        sendMessage(obtainMessage(ON_DRAW, System.currentTimeMillis()))
                    } else {
                        sendMessageDelayed(
                            obtainMessage(
                                ON_DRAW,
                                System.currentTimeMillis() + delayTimeMillis
                            ), delayTimeMillis
                        )
                    }

                }


                else -> {
                }
            }
        }

    }


}