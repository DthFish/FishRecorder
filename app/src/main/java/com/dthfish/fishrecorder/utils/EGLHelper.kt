package com.dthfish.fishrecorder.utils

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface
import com.dthfish.fishrecorder.utils.GLUtil.checkEglError
import javax.microedition.khronos.egl.EGL10

/**
 * Description
 * Author DthFish
 * Date  2019-12-03.
 */
class EGLHelper private constructor() {

    companion object {

        /**
         * init flag: surface must be recordable.  This discourages EGL from using a
         * pixel format that cannot be converted efficiently to something usable by the video
         * encoder.
         */
        const val FLAG_RECORDABLE = 0x01

        /**
         * init flag: ask for GLES3, fall back to GLES2 if not available.  Without this
         * flag, GLES2 is used.
         */
        const val FLAG_TRY_GLES3 = 0x02

        fun obtain(isCodec: Boolean = false): EGLHelper {
            val eglHelper = EGLHelper()
            val flags = if (isCodec) {
                FLAG_TRY_GLES3
            } else {
                FLAG_TRY_GLES3 or FLAG_RECORDABLE
            }
            eglHelper.init(null, flags)
            return eglHelper
        }

    }


    // Android-specific extension.
    private val EGL_RECORDABLE_ANDROID = 0x3142

    private var eglDisplay: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext? = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface? = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    private var glVersion = -1

    private fun init(sharedContext: EGLContext?, flags: Int) {
        val fixedSharedContext = sharedContext ?: EGL14.EGL_NO_CONTEXT

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }
        if (flags and FLAG_TRY_GLES3 != 0) {
            val config = getConfig(flags, 3)
            if (config != null) {
                val attrib3List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
                val context = EGL14.eglCreateContext(
                    eglDisplay, config, fixedSharedContext,
                    attrib3List, 0
                )

                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    Log.d(TAG, "Got GLES 3 config")
                    eglConfig = config
                    eglContext = context
                    glVersion = 3
                }
            }
        }
        // GLES 2 only, or GLES 3 attempt failed
        if (eglContext === EGL14.EGL_NO_CONTEXT) {
            Log.d(TAG, "Trying GLES 2")
            val config =
                getConfig(flags, 2) ?: throw RuntimeException("Unable to find a suitable EGLConfig")
            val attrib2List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(
                eglDisplay, config, fixedSharedContext,
                attrib2List, 0
            )
            checkEglError("eglCreateContext")
            eglConfig = config
            eglContext = context
            glVersion = 2
        }

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(
            eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
            values, 0
        )
        Log.d(TAG, "EGLContext created, client version " + values[0])
    }

    fun createScreenSurface(surface: Any) {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw RuntimeException("invalid surface: $surface")
        }
        val surfaceAttrs = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttrs, 0)
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == null) {
            throw RuntimeException("surface was null")
        }
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        checkEglError("eglMakeCurrent")
    }

    fun createOffScreenSurface(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            throw RuntimeException("eglCreateOffScreenSurface, but width or height < 0")
        }
        val pbAttrs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbAttrs, 0)
        checkEglError("eglCreatePbufferSurface")
        if (eglSurface == null) {
            throw RuntimeException("surface was null")
        }
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        checkEglError("eglMakeCurrent")
    }

    fun createMediaCodecSurface(surface: Any) {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw RuntimeException("invalid surface: $surface")
        }
        val surfaceAttrs = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttrs, 0)
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == null) {
            throw RuntimeException("surface was null")
        }
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        checkEglError("eglMakeCurrent")
    }

    fun makeCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        checkEglError("eglMakeCurrent")
    }

    private fun pause() {
        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            return
        }
        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    fun destroy() {
        pause()
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(eglDisplay)
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
    }

    fun swapBuffers() {
        if (eglDisplay != null && eglSurface != null) {
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }
    }

    fun setPresentationTime(presentationTime: Long) {
        if (null != eglDisplay && null != eglSurface) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTime)
        }
    }

    private fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            //EGL14.EGL_DEPTH_SIZE, 16,
            //EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            EGL14.EGL_NONE, 0, // placeholder for recordable [@-3]
            EGL14.EGL_NONE
        )
        if (flags and FLAG_RECORDABLE != 0) {
            attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID
            attribList[attribList.size - 2] = 1
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            Log.w(TAG, "unable to find RGB8888 / $version EGLConfig")
            return null
        }
        return configs[0]
    }

}