package com.dthfish.fishrecorder.audio

import android.media.AudioRecord
import android.util.Log
import com.dthfish.fishrecorder.audio.bean.AudioConfig
import com.dthfish.fishrecorder.utils.TAG

/**
 * Description
 * Author DthFish
 * Date  2019-12-04.
 */
class AudioRecorder(private val config: AudioConfig = AudioConfig()) {

    private var audioRecord: AudioRecord? = null
    private var audioBuffer: ByteArray = ByteArray(config.getMinBufferSize())
    private var isStart = false
    private var audioRecordThread: AudioRecordThread? = null

    private var consumer: IAudioConsumer? = null
    private var consumerFactory: IAudioConsumerFactory? = null

    fun setConsumerFactory(factory: IAudioConsumerFactory) {
        consumerFactory = factory
    }

    @Synchronized
    fun start() {
        if (isStart) return
        isStart = true

        try {
            audioRecord = AudioRecord(
                config.getAudioSource(),
                config.getSampleRate(),
                config.getChannel(),
                config.getFormat(),
                config.getMinBufferSize()
            )
        } catch (ex: IllegalArgumentException) {
            Log.e(TAG, "AudioRecorder prepare fail")
            ex.printStackTrace()
        }

        consumer = consumerFactory?.createConsumer(config)
        audioRecordThread = AudioRecordThread()
        audioRecordThread?.start()

    }

    @Synchronized
    fun stop() {
        if (!isStart) return
        isStart = false
        audioRecordThread?.quit()
        try {
            audioRecordThread?.join()
        } catch (ignored: InterruptedException) {
        }
        audioRecordThread = null

    }

    inner class AudioRecordThread : Thread("AudioRecord") {

        private var isStart = true

        fun quit() {
            isStart = false
        }

        private fun performStart() {
            Log.d(TAG, "AudioRecordThread performStart")
            consumer?.start()

            audioRecord?.startRecording()
        }

        override fun run() {
            performStart()
            while (isStart) {
                val size = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (consumer != null && size > 0) {
                    consumer?.consume(audioBuffer, size, false)
                }
            }
            // 再读一次打上 end 标记
            val size = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
            if (consumer != null && size > 0) {
                consumer?.consume(audioBuffer, size, true)
            }
            performStop()
        }

        private fun performStop() {
            Log.d(TAG, "AudioRecordThread performStop")
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            consumer?.stop()
            consumer = null
        }
    }

}