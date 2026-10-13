/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

package com.debloat.purify

data class DisplayAppInfo(
    val appInfo: AppInfo?,
    val deletedAppInfo: DeletedAppInfo?,
    val status: AppStatus
)