package kr.sanho.chabakine_pleos.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class BatteryForecastTest {
    private val korea = ZoneId.of("Asia/Seoul")
    private fun at(h: Int, m: Int = 0) = ZonedDateTime.of(2026, 11, 10, h, m, 0, 0, korea)

    @Test
    fun 와트를_용량으로_퍼센트로() {
        // 77 kWh 차에서 1 kW → 시간당 약 1.3%
        assertEquals(1.2987f, BatteryForecast.percentPerHour(1000f, 77_000f), 0.001f)
        assertEquals(0f, BatteryForecast.percentPerHour(1000f, 0f), 0f)
    }

    @Test
    fun 오전_7시까지_남은_시간() {
        assertEquals(9f, BatteryForecast.hoursUntil(at(22)), 0.001f)
        assertEquals(8f, BatteryForecast.hoursUntil(at(23)), 0.001f)
        // 자정 넘으면 그날 7시
        assertEquals(5f, BatteryForecast.hoursUntil(at(2)), 0.001f)
        // 7시가 지났으면 다음 날 7시
        assertEquals(23.5f, BatteryForecast.hoursUntil(at(7, 30)), 0.001f)
        assertEquals(24f, BatteryForecast.hoursUntil(at(7)), 0.001f)
    }

    @Test
    fun 공조는_온도_차가_클수록_많이() {
        val p = CampPreset(autoClimate = false, temperatureC = 22f)
        val mild = BatteryForecast.climateWatts(p, 20f)
        val cold = BatteryForecast.climateWatts(p, 0f)
        assertTrue(cold > mild)
        assertEquals(BatteryForecast.CLIMATE_UNKNOWN_W, BatteryForecast.climateWatts(p, null), 0f)
        assertEquals(BatteryForecast.CLIMATE_UNKNOWN_W, BatteryForecast.climateWatts(null, 10f), 0f)
    }

    @Test
    fun 환기는_간격이_짧고_폭이_넓을수록_많이() {
        val cap = 77_000f
        val v30 = BatteryForecast.model(cap, CampPreset(ventIntervalMin = 30), 10f).vent
        val v60 = BatteryForecast.model(cap, CampPreset(ventIntervalMin = 60), 10f).vent
        val wide = BatteryForecast.model(cap, CampPreset(ventIntervalMin = 60, windowOpenCm = 3), 10f).vent
        val none = BatteryForecast.model(cap, CampPreset(windowOpenCm = null), 10f).vent
        assertTrue(v30 > v60)
        assertTrue(wide > v60)
        assertEquals(0f, none, 0f)
    }

    @Test
    fun 실측은_30분과_0점5퍼센트가_넘어야() {
        val h = 3_600_000L
        assertNull(BatteryForecast.measuredRate(80f, 0, 79f, 20 * 60_000L))
        assertNull(BatteryForecast.measuredRate(80f, 0, 79.8f, 2 * h))
        assertNull(BatteryForecast.measuredRate(null, null, 70f, 2 * h))
        assertEquals(2f, BatteryForecast.measuredRate(80f, 0, 76f, 2 * h)!!, 0.001f)
    }

    @Test
    fun 실측_기록_어림_순서로_고른다() {
        val m = BatteryForecast.forecast(72f, 9f, measured = 2f, history = HistoryRate(1f, 3), model = 1.5f)
        assertEquals(RateSource.MEASURED, m.source)
        assertEquals(18, m.usePercent)
        assertEquals(54, m.leftPercent)
        val h = BatteryForecast.forecast(72f, 9f, measured = null, history = HistoryRate(1f, 3), model = 1.5f)
        assertEquals(RateSource.HISTORY, h.source)
        assertEquals(3, h.nights)
        assertEquals(63, h.leftPercent)
        val e = BatteryForecast.forecast(72f, 9f, measured = null, history = null, model = 1.6f)
        assertEquals(RateSource.ESTIMATE, e.source)
        assertEquals(14, e.usePercent)
        assertEquals(58, e.leftPercent)
    }

    @Test
    fun 영퍼센트_아래로_내려가지_않는다() {
        val f = BatteryForecast.forecast(10f, 9f, measured = 3f, history = null, model = 1f)
        assertEquals(10, f.usePercent)
        assertEquals(0, f.leftPercent)
    }

    @Test
    fun 온도_구간() {
        assertEquals(5, BatteryForecast.tempBand(7f))
        assertEquals(-5, BatteryForecast.tempBand(-3f))
        assertEquals(0, BatteryForecast.tempBand(0f))
        assertNull(BatteryForecast.tempBand(null))
    }
}
