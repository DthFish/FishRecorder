package com.dthfish.fishrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dthfish.fishrecorder.utils.PermissionUtil
import com.dthfish.fishrecorder.utils.TAG
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnGL.setOnClickListener {
            if (verifyVideoPermissions(this)) {
                startActivity(Intent(this, RecordWidthFilterActivity::class.java))
            }
        }

    }

    private fun verifyVideoPermissions(activity: Activity): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
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
