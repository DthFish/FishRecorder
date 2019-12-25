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
class GLVideoRecorder(private val config: VideoConfig = VideoConfig.obtainGL()) {
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

        } catch (e: Exception) {
            e.printStackTrace()
        }
        camera?.apply {
            Camera1Util.selectPreviewParams(this, config)
            Camera1Util.applyPreviewParams(this, config)
        }

        openGLThread.start()
        openGLHandler = OpenGLHandler(openGLThread.looper)
        openGLHandler.sendMessage(openGLHandler.obtainMessage(ON_CREATE))

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
            openGLHandler.sendMessage(openGLHandler.obtainMessage(STOP_PREVIEW))
            camera?.stopPreview()
            camera?.release()
            isPreviewing = false
        }

    }

    fun startRecording() {

    }

    fun stopRecording() {

    }

    fun destroy() {

    }

    companion object {
        const val ON_CREATE = 0
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
        private var offScreenGL: OffScreenGL? = null

        private var previewGL: PreviewGL? = null

        @Volatile
        private var hasFrame = false

        fun getCameraTexture(): SurfaceTexture? {
            val cameraTexture = offScreenGL?.getCameraTexture()
            cameraTexture?.setOnFrameAvailableListener {
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
                ON_CREATE -> {
                    if (offScreenGL == null) {
                        offScreenGL = OffScreenGL(config)
                    }
                }
                START_PREVIEW -> {
                    Log.d(TAG, "START_PREVIEW")
                    skipFrameTimeMillis = 0
                    if (previewGL == null) {
                        previewGL = PreviewGL(
                            offScreenGL?.getSharedContext(),
                            config,
                            msg.obj as SurfaceTexture,
                            offScreenGL?.getOutputTextureId() ?: 0
                        )
                    }
                }

                STOP_PREVIEW -> {
                    Log.d(TAG, "STOP_PREVIEW skipFrameTimeMillis=$skipFrameTimeMillis")
                    previewGL?.destroy()
                    previewGL = null
                    removeCallbacksAndMessages(null)
                    synchronized(frameCountLock) {
                        frameCount = 0
                    }
                }
                ON_FRAME -> {
                    Log.d(TAG, "ON_FRAME")
                    synchronized(frameCountLock) {
                        // 这里必须要有，不然会是黑屏
                        offScreenGL?.makeCurrent()
                        while (frameCount != 0L) {
                            offScreenGL?.updateTexImage()
                            frameCount--
                        }
                    }
                    hasFrame = true

                }
                ON_DRAW -> {
                    if (!isPreviewing) {
                        return
                    }
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
                        offScreenGL?.draw()
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