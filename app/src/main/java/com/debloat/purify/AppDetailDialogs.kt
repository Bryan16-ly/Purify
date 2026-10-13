/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

package com.debloat.purify

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rikka.shizuku.Shizuku
import java.util.Locale

@Composable
fun AppDetailDialog(
    app: AppInfo,
    allApps: List<AppInfo>,
    accessMethod: AccessMethod,
    isGlass: Boolean,
    glassRimBorder: BorderStroke,
    onDismiss: () -> Unit,
    onAppDisabled: (String) -> Unit,
    onAppUninstalled: (String) -> Unit,
    onAppListStorageUpdate: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        val dialogShape = RoundedCornerShape(26.dp)
        val glassDialogModifier = if (isGlass) {
            Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xF035354A), Color(0xF01A1A2A))
                    ),
                    dialogShape
                )
                .border(glassRimBorder, dialogShape)
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, dialogShape)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.90f)
                .clip(dialogShape)
                .then(glassDialogModifier)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = app.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (app.isEnabled) "Status: AKTIF" else "Status: NONAKTIF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (app.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gagal membuka Info Aplikasi Sistem", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Buka Info Aplikasi Sistem",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppInfoRow(title = "Nama Paket", value = app.packageName)
                AppInfoRow(title = "Versi", value = "${app.versionName} (${app.versionCode})")
                AppInfoRow(title = "Target / Min SDK", value = "${app.targetSdk} / ${app.minSdk}")
                AppInfoRow(title = "UID", value = app.uid.toString())
                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Penyimpanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AppInfoRow(title = "Aplikasi (APK)", value = formatFileSize(app.apkSize))
                AppInfoRow(title = "Data", value = formatFileSize(app.dataSize))
                AppInfoRow(title = "Cache", value = formatFileSize(app.cacheSize))
                AppInfoRow(title = "Total Ukuran", value = formatFileSize(app.totalSize))
                Spacer(modifier = Modifier.height(8.dp))

                AppInfoRow(title = "Pemasang", value = app.installer)
                if (accessMethod == AccessMethod.ROOT) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Lokasi APK", style = MaterialTheme.typography.labelMedium)
                    Text(text = app.apkPath, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Izin Aplikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                if (app.requestedPermissions.isEmpty()) {
                    Text(text = "Tidak ada izin yang diminta", style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(app.requestedPermissions) { permission ->
                                val granted = permission in app.grantedPermissions
                                val symbol = if (granted) "✓" else "○"
                                Text(
                                    text = "$symbol ${permission.substringAfterLast('.')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (granted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Opsi Nonaktifkan/Aktifkan HANYA tersedia jika menggunakan akses ROOT
                    if (accessMethod == AccessMethod.ROOT) {
                        Button(
                            onClick = {
                                val isEnabling = !app.isEnabled
                                changePackageState(context, app.packageName, accessMethod, isEnabling) { success ->
                                    if (success) {
                                        onDismiss()
                                        if (isEnabling) {
                                            val updatedApps = allApps.map { if (it.packageName == app.packageName) it.copy(isEnabled = true) else it }
                                            AppListStorage.saveApps(context, updatedApps)
                                        } else {
                                            onAppDisabled(app.packageName)
                                            val updatedApps = allApps.map { if (it.packageName == app.packageName) it.copy(isEnabled = false) else it }
                                            AppListStorage.saveApps(context, updatedApps)
                                        }
                                        onAppListStorageUpdate()
                                    }
                                }
                            }
                        ) {
                            Text(text = if (app.isEnabled) "Nonaktifkan" else "Aktifkan")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            PackageManagerAction.uninstall(context, app, accessMethod) { success ->
                                if (success) {
                                    onDismiss()
                                    onAppUninstalled(app.packageName)
                                    onAppListStorageUpdate()
                                }
                            }
                        }
                    ) {
                        Text(text = "Hapus")
                    }
                }
            }
        }
    }
}

@Composable
fun DeletedAppDetailDialog(
    app: DeletedAppInfo,
    accessMethod: AccessMethod,
    isGlass: Boolean,
    glassRimBorder: BorderStroke,
    onDismiss: () -> Unit,
    onAppRestored: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        val dialogShape = RoundedCornerShape(26.dp)
        val glassDialogModifier = if (isGlass) {
            Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xF035354A), Color(0xF01A1A2A))
                    ),
                    dialogShape
                )
                .border(glassRimBorder, dialogShape)
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, dialogShape)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.90f)
                .clip(dialogShape)
                .then(glassDialogModifier)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = app.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Status: TERHAPUS (User 0)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gagal membuka Info Aplikasi Sistem", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Buka Info Aplikasi Sistem",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                AppInfoRow(title = "Nama Paket", value = app.packageName)
                AppInfoRow(title = "Versi", value = "${app.versionName} (${app.versionCode})")
                AppInfoRow(title = "Ukuran APK", value = formatFileSize(app.apkSize))
                AppInfoRow(title = "Lokasi APK", value = app.apkPath)
                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            PackageManagerAction.restore(context, app, accessMethod) { success ->
                                if (success) {
                                    onDismiss()
                                    onAppRestored()
                                }
                            }
                        }
                    ) {
                        Text(text = "Pulihkan")
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoRow(title: String, value: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText(title, value))
                Toast.makeText(context, "$title disalin", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 4.dp)
    ) {
        Text(text = "$title:", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(text = value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun changePackageState(
    context: Context,
    packageName: String,
    accessMethod: AccessMethod,
    enable: Boolean,
    onResult: (Boolean) -> Unit
) {
    Thread {
        var success = false
        var output = ""
        var error = ""

        try {
            when (accessMethod) {
                AccessMethod.ADB -> {
                    error = "Penonaktifan aplikasi tidak didukung dalam mode ADB."
                    success = false
                }
                AccessMethod.ROOT -> {
                    val rootCmd = if (enable) {
                        "pm enable --user 0 $packageName; cmd package unsuspend --user 0 $packageName"
                    } else {
                        "pm disable-user --user 0 $packageName"
                    }
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", rootCmd))
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

        Handler(Looper.getMainLooper()).post {
            val successMessage = if (enable) "Aplikasi berhasil diaktifkan kembali" else "Aplikasi berhasil dinonaktifkan"
            val failMessage = if (enable) "Gagal mengaktifkan aplikasi" else "Gagal mematikan aplikasi"
            val message = if (success) successMessage else error.trim().ifEmpty { output.trim() }.ifEmpty { failMessage }
            Toast.makeText(context, message.take(300), Toast.LENGTH_LONG).show()
            onResult(success)
        }
    }.start()
}

fun formatFileSize(size: Long): String {
    if (size < 1024L) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}