package com.vasilev.voicedb.ui.theme

import androidx.compose.ui.graphics.Color
import com.vasilev.voicedb.audio.VoiceLevel

val BgDeep = Color(0xFF0B101B)
val SurfaceCard = Color(0xFF121B2E)
val SurfaceElevated = Color(0xFF19243C)
val BorderSubtle = Color(0xFF22314F)

val TextPrimary = Color(0xFFF4F7FB)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val AccentCyan = Color(0xFF22D3EE)
val AccentGreen = Color(0xFF22C55E)
val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)
val AccentBlue = Color(0xFF38BDF8)

fun colorForDb(db: Float): Color = when (VoiceLevel.fromDb(db)) {
    VoiceLevel.SILENCE -> TextSecondary
    VoiceLevel.QUIET -> AccentCyan
    VoiceLevel.NORMAL -> AccentGreen
    VoiceLevel.LOUD -> AccentAmber
    VoiceLevel.VERY_LOUD -> AccentRed
}
