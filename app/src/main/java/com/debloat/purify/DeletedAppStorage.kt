package com.debloat.purify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object DeletedAppStorage {

    private const val PREF_NAME = "purify_deleted_apps"
    private const val KEY_APPS = "deleted_apps"

    fun saveApp(context: Context, app: AppInfo) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existingJson = prefs.getString(KEY_APPS, null)
        val jsonArray = if (existingJson != null) JSONArray(existingJson) else JSONArray()

        // Cek duplikat secara efisien
        for (i in 0 until jsonArray.length()) {
            if (jsonArray.getJSONObject(i).optString("packageName") == app.packageName) {
                return
            }
        }

        val jsonObject = JSONObject().apply {
            put("packageName", app.packageName)
            put("appName", app.appName)
            put("isSystemApp", app.isSystemApp)
            put("versionName", app.versionName)
            put("versionCode", app.versionCode)
            put("apkPath", app.apkPath)
            put("apkSize", app.apkSize)
        }

        jsonArray.put(jsonObject)
        prefs.edit().putString(KEY_APPS, jsonArray.toString()).apply()
    }

    fun removeApp(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existingJson = prefs.getString(KEY_APPS, null) ?: return

        try {
            val jsonArray = JSONArray(existingJson)
            val newArray = JSONArray()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("packageName") != packageName) {
                    newArray.put(obj)
                }
            }

            prefs.edit().putString(KEY_APPS, newArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadApps(context: Context): List<DeletedAppInfo> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_APPS, null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonString)
            val apps = mutableListOf<DeletedAppInfo>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                apps.add(
                    DeletedAppInfo(
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        isSystemApp = obj.optBoolean("isSystemApp", false),
                        versionName = obj.optString("versionName", "Unknown"),
                        versionCode = obj.optLong("versionCode", 0L),
                        apkPath = obj.optString("apkPath", ""),
                        apkSize = obj.optLong("apkSize", 0L)
                    )
                )
            }
            apps
        } catch (_: Exception) {
            emptyList()
        }
    }
}
