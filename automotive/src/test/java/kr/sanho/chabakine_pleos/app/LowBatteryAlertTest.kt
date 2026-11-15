package kr.sanho.chabakine_pleos.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LowBatteryAlertTest {
    private val hour = 3_600_000L

    @Test
    fun 줄어든_양과_10퍼센트까지_어림() {
        // 3시간에 24% → 18%: 시간당 2%, 10%까지 8% → 4시간
        val a = LowBatteryAlert(percent = 18, startPercent = 24f, startMs = 0, atMs = 3 * hour)
        assertEquals("잠든 뒤 3시간 동안 6% 줄었어요 · 이대로면 약 4시간 뒤 10%", a.drainText)
    }

    @Test
    fun 한_시간이_안_되면_분으로() {
        val a = LowBatteryAlert(percent = 19, startPercent = 21f, startMs = 0, atMs = 30 * 60_000L)
        assertEquals("잠든 뒤 30분 동안 2% 줄었어요 · 이대로면 약 2시간 뒤 10%", a.drainText)
    }

    @Test
    fun 잠든_지_15분이_안_됐거나_줄지_않았으면_없음() {
        assertNull(LowBatteryAlert(18, 20f, 0, 10 * 60_000L).drainText)
        assertNull(LowBatteryAlert(18, 18f, 0, 2 * hour).drainText)
        assertNull(LowBatteryAlert(18, null, null, 2 * hour).drainText)
    }

    @Test
    fun 이미_10퍼센트_이하면_어림_시간은_뺀다() {
        val a = LowBatteryAlert(percent = 9, startPercent = 15f, startMs = 0, atMs = 2 * hour)
        assertEquals("잠든 뒤 2시간 동안 6% 줄었어요", a.drainText)
    }
}
