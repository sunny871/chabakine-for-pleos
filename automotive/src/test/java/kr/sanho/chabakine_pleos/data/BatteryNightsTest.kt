package kr.sanho.chabakine_pleos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryNightsTest {
    private val min = 60_000L
    private fun s(m: Int, pct: Float, temp: Float? = 8f, climate: Boolean = true) = BatterySample(m * min, pct, temp, climate)

    @Test
    fun 한_밤을_시간당_퍼센트로() {
        // 4시간에 80 → 72: 시간당 2%
        val n = BatteryNights.summarize((0..24).map { s(it * 10, 80f - it / 3f) })!!
        assertEquals(4f, n.hours, 0.001f)
        assertEquals(2f, n.ratePerHour, 0.01f)
        assertEquals(5, n.tempBand)
        assertEquals(true, n.climateOn)
    }

    @Test
    fun 두_시간보다_짧으면_버린다() {
        assertNull(BatteryNights.summarize((0..10).map { s(it * 10, 80f - it * 0.2f) }))
        assertNull(BatteryNights.summarize(listOf(s(0, 80f))))
    }

    @Test
    fun 충전으로_늘어난_곳에서_나누고_긴_쪽만() {
        // 0~60분 80→78, 충전으로 90, 70~250분 90→84
        val a = (0..6).map { s(it * 10, 80f - it / 3f) }
        val b = (7..25).map { s(it * 10, 90f - (it - 7) / 3f) }
        val n = BatteryNights.summarize(a + b)!!
        assertEquals(3f, n.hours, 0.001f)
        assertEquals(2f, n.ratePerHour, 0.01f)
    }

    @Test
    fun 끊긴_곳에서_나눈다() {
        // 2시간 기록 뒤 3시간 끊김(앱 꺼짐), 다시 30분
        val a = (0..12).map { s(it * 10, 80f - it * 0.2f) }
        val b = (0..3).map { s(300 + it * 10, 70f - it * 0.2f) }
        val n = BatteryNights.summarize(a + b)!!
        assertEquals(2f, n.hours, 0.001f)
    }

    @Test
    fun 같은_조건_2밤부터_중앙값() {
        val one = listOf(NightSummary(1, 5, true, 8f, 1.2f))
        assertNull(BatteryNights.historyRate(one, 5, true))
        val three = one + NightSummary(2, 5, true, 8f, 1.6f) + NightSummary(3, 5, true, 8f, 2.0f) + NightSummary(4, 0, true, 8f, 9f) + NightSummary(5, 5, false, 8f, 9f)
        val r = BatteryNights.historyRate(three, 5, true)!!
        assertEquals(1.6f, r.ratePerHour, 0.001f)
        assertEquals(3, r.nights)
        val two = BatteryNights.historyRate(one + NightSummary(2, 5, true, 8f, 1.6f), 5, true)!!
        assertEquals(1.4f, two.ratePerHour, 0.001f)
    }

    @Test
    fun 최근_30밤만() {
        var list = emptyList<NightSummary>()
        repeat(35) { list = BatteryNights.add(list, NightSummary(it.toLong(), 5, true, 8f, 1f)) }
        assertEquals(30, list.size)
        assertEquals(34L, list.first().endMs)
    }

    @Test
    fun 저장_형식_되읽기() {
        val sample = BatterySample(123, 71.5f, null, false)
        assertEquals(sample, BatteryNights.decodeSample(BatteryNights.encodeSample(sample)))
        val night = NightSummary(9, -5, true, 7.5f, 1.3f)
        assertEquals(night, BatteryNights.decodeNight(BatteryNights.encodeNight(night)))
        assertNull(BatteryNights.decodeNight("깨진,줄"))
    }
}
