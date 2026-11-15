package kr.sanho.chabakine_pleos.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 23 공조 자동 온도와 창문 여는 폭(cm → 위치 %) */
class CampPresetTest {

    @Test
    fun comfortTemperatureFollowsOutside() {
        assertEquals(22f, CampPreset.comfortTemperature(null))
        assertEquals(24f, CampPreset.comfortTemperature(-3f))
        assertEquals(23f, CampPreset.comfortTemperature(10f))
        assertEquals(22f, CampPreset.comfortTemperature(25f))
        assertEquals(20f, CampPreset.comfortTemperature(30f))
    }

    @Test
    fun windowWidthToPercent() {
        assertEquals(4, CampPreset(windowOpenCm = 2).windowOpenPercent)
        assertEquals(2, CampPreset(windowOpenCm = 1).windowOpenPercent)
        assertNull(CampPreset(windowOpenCm = null).windowOpenPercent)
    }
}
