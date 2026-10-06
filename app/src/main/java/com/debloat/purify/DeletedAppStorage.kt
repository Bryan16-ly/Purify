package com.debloat.purify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object DeletedAppStorage {

    private const val PREF_NAME = "purify_deleted_apps"
    private const val KEY_APPS = "deleted_apps"

    fun saveApp(
        context: Context,
        app: AppInfo
    ) {

        val preferences =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        val existingJson =
            preferences.getString(
                KEY_APPS,
                null
            )

        val jsonArray =
            if (existingJson != null) {
                JSONArray(existingJson)
            } else {
                JSONArray()
            }

        /*
         * Jangan menyimpan aplikasi yang sama
         * berkali-kali.
         */
        for (index in 0 until jsonArray.length()) {

            val existing =
                jsonArray.getJSONObject(index)

            if (
                existing.optString("packageName") ==
                app.packageName
            ) {
                return
            }
        }

        val jsonObject =
            JSONObject()

        jsonObject.put(
            "packageName",
            app.packageName
        )

        jsonObject.put(
            "appName",
            app.appName
        )

        jsonObject.put(
            "isSystemApp",
            app.isSystemApp
        )

        jsonObject.put(
            "versionName",
            app.versionName
        )

        jsonObject.put(
            "versionCode",
            app.versionCode
        )

        jsonObject.put(
            "apkPath",
            app.apkPath
        )

        jsonObject.put(
            "apkSize",
            app.apkSize
        )

        jsonArray.put(
            jsonObject
        )

        preferences
            .edit()
            .putString(
                KEY_APPS,
                jsonArray.toString()
            )
            .apply()
    }

    fun loadApps(
        context: Context
    ): List<DeletedAppInfo> {

        val preferences =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        val jsonString =
            preferences.getString(
                KEY_APPS,
                null
            )
                ?: return emptyList()

        return try {

            val jsonArray =
                JSONArray(jsonString)

            val apps =
                mutableListOf<DeletedAppInfo>()

            for (index in 0 until jsonArray.length()) {

                val jsonObject =
                    jsonArray.getJSONObject(index)

                apps.add(
                    DeletedAppInfo(
                        packageName =
                            jsonObject.optString(
                                "packageName"
                            ),

                        appName =
                            jsonObject.optString(
                                "appName"
                            ),

                        isSystemApp =
                            jsonObject.optBoolean(
                                "isSystemApp",
                                false
                            ),

                        versionName =
                            jsonObject.optString(
                                "versionName",
                                "Unknown"
                            ),

                        versionCode =
                            jsonObject.optLong(
                                "versionCode",
                                0L
                            ),

                        apkPath =
                            jsonObject.optString(
                                "apkPath",
                                ""
                            ),

                        apkSize =
                            jsonObject.optLong(
                                "apkSize",
                                0L
                            )
                    )
                )
            }

            apps

        } catch (_: Exception) {

            emptyList()
        }
    }
}