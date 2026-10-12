/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files.
 */

package com.debloat.purify

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import rikka.shizuku.Shizuku
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    accessMethod: AccessMethod,
    darkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onAppDisabled: (String) -> Unit,
    onAppUninstalled: (String) -> Unit,
    onAppRestored: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current

    // Pengambilan versi aplikasi secara dinamis dari konfigurasi build
    val appVersionName = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "1.0.0"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            }
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    var displayApps by remember { mutableStateOf(emptyList<DisplayAppInfo>()) }

    LaunchedEffect(apps) {
        displayApps = AppListStorage.loadDisplayApps(context)
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedDeletedApp by remember { mutableStateOf<DeletedAppInfo?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showSettings by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDisabledApps by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler(enabled = showAboutDialog) { showAboutDialog = false }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedPackages = emptySet()
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    BackHandler(enabled = selectedApp != null || selectedDeletedApp != null) {
        selectedApp = null
        selectedDeletedApp = null
    }

    BackHandler(enabled = showDisabledApps && selectedApp == null && selectedDeletedApp == null && !showSettings && !selectionMode) {
        showDisabledApps = false
        selectedFilter = "ALL"
    }

    val installedDisplayApps = displayApps.filter { it.appInfo != null }
    val activeApps = installedDisplayApps.mapNotNull { it.appInfo }.filter { it.isEnabled }
    val disabledApps = installedDisplayApps.mapNotNull { it.appInfo }.filter { !it.isEnabled }
    val deletedApps = displayApps.filter { it.status == AppStatus.DELETED }.mapNotNull { it.deletedAppInfo }
    val currentInstalledApps = if (showDisabledApps) disabledApps else activeApps

    val filteredInstalledApps = remember(currentInstalledApps, selectedFilter, searchQuery, showDisabledApps) {
        currentInstalledApps
            .filter { app ->
                when (selectedFilter) {
                    "USER" -> !app.isSystemApp
                    "SYSTEM" -> app.isSystemApp
                    else -> true
                }
            }
            .filter { app ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    val filteredDeletedApps = remember(deletedApps, selectedFilter) {
        deletedApps.filter { app ->
            when (selectedFilter) {
                "USER" -> !app.isSystemApp
                "SYSTEM" -> app.isSystemApp
                else -> true
            }
        }
    }

    val allVisibleSelected = filteredInstalledApps.isNotEmpty() && filteredInstalledApps.all { it.packageName in selectedPackages }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        
        // Header Atas
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                IconButton(onClick = { selectionMode = false; selectedPackages = emptySet() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Batal memilih")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pilih Aplikasi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedPackages.size} dipilih",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        selectedPackages = if (allVisibleSelected) {
                            selectedPackages - filteredInstalledApps.map { it.packageName }.toSet()
                        } else {
                            selectedPackages + filteredInstalledApps.map { it.packageName }.toSet()
                        }
                    }
                ) {
                    Text(text = if (allVisibleSelected) "Batal" else "Semua")
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showDisabledApps) "Aplikasi Nonaktif" else "Aplikasi",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (showDisabledApps) "Total: ${disabledApps.size + deletedApps.size}" 
                               else "Aktif: ${activeApps.size}  •  Pengguna: ${activeApps.count { !it.isSystemApp }}  •  Sistem: ${activeApps.count { it.isSystemApp }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { selectionMode = true; selectedPackages = emptySet() }) {
                    Icon(imageVector = Icons.Default.Checklist, contentDescription = "Mode Tandai")
                }

                Box {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Pengaturan")
                    }

                    DropdownMenu(
                        expanded = showSettings,
                        onDismissRequest = { showSettings = false },
                        modifier = Modifier.width(230.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Ubah metode akses", style = MaterialTheme.typography.bodyLarge)
                                    Text("Saat ini: ${accessMethod.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                showSettings = false
                                onSettingsClick()
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tema gelap", style = MaterialTheme.typography.bodyLarge)
                                    Switch(
                                        checked = darkTheme,
                                        onCheckedChange = { enabled ->
                                            onThemeChanged(enabled)
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            },
                            onClick = {
                                onThemeChanged(!darkTheme)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Tentang Purify", style = MaterialTheme.typography.bodyLarge)
                                    Text("Informasi aplikasi & lisensi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                showSettings = false
                                showAboutDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            placeholder = { Text(text = "Cari nama atau paket aplikasi...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus teks")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !showDisabledApps,
                onClick = { showDisabledApps = false; selectedFilter = "ALL" },
                label = { Text("Aktif (${activeApps.size})") }
            )
            FilterChip(
                selected = showDisabledApps,
                onClick = { showDisabledApps = true; selectedFilter = "ALL" },
                label = { Text("Nonaktif (${disabledApps.size + deletedApps.size})") }
            )
            Spacer(modifier = Modifier.weight(1f))
            FilterChip(
                selected = selectedFilter == "USER",
                onClick = { selectedFilter = if (selectedFilter == "USER") "ALL" else "USER" },
                label = { Text("Pengguna") }
            )
            FilterChip(
                selected = selectedFilter == "SYSTEM",
                onClick = { selectedFilter = if (selectedFilter == "SYSTEM") "ALL" else "SYSTEM" },
                label = { Text("Sistem") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List Aplikasi
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(items = filteredInstalledApps, key = { "installed_" + it.packageName }) { app ->
                AppItem(
                    app = app,
                    disabled = !app.isEnabled,
                    selected = app.packageName in selectedPackages,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            selectedPackages = if (app.packageName in selectedPackages) selectedPackages - app.packageName else selectedPackages + app.packageName
                        } else {
                            selectedApp = app
                        }
                    }
                )
            }
            if (showDisabledApps) {
                items(items = filteredDeletedApps, key = { "deleted_" + it.packageName }) { app ->
                    DeletedAppItem(app = app, onClick = { selectedDeletedApp = app })
                }
            }
        }

        // Action bar saat selection mode
        if (selectionMode && !showDisabledApps) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val selectedApps = filteredInstalledApps.filter { it.packageName in selectedPackages }
                        if (selectedApps.isNotEmpty()) {
                            PackageManagerAction.disableMultiple(context, selectedApps, accessMethod) { successCount, _ ->
                                selectionMode = false
                                selectedPackages = emptySet()
                                if (successCount == selectedApps.size) {
                                    selectedApps.forEach { onAppDisabled(it.packageName) }
                                    displayApps = AppListStorage.loadDisplayApps(context)
                                }
                            }
                        }
                    },
                    enabled = selectedPackages.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Nonaktifkan (${selectedPackages.size})")
                }

                Button(
                    onClick = {
                        val selectedApps = filteredInstalledApps.filter { it.packageName in selectedPackages }
                        if (selectedApps.isNotEmpty()) {
                            PackageManagerAction.uninstallMultiple(context, selectedApps, accessMethod) { successCount, _ ->
                                selectionMode = false
                                selectedPackages = emptySet()
                                if (successCount == selectedApps.size) {
                                    selectedApps.forEach { onAppUninstalled(it.packageName) }
                                    displayApps = AppListStorage.loadDisplayApps(context)
                                }
                            }
                        }
                    },
                    enabled = selectedPackages.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Hapus (${selectedPackages.size})")
                }
            }
        }
    }

    // Pop-up Dialog Tentang Purify & Lisensi
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Purify",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v$appVersionName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pengelola aplikasi dan paket Android.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    HorizontalDivider(thickness = 0.5.dp)

                    Text(
                        text = "Fitur Utama",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val features = listOf(
                        "Melihat aplikasi pengguna dan aplikasi sistem secara terpisah.",
                        "Mengaktifkan atau menonaktifkan aplikasi sistem/pengguna.",
                        "Menghapus paket dan memulihkan paket yang tercatat sebagai terhapus.",
                        "Melihat detail aplikasi lengkap (versi, SDK, ukuran penyimpanan, pemasang, dan izin).",
                        "Mengelola beberapa aplikasi sekaligus (pemberhentian/penghapusan massal)."
                    )

                    features.forEach { feature ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    Text(
                        text = "Pengembang & Hak Cipta",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider(thickness = 0.5.dp)

                    Text(
                        text = "Lisensi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MIT License\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Detail Pop-up untuk Aplikasi Terpasang
    selectedApp?.let { app ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).pointerInput(Unit) {
                detectTapGestures(onTap = { selectedApp = null })
            }
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.90f)
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) },
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                    Text(text = app.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (app.isEnabled) "Status: AKTIF" else "Status: NONAKTIF",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (app.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
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
                        TextButton(onClick = { selectedApp = null }) {
                            Text("Tutup")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val isEnabling = !app.isEnabled
                                changePackageState(context, app.packageName, accessMethod, isEnabling) { success ->
                                    if (success) {
                                        selectedApp = null
                                        if (isEnabling) {
                                            val updatedApps = apps.map { if (it.packageName == app.packageName) it.copy(isEnabled = true) else it }
                                            AppListStorage.saveApps(context, updatedApps)
                                        } else {
                                            onAppDisabled(app.packageName)
                                            val updatedApps = apps.map { if (it.packageName == app.packageName) it.copy(isEnabled = false) else it }
                                            AppListStorage.saveApps(context, updatedApps)
                                        }
                                        displayApps = AppListStorage.loadDisplayApps(context)
                                    }
                                }
                            }
                        ) {
                            Text(text = if (app.isEnabled) "Nonaktifkan" else "Aktifkan")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                PackageManagerAction.uninstall(context, app, accessMethod) { success ->
                                    if (success) {
                                        selectedApp = null
                                        onAppUninstalled(app.packageName)
                                        displayApps = AppListStorage.loadDisplayApps(context)
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

    // Detail Pop-up untuk Aplikasi Terhapus
    selectedDeletedApp?.let { app ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).pointerInput(Unit) {
                detectTapGestures(onTap = { selectedDeletedApp = null })
            }
        ) {
            Surface(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.90f), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                    Text(text = app.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Status: TERHAPUS (User 0)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(14.dp))

                    AppInfoRow(title = "Nama Paket", value = app.packageName)
                    AppInfoRow(title = "Versi", value = "${app.versionName} (${app.versionCode})")
                    AppInfoRow(title = "Ukuran APK", value = formatFileSize(app.apkSize))
                    AppInfoRow(title = "Lokasi APK", value = app.apkPath)
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedDeletedApp = null }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                PackageManagerAction.restore(context, app, accessMethod) { success ->
                                    if (success) {
                                        selectedDeletedApp = null
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
}

@Composable
private fun AppItem(
    app: AppInfo,
    disabled: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cachedBitmap = remember<androidx.compose.ui.graphics.ImageBitmap?>(app.packageName) {
                try {
                    val drawable = app.icon
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap.asImageBitmap()
                } catch (_: Exception) { null }
            }

            if (cachedBitmap != null) {
                Image(
                    bitmap = cachedBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() }
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (disabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            else if (app.isSystemApp) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = if (disabled) "NONAKTIF" else if (app.isSystemApp) "SYSTEM" else "USER",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (disabled) MaterialTheme.colorScheme.error
                                else if (app.isSystemApp) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeletedAppItem(app: DeletedAppInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                Text(
                    text = "TERHAPUS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun changePackageState(
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
        val actionCommand = if (enable) "enable" else "disable-user"
        val fullCommand = "pm $actionCommand --user 0 $packageName"

        try {
            when (accessMethod) {
                AccessMethod.ADB -> {
                    if (!Shizuku.pingBinder()) {
                        error = "Shizuku tidak aktif"
                    } else if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        error = "Izin Shizuku belum diberikan"
                    } else {
                        val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                        val newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                        newProcessMethod.isAccessible = true
                        val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", fullCommand), null, null) as rikka.shizuku.ShizukuRemoteProcess
                        output = process.inputStream.bufferedReader().use { it.readText() }
                        error = process.errorStream.bufferedReader().use { it.readText() }
                        process.waitFor()
                        success = process.exitValue() == 0
                        process.destroy()
                    }
                }
                AccessMethod.ROOT -> {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", fullCommand))
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
            val successMessage = if (enable) "Aplikasi berhasil diaktifkan" else "Aplikasi berhasil dinonaktifkan"
            val failMessage = if (enable) "Gagal mengaktifkan aplikasi" else "Gagal mematikan aplikasi"
            val message = if (success) successMessage else error.trim().ifEmpty { output.trim() }.ifEmpty { failMessage }
            Toast.makeText(context, message.take(300), Toast.LENGTH_LONG).show()
            onResult(success)
        }
    }.start()
}

@Composable
private fun AppInfoRow(title: String, value: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText(title, value))
            Toast.makeText(context, "$title disalin", Toast.LENGTH_SHORT).show()
        }.padding(vertical = 4.dp)
    ) {
        Text(text = "$title:", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(text = value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatFileSize(size: Long): String {
    if (size < 1024L) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}