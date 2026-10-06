package com.debloat.purify

data class DeletedAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val versionName: String,
    val versionCode: Long,
    val apkPath: String,
    val apkSize: Long
)