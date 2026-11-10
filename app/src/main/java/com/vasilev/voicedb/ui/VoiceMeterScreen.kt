package com.vasilev.voicedb.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilev.voicedb.R
import com.vasilev.voicedb.audio.VoiceLevel
import com.vasilev.voicedb.ui.components.CalibrationDialog
import com.vasilev.voicedb.ui.components.DecibelGraph
import com.vasilev.voicedb.ui.theme.AccentAmber
import com.vasilev.voicedb.ui.theme.AccentCyan
import com.vasilev.voicedb.ui.theme.AccentGreen
import com.vasilev.voicedb.ui.theme.AccentRed
import com.vasilev.voicedb.ui.theme.BgDeep
import com.vasilev.voicedb.ui.theme.BorderSubtle
import com.vasilev.voicedb.ui.theme.SurfaceCard
import com.vasilev.voicedb.ui.theme.SurfaceElevated
import com.vasilev.voicedb.ui.theme.TextMuted
import com.vasilev.voicedb.ui.theme.TextPrimary
import com.vasilev.voicedb.ui.theme.TextSecondary
import com.vasilev.voicedb.ui.theme.colorForDb
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VoiceMeterScreen(
    uiState: VoiceMeterUiState,
    showOpenSettingsAction: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleListening: () -> Unit,
    onResetStats: () -> Unit,
    onCalibrationChange: (Float) -> Unit
) {
    val view = LocalView.current
    DisposableEffect(uiState.isListening) {
        view.keepScreenOn = uiState.isListening
        onDispose {
            view.keepScreenOn = false
        }
    }

    var showCalibrationDialog by remember { mutableStateOf(false) }

    if (showCalibrationDialog) {
        CalibrationDialog(
            offsetDb = uiState.calibrationOffsetDb,
            onOffsetChange = onCalibrationChange,
            onDismiss = { showCalibrationDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopStatusBar(
            uiState = uiState,
            onOpenCalibration = { showCalibrationDialog = true }
        )

        if (!uiState.hasMicPermission) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PermissionCard(
                    showOpenSettingsAction = showOpenSettingsAction,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        } else {
            HeroDecibelCard(
                currentDb = uiState.currentDb,
                isListening = uiState.isListening,
                voiceLevel = uiState.voiceLevel
            )

            if (uiState.micError) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AccentRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(R.string.error_mic_unavailable),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            DecibelGraph(
                history = uiState.history,
                maxHistorySize = uiState.maxHistorySize,
                maxDb = uiState.maxDb,
                isListening = uiState.isListening,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
            )

            StatsRow(
                minDb = uiState.minDb,
                avgDb = uiState.avgDb,
                maxDb = uiState.maxDb
            )

            ControlsRow(
                isListening = uiState.isListening,
                hasHistory = uiState.history.isNotEmpty() || uiState.maxDb != null,
                onToggleListening = onToggleListening,
                onResetStats = onResetStats
            )
        }
    }
}

@Composable
private fun TopStatusBar(
    uiState: VoiceMeterUiState,
    onOpenCalibration: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "liveDot")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dotColor = when {
        !uiState.hasMicPermission -> AccentRed
        uiState.isListening -> AccentGreen
        else -> TextMuted
    }
    val statusText = when {
        !uiState.hasMicPermission -> stringResource(R.string.status_no_permission)
        uiState.isListening -> stringResource(R.string.status_listening)
        else -> stringResource(R.string.status_paused)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(if (uiState.isListening) pulseAlpha else 1f)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        if (uiState.hasMicPermission) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val offsetInt = uiState.calibrationOffsetDb.roundToInt()
                if (offsetInt != 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentCyan.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = stringResource(R.string.calibration_offset_value, offsetInt),
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    IconButton(onClick = onOpenCalibration) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = stringResource(R.string.action_calibrate),
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroDecibelCard(
    currentDb: Float,
    isListening: Boolean,
    voiceLevel: VoiceLevel
) {
    val targetColor = if (isListening || currentDb > 0f) colorForDb(currentDb) else TextSecondary
    val accentColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 180),
        label = "dbColor"
    )
    val barFraction by animateFloatAsState(
        targetValue = (currentDb / 110f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 120),
        label = "barFraction"
    )

    val levelLabel = when {
        !isListening && currentDb <= 0f -> stringResource(R.string.status_paused)
        voiceLevel == VoiceLevel.SILENCE -> stringResource(R.string.level_silence)
        voiceLevel == VoiceLevel.QUIET -> stringResource(R.string.level_quiet)
        voiceLevel == VoiceLevel.NORMAL -> stringResource(R.string.level_normal)
        voiceLevel == VoiceLevel.LOUD -> stringResource(R.string.level_loud)
        else -> stringResource(R.string.level_very_loud)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", currentDb),
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.unit_db),
                    color = TextSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f))
            ) {
                Text(
                    text = levelLabel,
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AccentCyan,
                                    AccentGreen,
                                    AccentAmber,
                                    AccentRed
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    minDb: Float?,
    avgDb: Float?,
    maxDb: Float?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = stringResource(R.string.stat_min),
            value = minDb,
            accent = AccentCyan,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.stat_avg),
            value = avgDb,
            accent = AccentGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.stat_max),
            value = maxDb,
            accent = AccentAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Float?,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val emptyText = stringResource(R.string.stat_empty)
    val unitDb = stringResource(R.string.unit_db)
    val formatted = value?.let {
        String.format(Locale.getDefault(), "%.1f %s", it, unitDb)
    } ?: emptyText

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatted,
                style = TextStyle(
                    color = if (value != null) accent else TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }
}

@Composable
private fun ControlsRow(
    isListening: Boolean,
    hasHistory: Boolean,
    onToggleListening: () -> Unit,
    onResetStats: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onToggleListening,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) SurfaceElevated else AccentGreen,
                contentColor = if (isListening) TextPrimary else BgDeep
            ),
            border = if (isListening) BorderStroke(1.dp, BorderSubtle) else null,
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isListening) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (isListening) R.string.action_pause else R.string.action_start
                ),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = onResetStats,
            enabled = hasHistory,
            modifier = Modifier.height(54.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (hasHistory) BorderSubtle else BorderSubtle.copy(alpha = 0.45f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary,
                disabledContentColor = TextMuted
            ),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reset),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_reset),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PermissionCard(
    showOpenSettingsAction: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = AccentCyan.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.permission_title),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.permission_description),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = BgDeep
                )
            ) {
                Text(
                    text = stringResource(R.string.permission_grant),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (showOpenSettingsAction) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.permission_settings),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
