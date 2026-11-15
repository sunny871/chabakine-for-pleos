package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.shared.place.Amenities
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.RuleSignal
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampPromptRuleTest {

    private fun place(id: String, lat: Double, lng: Double) = Place(
        id = id, name = id, region = "강원 강릉", latitude = lat, longitude = lng, signal = Signal.GREEN, reason = "",
        stay = mapOf(StayRule.CAR to RuleSignal(Signal.GREEN, "")), evidence = emptyList(), site = null,
        amenities = Amenities(), photoUrls = emptyList(), sourceName = null, verifiedAt = null,
        confidence = 1, verification = null, seasonRules = emptyList(), isDemo = false,
    )

    // 남항진해변 근처 두 곳과 20 km 떨어진 휴게소 자리
    private val beach = place("beach", 37.7700, 128.9500)
    private val nearBeach = place("near", 37.7740, 128.9520)
    private val far = place("far", 37.9000, 128.8200)
    private val places = listOf(beach, nearBeach, far)

    @Test
    fun restAreaFarFromAnyPlaceDoesNotAsk() =
        assertNull(campPromptPlace(places, LatLng(37.60, 129.05), arrivalPlace = beach, arrivedByNavi = false))

    @Test
    fun nearGuidedDestinationAsksForIt() =
        assertEquals(beach, campPromptPlace(places, LatLng(37.7705, 128.9505), arrivalPlace = beach, arrivedByNavi = false))

    @Test
    fun withoutGuidanceAsksNearestWithinRadius() =
        assertEquals(nearBeach, campPromptPlace(places, LatLng(37.7738, 128.9519), arrivalPlace = null, arrivedByNavi = false))

    @Test
    fun guidedDestinationFarButOtherPlaceNearAsksTheNearOne() =
        assertEquals(nearBeach, campPromptPlace(places, LatLng(37.7741, 128.9521), arrivalPlace = far, arrivedByNavi = false))

    @Test
    fun unknownLocationAsksOnlyWhenNaviSaidArrived() {
        assertNull(campPromptPlace(places, null, arrivalPlace = beach, arrivedByNavi = false))
        assertEquals(beach, campPromptPlace(places, null, arrivalPlace = beach, arrivedByNavi = true))
        assertNull(campPromptPlace(places, null, arrivalPlace = null, arrivedByNavi = true))
    }

    @Test
    fun speechAsksAndExplainsHowToAnswer() {
        assertEquals(
            "육백마지기에 도착했어요. 차박 모드를 켤까요? 마이크를 누르고 \"켜 줘\" 또는 \"나중에\"라고 답해 주세요.",
            campPromptSpeech("육백마지기", arrived = true, canAnswer = true),
        )
        assertEquals("남항진해변 근처에 주차했어요. 차박 모드를 켤까요?", campPromptSpeech("남항진해변", arrived = false, canAnswer = false))
    }
}
