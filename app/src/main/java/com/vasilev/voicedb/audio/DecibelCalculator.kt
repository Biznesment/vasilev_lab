package com.vasilev.voicedb.audio

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Qualitative loudness classification oriented toward human voice levels.
 */
enum class VoiceLevel {
    SILENCE,
    QUIET,
    NORMAL,
    LOUD,
    VERY_LOUD;

    companion object {
        fun fromDb(db: Float): VoiceLevel = when {
            db < 35f -> SILENCE
            db < 50f -> QUIET
            db < 70f -> NORMAL
            db < 85f -> LOUD
            else -> VERY_LOUD
        }
    }
}

/**
 * First-order IIR high-pass filter that removes DC bias and low-frequency rumble
 * below the fundamental range of human speech (~85 Hz) while keeping voice energy intact.
 */
class HighPassFilter(
    cutoffHz: Double = 85.0,
    sampleRateHz: Int = 44_100
) {
    private val alpha: Double
    private var prevInput: Double = 0.0
    private var prevOutput: Double = 0.0
    private var initialized: Boolean = false

    init {
        require(cutoffHz > 0.0) { "cutoffHz must be positive" }
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        val rc = 1.0 / (2.0 * PI * cutoffHz)
        val dt = 1.0 / sampleRateHz
        alpha = rc / (rc + dt)
    }

    fun process(sample: Double): Double {
        if (!initialized) {
            prevInput = sample
            prevOutput = 0.0
            initialized = true
            return 0.0
        }
        val output = alpha * (prevOutput + sample - prevInput)
        prevInput = sample
        prevOutput = output
        return output
    }

    fun reset() {
        prevInput = 0.0
        prevOutput = 0.0
        initialized = false
    }
}

/**
 * Fast-attack / smooth-release exponential moving average smoother matching
 * sound-level meter "Fast" visual ballistics.
 */
class DecibelSmoother(
    private val attackAlpha: Float = 0.55f,
    private val releaseAlpha: Float = 0.22f
) {
    private var current: Float? = null

    fun smooth(rawDb: Float): Float {
        val prev = current
        val next = if (prev == null) {
            rawDb
        } else {
            val alpha = if (rawDb >= prev) attackAlpha else releaseAlpha
            prev + alpha * (rawDb - prev)
        }
        current = next
        return next
    }

    fun reset() {
        current = null
    }
}

/**
 * Converts 16-bit PCM audio frames into approximate dB SPL values tuned for voice loudness.
 */
class DecibelCalculator(
    sampleRateHz: Int = 44_100,
    cutoffHz: Double = 85.0
) {
    private val highPassFilter = HighPassFilter(cutoffHz = cutoffHz, sampleRateHz = sampleRateHz)

    fun calculateDb(
        samples: ShortArray,
        length: Int = samples.size,
        calibrationOffsetDb: Float = 0f
    ): Float {
        if (samples.isEmpty() || length <= 0) return MIN_DB
        val count = minOf(length, samples.size)

        var sumSquares = 0.0
        for (i in 0 until count) {
            val normalized = samples[i] / PCM_16_MAX
            val filtered = highPassFilter.process(normalized)
            sumSquares += filtered * filtered
        }

        val rms = sqrt(sumSquares / count)
        if (rms < SILENCE_RMS_THRESHOLD) return MIN_DB

        val dbfs = 20.0 * log10(rms)
        val splDb = dbfs + DEFAULT_SPL_OFFSET_DB + calibrationOffsetDb
        return splDb.toFloat().coerceIn(MIN_DB, MAX_DB)
    }

    fun reset() {
        highPassFilter.reset()
    }

    companion object {
        const val PCM_16_MAX = 32768.0
        const val DEFAULT_SPL_OFFSET_DB = 96.0
        const val MIN_DB = 0f
        const val MAX_DB = 120f
        const val SILENCE_RMS_THRESHOLD = 1e-6
    }
}
