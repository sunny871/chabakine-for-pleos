package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.app.SleepAlerts
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WakeAlarmTest {

    @Test
    fun 기본은_오전_7시() = assertEquals("오전 7:00", SleepAlerts().wakeLabel)

    @Test
    fun 시각_옮기기는_하루를_돈다() {
        assertEquals("오후 11:55", SleepAlerts(wakeMinutes = 0).shiftWake(-5).wakeLabel)
        assertEquals("오전 12:00", SleepAlerts(wakeMinutes = 23 * 60).shiftWake(60).wakeLabel)
        assertEquals("오후 12:30", SleepAlerts(wakeMinutes = 12 * 60 + 25).shiftWake(5).wakeLabel)
    }

    @Test
    fun 남은_시간은_지났으면_다음날() {
        assertEquals(30 * 60_000L, millisUntil(7 * 60, LocalTime.of(6, 30)))
        assertEquals(23 * 3600_000L, millisUntil(7 * 60, LocalTime.of(8, 0)))
    }

    @Test
    fun 기본_소리와_다시_알림() {
        assertEquals(false, SleepAlerts().wakeVoiceOnly)
        assertEquals(5, SleepAlerts().snoozeMinutes)
    }

    @Test
    fun 시각_정하기는_하루_안으로() {
        assertEquals("오전 6:30", SleepAlerts().wakeAt(6 * 60 + 30).wakeLabel)
        assertEquals("오전 7:25", SleepAlerts().wakeAt(24 * 60 + 7 * 60 + 25).wakeLabel)
    }

    @Test
    fun 남은_시간_글자() {
        assertEquals("8시간 50분 뒤", untilText(7 * 60, LocalTime.of(22, 10)))
        assertEquals("30분 뒤", untilText(7 * 60, LocalTime.of(6, 30)))
        assertEquals("2시간 뒤", untilText(7 * 60, LocalTime.of(5, 0)))
    }

    @Test
    fun 다음_아침은_정오_전이면_오늘() {
        assertEquals(LocalDate.of(2026, 11, 5), nextMorning(LocalDateTime.of(2026, 11, 5, 3, 0)))
        assertEquals(LocalDate.of(2026, 11, 6), nextMorning(LocalDateTime.of(2026, 11, 5, 23, 0)))
    }

    @Test
    fun 빠른_시각은_일출을_5분_단위로_내린다() {
        val chips = quickTimes(LocalTime.of(6, 57))
        assertEquals("일출 6:55" to 6 * 60 + 55, chips[0])
        assertEquals("일출 30분 뒤" to 7 * 60 + 25, chips[1])
        assertEquals(listOf("6:30", "7:00", "8:00"), quickTimes(null).map { it.first })
        // 일출이 7:00이면 "7:00" 칩은 빠진다
        assertEquals(listOf("일출 7:00", "일출 30분 뒤", "6:30", "8:00"), quickTimes(LocalTime.of(7, 2)).map { it.first })
    }
}
