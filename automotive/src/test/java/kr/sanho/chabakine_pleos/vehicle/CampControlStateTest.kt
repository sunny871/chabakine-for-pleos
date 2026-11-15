package kr.sanho.chabakine_pleos.vehicle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampControlStateTest {

    @Test
    fun linkedNeedsSdkAndPermission() {
        assertFalse(CampControlState(ready = true, approved = false).linked)
        assertFalse(CampControlState(ready = false, approved = true).linked)
        assertTrue(CampControlState(ready = true, approved = true).linked)
    }

    @Test
    fun appliedStates() {
        val idle = CampControlState()
        assertTrue(idle.notApplied)
        assertFalse(idle.allDone)

        val done = CampControlState(lights = ControlResult.Done, climate = ControlResult.Done, windows = ControlResult.Done, seat = ControlResult.Done)
        assertFalse(done.notApplied)
        assertTrue(done.allDone)
        // 앞좌석도 23에서 맞추는 항목이라 끝나야 모두 적용이다
        assertFalse(done.copy(seat = ControlResult.Idle).allDone)
        assertTrue(done.copy(seat = ControlResult.Working).working)

        val partial = done.copy(windows = ControlResult.Unsupported("창문"))
        assertFalse(partial.allDone)
        assertFalse(partial.working)
        assertTrue(done.copy(climate = ControlResult.Working).working)
    }
}
