package com.debloat.purify

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method

object PackageManagerAction {

    /*
     * ==========================================
     * DISABLE SATU APLIKASI
     * ==========================================
     */

    fun disable(
        context: Context,
        packageName: String,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {
        execute(
            context = context,
            command =
                "pm disable-user --user 0 $packageName",
            accessMethod = accessMethod,
            onResult = onResult
        )
    }

    /*
     * ==========================================
     * DISABLE BANYAK APLIKASI
     * ==========================================
     */

    fun disableMultiple(
        context: Context,
        apps: List<AppInfo>,
        accessMethod: AccessMethod,
        onComplete: (Int, Int) -> Unit
    ) {

        executeMultiple(
            context = context,
            apps = apps,
            accessMethod = accessMethod,

            commandBuilder = { app ->
                "pm disable-user --user 0 ${app.packageName}"
            },

            onComplete = onComplete
        )
    }

    /*
     * ==========================================
     * UNINSTALL SATU APLIKASI
     * ==========================================
     */

    fun uninstall(
        context: Context,
        app: AppInfo,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {

        execute(
            context = context,

            command =
                "pm uninstall --user 0 ${app.packageName}",

            accessMethod = accessMethod
        ) { success ->

            if (success) {

                DeletedAppStorage.saveApp(
                    context = context,
                    app = app
                )
            }

            onResult(success)
        }
    }

    /*
     * ==========================================
     * UNINSTALL BANYAK APLIKASI
     * ==========================================
     */

    fun uninstallMultiple(
        context: Context,
        apps: List<AppInfo>,
        accessMethod: AccessMethod,
        onComplete: (Int, Int) -> Unit
    ) {

        executeMultiple(
            context = context,
            apps = apps,
            accessMethod = accessMethod,

            commandBuilder = { app ->
                "pm uninstall --user 0 ${app.packageName}"
            },

            onComplete = onComplete,

            onSuccess = { app ->

                DeletedAppStorage.saveApp(
                    context = context,
                    app = app
                )
            }
        )
    }

    /*
     * ==========================================
     * EXECUTE SATU PERINTAH
     * ==========================================
     */

    private fun execute(
        context: Context,
        command: String,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {

        Thread {

            val result =
                runCommand(
                    command = command,
                    accessMethod = accessMethod
                )

            val message =
                if (result.success) {

                    "Perintah berhasil\n" +
                        result.output.trim()

                } else {

                    val reason =
                        result.error
                            .trim()
                            .ifEmpty {
                                result.output.trim()
                            }

                    if (reason.isEmpty()) {
                        "Perintah gagal dijalankan"
                    } else {
                        reason.take(300)
                    }
                }

            Handler(
                Looper.getMainLooper()
            ).post {

                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_LONG
                ).show()

                onResult(
                    result.success
                )
            }

        }.start()
    }
    
    /*
    * ==========================================
    * RESTORE SATU APLIKASI
    * ==========================================
    *
    * Mengembalikan aplikasi yang sebelumnya
    * di-uninstall untuk user 0.
    *
    * APK sistem tidak di-install ulang dari file.
    * Android hanya mengaktifkan kembali package
    * yang masih tersedia di partisi sistem.
    */

    fun restore(
        context: Context,
        app: DeletedAppInfo,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {

        execute(
            context = context,

            command =
                "cmd package install-existing --user 0 ${app.packageName}",

            accessMethod = accessMethod
        ) { success ->

            if (success) {

                DeletedAppStorage.removeApp(
                    context = context,
                    packageName = app.packageName
                )
            }

            onResult(success)
        }
    }

    /*
     * ==========================================
     * EXECUTE BANYAK PERINTAH
     * ==========================================
     *
     * Aplikasi dijalankan satu per satu.
     *
     * Tidak dijalankan secara paralel agar:
     *
     * 1. Shizuku tidak dibanjiri proses.
     * 2. Hasil tiap aplikasi bisa diketahui.
     * 3. Kalau satu gagal, aplikasi berikutnya
     *    tetap dicoba.
     */

    private fun executeMultiple(
        context: Context,
        apps: List<AppInfo>,
        accessMethod: AccessMethod,
        commandBuilder: (AppInfo) -> String,
        onComplete: (Int, Int) -> Unit,
        onSuccess: ((AppInfo) -> Unit)? = null
    ) {

        Thread {

            var successCount = 0
            var failedCount = 0

            apps.forEach { app ->

                val result =
                    runCommand(
                        command =
                            commandBuilder(app),

                        accessMethod =
                            accessMethod
                    )

                if (result.success) {

                    successCount++

                    onSuccess?.invoke(app)

                } else {

                    failedCount++
                }
            }

            val finalSuccessCount =
                successCount

            val finalFailedCount =
                failedCount

            Handler(
                Looper.getMainLooper()
            ).post {

                Toast.makeText(
                    context,
                    "Berhasil: $finalSuccessCount | " +
                        "Gagal: $finalFailedCount",
                    Toast.LENGTH_LONG
                ).show()

                onComplete(
                    finalSuccessCount,
                    finalFailedCount
                )
            }

        }.start()
    }

    /*
     * ==========================================
     * LOW LEVEL COMMAND EXECUTOR
     * ==========================================
     */

    private fun runCommand(
        command: String,
        accessMethod: AccessMethod
    ): CommandResult {

        var success = false
        var output = ""
        var error = ""

        try {

            when (accessMethod) {

                /*
                 * ==================================
                 * ADB / SHIZUKU
                 * ==================================
                 */

                AccessMethod.ADB -> {

                    if (!Shizuku.pingBinder()) {

                        error =
                            "Shizuku tidak aktif"

                    } else if (
                        Shizuku.checkSelfPermission() !=
                        PackageManager.PERMISSION_GRANTED
                    ) {

                        error =
                            "Permission Shizuku belum diberikan"

                    } else {

                        val shizukuClass =
                            Class.forName(
                                "rikka.shizuku.Shizuku"
                            )

                        val newProcessMethod:
                            Method =
                            shizukuClass.getDeclaredMethod(
                                "newProcess",
                                Array<String>::class.java,
                                Array<String>::class.java,
                                String::class.java
                            )

                        newProcessMethod.isAccessible =
                            true

                        val process =
                            newProcessMethod.invoke(
                                null,

                                arrayOf(
                                    "sh",
                                    "-c",
                                    command
                                ),

                                null,
                                null

                            ) as ShizukuRemoteProcess

                        output =
                            process.inputStream
                                .bufferedReader()
                                .use {
                                    it.readText()
                                }

                        error =
                            process.errorStream
                                .bufferedReader()
                                .use {
                                    it.readText()
                                }

                        process.waitFor()

                        success =
                            process.exitValue() == 0

                        process.destroy()
                    }
                }

                /*
                 * ==================================
                 * ROOT
                 * ==================================
                 */

                AccessMethod.ROOT -> {

                    val process =
                        Runtime.getRuntime().exec(
                            arrayOf(
                                "su",
                                "-c",
                                command
                            )
                        )

                    output =
                        process.inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    error =
                        process.errorStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    process.waitFor()

                    success =
                        process.exitValue() == 0

                    process.destroy()
                }
            }

        } catch (exception: Exception) {

            error =
                exception.message
                    ?: exception.javaClass.simpleName

            success = false
        }

        return CommandResult(
            success = success,
            output = output,
            error = error
        )
    }

    /*
     * ==========================================
     * COMMAND RESULT
     * ==========================================
     */

    private data class CommandResult(
        val success: Boolean,
        val output: String,
        val error: String
    )
}