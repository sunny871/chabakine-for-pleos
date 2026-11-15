package kr.sanho.chabakine_pleos.data

import kr.sanho.chabakine_pleos.shared.place.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FestivalOngoingTest {

    private fun f(start: String, end: String) = Festival("x", "축제", start, end, LatLng(37.0, 127.0))
    private val today = LocalDate.of(2026, 11, 15)

    @Test
    fun ongoingIncludesFirstAndLastDay() {
        assertTrue(f("2026-10-31", "2026-12-31").isOngoing(today))
        assertTrue(f("2026-11-15", "2026-11-15").isOngoing(today))
    }

    @Test
    fun notStartedOrEndedIsHidden() {
        // QA-11: 12-11 시작 축제는 11-15에 보이지 않는다
        assertFalse(f("2026-12-11", "2027-01-31").isOngoing(today))
        assertFalse(f("2026-10-01", "2026-11-14").isOngoing(today))
    }

    @Test
    fun badDateIsHidden() {
        assertFalse(f("", "2026-12-31").isOngoing(today))
    }
}
