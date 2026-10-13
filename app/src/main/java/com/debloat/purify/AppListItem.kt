/*
 * Purify - Pengelola Aplikasi & Paket Android
 * Copyright (c) 2026 Achmad Riyan Sholihin (Bryan16-ly)
 *
 * Licensed under the MIT License.
 */

package com.debloat.purify

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AppItem(
    app: AppInfo,
    disabled: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    isGlass: Boolean,
    cardAlpha: Float,
    glassRimBorder: BorderStroke,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    val cardBackground = if (isGlass) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3B3B54).copy(alpha = (cardAlpha * 1.35f).coerceAtMost(0.95f)),
                Color(0xFF181828).copy(alpha = cardAlpha)
            )
        )
    } else null

    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .then(
            if (isGlass) {
                Modifier
                    .background(cardBackground!!)
                    .border(glassRimBorder, cardShape)
            } else {
                Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
            }
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp)

    Row(
        modifier = cardModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cachedBitmap = remember(app.packageName) {
            try {
                val drawable = app.icon
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (disabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        else if (app.isSystemApp) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (disabled) "NONAKTIF" else if (app.isSystemApp) "SYSTEM" else "USER",
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

@Composable
fun DeletedAppItem(
    app: DeletedAppInfo,
    isGlass: Boolean,
    cardAlpha: Float,
    glassRimBorder: BorderStroke,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .then(
            if (isGlass) {
                Modifier
                    .background(Color(0xFF28283C).copy(alpha = cardAlpha))
                    .border(glassRimBorder, cardShape)
            } else {
                Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
            }
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp)

    Row(
        modifier = cardModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape),
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "TERHAPUS",
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