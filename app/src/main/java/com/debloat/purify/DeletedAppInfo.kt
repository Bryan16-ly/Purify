/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

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