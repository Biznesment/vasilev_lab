package com.vasilev.voicedb.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vasilev.voicedb.audio.AudioMeterEvent
import com.vasilev.voicedb.audio.VoiceMeterEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceMeterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val engine = VoiceMeterEngine(application)
    private val statsTracker = VoiceStatsTracker(VoiceStatsTracker.DEFAULT_HISTORY_SIZE)

    @Volatile
    private var calibrationOffsetDb: Float = prefs.getFloat(KEY_CALIBRATION_OFFSET, 0f)
        .coerceIn(MIN_CALIBRATION_DB, MAX_CALIBRATION_DB)

    private val _uiState = MutableStateFlow(
        VoiceMeterUiState(
            calibrationOffsetDb = calibrationOffsetDb
        )
    )
    val uiState: StateFlow<VoiceMeterUiState> = _uiState.asStateFlow()

    private var meterJob: Job? = null
    private var desiredListening: Boolean = false
    private var isInForeground: Boolean = false

    fun onPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasMicPermission = granted) }
        if (granted && isInForeground && !_uiState.value.userPaused) {
            startListening()
        } else if (!granted) {
            stopListening()
        }
    }

    fun onForeground(hasPermission: Boolean) {
        isInForeground = true
        _uiState.update { it.copy(hasMicPermission = hasPermission) }
        if (hasPermission && !_uiState.value.userPaused) {
            startListening()
        }
    }

    fun onBackground() {
        isInForeground = false
        stopListening()
    }

    fun toggleListening() {
        val state = _uiState.value
        if (!state.hasMicPermission) return

        if (state.isListening) {
            _uiState.update { it.copy(userPaused = true) }
            stopListening()
        } else {
            _uiState.update { it.copy(userPaused = false, micError = false) }
            startListening()
        }
    }

    fun resetStats() {
        val empty = statsTracker.reset()
        _uiState.update {
            it.copy(
                currentDb = if (it.isListening) it.currentDb else 0f,
                minDb = empty.minDb,
                avgDb = empty.avgDb,
                maxDb = empty.maxDb,
                history = empty.history
            )
        }
    }

    fun setCalibrationOffset(offsetDb: Float) {
        val clamped = offsetDb.coerceIn(MIN_CALIBRATION_DB, MAX_CALIBRATION_DB)
        calibrationOffsetDb = clamped
        prefs.edit().putFloat(KEY_CALIBRATION_OFFSET, clamped).apply()
        _uiState.update { it.copy(calibrationOffsetDb = clamped) }
    }

    private fun startListening() {
        if (desiredListening && meterJob?.isActive == true) return
        desiredListening = true
        val previousJob = meterJob
        meterJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            if (!desiredListening) return@launch
            _uiState.update { it.copy(isListening = true, micError = false) }

            engine.observeDecibels { calibrationOffsetDb }.collect { event ->
                when (event) {
                    is AudioMeterEvent.Reading -> {
                        val snapshot = statsTracker.record(event.db)
                        _uiState.update {
                            it.copy(
                                isListening = true,
                                micError = false,
                                currentDb = snapshot.currentDb,
                                minDb = snapshot.minDb,
                                avgDb = snapshot.avgDb,
                                maxDb = snapshot.maxDb,
                                history = snapshot.history
                            )
                        }
                    }
                    AudioMeterEvent.PermissionMissing -> {
                        _uiState.update {
                            it.copy(
                                isListening = false,
                                hasMicPermission = false
                            )
                        }
                    }
                    AudioMeterEvent.HardwareUnavailable -> {
                        _uiState.update {
                            it.copy(
                                isListening = false,
                                micError = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopListening() {
        desiredListening = false
        meterJob?.cancel()
        _uiState.update { it.copy(isListening = false) }
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    companion object {
        private const val PREFS_NAME = "voice_db_prefs"
        private const val KEY_CALIBRATION_OFFSET = "calibration_offset_db"
        const val MIN_CALIBRATION_DB = -20f
        const val MAX_CALIBRATION_DB = 20f
    }
}
