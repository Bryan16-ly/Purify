package com.debloat.purify

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuManager {

    const val REQUEST_CODE = 1001

    /**
     * Mengecek apakah service Shizuku sedang aktif.
     */
    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Mengecek apakah Purify sudah mendapatkan
     * permission Shizuku.
     */
    fun hasPermission(): Boolean {
        if (!isRunning()) {
            return false
        }
        return try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() ==
                        PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Meminta permission Shizuku.
     */
    fun requestPermission() {
        if (!isRunning()) {
            return
        }
        try {
            if (Shizuku.isPreV11()) {
                return
            }

            if (!hasPermission()) {
                Shizuku.requestPermission(
                    REQUEST_CODE
                )
            }
        } catch (_: Throwable) {
        }
    }
}