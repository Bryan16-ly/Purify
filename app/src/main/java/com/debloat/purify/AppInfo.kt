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
