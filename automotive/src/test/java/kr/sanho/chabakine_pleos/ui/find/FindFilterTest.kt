package kr.sanho.chabakine_pleos.ui.find

import kr.sanho.chabakine_pleos.shared.place.Amenities
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.RuleSignal
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.Site
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FindFilterTest {

    private fun item(
        id: String,
        signal: Signal,
        km: Double = 5.0,
        toilet: Boolean? = null,
        region: String = "강원 강릉",
        evidence: List<String> = emptyList(),
        site: Site? = null,
    ) = PlaceItem(
        Place(
            id = id, name = id, region = region, latitude = 0.0, longitude = 0.0, signal = signal, reason = "",
            stay = mapOf(StayRule.CAR to RuleSignal(signal, "")), evidence = evidence, site = site,
            amenities = Amenities(toilet = toilet), photoUrls = emptyList(), sourceName = null, verifiedAt = null,
            confidence = 1, verification = null, seasonRules = emptyList(), isDemo = false,
        ),
        signal, km, null,
    )

    private val items = listOf(
        item("KR-42-GN-001", Signal.GREEN, km = 3.0, toilet = true, evidence = listOf("공식 안내")),
        item("KR-42-GN-002", Signal.YELLOW, km = 12.0),
        item("KR-42-GN-003", Signal.RED, km = 8.0, region = "강원 속초"),
        item("KR-42-GN-004", Signal.UNKNOWN, km = 40.0, site = Site(heightLimitKnown = true)),
    )

    private fun ids(f: FindFilter, favorites: Set<String> = emptySet()) =
        items.applyFilter(f, favorites).map { it.place.id.takeLast(1) }

    @Test
    fun defaultShowsAllowedAndConditionalOnly() = assertEquals(listOf("1", "2"), ids(FindFilter()))

    @Test
    fun allowedOnlyHidesRedToo() = assertEquals(listOf("1"), ids(FindFilter().withAllowedOnly()))

    @Test
    fun allShowsUnknownAndRed() =
        assertEquals(listOf("1", "2", "3", "4"), ids(FindFilter().withSignal(SignalScope.ALL)))

    @Test
    fun allowedOnlyChipIsOffWhenRedShownSeparately() {
        // 22에서 "금지 보기"를 켜면 더 이상 "허용만"이 아니다
        assertEquals(true, FindFilter().withAllowedOnly().allowedOnly)
        assertEquals(false, FindFilter().withAllowedOnly().copy(red = true).allowedOnly)
    }

    @Test
    fun distanceAmenityAndOfficial() {
        assertEquals(listOf("1", "3"), ids(FindFilter(maxDistanceKm = 10).withSignal(SignalScope.ALL)))
        assertEquals(listOf("1"), ids(FindFilter(toilet = true)))
        assertEquals(listOf("1"), ids(FindFilter(officialOnly = true)))
        assertEquals(listOf("4"), ids(FindFilter(noHeightLimit = true).withSignal(SignalScope.ALL)))
        assertEquals(listOf("2"), ids(FindFilter(favoritesOnly = true), setOf("KR-42-GN-002")))
    }

    @Test
    fun queryKeepsOnlyMatchingNameOrRegion() {
        assertEquals(listOf("3"), ids(FindFilter(query = "속초").withSignal(SignalScope.ALL)))
        assertEquals(listOf("2"), ids(FindFilter(query = "GN- 002")))
        assertEquals(listOf("1", "2"), ids(FindFilter(query = " ")))
    }

    @Test
    fun signalScopeRoundTripAndCounts() {
        SignalScope.entries.forEach { assertEquals(it, FindFilter().withSignal(it).signalScope) }
        assertEquals(false, FindFilter().anyOn)
        assertEquals(true, FindFilter(toilet = true).anyOn)
        assertEquals(2, FindFilter(maxDistanceKm = 30, water = true, toilet = true).moreCount)
        assertEquals("속초", FindFilter(query = "속초", toilet = true).reset().query)
    }
}
