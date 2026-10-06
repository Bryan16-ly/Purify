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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
    onAppDisabled: (String) -> Unit,
    onAppUninstalled: (String) -> Unit,
    onSettingsClick: () -> Unit
) {

    val context =
        LocalContext.current

    /*
     * ==========================================
     * DISPLAY DATA
     * ==========================================
     *
     * Tidak lagi hanya menggunakan List<AppInfo>.
     *
     * loadDisplayApps() menggabungkan:
     *
     * 1. aplikasi aktif
     * 2. aplikasi disabled
     * 3. aplikasi yang sudah di-uninstall
     *
     * Jadi halaman Disabled bisa menampilkan
     * aplikasi DELETED juga.
     */

    var displayApps by remember {
        mutableStateOf(
            emptyList<DisplayAppInfo>()
        )
    }

    /*
     * ==========================================
     * LOAD DISPLAY DATA
     * ==========================================
     */

    LaunchedEffect(apps) {

        displayApps =
            AppListStorage.loadDisplayApps(
                context
            )
    }

    /*
     * ==========================================
     * SELECTED APP
     * ==========================================
     */

    var selectedApp by remember {
        mutableStateOf<AppInfo?>(null)
    }

    /*
     * ==========================================
     * SELECTED DELETED APP
     * ==========================================
     */

    var selectedDeletedApp by remember {
        mutableStateOf<DeletedAppInfo?>(null)
    }

    /*
     * ==========================================
     * FILTER
     * ==========================================
     */

    var selectedFilter by remember {
        mutableStateOf("ALL")
    }

    /*
     * ==========================================
     * SETTINGS
     * ==========================================
     */

    var showSettings by remember {
        mutableStateOf(false)
    }

    /*
     * ==========================================
     * SEARCH
     * ==========================================
     *
     * Search hanya digunakan di halaman
     * Applications.
     *
     * Disabled Applications tidak memakai
     * search lagi.
     */

    var isSearching by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    /*
     * ==========================================
     * HALAMAN
     * ==========================================
     *
     * false = Applications
     * true  = Disabled Applications
     */

    var showDisabledApps by remember {
        mutableStateOf(false)
    }

    /*
     * ==========================================
     * MODE TANDAI
     * ==========================================
     */

    var selectionMode by remember {
        mutableStateOf(false)
    }

    var selectedPackages by remember {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }

    /*
     * ==========================================
     * BACK HANDLER
     * ==========================================
     */

    BackHandler(
        enabled = selectionMode
    ) {

        selectionMode = false

        selectedPackages =
            emptySet()
    }

    BackHandler(
        enabled = showSettings
    ) {

        showSettings = false
    }

    BackHandler(
        enabled =
            selectedApp != null ||
                selectedDeletedApp != null
    ) {

        selectedApp = null
        selectedDeletedApp = null
    }

    BackHandler(
        enabled =
            showDisabledApps &&
                selectedApp == null &&
                selectedDeletedApp == null &&
                !showSettings &&
                !selectionMode
    ) {

        showDisabledApps = false

        selectedFilter = "ALL"
    }

    /*
     * ==========================================
     * DATA INSTALLED
     * ==========================================
     */

    val installedDisplayApps =
        displayApps.filter {
            it.appInfo != null
        }

    /*
     * ==========================================
     * ACTIVE APPS
     * ==========================================
     */

    val activeApps =
        installedDisplayApps
            .mapNotNull {
                it.appInfo
            }
            .filter {
                it.isEnabled
            }

    /*
     * ==========================================
     * DISABLED APPS
     * ==========================================
     */

    val disabledApps =
        installedDisplayApps
            .mapNotNull {
                it.appInfo
            }
            .filter {
                !it.isEnabled
            }

    /*
     * ==========================================
     * DELETED APPS
     * ==========================================
     */

    val deletedApps =
        displayApps
            .filter {
                it.status ==
                    AppStatus.DELETED
            }
            .mapNotNull {
                it.deletedAppInfo
            }

    /*
     * ==========================================
     * CURRENT INSTALLED APPS
     * ==========================================
     */

    val currentInstalledApps =
        if (showDisabledApps) {

            disabledApps

        } else {

            activeApps
        }

    /*
     * ==========================================
     * FILTER INSTALLED APPS
     * ==========================================
     */

    val filteredInstalledApps =
        currentInstalledApps
            .filter { app ->

                when (selectedFilter) {

                    "USER" ->
                        !app.isSystemApp

                    "SYSTEM" ->
                        app.isSystemApp

                    else ->
                        true
                }
            }
            .filter { app ->

                /*
                 * Search hanya berlaku
                 * di halaman Applications.
                 */

                if (
                    showDisabledApps ||
                        searchQuery.isBlank()
                ) {

                    true

                } else {

                    app.appName.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                        app.packageName.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                }
            }

    /*
     * ==========================================
     * FILTER DELETED APPS
     * ==========================================
     */

    val filteredDeletedApps =
        deletedApps.filter { app ->

            when (selectedFilter) {

                "USER" ->
                    !app.isSystemApp

                "SYSTEM" ->
                    app.isSystemApp

                else ->
                    true
            }
        }

    /*
     * ==========================================
     * ALL VISIBLE SELECTED
     * ==========================================
     */

    val allVisibleSelected =
        filteredInstalledApps.isNotEmpty() &&
            filteredInstalledApps.all {

                it.packageName in
                    selectedPackages
            }

    /*
     * ==========================================
     * HEADER
     * ==========================================
     */

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * ==================================
             * SELECTION HEADER
             * ==================================
             */

            if (selectionMode) {

                IconButton(

                    onClick = {

                        selectionMode = false

                        selectedPackages =
                            emptySet()
                    }
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.ArrowBack,

                        contentDescription =
                            "Batal memilih"
                    )
                }

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Pilih Aplikasi",

                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(

                        text =
                            "${selectedPackages.size} dipilih",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }

                TextButton(

                    onClick = {

                        if (
                            allVisibleSelected
                        ) {

                            selectedPackages =
                                selectedPackages -
                                    filteredInstalledApps
                                        .map {
                                            it.packageName
                                        }
                                        .toSet()

                        } else {

                            selectedPackages =
                                selectedPackages +
                                    filteredInstalledApps
                                        .map {
                                            it.packageName
                                        }
                                        .toSet()
                        }
                    }
                ) {

                    Text(

                        text =
                            if (
                                allVisibleSelected
                            ) {

                                "Batal Semua"

                            } else {

                                "Pilih Semua"
                            }
                    )
                }

            }

            /*
             * ==================================
             * DISABLED HEADER
             * ==================================
             */

            else if (
                showDisabledApps
            ) {

                IconButton(

                    onClick = {

                        showDisabledApps =
                            false

                        selectedFilter =
                            "ALL"

                        selectedApp =
                            null

                        selectedDeletedApp =
                            null
                    }
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.ArrowBack,

                        contentDescription =
                            "Kembali"
                    )
                }

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Disabled Applications",

                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(

                        text =
                            "Total: ${
                                disabledApps.size +
                                    deletedApps.size
                            }",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }

                /*
                 * TIDAK ADA SEARCH DI SINI.
                 */
            }

            /*
             * ==================================
             * SEARCH MODE
             * ==================================
             */

            else if (
                isSearching
            ) {

                OutlinedTextField(

                    value =
                        searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    singleLine = true,

                    placeholder = {

                        Text(
                            text =
                                "Cari aplikasi..."
                        )
                    },

                    leadingIcon = {

                        Icon(

                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                "Search"
                        )
                    },

                    trailingIcon = {

                        IconButton(

                            onClick = {

                                searchQuery = ""

                                isSearching =
                                    false
                            }
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Close,

                                contentDescription =
                                    "Tutup pencarian"
                            )
                        }
                    }
                )
            }

            /*
             * ==================================
             * NORMAL APPLICATION HEADER
             * ==================================
             */

            else {

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Applications",

                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(

                        text =
                            "Total: ${activeApps.size} | " +
                                "User: ${
                                    activeApps.count {
                                        !it.isSystemApp
                                    }
                                } | " +
                                "System: ${
                                    activeApps.count {
                                        it.isSystemApp
                                    }
                                }",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }

                /*
                 * SEARCH HANYA DI HALAMAN UTAMA
                 */

                IconButton(

                    onClick = {

                        isSearching =
                            true
                    }
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            "Cari aplikasi"
                    )
                }

                IconButton(

                    onClick = {

                        showSettings =
                            true
                    }
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Settings,

                        contentDescription =
                            "Settings"
                    )
                }
            }
        }

        /*
         * ==========================================
         * DISABLED + TANDAI
         * ==========================================
         */

        if (
            !showDisabledApps &&
                !selectionMode &&
                !isSearching
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Button(

                    onClick = {

                        showDisabledApps =
                            true

                        selectedFilter =
                            "ALL"

                        searchQuery =
                            ""

                        isSearching =
                            false
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Disabled (${
                                disabledApps.size +
                                    deletedApps.size
                            })"
                    )
                }

                Button(

                    onClick = {

                        selectionMode =
                            true

                        selectedPackages =
                            emptySet()
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Tandai"
                    )
                }
            }
        }

        /*
         * ==========================================
         * FILTER
         * ==========================================
         */

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            FilterChip(

                selected =
                    selectedFilter == "ALL",

                onClick = {

                    selectedFilter =
                        "ALL"
                },

                label = {

                    Text(
                        "Semua"
                    )
                }
            )

            FilterChip(

                selected =
                    selectedFilter == "USER",

                onClick = {

                    selectedFilter =
                        "USER"
                },

                label = {

                    Text(
                        "Pengguna"
                    )
                }
            )

            FilterChip(

                selected =
                    selectedFilter == "SYSTEM",

                onClick = {

                    selectedFilter =
                        "SYSTEM"
                },

                label = {

                    Text(
                        "Sistem"
                    )
                }
            )
        }

        /*
         * ==========================================
         * SEARCH RESULT
         * ==========================================
         *
         * Hanya tampil pada Applications.
         */

        if (
            !showDisabledApps &&
                searchQuery.isNotBlank()
        ) {

            Text(

                text =
                    "${filteredInstalledApps.size} aplikasi ditemukan",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                modifier =
                    Modifier.padding(
                        bottom = 8.dp
                    )
            )
        }

        /*
         * ==========================================
         * APPLICATION LIST
         * ==========================================
         */

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            /*
             * ==================================
             * INSTALLED APPS
             * ==================================
             */

            items(

                items =
                    filteredInstalledApps,

                key = {

                    "installed_" +
                        it.packageName
                }
            ) { app ->

                AppItem(

                    app =
                        app,

                    disabled =
                        !app.isEnabled,

                    selected =
                        app.packageName in
                            selectedPackages,

                    selectionMode =
                        selectionMode,

                    onClick = {

                        if (
                            selectionMode
                        ) {

                            selectedPackages =

                                if (
                                    app.packageName in
                                        selectedPackages
                                ) {

                                    selectedPackages -
                                        app.packageName

                                } else {

                                    selectedPackages +
                                        app.packageName
                                }

                        } else {

                            selectedApp =
                                app
                        }
                    }
                )
            }

            /*
             * ==================================
             * DELETED APPS
             * ==================================
             *
             * Hanya muncul pada halaman Disabled.
             */

            if (
                showDisabledApps
            ) {

                items(

                    items =
                        filteredDeletedApps,

                    key = {

                        "deleted_" +
                            it.packageName
                    }
                ) { app ->

                    DeletedAppItem(

                        app =
                            app,

                        onClick = {

                            selectedDeletedApp =
                                app
                        }
                    )
                }
            }
        }

        /*
         * ==========================================
         * BULK ACTION BAR
         * ==========================================
         */

        if (
            selectionMode &&
                !showDisabledApps
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                /*
                 * ==================================
                 * DISABLE MASSAL
                 * ==================================
                 */

                Button(

                    onClick = {

                        val selectedApps =
                            filteredInstalledApps
                                .filter {

                                    it.packageName in
                                        selectedPackages
                                }

                        if (
                            selectedApps.isEmpty()
                        ) {

                            Toast.makeText(

                                context,

                                "Belum ada aplikasi yang dipilih",

                                Toast.LENGTH_SHORT

                            ).show()

                        } else {

                            PackageManagerAction
                                .disableMultiple(

                                    context =
                                        context,

                                    apps =
                                        selectedApps,

                                    accessMethod =
                                        accessMethod

                                ) { successCount, failedCount ->

                                    selectionMode =
                                        false

                                    selectedPackages =
                                        emptySet()

                                    /*
                                     * Karena callback bulk hanya
                                     * memberikan jumlah berhasil/gagal,
                                     * state UI diperbarui kalau
                                     * semuanya berhasil.
                                     *
                                     * Kalau ada kegagalan sebagian,
                                     * cache tidak boleh menganggap
                                     * semuanya berhasil.
                                     */

                                    if (
                                        successCount ==
                                            selectedApps.size
                                    ) {

                                        selectedApps.forEach {

                                            onAppDisabled(
                                                it.packageName
                                            )
                                        }

                                        displayApps =
                                            AppListStorage
                                                .loadDisplayApps(
                                                    context
                                                )
                                    }
                                }
                        }
                    },

                    enabled =
                        selectedPackages
                            .isNotEmpty(),

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Disable (${
                                selectedPackages.size
                            })"
                    )
                }

                /*
                 * ==================================
                 * HAPUS MASSAL
                 * ==================================
                 */

                Button(

                    onClick = {

                        val selectedApps =
                            filteredInstalledApps
                                .filter {

                                    it.packageName in
                                        selectedPackages
                                }

                        if (
                            selectedApps.isEmpty()
                        ) {

                            Toast.makeText(

                                context,

                                "Belum ada aplikasi yang dipilih",

                                Toast.LENGTH_SHORT

                            ).show()

                        } else {

                            PackageManagerAction
                                .uninstallMultiple(

                                    context =
                                        context,

                                    apps =
                                        selectedApps,

                                    accessMethod =
                                        accessMethod

                                ) { successCount, failedCount ->

                                    selectionMode =
                                        false

                                    selectedPackages =
                                        emptySet()

                                    if (
                                        successCount ==
                                            selectedApps.size
                                    ) {

                                        selectedApps.forEach {

                                            onAppUninstalled(
                                                it.packageName
                                            )
                                        }

                                        displayApps =
                                            AppListStorage
                                                .loadDisplayApps(
                                                    context
                                                )
                                    }
                                }
                        }
                    },

                    enabled =
                        selectedPackages
                            .isNotEmpty(),

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Hapus (${
                                selectedPackages.size
                            })"
                    )
                }
            }
        }
    }

    /*
     * ==========================================
     * APP DETAIL
     * ==========================================
     */

    selectedApp?.let { app ->

        Box(

            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Transparent
                    )
                    .pointerInput(Unit) {

                        detectTapGestures(

                            onTap = {

                                selectedApp =
                                    null
                            }
                        )
                    }
        ) {

            Box(

                modifier =
                    Modifier
                        .align(
                            Alignment.Center
                        )
                        .fillMaxWidth(0.90f)
                        .pointerInput(Unit) {

                            detectTapGestures(
                                onTap = {
                                    /*
                                     * Konsumsi touch.
                                     */
                                }
                            )
                        }
            ) {

                Surface(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            24.dp
                        ),

                    tonalElevation =
                        6.dp,

                    shadowElevation =
                        8.dp
                ) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                    ) {

                        Text(

                            text =
                                app.appName,

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )

                        Text(

                            text =
                                if (
                                    app.isEnabled
                                ) {

                                    "AKTIF"

                                } else {

                                    "DISABLED"
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )

                        AppInfoRow(
                            title =
                                "Package",
                            value =
                                app.packageName
                        )

                        AppInfoRow(
                            title =
                                "Version",
                            value =
                                "${app.versionName} " +
                                    "(${app.versionCode})"
                        )

                        AppInfoRow(
                            title =
                                "Target SDK",
                            value =
                                app.targetSdk.toString()
                        )

                        AppInfoRow(
                            title =
                                "Min SDK",
                            value =
                                app.minSdk.toString()
                        )

                        AppInfoRow(
                            title =
                                "UID",
                            value =
                                app.uid.toString()
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        Text(

                            text =
                                "Storage",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        AppInfoRow(
                            title =
                                "App",
                            value =
                                formatFileSize(
                                    app.apkSize
                                )
                        )

                        AppInfoRow(
                            title =
                                "Data",
                            value =
                                formatFileSize(
                                    app.dataSize
                                )
                        )

                        AppInfoRow(
                            title =
                                "Cache",
                            value =
                                formatFileSize(
                                    app.cacheSize
                                )
                        )

                        AppInfoRow(
                            title =
                                "Total",
                            value =
                                formatFileSize(
                                    app.totalSize
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        AppInfoRow(
                            title =
                                "Installer",
                            value =
                                app.installer
                        )

                        if (
                            accessMethod ==
                                AccessMethod.ROOT
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Text(

                                text =
                                    "APK Path",

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )

                            Text(

                                text =
                                    app.apkPath,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )

                        Text(

                            text =
                                "Permissions",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        if (
                            app.requestedPermissions
                                .isEmpty()
                        ) {

                            Text(

                                text =
                                    "No requested permissions",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                        } else {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(
                                            180.dp
                                        )
                            ) {

                                LazyColumn(

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            4.dp
                                        )
                                ) {

                                    items(
                                        app.requestedPermissions
                                    ) { permission ->

                                        val granted =
                                            permission in
                                                app.grantedPermissions

                                        val symbol =
                                            if (
                                                granted
                                            ) {
                                                "✓"
                                            } else {
                                                "○"
                                            }

                                        Text(

                                            text =
                                                "$symbol " +
                                                    permission
                                                        .substringAfterLast(
                                                            '.'
                                                        ),

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    20.dp
                                )
                        )

                        /*
                         * ==================================
                         * ACTION BUTTON
                         * ==================================
                         */

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.End
                        ) {

                            /*
                             * ==================================
                             * ENABLE
                             * ==================================
                             */

                            if (
                                !app.isEnabled
                            ) {

                                Button(

                                    onClick = {

                                        enablePackage(

                                            context =
                                                context,

                                            packageName =
                                                app.packageName,

                                            accessMethod =
                                                accessMethod

                                        ) { success ->

                                            if (
                                                success
                                            ) {

                                                selectedApp =
                                                    null

                                                /*
                                                 * Update cache
                                                 * tanpa scan.
                                                 */

                                                val updatedApps =
                                                    apps.map {

                                                        currentApp ->

                                                        if (
                                                            currentApp
                                                                .packageName ==
                                                                app.packageName
                                                        ) {

                                                            currentApp
                                                                .copy(
                                                                    isEnabled =
                                                                        true
                                                                )

                                                        } else {

                                                            currentApp
                                                        }
                                                    }

                                                AppListStorage
                                                    .saveApps(

                                                        context =
                                                            context,

                                                        apps =
                                                            updatedApps
                                                    )

                                                /*
                                                 * Refresh display
                                                 * dari cache.
                                                 */

                                                displayApps =
                                                    AppListStorage
                                                        .loadDisplayApps(
                                                            context
                                                        )
                                            }
                                        }
                                    }
                                ) {

                                    Text(
                                        text =
                                            "Enable"
                                    )
                                }

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            8.dp
                                        )
                                )
                            }

                            /*
                             * ==================================
                             * HAPUS
                             * ==================================
                             */

                            Button(

                                onClick = {

                                    PackageManagerAction
                                        .uninstall(

                                            context =
                                                context,

                                            app =
                                                app,

                                            accessMethod =
                                                accessMethod

                                        ) { success ->

                                            if (
                                                success
                                            ) {

                                                selectedApp =
                                                    null

                                                onAppUninstalled(
                                                    app.packageName
                                                )

                                                /*
                                                 * PackageManagerAction
                                                 * sudah menyimpan aplikasi
                                                 * ke DeletedAppStorage.
                                                 *
                                                 * Kita hanya reload daftar
                                                 * display tanpa scan.
                                                 */

                                                displayApps =
                                                    AppListStorage
                                                        .loadDisplayApps(
                                                            context
                                                        )
                                            }
                                        }
                                }
                            ) {

                                Text(
                                    text =
                                        "Hapus"
                                )
                            }
                        }
                    }
                }

                /*
                 * ==================================
                 * CLOSE BUTTON
                 * ==================================
                 */

                Button(

                    onClick = {

                        selectedApp =
                            null
                    },

                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .offset(
                                y = 64.dp
                            )
                ) {

                    Text(
                        text =
                            "×"
                    )
                }
            }
        }
    }

    /*
     * ==========================================
     * DELETED APP DETAIL
     * ==========================================
     */

    selectedDeletedApp?.let { app ->

        Box(

            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Transparent
                    )
                    .pointerInput(Unit) {

                        detectTapGestures(

                            onTap = {

                                selectedDeletedApp =
                                    null
                            }
                        )
                    }
        ) {

            Surface(

                modifier =
                    Modifier
                        .align(
                            Alignment.Center
                        )
                        .fillMaxWidth(0.90f),

                shape =
                    RoundedCornerShape(
                        24.dp
                    ),

                tonalElevation =
                    6.dp,

                shadowElevation =
                    8.dp
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                ) {

                    Text(

                        text =
                            app.appName,

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(

                        text =
                            "DELETED",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )

                    AppInfoRow(
                        title =
                            "Package",
                        value =
                            app.packageName
                    )

                    AppInfoRow(
                        title =
                            "Version",
                        value =
                            "${app.versionName} " +
                                "(${app.versionCode})"
                    )

                    AppInfoRow(
                        title =
                            "APK",
                        value =
                            formatFileSize(
                                app.apkSize
                            )
                    )

                    AppInfoRow(
                        title =
                            "APK Path",
                        value =
                            app.apkPath
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )

                    Text(

                        text =
                            "Aplikasi ini sudah dihapus " +
                                "untuk user 0.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.End
                    ) {

                        Button(

                            onClick = {

                                selectedDeletedApp =
                                    null
                            }
                        ) {

                            Text(
                                text =
                                    "Tutup"
                            )
                        }
                    }
                }
            }
        }
    }

    /*
     * ==========================================
     * SETTINGS DIALOG
     * ==========================================
     */

    if (
        showSettings
    ) {

        AlertDialog(

            onDismissRequest = {

                showSettings =
                    false
            },

            title = {

                Text(
                    text =
                        "Settings"
                )
            },

            text = {

                Text(

                    text =
                        "Metode akses saat ini: " +
                            accessMethod.name
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showSettings =
                            false

                        onSettingsClick()
                    }
                ) {

                    Text(
                        text =
                            "Ubah metode akses"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showSettings =
                            false
                    }
                ) {

                    Text(
                        text =
                            "Batal"
                    )
                }
            }
        )
    }
}


/*
 * ==========================================
 * ENABLE PACKAGE
 * ==========================================
 *
 * Menjalankan:
 *
 * pm enable --user 0 PACKAGE
 *
 * Tidak melakukan scan.
 */

private fun enablePackage(

    context: Context,

    packageName: String,

    accessMethod: AccessMethod,

    onResult: (Boolean) -> Unit

) {

    Thread {

        var success =
            false

        var output =
            ""

        var error =
            ""

        try {

            when (
                accessMethod
            ) {

                AccessMethod.ADB -> {

                    if (
                        !Shizuku.pingBinder()
                    ) {

                        error =
                            "Shizuku tidak aktif"

                    } else if (
                        Shizuku.checkSelfPermission() !=
                            android.content.pm.PackageManager
                                .PERMISSION_GRANTED
                    ) {

                        error =
                            "Permission Shizuku belum diberikan"

                    } else {

                        val shizukuClass =
                            Class.forName(
                                "rikka.shizuku.Shizuku"
                            )

                        val newProcessMethod =
                            shizukuClass
                                .getDeclaredMethod(

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

                                    "pm enable --user 0 " +
                                        packageName
                                ),

                                null,

                                null

                            ) as rikka.shizuku
                                .ShizukuRemoteProcess

                        output =
                            process
                                .inputStream
                                .bufferedReader()
                                .use {
                                    it.readText()
                                }

                        error =
                            process
                                .errorStream
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

                AccessMethod.ROOT -> {

                    val process =
                        Runtime
                            .getRuntime()
                            .exec(

                                arrayOf(

                                    "su",

                                    "-c",

                                    "pm enable --user 0 " +
                                        packageName
                                )
                            )

                    output =
                        process
                            .inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    error =
                        process
                            .errorStream
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

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: exception
                        .javaClass
                        .simpleName

            success =
                false
        }

        Handler(
            Looper.getMainLooper()
        ).post {

            val message =

                if (
                    success
                ) {

                    "Aplikasi berhasil di-enable"

                } else {

                    error
                        .trim()
                        .ifEmpty {
                            output.trim()
                        }
                        .ifEmpty {
                            "Gagal mengaktifkan aplikasi"
                        }
                }

            Toast.makeText(

                context,

                message.take(300),

                Toast.LENGTH_LONG

            ).show()

            onResult(
                success
            )
        }

    }.start()
}


/*
 * ==========================================
 * APP INFO ROW
 * ==========================================
 */

@Composable
private fun AppInfoRow(

    title: String,

    value: String

) {

    val context =
        LocalContext.current

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {

                    val clipboard =
                        context.getSystemService(
                            ClipboardManager::class.java
                        )

                    clipboard?.setPrimaryClip(

                        ClipData.newPlainText(

                            title,

                            value
                        )
                    )

                    Toast.makeText(

                        context,

                        "$title disalin",

                        Toast.LENGTH_SHORT

                    ).show()
                }
                .padding(
                    vertical = 5.dp
                )
    ) {

        Text(

            text =
                "$title:",

            modifier =
                Modifier.width(
                    90.dp
                ),

            style =
                MaterialTheme
                    .typography
                    .labelMedium
        )

        Text(

            text =
                value,

            modifier =
                Modifier.weight(1f),

            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}


/*
 * ==========================================
 * FILE SIZE FORMATTER
 * ==========================================
 */

private fun formatFileSize(
    size: Long
): String {

    if (
        size < 1024L
    ) {

        return "$size B"
    }

    val kb =
        size / 1024.0

    if (
        kb < 1024.0
    ) {

        return String.format(

            Locale.US,

            "%.1f KB",

            kb
        )
    }

    val mb =
        kb / 1024.0

    if (
        mb < 1024.0
    ) {

        return String.format(

            Locale.US,

            "%.1f MB",

            mb
        )
    }

    val gb =
        mb / 1024.0

    return String.format(

        Locale.US,

        "%.2f GB",

        gb
    )
}


/*
 * ==========================================
 * APPLICATION ITEM
 * ==========================================
 */

@Composable
private fun AppItem(

    app: AppInfo,

    disabled: Boolean,

    selected: Boolean,

    selectionMode: Boolean,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            AndroidView(

                factory = { context ->

                    ImageView(
                        context
                    )
                },

                update = { imageView ->

                    imageView.setImageDrawable(
                        app.icon
                    )
                },

                modifier =
                    Modifier.size(
                        36.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(

                    text =
                        app.appName,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(

                    text =
                        app.packageName,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(

                        text =
                            if (
                                app.isSystemApp
                            ) {

                                "SYSTEM"

                            } else {

                                "USER"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(

                        text =
                            if (
                                disabled
                            ) {

                                "DISABLED"

                            } else {

                                "AKTIF"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )
                }
            }

            if (
                selectionMode
            ) {

                Checkbox(

                    checked =
                        selected,

                    onCheckedChange = {

                        onClick()
                    }
                )
            }
        }
    }
}


/*
 * ==========================================
 * DELETED APPLICATION ITEM
 * ==========================================
 */

@Composable
private fun DeletedAppItem(

    app: DeletedAppInfo,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * Tidak mencoba mengambil icon dari
             * PackageManager karena aplikasi sudah
             * dihapus untuk user.
             *
             * Ikon sederhana dipakai sebagai penanda
             * bahwa item adalah DELETED.
             */

            Box(

                modifier =
                    Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant,

                            RoundedCornerShape(
                                18.dp
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "×",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )

            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(

                    text =
                        app.appName,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(

                    text =
                        app.packageName,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Row {

                    Text(

                        text =
                            if (
                                app.isSystemApp
                            ) {

                                "SYSTEM"

                            } else {

                                "USER"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(

                        text =
                            "DELETED",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )
                }
            }
        }
    }
}