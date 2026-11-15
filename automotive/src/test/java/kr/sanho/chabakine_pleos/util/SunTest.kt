package kr.sanho.chabakine_pleos.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SunTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun assertNear(expected: LocalTime, actual: LocalTime?) {
        assertNotNull(actual)
        val diff = Duration.between(expected, actual).abs().toMinutes()
        assertTrue("예상 $expected, 계산 $actual", diff <= 3)
    }

    @Test
    fun 서울_하지_일출() = assertNear(LocalTime.of(5, 11), sunrise(LocalDate.of(2026, 6, 21), 37.5665, 126.9780, seoul))

    @Test
    fun 서울_가을_일출() = assertNear(LocalTime.of(6, 56), sunrise(LocalDate.of(2026, 10, 29), 37.5665, 126.9780, seoul))
}
