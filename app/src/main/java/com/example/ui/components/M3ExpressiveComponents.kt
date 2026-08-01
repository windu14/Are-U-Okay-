package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelRose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    text: String = "Memuat lagu dari iTunes..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_loading")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            // Expressive Glowing Background Ring
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .background(
                        color = PastelLavender.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )

            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = PastelLavender,
                trackColor = PastelRose.copy(alpha = 0.3f),
                strokeWidth = 4.dp
            )

            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = PastelRose,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddNoteClick: () -> Unit,
    onAddPhotoPostClick: (() -> Unit)? = null,
    onAddMusicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FloatingActionButtonMenu(
            modifier = Modifier.align(Alignment.BottomEnd),
            expanded = expanded,
            button = {
                ToggleFloatingActionButton(
                    modifier = Modifier.animateFloatingActionButton(
                        visible = true,
                        alignment = Alignment.BottomEnd
                    ),
                    checked = expanded,
                    containerSize = ToggleFloatingActionButtonDefaults.containerSizeLarge(),
                    onCheckedChange = { onExpandedChange(!expanded) },
                    containerColor = { progress ->
                        if (progress > 0.5f) PastelRose else PastelLavender
                    }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Menu Tambah",
                        tint = Color(0xFF261833)
                    )
                }
            }
        ) {
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onAddNoteClick()
                },
                icon = { Icon(Icons.Filled.Create, contentDescription = "Tulis Curhatan", tint = PastelRose) },
                text = { Text(text = "Tulis Curhatan", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onAddPhotoPostClick?.invoke()
                },
                icon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Posting Foto GDrive", tint = PastelLavender) },
                text = { Text(text = "Posting Foto", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onAddMusicClick()
                },
                icon = { Icon(Icons.Filled.MusicNote, contentDescription = "Cari & Attach Lagu", tint = PastelLavender) },
                text = { Text(text = "Attach Lagu iTunes", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) PastelLavender else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color(0xFF261833) else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
