package app.lastcalled.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    @Test
    fun formatsElapsedTimeFromTheCurrentDeviceClock() {
        val now = 1_800_000_000_000L
        assertEquals("Just now", RelativeTime.format(now - 20_000, now))
        assertEquals("3 hours ago", RelativeTime.format(now - 3 * 3_600_000, now))
        assertEquals("2 days ago", RelativeTime.format(now - 2 * 86_400_000, now))
        assertEquals("18 days ago", RelativeTime.format(now - 18 * 86_400_000, now))
    }

    @Test
    fun futureTimestampsDoNotProduceNegativeDurations() {
        val now = 1_800_000_000_000L
        assertEquals("Just now", RelativeTime.format(now + 60_000, now))
    }
}
