package com.dthfish.fishrecorder.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dthfish.fishrecorder.R
import com.dthfish.fishrecorder.audio.AudioRecorder
import com.dthfish.fishrecorder.audio.IAudioConsumer
import com.dthfish.fishrecorder.audio.IAudioConsumerFactory
import com.dthfish.fishrecorder.audio.bean.AudioConfig
import com.dthfish.fishrecorder.audio.consumer.pcm.PcmSaver
import com.dthfish.fishrecorder.utils.PermissionUtil
import com.dthfish.fishrecorder.utils.TAG
import kotlinx.android.synthetic.main.activity_record_pcm.*

/**
 * Description 保存 pcm，单独保存 pcm 其实没有什么意义，
 * 因为读取的人并不知道里面具体的声道数、采样率等信息，
 * 除非是明确知道这些信息的才能进行用 AudioTrack 进行播放，
 * 通常我们会用 Wave 格式保存，因为在 Wave 头中我们会保存相应的描述信息。
 * Author DthFish
 * Date  2019-12-05.
 */
class RecordPcmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_pcm)
        val audioRecorder = AudioRecorder()
        audioRecorder.setConsumerFactory(object : IAudioConsumerFactory {
            override fun createConsumer(config: AudioConfig): IAudioConsumer {
                val path =
                    getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath + "/pcm_${System.currentTimeMillis()}.pcm"
                return PcmSaver(path)
            }

        })

        btnRecord.setOnClickListener {
            if (verifyAudioPermissions(this@RecordPcmActivity)) {
                audioRecorder.start()
            }
        }

        btnStop.setOnClickListener {
            audioRecorder.stop()
        }


    }

    private fun verifyAudioPermissions(activity: Activity): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        return PermissionUtil.checkAndRequestPermission(activity, permissions)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (1 == requestCode) {
            var result = 0
            grantResults.forEach {
                result = result or it
            }
            if (result == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult")
            }
        }
    }
}