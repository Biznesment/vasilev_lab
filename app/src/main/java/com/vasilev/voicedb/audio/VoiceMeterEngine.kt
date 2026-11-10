package com.vasilev.voicedb.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

sealed interface AudioMeterEvent {
    data class Reading(val db: Float) : AudioMeterEvent
    data object PermissionMissing : AudioMeterEvent
    data object HardwareUnavailable : AudioMeterEvent
}

/**
 * Streams 20 Hz voice decibel readings from Android's [AudioRecord].
 */
class VoiceMeterEngine(
    private val context: Context
) {
    @SuppressLint("MissingPermission")
    fun observeDecibels(
        calibrationProvider: () -> Float
    ): Flow<AudioMeterEvent> = flow {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            emit(AudioMeterEvent.PermissionMissing)
            return@flow
        }

        val setup = createBestAudioRecord()
        if (setup == null) {
            emit(AudioMeterEvent.HardwareUnavailable)
            return@flow
        }

        val (recorder, sampleRate, chunkSamples) = setup
        val calculator = DecibelCalculator(sampleRateHz = sampleRate)
        val smoother = DecibelSmoother()
        val buffer = ShortArray(chunkSamples)

        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                emit(AudioMeterEvent.HardwareUnavailable)
                return@flow
            }

            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, chunkSamples)
                when {
                    read > 0 -> {
                        val rawDb = calculator.calculateDb(
                            samples = buffer,
                            length = read,
                            calibrationOffsetDb = calibrationProvider()
                        )
                        emit(AudioMeterEvent.Reading(smoother.smooth(rawDb)))
                    }
                    read == 0 -> {
                        delay(10L)
                    }
                    else -> {
                        emit(AudioMeterEvent.HardwareUnavailable)
                        break
                    }
                }
            }
        } catch (_: SecurityException) {
            emit(AudioMeterEvent.PermissionMissing)
        } catch (_: IllegalStateException) {
            emit(AudioMeterEvent.HardwareUnavailable)
        } finally {
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            }
            runCatching { recorder.release() }
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    private fun createBestAudioRecord(): Triple<AudioRecord, Int, Int>? {
        val sampleRates = intArrayOf(44_100, 16_000)
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sources) {
            for (sampleRate in sampleRates) {
                val minBufferBytes = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBufferBytes <= 0) continue

                val chunkSamples = sampleRate / UPDATES_PER_SECOND
                val bufferBytes = maxOf(minBufferBytes, chunkSamples * BYTES_PER_SAMPLE * 4)

                val candidate = try {
                    AudioRecord(
                        source,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferBytes
                    )
                } catch (_: IllegalArgumentException) {
                    null
                } catch (_: SecurityException) {
                    return null
                }

                if (candidate != null) {
                    if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                        return Triple(candidate, sampleRate, chunkSamples)
                    }
                    runCatching { candidate.release() }
                }
            }
        }
        return null
    }

    companion object {
        const val UPDATES_PER_SECOND = 20
        private const val BYTES_PER_SAMPLE = 2
    }
}
