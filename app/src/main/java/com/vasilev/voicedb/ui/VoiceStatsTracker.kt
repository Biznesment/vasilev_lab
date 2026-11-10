package com.vasilev.voicedb.ui

/**
 * Accumulates rolling voice loudness history and session summary statistics (min/avg/max).
 */
class VoiceStatsTracker(
    private val maxHistorySize: Int = DEFAULT_HISTORY_SIZE
) {
    private val samples = ArrayDeque<Float>(maxHistorySize)
    private var minDb: Float? = null
    private var maxDb: Float? = null
    private var sumDb: Double = 0.0
    private var totalCount: Long = 0L

    init {
        require(maxHistorySize > 0) { "maxHistorySize must be positive" }
    }

    data class Snapshot(
        val currentDb: Float,
        val minDb: Float?,
        val avgDb: Float?,
        val maxDb: Float?,
        val history: List<Float>
    )

    fun record(db: Float): Snapshot {
        if (samples.size >= maxHistorySize) {
            samples.removeFirst()
        }
        samples.addLast(db)

        minDb = minDb?.let { minOf(it, db) } ?: db
        maxDb = maxDb?.let { maxOf(it, db) } ?: db
        sumDb += db
        totalCount += 1L

        val avgDb = (sumDb / totalCount).toFloat()
        return Snapshot(
            currentDb = db,
            minDb = minDb,
            avgDb = avgDb,
            maxDb = maxDb,
            history = samples.toList()
        )
    }

    fun reset(): Snapshot {
        samples.clear()
        minDb = null
        maxDb = null
        sumDb = 0.0
        totalCount = 0L
        return Snapshot(
            currentDb = 0f,
            minDb = null,
            avgDb = null,
            maxDb = null,
            history = emptyList()
        )
    }

    companion object {
        const val DEFAULT_HISTORY_SIZE = 300 // 15 seconds at 20 Hz
    }
}
