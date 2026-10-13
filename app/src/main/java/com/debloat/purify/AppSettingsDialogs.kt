/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

package com.debloat.purify

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ThemeSettingsDialog(
    themeMode: String,
    isGlass: Boolean,
    bgBlurDp: Float,
    onBgBlurChange: (Float) -> Unit,
    cardAlpha: Float,
    onCardAlphaChange: (Float) -> Unit,
    hasCustomBg: Boolean,
    showBgInOtherThemes: Boolean,
    onShowBgInOtherThemesChange: (Boolean) -> Unit,
    onSelectGalleryImage: () -> Unit,
    onRemoveGalleryImage: () -> Unit,
    onThemeChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    dialogContainerColor: Color,
    dialogGlassBorder: BorderStroke?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainerColor,
        modifier = Modifier.then(
            if (dialogGlassBorder != null) Modifier.border(dialogGlassBorder, RoundedCornerShape(28.dp)) else Modifier
        ),
        icon = { Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(text = "Pilih Tema Tampilan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onThemeChanged("LIGHT") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (themeMode == "LIGHT") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == "LIGHT", onClick = { onThemeChanged("LIGHT") })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Tampilan Terang", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onThemeChanged("DARK") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (themeMode == "DARK") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == "DARK", onClick = { onThemeChanged("DARK") })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Tampilan Gelap", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onThemeChanged("GLASS") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (themeMode == "GLASS") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == "GLASS", onClick = { onThemeChanged("GLASS") })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Liquid Glass ✨", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }

                if (isGlass) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))

                    Text(
                        text = "Pengaturan Keburaman (Liquid Glass)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Keburaman Latar Wallpaper: ${bgBlurDp.toInt()} dp",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = bgBlurDp,
                        onValueChange = onBgBlurChange,
                        valueRange = 0f..50f
                    )

                    Text(
                        text = "Ketebalan Kaca Kartu: ${(cardAlpha * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = cardAlpha,
                        onValueChange = onCardAlphaChange,
                        valueRange = 0.05f..0.85f
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))

                Text(
                    text = "Gambar Latar (Galeri)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = onSelectGalleryImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (hasCustomBg) "Ganti Gambar Latar" else "Pilih Gambar Latar")
                }

                if (hasCustomBg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gunakan di Tema Terang & Gelap",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Aktifkan jika ingin memakai foto galeri di luar mode Liquid Glass",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showBgInOtherThemes,
                            onCheckedChange = onShowBgInOtherThemesChange
                        )
                    }

                    TextButton(
                        onClick = onRemoveGalleryImage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Hapus Gambar Latar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun AboutDialog(
    appVersionName: String,
    onDismiss: () -> Unit,
    dialogContainerColor: Color,
    dialogGlassBorder: BorderStroke?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainerColor,
        modifier = Modifier.then(
            if (dialogGlassBorder != null) Modifier.border(dialogGlassBorder, RoundedCornerShape(28.dp)) else Modifier
        ),
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

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))

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

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))

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

                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))

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
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}