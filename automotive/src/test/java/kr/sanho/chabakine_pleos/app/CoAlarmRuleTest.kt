package kr.sanho.chabakine_pleos.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoAlarmRuleTest {

    /** [ppm]을 [fromMs]부터 [toMs]까지 0.5초마다 넣는다 */
    private fun feed(state: CoAlarmState, ppm: Int, fromMs: Long, toMs: Long): CoAlarmState {
        var s = state
        var t = fromMs
        while (t <= toMs) {
            s = nextCoAlarm(s, ppm, t)
            t += 500
        }
        return s
    }

    @Test
    fun levelBands() {
        assertEquals(CoLevel.NONE, CoLevel.of(49))
        assertEquals(CoLevel.CAUTION, CoLevel.of(50))
        assertEquals(CoLevel.WARNING, CoLevel.of(199))
        assertEquals(CoLevel.DANGER, CoLevel.of(200))
    }

    @Test
    fun levelChangesOnlyAfterFiveSeconds() {
        val s = feed(CoAlarmState(), 60, 0, 4_500)
        assertEquals(CoLevel.NONE, s.level)
        assertEquals(CoLevel.CAUTION, feed(s, 60, 5_000, 5_500).level)
    }

    @Test
    fun briefSpikeDoesNotChangeLevel() {
        var s = feed(CoAlarmState(), 60, 0, 6_000)
        s = feed(s, 150, 6_500, 9_000)
        s = feed(s, 60, 9_500, 20_000)
        assertEquals(CoLevel.CAUTION, s.level)
    }

    @Test
    fun clearsAfterTenSecondsBelow50() {
        var s = feed(CoAlarmState(), 250, 0, 6_000)
        assertEquals(CoLevel.DANGER, s.level)
        s = feed(s, 20, 6_500, 15_000)
        assertEquals(CoLevel.DANGER, s.level)
        s = feed(s, 20, 15_500, 17_000)
        assertEquals(CoLevel.NONE, s.level)
    }

    @Test
    fun dismissedRingsAgainWhenLevelRises() {
        var s = feed(CoAlarmState(), 60, 0, 6_000).copy(dismissed = true)
        assertFalse(s.ringing)
        s = feed(s, 150, 6_500, 12_000)
        assertEquals(CoLevel.WARNING, s.level)
        assertTrue(s.ringing)
    }

    @Test
    fun dismissedStaysQuietWhenLevelFalls() {
        var s = feed(CoAlarmState(), 150, 0, 6_000).copy(dismissed = true)
        s = feed(s, 60, 6_500, 12_000)
        assertEquals(CoLevel.CAUTION, s.level)
        assertFalse(s.ringing)
    }
}
