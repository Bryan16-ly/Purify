package com.debloat.purify

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject

object AppListStorage {

    private const val PREF_NAME = "purify_app_cache"
    private const val KEY_APPS = "apps"

    /*
     * ==========================================
     * CEK APAKAH CACHE SUDAH ADA
     * ==========================================
     */

    fun hasCache(
        context: Context
    ): Boolean {

        val preferences =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return preferences.contains(KEY_APPS)
    }

    /*
     * ==========================================
     * SIMPAN APP LIST
     * ==========================================
     */

    fun saveApps(
        context: Context,
        apps: List<AppInfo>
    ) {

        val jsonArray =
            JSONArray()

        apps.forEach { app ->

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
 			   "isEnabled",
   			 app.isEnabled
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
                "targetSdk",
                app.targetSdk
            )

            jsonObject.put(
                "minSdk",
                app.minSdk
            )

            jsonObject.put(
                "uid",
                app.uid
            )

            jsonObject.put(
                "apkPath",
                app.apkPath
            )

            jsonObject.put(
                "apkSize",
                app.apkSize
            )

            jsonObject.put(
                "dataSize",
                app.dataSize
            )

            jsonObject.put(
                "cacheSize",
                app.cacheSize
            )

            jsonObject.put(
                "totalSize",
                app.totalSize
            )

            jsonObject.put(
                "installer",
                app.installer
            )

            /*
             * REQUESTED PERMISSIONS
             */

            val requestedPermissions =
                JSONArray()

            app.requestedPermissions.forEach { permission ->

                requestedPermissions.put(
                    permission
                )
            }

            jsonObject.put(
                "requestedPermissions",
                requestedPermissions
            )

            /*
             * GRANTED PERMISSIONS
             */

            val grantedPermissions =
                JSONArray()

            app.grantedPermissions.forEach { permission ->

                grantedPermissions.put(
                    permission
                )
            }

            jsonObject.put(
                "grantedPermissions",
                grantedPermissions
            )

            jsonArray.put(
                jsonObject
            )
        }

        context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_APPS,
                jsonArray.toString()
            )
            .apply()
    }

    /*
     * ==========================================
     * LOAD APP LIST
     * ==========================================
     */

    fun loadApps(
        context: Context
    ): List<AppInfo> {

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
                JSONArray(
                    jsonString
                )

            val packageManager =
                context.packageManager

            val apps =
                mutableListOf<AppInfo>()

            for (index in 0 until jsonArray.length()) {

                try {

                    val jsonObject =
                        jsonArray.getJSONObject(
                            index
                        )

                    val packageName =
                        jsonObject.getString(
                            "packageName"
                        )

                    /*
                     * Pastikan aplikasi masih
                     * terpasang di perangkat.
                     */

                    val applicationInfo =
                        packageManager.getApplicationInfo(
                            packageName,
                            0
                        )

                    /*
                     * Ambil icon terbaru dari sistem.
                     *
                     * Drawable tidak disimpan ke JSON.
                     */

                    val icon =
                        packageManager.getApplicationIcon(
                            applicationInfo
                        )

                    /*
                     * REQUESTED PERMISSIONS
                     */

                    val requestedPermissions =
                        mutableListOf<String>()

                    val requestedArray =
                        jsonObject.optJSONArray(
                            "requestedPermissions"
                        )

                    if (requestedArray != null) {

                        for (
                            permissionIndex
                            in 0 until requestedArray.length()
                        ) {

                            requestedPermissions.add(
                                requestedArray.getString(
                                    permissionIndex
                                )
                            )
                        }
                    }

                    /*
                     * GRANTED PERMISSIONS
                     */

                    val grantedPermissions =
                        mutableListOf<String>()

                    val grantedArray =
                        jsonObject.optJSONArray(
                            "grantedPermissions"
                        )

                    if (grantedArray != null) {

                        for (
                            permissionIndex
                            in 0 until grantedArray.length()
                        ) {

                            grantedPermissions.add(
                                grantedArray.getString(
                                    permissionIndex
                                )
                            )
                        }
                    }

                    val appInfo =
                        AppInfo(

                            packageName =
                                packageName,

                            appName =
                                jsonObject.getString(
                                    "appName"
                                ),

                            isSystemApp =
                                jsonObject.optBoolean(
                                    "isSystemApp",
                                    false
                                ),
                            
                            isEnabled =
  							  jsonObject.optBoolean(
    							    "isEnabled",
    							    applicationInfo.enabled
   							 ),

                            icon =
                                icon,

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

                            targetSdk =
                                jsonObject.optInt(
                                    "targetSdk",
                                    0
                                ),

                            minSdk =
                                jsonObject.optInt(
                                    "minSdk",
                                    0
                                ),

                            uid =
                                jsonObject.optInt(
                                    "uid",
                                    applicationInfo.uid
                                ),

                            apkPath =
                                jsonObject.optString(
                                    "apkPath",
                                    applicationInfo.sourceDir
                                        ?: ""
                                ),

                            apkSize =
                                jsonObject.optLong(
                                    "apkSize",
                                    0L
                                ),

                            dataSize =
                                jsonObject.optLong(
                                    "dataSize",
                                    0L
                                ),

                            cacheSize =
                                jsonObject.optLong(
                                    "cacheSize",
                                    0L
                                ),

                            totalSize =
                                jsonObject.optLong(
                                    "totalSize",
                                    0L
                                ),

                            requestedPermissions =
                                requestedPermissions,

                            grantedPermissions =
                                grantedPermissions,

                            installer =
                                jsonObject.optString(
                                    "installer",
                                    "Unknown"
                                )
                        )

                    apps.add(
                        appInfo
                    )

                } catch (_: Exception) {

                    /*
                     * Kalau satu aplikasi sudah
                     * tidak ada / datanya rusak,
                     * lewati aplikasi tersebut.
                     */

                }
            }

            apps.sortedBy {
                it.appName.lowercase()
            }

        } catch (_: Exception) {

            emptyList()
        }
    }

    /*
     * ==========================================
     * HAPUS CACHE
     * ==========================================
     *
     * Dipakai nanti ketika kita membuat
     * fitur "Scan aplikasi" manual.
     */

    fun clearCache(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_APPS
            )
            .apply()
    }
    
    fun loadDisplayApps(
    	context: Context
	): List<DisplayAppInfo> {

 	   val installedApps =
        loadApps(context)

  	  val deletedApps =
        DeletedAppStorage.loadApps(context)

	    val displayApps =
        mutableListOf<DisplayAppInfo>()

 	   /*
    	 * Aplikasi yang masih terinstall.
   	  */
	    installedApps.forEach { app ->

   	     val status =
    	        if (app.isEnabled) {
       	         AppStatus.ACTIVE
     	       } else {
       	         AppStatus.DISABLED
        	    }

    	    displayApps.add(
     	       DisplayAppInfo(
      	          appInfo = app,
     	           deletedAppInfo = null,
     	           status = status
   	         )
    	    )
   	 }

  	  /*
    	 * Aplikasi yang sudah dihapus.
   	  */
  	  deletedApps.forEach { deletedApp ->

   	     /*
 	        * Jangan tampilkan sebagai DELETED kalau
  	       * package tersebut ternyata sudah terinstall
  	       * kembali.
  	       */
    	    val alreadyInstalled =
   	         installedApps.any {
         	       it.packageName ==
         	           deletedApp.packageName
      	      }

      	  if (!alreadyInstalled) {

  	          displayApps.add(
         	       DisplayAppInfo(
      	              appInfo = null,
     	               deletedAppInfo = deletedApp,
      	              status = AppStatus.DELETED
        	        )
	            )
	        }
 	   }

	    return displayApps.sortedBy {
    	    when (it.status) {

     	       AppStatus.ACTIVE,
       	     AppStatus.DISABLED -> {
    	            it.appInfo?.appName
      	              ?.lowercase()
       	             ?: ""
        	    }

      	      AppStatus.DELETED -> {
      	          it.deletedAppInfo?.appName
       	             ?.lowercase()
    	                ?: ""
         	   }
    	    }
   	 }
	}
}