package com.vasilev.voicedb.ui

import com.vasilev.voicedb.audio.VoiceLevel

data class VoiceMeterUiState(
    val isListening: Boolean = false,
    val userPaused: Boolean = false,
    val hasMicPermission: Boolean = false,
    val currentDb: Float = 0f,
    val minDb: Float? = null,
    val avgDb: Float? = null,
    val maxDb: Float? = null,
    val history: List<Float> = emptyList(),
    val maxHistorySize: Int = VoiceStatsTracker.DEFAULT_HISTORY_SIZE,
    val calibrationOffsetDb: Float = 0f,
    val micError: Boolean = false
) {
    val voiceLevel: VoiceLevel
        get() = VoiceLevel.fromDb(currentDb)
}
