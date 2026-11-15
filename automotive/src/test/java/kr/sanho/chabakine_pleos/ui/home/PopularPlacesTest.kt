package kr.sanho.chabakine_pleos.ui.home

import kr.sanho.chabakine_pleos.shared.place.Amenities
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Rating
import kr.sanho.chabakine_pleos.shared.place.RuleSignal
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import org.junit.Assert.assertEquals
import org.junit.Test

class PopularPlacesTest {

    private fun item(id: String, signal: Signal, rating: Rating?, km: Double = 5.0) = PlaceItem(
        Place(
            id = id, name = id, region = "강원 강릉", latitude = 0.0, longitude = 0.0, signal = signal, reason = "",
            stay = mapOf(StayRule.CAR to RuleSignal(signal, "")), evidence = emptyList(), site = null,
            amenities = Amenities(), photoUrls = emptyList(), sourceName = null, verifiedAt = null,
            confidence = 1, verification = null, seasonRules = emptyList(), isDemo = false,
        ),
        signal, km, rating,
    )

    @Test
    fun moreReviewsFirstThenHigherRatingAndNoRedOrUnrated() {
        val items = listOf(
            item("a", Signal.GREEN, Rating(1, 5.0)),
            item("b", Signal.YELLOW, Rating(3, 4.0)),
            item("c", Signal.GREEN, Rating(3, 4.7)),
            item("red", Signal.RED, Rating(9, 5.0)),
            item("none", Signal.GREEN, null),
        )
        assertEquals(listOf("c", "b", "a"), popularPlaces(items).map { it.place.id })
    }
}
