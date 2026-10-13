/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

package com.debloat.purify

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    accessMethod: AccessMethod,
    themeMode: String,
    onThemeChanged: (String) -> Unit,
    onAppDisabled: (String) -> Unit,
    onAppUninstalled: (String) -> Unit,
    onAppRestored: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit = onAppRestored
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("purify_preferences", Context.MODE_PRIVATE) }

    // State Keburaman & Ketebalan Kaca untuk Liquid Glass
    var bgBlurDp by remember { mutableFloatStateOf(prefs.getFloat("glass_bg_blur", 28f)) }
    var cardAlpha by remember { mutableFloatStateOf(prefs.getFloat("glass_card_alpha", 0.40f)) }
    var showBgInOtherThemes by remember { mutableStateOf(prefs.getBoolean("show_bg_in_other_themes", false)) }

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

    // Gambar Latar Belakang Galeri
    var customBgBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    fun refreshCustomBackground() {
        try {
            val bgFile = File(context.filesDir, "custom_background.jpg")
            if (bgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
                customBgBitmap = bitmap?.asImageBitmap()
            } else {
                customBgBitmap = null
            }
        } catch (_: Exception) {
            customBgBitmap = null
        }
    }

    LaunchedEffect(Unit) {
        refreshCustomBackground()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    File(context.filesDir, "custom_background.jpg").outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                refreshCustomBackground()
                Toast.makeText(context, "Gambar latar berhasil dipasang", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedDeletedApp by remember { mutableStateOf<DeletedAppInfo?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showSettings by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDisabledApps by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler(enabled = showAboutDialog) { showAboutDialog = false }
    BackHandler(enabled = showThemeDialog) { showThemeDialog = false }

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

    val installedDisplayApps = remember(displayApps) { displayApps.filter { it.appInfo != null } }
    val activeApps = remember(installedDisplayApps) { installedDisplayApps.mapNotNull { it.appInfo }.filter { it.isEnabled } }
    val disabledApps = remember(installedDisplayApps) { installedDisplayApps.mapNotNull { it.appInfo }.filter { !it.isEnabled } }
    val deletedApps = remember(displayApps) { displayApps.filter { it.status == AppStatus.DELETED }.mapNotNull { it.deletedAppInfo } }
    val currentInstalledApps = if (showDisabledApps) disabledApps else activeApps

    val filteredInstalledApps = remember(currentInstalledApps, selectedFilter, searchQuery) {
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

    val allVisibleSelected = remember(filteredInstalledApps, selectedPackages) {
        filteredInstalledApps.isNotEmpty() && filteredInstalledApps.all { it.packageName in selectedPackages }
    }

    val isGlass = themeMode == "GLASS"

    val glassRimBorder = BorderStroke(
        width = 1.2.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    )

    val dialogContainerColor = if (isGlass) Color(0xF2161622) else MaterialTheme.colorScheme.surfaceContainerHigh
    val dialogGlassBorder = if (isGlass) glassRimBorder else null

    val shouldShowCustomBg = customBgBitmap != null && (isGlass || showBgInOtherThemes)

    Box(modifier = Modifier.fillMaxSize()) {
        if (shouldShowCustomBg) {
            val bgModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isGlass && bgBlurDp > 0f) {
                Modifier.fillMaxSize().blur(bgBlurDp.dp)
            } else {
                Modifier.fillMaxSize()
            }
            Image(
                bitmap = customBgBitmap!!,
                contentDescription = null,
                modifier = bgModifier,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isGlass) Color(0x66080810)
                        else if (themeMode == "DARK") Color.Black.copy(alpha = 0.65f)
                        else Color.White.copy(alpha = 0.65f)
                    )
            )
        } else if (isGlass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF181825),
                                Color(0xFF0F0F17)
                            )
                        )
                    )
            )
        }

        // Tampilan Utama UI Aplikasi
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

                    // Tombol Refresh/Memuat Ulang
                    IconButton(
                        onClick = {
                            displayApps = AppListStorage.loadDisplayApps(context)
                            onRefreshClick()
                            Toast.makeText(context, "Memperbarui daftar aplikasi...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang / Refresh"
                        )
                    }

                    // Tombol Tandai Massal
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
                            modifier = Modifier
                                .width(230.dp)
                                .then(
                                    if (isGlass) {
                                        Modifier
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0xF02A2A3C), Color(0xF0181826))
                                                ),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .border(glassRimBorder, RoundedCornerShape(20.dp))
                                    } else Modifier
                                ),
                            containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
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
                            HorizontalDivider(thickness = 0.5.dp, color = if (isGlass) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Tema Tampilan", style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = when (themeMode) {
                                                "GLASS" -> "Liquid Glass ✨"
                                                "DARK" -> "Tampilan Gelap"
                                                else -> "Tampilan Terang"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    showSettings = false
                                    showThemeDialog = true
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = if (isGlass) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant)
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

            // Search Bar Bergaya Kapsul Kaca
            val searchShape = RoundedCornerShape(28.dp)
            val searchModifier = if (isGlass) {
                Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF323248).copy(alpha = (cardAlpha * 1.2f).coerceAtMost(0.9f)),
                                Color(0xFF1B1B2A).copy(alpha = cardAlpha)
                            )
                        ),
                        searchShape
                    )
                    .border(glassRimBorder, searchShape)
            } else Modifier

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .then(searchModifier),
                shape = searchShape,
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
                    unfocusedContainerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    focusedContainerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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

            // List Utama Aplikasi
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = filteredInstalledApps,
                    key = { it.packageName },
                    contentType = { "installed_app" }
                ) { app ->
                    AppItem(
                        app = app,
                        disabled = !app.isEnabled,
                        selected = app.packageName in selectedPackages,
                        selectionMode = selectionMode,
                        isGlass = isGlass,
                        cardAlpha = cardAlpha,
                        glassRimBorder = glassRimBorder,
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
                    items(
                        items = filteredDeletedApps,
                        key = { "deleted_" + it.packageName },
                        contentType = { "deleted_app" }
                    ) { app ->
                        DeletedAppItem(
                            app = app,
                            isGlass = isGlass,
                            cardAlpha = cardAlpha,
                            glassRimBorder = glassRimBorder,
                            onClick = { selectedDeletedApp = app }
                        )
                    }
                }
            }

            // Action bar saat selection mode
            if (selectionMode && !showDisabledApps) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tombol penonaktifkan massal HANYA ada di mode ROOT
                    if (accessMethod == AccessMethod.ROOT) {
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(text = "Hapus (${selectedPackages.size})")
                    }
                }
            }
        }
    }

    // Dialog Pengaturan Tema
    if (showThemeDialog) {
        ThemeSettingsDialog(
            themeMode = themeMode,
            isGlass = isGlass,
            bgBlurDp = bgBlurDp,
            onBgBlurChange = {
                bgBlurDp = it
                prefs.edit().putFloat("glass_bg_blur", it).apply()
            },
            cardAlpha = cardAlpha,
            onCardAlphaChange = {
                cardAlpha = it
                prefs.edit().putFloat("glass_card_alpha", it).apply()
            },
            hasCustomBg = customBgBitmap != null,
            showBgInOtherThemes = showBgInOtherThemes,
            onShowBgInOtherThemesChange = {
                showBgInOtherThemes = it
                prefs.edit().putBoolean("show_bg_in_other_themes", it).apply()
            },
            onSelectGalleryImage = { galleryLauncher.launch("image/*") },
            onRemoveGalleryImage = {
                try {
                    val bgFile = File(context.filesDir, "custom_background.jpg")
                    if (bgFile.exists()) bgFile.delete()
                    refreshCustomBackground()
                    Toast.makeText(context, "Gambar latar dihapus", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            },
            onThemeChanged = onThemeChanged,
            onDismiss = { showThemeDialog = false },
            dialogContainerColor = dialogContainerColor,
            dialogGlassBorder = dialogGlassBorder
        )
    }

    // Dialog Tentang Purify
    if (showAboutDialog) {
        AboutDialog(
            appVersionName = appVersionName,
            onDismiss = { showAboutDialog = false },
            dialogContainerColor = dialogContainerColor,
            dialogGlassBorder = dialogGlassBorder
        )
    }

    // Detail Pop-up Aplikasi Terpasang
    selectedApp?.let { app ->
        AppDetailDialog(
            app = app,
            allApps = apps,
            accessMethod = accessMethod,
            isGlass = isGlass,
            glassRimBorder = glassRimBorder,
            onDismiss = { selectedApp = null },
            onAppDisabled = onAppDisabled,
            onAppUninstalled = onAppUninstalled,
            onAppListStorageUpdate = { displayApps = AppListStorage.loadDisplayApps(context) }
        )
    }

    // Detail Pop-up Aplikasi Terhapus
    selectedDeletedApp?.let { app ->
        DeletedAppDetailDialog(
            app = app,
            accessMethod = accessMethod,
            isGlass = isGlass,
            glassRimBorder = glassRimBorder,
            onDismiss = { selectedDeletedApp = null },
            onAppRestored = {
                selectedDeletedApp = null
                onAppRestored()
            }
        )
    }
}