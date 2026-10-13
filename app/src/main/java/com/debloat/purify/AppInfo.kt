/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

package com.debloat.purify

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val icon: Drawable,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val uid: Int,
    val apkPath: String,
    val apkSize: Long,
    val dataSize: Long,
    val cacheSize: Long,
    val totalSize: Long,
    val requestedPermissions: List<String>,
    val grantedPermissions: List<String>,
    val installer: String
)
