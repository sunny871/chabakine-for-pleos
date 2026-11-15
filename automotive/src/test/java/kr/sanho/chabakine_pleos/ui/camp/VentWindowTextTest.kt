package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.vehicle.CampPreset
import org.junit.Assert.assertEquals
import org.junit.Test

/** 05 차량 카드 "창문 환기" 값 */
class VentWindowTextTest {
    private val now = 1_000_000L

    @Test
    fun waitingShowsMinutesAndWidth() {
        assertEquals("23분 뒤 · 2 cm", ventWindowText(CampPreset(windowOpenCm = 2), now + 22 * 60_000 + 30_000, null, now))
        assertEquals("1분 뒤 · 2 cm", ventWindowText(CampPreset(windowOpenCm = 2), now + 5_000, null, now))
    }

    @Test
    fun ventingShowsCloseTime() {
        assertEquals("열림 · 2분 뒤 닫힘", ventWindowText(CampPreset(windowOpenCm = 2), null, now + 90_000, now))
    }

    @Test
    fun notifyOnlyAndStopped() {
        assertEquals("60분 뒤 알림", ventWindowText(CampPreset(windowOpenCm = null), now + 60 * 60_000, null, now))
        assertEquals("3 cm", ventWindowText(CampPreset(windowOpenCm = 3), null, null, now))
        assertEquals("알림만", ventWindowText(CampPreset(windowOpenCm = null), null, null, now))
        // 창문을 움직일 수 없는 차는 폭을 골랐어도 알림만
        assertEquals("10분 뒤 알림", ventWindowText(CampPreset(windowOpenCm = 2), now + 10 * 60_000, null, now, windowsMovable = false))
    }
}
