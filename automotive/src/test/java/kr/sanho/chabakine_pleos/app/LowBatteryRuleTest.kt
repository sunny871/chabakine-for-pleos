package kr.sanho.chabakine_pleos.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LowBatteryRuleTest {

    @Test
    fun aboveTwentyUsesTwenty() {
        assertEquals(20f, LowBatteryRule.threshold(72f))
        assertEquals(20f, LowBatteryRule.threshold(20f))
        assertTrue(LowBatteryRule.shouldAlert(72f, 19f))
        assertFalse(LowBatteryRule.shouldAlert(72f, 20f))
    }

    @Test
    fun alreadyBelowTwentyWaitsForTen() {
        // 15%로 잠들면 20%가 아니라 10% 아래로 내려갈 때 깨운다
        assertEquals(10f, LowBatteryRule.threshold(15f))
        assertFalse(LowBatteryRule.shouldAlert(15f, 15f))
        assertFalse(LowBatteryRule.shouldAlert(15f, 10f))
        assertTrue(LowBatteryRule.shouldAlert(15f, 9f))
    }

    @Test
    fun belowTenWaitsForFive() {
        assertEquals(5f, LowBatteryRule.threshold(8f))
        assertTrue(LowBatteryRule.shouldAlert(8f, 4f))
    }

    @Test
    fun belowFiveHasNoStep() {
        assertNull(LowBatteryRule.threshold(4f))
        assertFalse(LowBatteryRule.shouldAlert(4f, 1f))
    }

    @Test
    fun noStepNoteOnlyBelowFive() {
        assertEquals("배터리 4% · 5% 아래라 더 알리지 않아요", LowBatteryRule.noStepNote(4.6f))
        assertNull(LowBatteryRule.noStepNote(5f))
        assertNull(LowBatteryRule.noStepNote(72f))
        assertNull(LowBatteryRule.noStepNote(null))
    }

    @Test
    fun unknownStartUsesTwenty() {
        assertEquals(20f, LowBatteryRule.threshold(null))
        assertTrue(LowBatteryRule.shouldAlert(null, 18f))
        assertFalse(LowBatteryRule.shouldAlert(30f, null))
    }
}
