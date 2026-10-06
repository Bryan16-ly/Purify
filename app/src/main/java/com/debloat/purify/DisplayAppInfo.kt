package com.debloat.purify

data class DisplayAppInfo(
    val appInfo: AppInfo?,
    val deletedAppInfo: DeletedAppInfo?,
    val status: AppStatus
)