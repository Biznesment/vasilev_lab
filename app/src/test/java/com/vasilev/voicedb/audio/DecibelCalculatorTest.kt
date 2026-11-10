package com.vasilev.voicedb.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class DecibelCalculatorTest {

    private fun generateSine(
        frequencyHz: Double,
        amplitudeFraction: Double,
        sampleRateHz: Int = 44_100,
        sampleCount: Int = 4_410
    ): ShortArray {
        val amplitude = amplitudeFraction * Short.MAX_VALUE
        return ShortArray(sampleCount) { i ->
            val angle = 2.0 * PI * frequencyHz * i / sampleRateHz
            (sin(angle) * amplitude).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    @Test
    fun emptyOrZeroSamples_returnsMinDb() {
        val calculator = DecibelCalculator()
        assertEquals(0f, calculator.calculateDb(ShortArray(0)), 0.001f)
        assertEquals(0f, calculator.calculateDb(ShortArray(2205)), 0.001f)
        assertEquals(0f, calculator.calculateDb(ShortArray(100), length = 0), 0.001f)
    }

    @Test
    fun constantDcOffset_isFilteredOutToSilence() {
        val calculator = DecibelCalculator()
        val dcSignal = ShortArray(2205) { 12_000 }
        val db = calculator.calculateDb(dcSignal)
        assertEquals(0f, db, 0.001f)
    }

    @Test
    fun voiceBandSineWave_doublesAmplitudeIncreasesByAbout6Db() {
        val calcLow = DecibelCalculator()
        val calcHigh = DecibelCalculator()

        // Run a short warmup so filter reaches steady state, then measure
        val quarter = generateSine(frequencyHz = 1000.0, amplitudeFraction = 0.25, sampleCount = 4410)
        val half = generateSine(frequencyHz = 1000.0, amplitudeFraction = 0.50, sampleCount = 4410)

        calcLow.calculateDb(quarter)
        val dbQuarter = calcLow.calculateDb(quarter)

        calcHigh.calculateDb(half)
        val dbHalf = calcHigh.calculateDb(half)

        val diff = dbHalf - dbQuarter
        assertEquals(6.02f, diff, 0.35f)
        assertTrue("Expected normal-to-loud dB range for 0.5 full scale, got $dbHalf", dbHalf in 80f..95f)
    }

    @Test
    fun lowFrequencyRumble_isStronglyAttenuatedComparedToVoice() {
        val rumbleCalc = DecibelCalculator(cutoffHz = 85.0)
        val voiceCalc = DecibelCalculator(cutoffHz = 85.0)

        val rumble20Hz = generateSine(frequencyHz = 20.0, amplitudeFraction = 0.5, sampleCount = 8820)
        val voice1kHz = generateSine(frequencyHz = 1000.0, amplitudeFraction = 0.5, sampleCount = 8820)

        val rumbleDb = rumbleCalc.calculateDb(rumble20Hz)
        val voiceDb = voiceCalc.calculateDb(voice1kHz)

        assertTrue(
            "20 Hz rumble ($rumbleDb dB) should be attenuated by >10 dB vs 1 kHz voice ($voiceDb dB)",
            voiceDb - rumbleDb > 10f
        )
    }

    @Test
    fun calibrationOffset_shiftsOutputAndClampsWithinBounds() {
        val samples = generateSine(frequencyHz = 1000.0, amplitudeFraction = 0.1, sampleCount = 4410)

        val baseCalc = DecibelCalculator()
        val baseDb = baseCalc.calculateDb(samples, calibrationOffsetDb = 0f)

        val plusCalc = DecibelCalculator()
        val plusDb = plusCalc.calculateDb(samples, calibrationOffsetDb = 10f)
        assertEquals(baseDb + 10f, plusDb, 0.05f)

        val clampedHigh = DecibelCalculator().calculateDb(samples, calibrationOffsetDb = 200f)
        assertEquals(DecibelCalculator.MAX_DB, clampedHigh, 0.001f)

        val clampedLow = DecibelCalculator().calculateDb(samples, calibrationOffsetDb = -200f)
        assertEquals(DecibelCalculator.MIN_DB, clampedLow, 0.001f)
    }

    @Test
    fun decibelSmoother_initializesImmediatelyAndAttacksFasterThanRelease() {
        val smoother = DecibelSmoother(attackAlpha = 0.6f, releaseAlpha = 0.2f)

        assertEquals(50f, smoother.smooth(50f), 0.001f)

        // Attack from 50 -> 70: 50 + 0.6 * 20 = 62
        val afterAttack = smoother.smooth(70f)
        assertEquals(62f, afterAttack, 0.001f)

        // Release from 62 -> 42: 62 + 0.2 * (-20) = 58
        val afterRelease = smoother.smooth(42f)
        assertEquals(58f, afterRelease, 0.001f)

        smoother.reset()
        assertEquals(30f, smoother.smooth(30f), 0.001f)
    }

    @Test
    fun voiceLevel_mapsThresholdsCorrectly() {
        assertEquals(VoiceLevel.SILENCE, VoiceLevel.fromDb(0f))
        assertEquals(VoiceLevel.SILENCE, VoiceLevel.fromDb(34.9f))
        assertEquals(VoiceLevel.QUIET, VoiceLevel.fromDb(35f))
        assertEquals(VoiceLevel.QUIET, VoiceLevel.fromDb(49.9f))
        assertEquals(VoiceLevel.NORMAL, VoiceLevel.fromDb(50f))
        assertEquals(VoiceLevel.NORMAL, VoiceLevel.fromDb(69.9f))
        assertEquals(VoiceLevel.LOUD, VoiceLevel.fromDb(70f))
        assertEquals(VoiceLevel.LOUD, VoiceLevel.fromDb(84.9f))
        assertEquals(VoiceLevel.VERY_LOUD, VoiceLevel.fromDb(85f))
        assertEquals(VoiceLevel.VERY_LOUD, VoiceLevel.fromDb(110f))
    }
}
