package com.vasilev.voicedb.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilev.voicedb.R
import com.vasilev.voicedb.ui.VoiceMeterViewModel
import com.vasilev.voicedb.ui.theme.AccentCyan
import com.vasilev.voicedb.ui.theme.BgDeep
import com.vasilev.voicedb.ui.theme.SurfaceElevated
import com.vasilev.voicedb.ui.theme.TextPrimary
import com.vasilev.voicedb.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun CalibrationDialog(
    offsetDb: Float,
    onOffsetChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val rounded = offsetDb.roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = stringResource(R.string.calibration_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.calibration_subtitle),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.calibration_offset_value, rounded),
                    style = TextStyle(
                        color = AccentCyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = offsetDb,
                    onValueChange = { onOffsetChange(it.roundToInt().toFloat()) },
                    valueRange = VoiceMeterViewModel.MIN_CALIBRATION_DB..VoiceMeterViewModel.MAX_CALIBRATION_DB,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onOffsetChange(0f) }) {
                Text(
                    text = stringResource(R.string.calibration_reset),
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = BgDeep
                )
            ) {
                Text(
                    text = stringResource(R.string.calibration_done),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
