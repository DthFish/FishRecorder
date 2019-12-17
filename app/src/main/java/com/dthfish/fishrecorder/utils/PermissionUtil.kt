package com.dthfish.fishrecorder.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * Description
 * Author DthFish
 * Date  2019-12-05.
 */
object PermissionUtil {

    fun checkAndRequestPermission(activity: Activity, permissions: Array<String>): Boolean {
        if (permissions.isEmpty()) {
            return true
        }

        var check = PackageManager.PERMISSION_GRANTED
        permissions.forEach {
            check = check or ActivityCompat.checkSelfPermission(
                activity,
                it
            )
        }
        return if (check != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity, permissions, 1
            )
            false
        } else {
            true
        }

    }
}