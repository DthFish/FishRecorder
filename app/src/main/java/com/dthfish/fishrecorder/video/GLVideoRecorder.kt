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
import com.dthfish.fishrecorder.video.bean.VideoConfig
import com.dthfish.fishrecorder.video.opengl.MediaCodecGL
import com.dthfish.fishrecorder.video.opengl.OffScreenGL
import com.dthfish.fishrecorder.video.opengl.PreviewGL
import java.io.IOException
import kotlin.system.measureTimeMillis

/**
 * Description
 * Author DthFish
 * Date  2019-12-10.
 */
class GLVideoRecorder(private val config: VideoConfig = VideoConfig.obtainGL()) {
    private var camera: Camera? = null

    /**
     * 是否在录制和是否在预览的逻辑相互独立，
     * 在录制的时候也可以不看预览画面
     */
    @Volatile
    private var isRecording = false
    @Volatile
    private var isPreviewing = false

    private val openGLThread = HandlerThread("OpenGLThread")
    private val openGLHandler: OpenGLHandler

    private var drawInterval: Int

    private var packerFactory: IVideoPackerFactory? = null

    fun setPackerFactory(factory: IVideoPackerFactory) {
        packerFactory = factory
    }

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
    fun swapCamera() {
        config.swapCamera()
        camera?.stopPreview()
        camera?.release()
        camera = null
        try {
            camera = Camera.open(config.getDefaultCamera())

        } catch (e: Exception) {
            e.printStackTrace()
        }
        camera?.apply {
            Camera1Util.selectPreviewParams(this, config)
            Camera1Util.applyPreviewParams(this, config)
        }

        openGLHandler.sendMessage(openGLHandler.obtainMessage(SWAP_CAMERA))
        try {
            val cameraTexture = openGLHandler.getCameraTexture()
            camera?.setPreviewTexture(cameraTexture)
        } catch (e: IOException) {
            e.printStackTrace()
            camera?.release()
            camera = null
        }
        camera?.startPreview()


    }

    @Synchronized
    fun startPreview(surface: SurfaceTexture, width: Int, height: Int) {
        if (isPreviewing) return
        config.setScreenWidth(width)
        config.setScreenHeight(height)
        if (!checkAndStart()) return
        openGLHandler.sendMessage(openGLHandler.obtainMessage(START_PREVIEW, surface))
        // 如果 startRecording 里面启动了 ON_DRAW 则这里不用再调用
        if (!isRecording) {
            openGLHandler.sendMessage(
                openGLHandler.obtainMessage(
                    ON_DRAW,
                    System.currentTimeMillis()
                )
            )
        }
        isPreviewing = true

    }

    @Synchronized
    fun updatePreview(width: Int, height: Int) {
        config.setScreenWidth(width)
        config.setScreenHeight(height)
    }

    @Synchronized
    fun stopPreview() {
        if (isPreviewing) {
            openGLHandler.sendMessage(openGLHandler.obtainMessage(STOP_PREVIEW))
            if (!isRecording) {
                camera?.stopPreview()
            }
            isPreviewing = false
        }

    }

    @Synchronized
    fun startRecording() {
        if (isRecording) return
        if (!checkAndStart()) return

        openGLHandler.sendMessage(openGLHandler.obtainMessage(START_RECORD))
        // 如果 startPreview 里面启动了 ON_DRAW 则这里不用再调用
        if (!isPreviewing) {
            openGLHandler.sendMessage(
                openGLHandler.obtainMessage(
                    ON_DRAW,
                    System.currentTimeMillis()
                )
            )
        }

        isRecording = true
    }

    private fun checkAndStart(): Boolean {
        if (!(isRecording || isPreviewing)) {
            try {
                val cameraTexture = openGLHandler.getCameraTexture()
                camera?.setPreviewTexture(cameraTexture)
            } catch (e: IOException) {
                e.printStackTrace()
                camera?.release()
                return false
            }
            camera?.startPreview()
        }
        return true
    }

    @Synchronized
    fun stopRecording() {
        if (isRecording) {
            openGLHandler.sendMessage(openGLHandler.obtainMessage(STOP_RECORD))
            if (!isPreviewing) {
                camera?.stopPreview()
            }
            isRecording = false
        }
    }

    @Synchronized
    fun destroy() {
        camera?.release()
        camera = null

        openGLHandler.sendEmptyMessage(ON_DESTROY)
        openGLThread.quitSafely()
        try {
            openGLThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    companion object {
        const val ON_CREATE = 0
        const val START_PREVIEW = 1
        const val STOP_PREVIEW = 2
        const val ON_FRAME = 3
        const val ON_DRAW = 4
        const val START_RECORD = 5
        const val STOP_RECORD = 6
        const val ON_DESTROY = 7
        const val SWAP_CAMERA = 8

    }

    inner class OpenGLHandler(looper: Looper) : Handler(looper) {
        private var frameCount = 0L
        private val frameCountLock = Any()

        /**
         * 初始为 0，每当绘制的时间超过 fps 计算出的时间就会累加
         * 直接把它除以 [drawInterval] 就相当于丢了多少帧
         */
        private var skipFrameTimeMillis = 0L

        private var recordSkipFrameTimeMillis = 0L

        /**
         * 这里直接创建的离屏的 egl 环境，是因为马上需要构造一个 SurfaceTexture
         * 用来获取相机的预览数据 [getCameraTexture]
         */
        private var offScreenGL: OffScreenGL? = null

        private var previewGL: PreviewGL? = null

        private var codecGL: MediaCodecGL? = null

        private var videoEncoder: VideoEncoder? = null

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
                    Log.d(TAG, "ON_CREATE")
                    if (offScreenGL == null) {
                        offScreenGL = OffScreenGL(config)
                    }
                }
                START_PREVIEW -> {
                    Log.d(TAG, "START_PREVIEW")
                    if (previewGL == null) {
                        skipFrameTimeMillis = 0
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

                    checkStopLoop()
                }
                START_RECORD -> {
                    Log.d(TAG, "START_RECORD")
                    if (codecGL == null) {
                        recordSkipFrameTimeMillis = 0
                        val mediaCodec = config.createMediaCodec()!!
                        val packer = packerFactory?.createPacker(config)!!
                        videoEncoder = VideoEncoder(mediaCodec, packer)

                        codecGL = MediaCodecGL(
                            offScreenGL?.getSharedContext(),
                            config,
                            mediaCodec.createInputSurface(),
                            offScreenGL?.getOutputTextureId() ?: 0
                        )

                        videoEncoder?.start()
                    }

                }
                STOP_RECORD -> {
                    Log.d(TAG, "STOP_RECORD recordSkipFrameTimeMillis=$recordSkipFrameTimeMillis")
                    codecGL?.destroy()
                    codecGL = null
                    videoEncoder?.stop()
                    videoEncoder = null

                    checkStopLoop()
                }
                ON_FRAME -> {
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
                    if (!(isPreviewing || isRecording)) {
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
                    // 延迟消息的时间 = 每一帧的时间间隔 - 在 Handler 调度中消耗的时间 - 绘制消耗的时间
                    val sendTimeMillis = msg.obj as Long
                    val spendTimeMillisInLoop = System.currentTimeMillis() - sendTimeMillis

                    val spendTimeMillisInDraw = measureTimeMillis {
                        offScreenGL?.draw()
                        codecGL?.draw()
                        previewGL?.draw()
                    }

                    val delayTimeMillis =
                        drawInterval - spendTimeMillisInDraw - spendTimeMillisInLoop
//                    Log.d(TAG, "ON_DRAW delayTimeMillis=$delayTimeMillis")

                    if (delayTimeMillis <= 0) {
                        skipFrameTimeMillis -= delayTimeMillis
                        recordSkipFrameTimeMillis -= delayTimeMillis
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

                ON_DESTROY -> {
                    Log.d(TAG, "ON_DESTROY")
                    offScreenGL?.destroy()
                    offScreenGL = null
                }
                SWAP_CAMERA -> {
                    Log.d(TAG, "SWAP_CAMERA")
                    offScreenGL?.swapCamera()
                }

                else -> {
                }
            }
        }

        private fun checkStopLoop() {
            if (isRecording || isPreviewing) {
                return
            }
            removeCallbacksAndMessages(null)
            synchronized(frameCountLock) {
                frameCount = 0
            }

        }

    }


}