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

    fun disable(
        context: Context,
        packageName: String,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {
        execute(context, "pm disable-user --user 0 $packageName", accessMethod, onResult)
    }

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
            commandBuilder = { "pm disable-user --user 0 ${it.packageName}" },
            onComplete = onComplete
        )
    }

    fun uninstall(
        context: Context,
        app: AppInfo,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {
        execute(context, "pm uninstall --user 0 ${app.packageName}", accessMethod) { success ->
            if (success) {
                DeletedAppStorage.saveApp(context, app)
            }
            onResult(success)
        }
    }

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
            commandBuilder = { "pm uninstall --user 0 ${it.packageName}" },
            onComplete = onComplete,
            onSuccess = { DeletedAppStorage.saveApp(context, it) }
        )
    }

    fun restore(
        context: Context,
        app: DeletedAppInfo,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {
        execute(context, "cmd package install-existing --user 0 ${app.packageName}", accessMethod) { success ->
            if (success) {
                DeletedAppStorage.removeApp(context, app.packageName)
            }
            onResult(success)
        }
    }

    private fun execute(
        context: Context,
        command: String,
        accessMethod: AccessMethod,
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            val result = runCommand(command, accessMethod)
            val message = if (result.success) {
                "Perintah berhasil\n" + result.output.trim()
            } else {
                val reason = result.error.trim().ifEmpty { result.output.trim() }
                if (reason.isEmpty()) "Perintah gagal dijalankan" else reason.take(300)
            }

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onResult(result.success)
            }
        }.start()
    }

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
                val result = runCommand(commandBuilder(app), accessMethod)
                if (result.success) {
                    successCount++
                    onSuccess?.invoke(app)
                } else {
                    failedCount++
                }
            }

            val finalSuccess = successCount
            val finalFailed = failedCount

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Berhasil: $finalSuccess | Gagal: $finalFailed", Toast.LENGTH_LONG).show()
                onComplete(finalSuccess, finalFailed)
            }
        }.start()
    }

    private fun runCommand(
        command: String,
        accessMethod: AccessMethod
    ): CommandResult {
        var success = false
        var output = ""
        var error = ""

        try {
            when (accessMethod) {
                AccessMethod.ADB -> {
                    if (!Shizuku.pingBinder()) {
                        error = "Shizuku tidak aktif"
                    } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                        error = "Izin Shizuku belum diberikan"
                    } else {
                        val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                        val newProcessMethod: Method = shizukuClass.getDeclaredMethod(
                            "newProcess",
                            Array<String>::class.java,
                            Array<String>::class.java,
                            String::class.java
                        )
                        newProcessMethod.isAccessible = true

                        val process = newProcessMethod.invoke(
                            null,
                            arrayOf("sh", "-c", command),
                            null,
                            null
                        ) as ShizukuRemoteProcess

                        output = process.inputStream.bufferedReader().use { it.readText() }
                        error = process.errorStream.bufferedReader().use { it.readText() }
                        process.waitFor()
                        success = process.exitValue() == 0
                        process.destroy()
                    }
                }
                AccessMethod.ROOT -> {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                    output = process.inputStream.bufferedReader().use { it.readText() }
                    error = process.errorStream.bufferedReader().use { it.readText() }
                    process.waitFor()
                    success = process.exitValue() == 0
                    process.destroy()
                }
            }
        } catch (exception: Exception) {
            error = exception.message ?: exception.javaClass.simpleName
            success = false
        }

        return CommandResult(success, output, error)
    }

    private data class CommandResult(
        val success: Boolean,
        val output: String,
        val error: String
    )
}
