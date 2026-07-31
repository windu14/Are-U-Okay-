package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AudioPlayerState
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import kotlin.math.sin

@Composable
fun SongCard(
    trackName: String,
    artistName: String,
    artworkUrl: String?,
    previewUrl: String?,
    playerState: AudioPlayerState,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardId: String? = null,    // Unique identifier for note card / item to avoid duplicate playing indicators
    attachCount: Int? = null, // For Top 3 songs badge
    rankIndex: Int? = null,   // 1, 2, 3 rank
    onAttachClick: (() -> Unit)? = null // Optional attach button in search sheet
) {
    val matchesUrl = playerState.currentPreviewUrl == previewUrl
    val matchesCard = cardId == null || playerState.activeCardId == null || playerState.activeCardId == cardId
    val isCurrentTrack = matchesUrl && matchesCard
    val isPlaying = isCurrentTrack && playerState.isPlaying
    val isBuffering = isCurrentTrack && playerState.isBuffering

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, PastelLavender) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Rank badge & Album Artwork if rankIndex present (Netflix Style Big Top Number with Overlap)
                    if (rankIndex != null) {
                        val strokeColor = when (rankIndex) {
                            1 -> PastelRose
                            2 -> PastelLavender
                            else -> PastelMint
                        }
                        val density = LocalDensity.current

                        Box(
                            modifier = Modifier
                                .width(76.dp)
                                .height(56.dp)
                        ) {
                            // Netflix-Style Giant Hollow Outline Rank Number
                            Text(
                                text = "$rankIndex",
                                style = TextStyle(
                                    fontSize = 62.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-3).sp,
                                    lineHeight = 62.sp,
                                    color = strokeColor,
                                    drawStyle = Stroke(
                                        width = with(density) { 3.5.dp.toPx() },
                                        join = StrokeJoin.Round
                                    )
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = 0.dp, y = (-2).dp)
                            )

                            // Album Artwork overlapping the giant number on the right
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .align(Alignment.CenterEnd)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.5.dp,
                                        when (rankIndex) {
                                            1 -> PastelRose.copy(alpha = 0.8f)
                                            2 -> PastelLavender.copy(alpha = 0.8f)
                                            else -> PastelMint.copy(alpha = 0.8f)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (!artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = artworkUrl,
                                        contentDescription = trackName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = PastelLavender,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Regular Artwork Box without rank number
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (!artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = artworkUrl,
                                    contentDescription = trackName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Song Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (attachCount != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PastelRose.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$attachCount x di-attach di curhatan",
                                    fontSize = 10.sp,
                                    color = PastelRose,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play / Pause Button
                    if (!previewUrl.isNull_or_blank()) {
                        IconButton(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying) PastelLavender else MaterialTheme.colorScheme.surface
                                )
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PastelLavender,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                                    tint = if (isPlaying) Color(0xFF261833) else PastelLavender,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Attach button if requested
                    if (onAttachClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = onAttachClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PastelMint,
                                contentColor = Color(0xFF0F3830)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attach", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Audio Progress Bar if playing
            if (isPlaying && isCurrentTrack) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlayingAudioWaveAnimation(isPlaying = isPlaying, modifier = Modifier.padding(end = 8.dp))
                    LinearWavyProgressIndicator(
                        progress = { playerState.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp),
                        color = PastelLavender,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview 30s",
                        fontSize = 10.sp,
                        color = PastelLavender,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = PastelLavender,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    wavelength: Dp = 20.dp,
    amplitude: Dp = 3.dp,
    strokeWidth: Dp = 2.5.dp
) {
    val density = LocalDensity.current
    val wavelengthPx = with(density) { wavelength.toPx() }
    val amplitudePx = with(density) { amplitude.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "wavy_progress")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val centerY = size.height / 2f
        val currentProgress = progress().coerceIn(0f, 1f)
        val activeWidth = width * currentProgress

        // 1. Draw Inactive Track (Straight Line)
        if (activeWidth < width) {
            drawLine(
                color = trackColor,
                start = Offset(activeWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // 2. Draw Active Progress (Sine Wavy Path)
        if (activeWidth > 0f) {
            val path = Path()
            val stepPx = 2f
            var x = 0f
            val startY = centerY + amplitudePx * sin((x / wavelengthPx) * (2 * Math.PI.toFloat()) - phaseShift)
            path.moveTo(0f, startY)

            while (x <= activeWidth) {
                val y = centerY + amplitudePx * sin((x / wavelengthPx) * (2 * Math.PI.toFloat()) - phaseShift)
                path.lineTo(x, y)
                x += stepPx
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun PlayingAudioWaveAnimation(
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val playFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "playFactor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val scale4 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((14 * (0.25f + (scale1 - 0.25f) * playFactor)).dp)
                .background(PastelRose, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * (0.2f + (scale2 - 0.2f) * playFactor)).dp)
                .background(PastelLavender, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((14 * (0.25f + (scale3 - 0.25f) * playFactor)).dp)
                .background(PastelMint, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * (0.2f + (scale4 - 0.2f) * playFactor)).dp)
                .background(PastelLavender.copy(alpha = 0.8f), CircleShape)
        )
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}
