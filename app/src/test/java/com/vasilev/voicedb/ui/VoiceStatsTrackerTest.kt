package com.vasilev.voicedb.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceStatsTrackerTest {

    @Test
    fun record_updatesMinAvgMaxAndHistory() {
        val tracker = VoiceStatsTracker(maxHistorySize = 10)

        val s1 = tracker.record(40f)
        assertEquals(40f, s1.currentDb, 0.001f)
        assertEquals(40f, s1.minDb!!, 0.001f)
        assertEquals(40f, s1.avgDb!!, 0.001f)
        assertEquals(40f, s1.maxDb!!, 0.001f)
        assertEquals(listOf(40f), s1.history)

        tracker.record(60f)
        val s3 = tracker.record(80f)
        assertEquals(80f, s3.currentDb, 0.001f)
        assertEquals(40f, s3.minDb!!, 0.001f)
        assertEquals(60f, s3.avgDb!!, 0.001f)
        assertEquals(80f, s3.maxDb!!, 0.001f)
        assertEquals(listOf(40f, 60f, 80f), s3.history)
    }

    @Test
    fun record_respectsMaxHistoryWindowSize() {
        val tracker = VoiceStatsTracker(maxHistorySize = 3)

        tracker.record(30f)
        tracker.record(40f)
        tracker.record(50f)
        val s4 = tracker.record(60f)

        assertEquals(listOf(40f, 50f, 60f), s4.history)
        // Session min/max/avg still track the whole session
        assertEquals(30f, s4.minDb!!, 0.001f)
        assertEquals(60f, s4.maxDb!!, 0.001f)
        assertEquals(45f, s4.avgDb!!, 0.001f)
    }

    @Test
    fun reset_clearsStatisticsAndHistory() {
        val tracker = VoiceStatsTracker(maxHistorySize = 5)
        tracker.record(55f)
        tracker.record(75f)

        val empty = tracker.reset()
        assertEquals(0f, empty.currentDb, 0.001f)
        assertNull(empty.minDb)
        assertNull(empty.avgDb)
        assertNull(empty.maxDb)
        assertTrue(empty.history.isEmpty())
    }
}
