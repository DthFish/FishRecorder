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
import com.dthfish.fishrecorder.audio.consumer.AudioEncoder
import com.dthfish.fishrecorder.audio.packer.aac.AacPacker
import com.dthfish.fishrecorder.utils.PermissionUtil
import com.dthfish.fishrecorder.utils.TAG
import kotlinx.android.synthetic.main.activity_record_aac.*

/**
 * Description 对把 pcm 编码为 Aac 格式，然后添加 ADTS 头再保存为文件。
 * Author DthFish
 * Date  2019-12-05.
 */
class RecordAacActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_aac)
        val audioRecorder = AudioRecorder()
        audioRecorder.setConsumerFactory(object : IAudioConsumerFactory {
            override fun createConsumer(config: AudioConfig): IAudioConsumer {
                val path =
                    getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath + "/aac_${System.currentTimeMillis()}.aac"
                return AudioEncoder(config, AacPacker(config, path))
            }

        })

        btnRecord.setOnClickListener {
            if (verifyAudioPermissions(this@RecordAacActivity)) {
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