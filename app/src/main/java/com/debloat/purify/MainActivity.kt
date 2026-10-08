package com.debloat.purify

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    var usageAccessGranted by mutableStateOf(false)
        private set

    var darkTheme by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usageAccessGranted = hasUsageAccess(this)
        val prefs = getSharedPreferences("purify_preferences", Context.MODE_PRIVATE)
        darkTheme = prefs.getBoolean("dark_theme", false)

        setContent {
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                PurifyApp(
                    usageAccessGranted = usageAccessGranted,
                    darkTheme = darkTheme,
                    onThemeChanged = { enabled ->
                        darkTheme = enabled
                        prefs.edit().putBoolean("dark_theme", enabled).apply()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageAccessGranted = hasUsageAccess(this)
    }
}

@Composable
fun PurifyApp(
    usageAccessGranted: Boolean,
    darkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("purify_preferences", Context.MODE_PRIVATE) }

    var selectedMethod by remember {
        mutableStateOf(
            preferences.getString("access_method", null)?.let { value ->
                runCatching { AccessMethod.valueOf(value) }.getOrNull()
            }
        )
    }

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var restoreRefreshKey by remember { mutableStateOf(0) }
    var showUsageAccessDialog by remember { mutableStateOf(!usageAccessGranted) }

    var showShizukuDialog by remember { mutableStateOf(false) }
    var shizukuPermissionGranted by remember {
        mutableStateOf(
            try { ShizukuManager.hasPermission() } catch (_: Throwable) { false }
        )
    }
    var shizukuDialogMessage by remember { mutableStateOf("") }
    val currentSelectedMethod by rememberUpdatedState(selectedMethod)

    fun updateAppDisabled(packageName: String) {
        apps = apps.map { app -> if (app.packageName == packageName) app.copy(isEnabled = false) else app }
        AppListStorage.saveApps(context = context, apps = apps)
    }

    fun removeAppLocally(packageName: String) {
        apps = apps.filterNot { app -> app.packageName == packageName }
        AppListStorage.saveApps(context = context, apps = apps)
    }

    // Listener otomatis dari Shizuku, gak perlu lagi pake while(true) nguras baterai
    DisposableEffect(Unit) {
        val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuManager.REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    shizukuPermissionGranted = true
                    showShizukuDialog = false
                } else {
                    shizukuPermissionGranted = false
                    shizukuDialogMessage = "Izin Shizuku ditolak. Purify tidak dapat menggunakan akses ADB."
                    showShizukuDialog = true
                }
            }
        }

        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            shizukuPermissionGranted = try { ShizukuManager.hasPermission() } catch (_: Throwable) { false }
            if (currentSelectedMethod == AccessMethod.ADB && shizukuPermissionGranted) {
                showShizukuDialog = false
            }
        }

        val binderDeadListener = Shizuku.OnBinderDeadListener {
            shizukuPermissionGranted = false
            if (currentSelectedMethod == AccessMethod.ADB) {
                apps = emptyList()
                shizukuDialogMessage = "Shizuku berhenti. Purify membutuhkan Shizuku untuk menggunakan akses ADB."
                showShizukuDialog = true
            }
        }

        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (_: Throwable) {}

        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(permissionListener)
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(usageAccessGranted) { showUsageAccessDialog = !usageAccessGranted }

    LaunchedEffect(shizukuPermissionGranted) {
        if (selectedMethod != AccessMethod.ADB || !shizukuPermissionGranted) return@LaunchedEffect
        val cachedApps = withContext(Dispatchers.IO) { AppListStorage.loadApps(context) }
        if (cachedApps.isNotEmpty()) apps = cachedApps
    }

    var scanStatusIndex by remember { mutableStateOf(0) }
    val scanStatuses = listOf(
        "Mencari aplikasi terpasang",
        "Membaca informasi aplikasi",
        "Menganalisis izin aplikasi",
        "Menghitung penggunaan penyimpanan",
        "Menyiapkan daftar aplikasi"
    )

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanStatusIndex = 0
            while (true) {
                delay(1400)
                scanStatusIndex = (scanStatusIndex + 1) % scanStatuses.size
            }
        }
    }

    LaunchedEffect(usageAccessGranted) {
        if (!usageAccessGranted) return@LaunchedEffect
        val cachedApps = withContext(Dispatchers.IO) { AppListStorage.loadApps(context) }
        if (cachedApps.isNotEmpty()) apps = cachedApps
    }

    LaunchedEffect(selectedMethod, usageAccessGranted, restoreRefreshKey) {
        if (!usageAccessGranted || selectedMethod == null) return@LaunchedEffect

        if (selectedMethod == AccessMethod.ADB) {
            if (!ShizukuManager.isRunning()) {
                shizukuPermissionGranted = false
                shizukuDialogMessage = "Shizuku belum berjalan. Jalankan Shizuku terlebih dahulu, lalu pilih kembali metode ADB."
                showShizukuDialog = true
                return@LaunchedEffect
            }
            if (!ShizukuManager.hasPermission()) {
                shizukuPermissionGranted = false
                shizukuDialogMessage = "Purify membutuhkan izin Shizuku untuk menggunakan akses ADB."
                showShizukuDialog = true
                return@LaunchedEffect
            }
            shizukuPermissionGranted = true
        }

        if (AppListStorage.hasCache(context) && restoreRefreshKey == 0) return@LaunchedEffect

        isScanning = true
        val scannedApps = withContext(Dispatchers.IO) { scanApplications(context) }
        withContext(Dispatchers.IO) { AppListStorage.saveApps(context, scannedApps) }
        apps = scannedApps
        isScanning = false
        if (restoreRefreshKey != 0) restoreRefreshKey = 0
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !usageAccessGranted -> Box(modifier = Modifier.fillMaxSize())
            selectedMethod == null -> {
                SetupScreen(
                    onMethodSelected = { method ->
                        preferences.edit().putString("access_method", method.name).apply()
                        selectedMethod = method
                    }
                )
            }
            isScanning -> ScanningScreen(status = scanStatuses[scanStatusIndex], accessMethod = selectedMethod)
            apps.isNotEmpty() -> {
                val currentMethod = selectedMethod ?: AccessMethod.ADB
                AppListScreen(
                    apps = apps,
                    accessMethod = currentMethod,
                    darkTheme = darkTheme,
                    onThemeChanged = onThemeChanged,
                    onAppDisabled = { packageName -> updateAppDisabled(packageName) },
                    onAppUninstalled = { packageName -> removeAppLocally(packageName) },
                    onAppRestored = { restoreRefreshKey++ },
                    onSettingsClick = { selectedMethod = null }
                )
            }
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    if (showUsageAccessDialog && !usageAccessGranted) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(imageVector = Icons.Default.Security, contentDescription = null) },
            title = { Text(text = "Izinkan Akses Penggunaan") },
            text = { Text(text = "Purify membutuhkan akses penggunaan untuk membaca informasi aplikasi dan penyimpanan dengan lebih lengkap.") },
            confirmButton = {
                TextButton(onClick = {
                    try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } 
                    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }) { Text(text = "Buka Pengaturan") }
            }
        )
    }

    if (showShizukuDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(imageVector = Icons.Default.Security, contentDescription = null) },
            title = { Text(text = "Shizuku diperlukan") },
            text = { Text(text = shizukuDialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    if (ShizukuManager.isRunning()) {
                        if (!ShizukuManager.hasPermission()) ShizukuManager.requestPermission()
                        else {
                            shizukuPermissionGranted = true
                            showShizukuDialog = false
                        }
                    } else {
                        try {
                            context.startActivity(context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api") ?: Intent(Settings.ACTION_SETTINGS))
                        } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                }) { Text(text = if (ShizukuManager.isRunning()) "Izinkan" else "Buka Shizuku") }
            },
            dismissButton = {
                TextButton(onClick = { showShizukuDialog = false; selectedMethod = null }) { Text(text = "Batal") }
            }
        )
    }
}

@Composable
private fun ScanningScreen(status: String, accessMethod: AccessMethod?) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Reverse), label = "scan_scale"
    )
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Reverse), label = "scan_alpha"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(96.dp).graphicsLayer { scaleX = scale; scaleY = scale }, strokeWidth = 5.dp)
                Icon(
                    imageVector = Icons.Default.Security, contentDescription = null,
                    modifier = Modifier.size(40.dp).graphicsLayer { alpha = iconAlpha }, tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "Memindai aplikasi", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = status, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Metode akses: ${accessMethod?.name}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Purify sedang bekerja...", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

// FUNGSI SUPER CEPAT BUAT SCAN
private fun scanApplications(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    // Ambil full package info sekaligus, kaga pake ngeloop manggil getPackageInfo lagi
    val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

    return installedPackages.mapNotNull { packageInfo ->
        try {
            val applicationInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            
            val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val grantedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestedPermissions.filter { permission ->
                    packageManager.checkPermission(permission, applicationInfo.packageName) == PackageManager.PERMISSION_GRANTED
                }
            } else {
                requestedPermissions.filterIndexed { index, _ ->
                    val flags = packageInfo.requestedPermissionsFlags
                    flags != null && index < flags.size && (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                }
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else {
                @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
            }

            val installer = try {
                val installerPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    packageManager.getInstallSourceInfo(applicationInfo.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION") packageManager.getInstallerPackageName(applicationInfo.packageName)
                }

                if (installerPackage.isNullOrBlank()) "Tidak diketahui"
                else try {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(installerPackage, 0)).toString()
                } catch (_: Exception) { installerPackage }
            } catch (_: Exception) { "Tidak diketahui" }

            val sourceDir = applicationInfo.sourceDir ?: return@mapNotNull null
            val apkFile = File(sourceDir)
            val apkSize = if (apkFile.exists()) apkFile.length() else 0L

            var dataSize = 0L
            var cacheSize = 0L
            var totalSize = 0L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val storageStatsManager = context.getSystemService(StorageStatsManager::class.java)
                    val storageManager = context.getSystemService(StorageManager::class.java)
                    val uuid = storageManager.getUuidForPath(apkFile)
                    val stats = storageStatsManager.queryStatsForPackage(uuid, applicationInfo.packageName, android.os.Process.myUserHandle())

                    dataSize = stats.dataBytes
                    cacheSize = stats.cacheBytes
                    totalSize = stats.appBytes + stats.dataBytes + stats.cacheBytes
                } catch (_: Exception) { totalSize = apkSize }
            } else {
                totalSize = apkSize
            }

            AppInfo(
                packageName = applicationInfo.packageName,
                appName = packageManager.getApplicationLabel(applicationInfo).toString(),
                isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isEnabled = applicationInfo.enabled,
                icon = packageManager.getApplicationIcon(applicationInfo),
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = versionCode,
                targetSdk = applicationInfo.targetSdkVersion,
                minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) applicationInfo.minSdkVersion else 0,
                uid = applicationInfo.uid,
                apkPath = sourceDir,
                apkSize = apkSize,
                dataSize = dataSize,
                cacheSize = cacheSize,
                totalSize = totalSize,
                requestedPermissions = requestedPermissions,
                grantedPermissions = grantedPermissions,
                installer = installer
            )
        } catch (_: Throwable) { null }
    }.sortedBy { it.appName.lowercase() }
}
