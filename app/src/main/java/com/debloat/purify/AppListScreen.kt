package com.debloat.purify

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import rikka.shizuku.Shizuku

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

    var displayApps by remember { mutableStateOf(emptyList<DisplayAppInfo>()) }

    LaunchedEffect(apps) {
        displayApps = AppListStorage.loadDisplayApps(context)
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedDeletedApp by remember { mutableStateOf<DeletedAppInfo?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") } // Tetap ALL buat logic internal
    var showSettings by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDisabledApps by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedPackages = emptySet()
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

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

    val filteredInstalledApps = currentInstalledApps
        .filter { app ->
            when (selectedFilter) {
                "USER" -> !app.isSystemApp
                "SYSTEM" -> app.isSystemApp
                else -> true
            }
        }
        .filter { app ->
            if (showDisabledApps || searchQuery.isBlank()) {
                true
            } else {
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

    val filteredDeletedApps = deletedApps.filter { app ->
        when (selectedFilter) {
            "USER" -> !app.isSystemApp
            "SYSTEM" -> app.isSystemApp
            else -> true
        }
    }

    val allVisibleSelected = filteredInstalledApps.isNotEmpty() && filteredInstalledApps.all { it.packageName in selectedPackages }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // ================= HEADER =================
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                IconButton(onClick = { selectionMode = false; selectedPackages = emptySet() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Batal memilih")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pilih Aplikasi", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${selectedPackages.size} dipilih", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(
                    onClick = {
                        if (allVisibleSelected) {
                            selectedPackages = selectedPackages - filteredInstalledApps.map { it.packageName }.toSet()
                        } else {
                            selectedPackages = selectedPackages + filteredInstalledApps.map { it.packageName }.toSet()
                        }
                    }
                ) {
                    Text(text = if (allVisibleSelected) "Batal Semua" else "Pilih Semua")
                }
            } else if (isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(text = "Cari aplikasi...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Pencarian") },
                    trailingIcon = {
                        IconButton(onClick = { searchQuery = ""; isSearching = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup pencarian")
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = if (showDisabledApps) "Aplikasi Nonaktif" else "Aplikasi", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (showDisabledApps) {
                        Text(text = "Total: ${disabledApps.size + deletedApps.size}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            text = "Total: ${activeApps.size} | Pengguna: ${activeApps.count { !it.isSystemApp }} | Sistem: ${activeApps.count { it.isSystemApp }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                IconButton(
 				   onClick = { 
   						     selectionMode = true
   						     selectedPackages = emptySet() 
  				  }
				) {
 				   Icon(
   				     imageVector = Icons.Default.Checklist,
 				       contentDescription = "Mode Tandai"
  				  )
				}


                IconButton(onClick = { isSearching = true }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Cari aplikasi")
                }
                
                Box {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Pengaturan")
                    }

                    DropdownMenu(
                        expanded = showSettings,
                        onDismissRequest = { showSettings = false },
                        modifier = Modifier.width(220.dp)
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
                                Column {
                                    Text("Pindai aplikasi", style = MaterialTheme.typography.bodyLarge)
                                    Text("Perbarui daftar manual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { 
                                showSettings = false
                                Toast.makeText(context, "Fungsi pindai aplikasi diklik!", Toast.LENGTH_SHORT).show()
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
                                            Toast.makeText(context, if (enabled) "Tema gelap aktif" else "Tema terang aktif", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            },
                            onClick = { 
                                val newState = !darkTheme
                                onThemeChanged(newState)
                                Toast.makeText(context, if (newState) "Tema gelap aktif" else "Tema terang aktif", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // ================= TAB AKTIF & NONAKTIF =================
        if (!selectionMode && !isSearching) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!showDisabledApps) {
                    Button(onClick = { }, modifier = Modifier.weight(1f)) {
                        Text("Aktif (${activeApps.size})")
                    }
                } else {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showDisabledApps = false; selectedFilter = "ALL"; searchQuery = "" }, 
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Aktif (${activeApps.size})")
                    }
                }

                if (showDisabledApps) {
                    Button(onClick = { }, modifier = Modifier.weight(1f)) {
                        Text("Nonaktif/Terhapus (${disabledApps.size + deletedApps.size})")
                    }
                } else {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showDisabledApps = true; selectedFilter = "ALL"; searchQuery = "" }, 
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nonaktif/Terhapus (${disabledApps.size + deletedApps.size})")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedFilter == "ALL", onClick = { selectedFilter = "ALL" }, label = { Text("Semua") })
            FilterChip(selected = selectedFilter == "USER", onClick = { selectedFilter = "USER" }, label = { Text("Pengguna") })
            FilterChip(selected = selectedFilter == "SYSTEM", onClick = { selectedFilter = "SYSTEM" }, label = { Text("Sistem") })
        }

        if (!showDisabledApps && searchQuery.isNotBlank()) {
            Text(text = "${filteredInstalledApps.size} aplikasi ditemukan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        if (selectionMode && !showDisabledApps) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val selectedApps = filteredInstalledApps.filter { it.packageName in selectedPackages }
                        if (selectedApps.isEmpty()) {
                            Toast.makeText(context, "Belum ada aplikasi yang dipilih", Toast.LENGTH_SHORT).show()
                        } else {
                            PackageManagerAction.disableMultiple(context = context, apps = selectedApps, accessMethod = accessMethod) { successCount, _ ->
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
                        if (selectedApps.isEmpty()) {
                            Toast.makeText(context, "Belum ada aplikasi yang dipilih", Toast.LENGTH_SHORT).show()
                        } else {
                            PackageManagerAction.uninstallMultiple(context = context, apps = selectedApps, accessMethod = accessMethod) { successCount, _ ->
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

    selectedApp?.let { app ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).pointerInput(Unit) {
                detectTapGestures(onTap = { selectedApp = null })
            }
        ) {
            Box(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.90f).pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                }
            ) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Text(text = app.appName, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = if (app.isEnabled) "AKTIF" else "NONAKTIF", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        AppInfoRow(title = "Nama Paket", value = app.packageName)
                        AppInfoRow(title = "Versi", value = "${app.versionName} (${app.versionCode})")
                        AppInfoRow(title = "Target SDK", value = app.targetSdk.toString())
                        AppInfoRow(title = "Min SDK", value = app.minSdk.toString())
                        AppInfoRow(title = "UID", value = app.uid.toString())
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Penyimpanan", style = MaterialTheme.typography.titleMedium)
                        AppInfoRow(title = "Aplikasi", value = formatFileSize(app.apkSize))
                        AppInfoRow(title = "Data", value = formatFileSize(app.dataSize))
                        AppInfoRow(title = "Cache", value = formatFileSize(app.cacheSize))
                        AppInfoRow(title = "Total", value = formatFileSize(app.totalSize))
                        Spacer(modifier = Modifier.height(8.dp))

                        AppInfoRow(title = "Pemasang", value = app.installer)
                        if (accessMethod == AccessMethod.ROOT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Lokasi APK", style = MaterialTheme.typography.labelMedium)
                            Text(text = app.apkPath, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Izin Aplikasi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (app.requestedPermissions.isEmpty()) {
                            Text(text = "Tidak ada izin yang diminta", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(app.requestedPermissions) { permission ->
                                        val granted = permission in app.grantedPermissions
                                        val symbol = if (granted) "✓" else "○"
                                        Text(text = "$symbol ${permission.substringAfterLast('.')}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!app.isEnabled) {
                                Button(
                                    onClick = {
                                        enablePackage(context = context, packageName = app.packageName, accessMethod = accessMethod) { success ->
                                            if (success) {
                                                selectedApp = null
                                                val updatedApps = apps.map { currentApp ->
                                                    if (currentApp.packageName == app.packageName) {
                                                        currentApp.copy(isEnabled = true)
                                                    } else {
                                                        currentApp
                                                    }
                                                }
                                                AppListStorage.saveApps(context = context, apps = updatedApps)
                                                displayApps = AppListStorage.loadDisplayApps(context)
                                            }
                                        }
                                    }
                                ) {
                                    Text(text = "Aktifkan")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Button(
                                    onClick = {
                                        disablePackage(context = context, packageName = app.packageName, accessMethod = accessMethod) { success ->
                                            if (success) {
                                                selectedApp = null
                                                onAppDisabled(app.packageName)
                                                val updatedApps = apps.map { currentApp ->
                                                    if (currentApp.packageName == app.packageName) {
                                                        currentApp.copy(isEnabled = false)
                                                    } else {
                                                        currentApp
                                                    }
                                                }
                                                AppListStorage.saveApps(context = context, apps = updatedApps)
                                                displayApps = AppListStorage.loadDisplayApps(context)
                                            }
                                        }
                                    }
                                ) {
                                    Text(text = "Nonaktifkan")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Button(
                                onClick = {
                                    PackageManagerAction.uninstall(context = context, app = app, accessMethod = accessMethod) { success ->
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

                Button(
                    onClick = { selectedApp = null },
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 64.dp)
                ) {
                    Text(text = "×")
                }
            }
        }
    }

    selectedDeletedApp?.let { app ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).pointerInput(Unit) {
                detectTapGestures(onTap = { selectedDeletedApp = null })
            }
        ) {
            Surface(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.90f), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(text = app.appName, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "TERHAPUS", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    AppInfoRow(title = "Nama Paket", value = app.packageName)
                    AppInfoRow(title = "Versi", value = "${app.versionName} (${app.versionCode})")
                    AppInfoRow(title = "APK", value = formatFileSize(app.apkSize))
                    AppInfoRow(title = "Lokasi APK", value = app.apkPath)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Aplikasi ini sudah dihapus untuk pengguna 0.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                PackageManagerAction.restore(context = context, app = app, accessMethod = accessMethod) { success ->
                                    if (success) {
                                        selectedDeletedApp = null
                                        onAppRestored()
                                    }
                                }
                            }
                        ) {
                            Text(text = "Pulihkan")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { selectedDeletedApp = null }) {
                            Text(text = "Tutup")
                        }
                    }
                }
            }
        }
    }
}

private fun enablePackage(context: Context, packageName: String, accessMethod: AccessMethod, onResult: (Boolean) -> Unit) {
    Thread {
        var success = false
        var output = ""
        var error = ""
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
                        val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "pm enable --user 0 $packageName"), null, null) as rikka.shizuku.ShizukuRemoteProcess
                        output = process.inputStream.bufferedReader().use { it.readText() }
                        error = process.errorStream.bufferedReader().use { it.readText() }
                        process.waitFor()
                        success = process.exitValue() == 0
                        process.destroy()
                    }
                }
                AccessMethod.ROOT -> {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm enable --user 0 $packageName"))
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
            val message = if (success) "Aplikasi berhasil diaktifkan" else error.trim().ifEmpty { output.trim() }.ifEmpty { "Gagal mengaktifkan aplikasi" }
            Toast.makeText(context, message.take(300), Toast.LENGTH_LONG).show()
            onResult(success)
        }
    }.start()
}

private fun disablePackage(context: Context, packageName: String, accessMethod: AccessMethod, onResult: (Boolean) -> Unit) {
    Thread {
        var success = false
        var output = ""
        var error = ""
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
                        val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "pm disable-user --user 0 $packageName"), null, null) as rikka.shizuku.ShizukuRemoteProcess
                        output = process.inputStream.bufferedReader().use { it.readText() }
                        error = process.errorStream.bufferedReader().use { it.readText() }
                        process.waitFor()
                        success = process.exitValue() == 0
                        process.destroy()
                    }
                }
                AccessMethod.ROOT -> {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm disable-user --user 0 $packageName"))
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
            val message = if (success) "Aplikasi berhasil dinonaktifkan" else error.trim().ifEmpty { output.trim() }.ifEmpty { "Gagal mematikan aplikasi" }
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
        }.padding(vertical = 5.dp)
    ) {
        Text(text = "$title:", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelMedium)
        Text(text = value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun AppItem(app: AppInfo, disabled: Boolean, selected: Boolean, selectionMode: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AndroidView(
                factory = { context -> ImageView(context) },
                update = { imageView -> imageView.setImageDrawable(app.icon) },
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (app.isSystemApp) "SISTEM" else "PENGGUNA", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (disabled) "NONAKTIF" else "AKTIF", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            }
        }
    }
}

@Composable
private fun DeletedAppItem(app: DeletedAppInfo, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(text = if (app.isSystemApp) "SISTEM" else "PENGGUNA", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "TERHAPUS", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
