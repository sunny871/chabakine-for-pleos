package kr.sanho.chabakine_pleos.shared.place

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaceParserTest {

    private val legacy = """
        [{ "id": "KR-42-GN-013", "name": "사천해변", "region": "강원 강릉",
           "latitude": 37.82, "longitude": 128.87, "signal": "red", "reason": "현수막",
           "amenities": { "toilet": true, "water": null }, "confidence": 2 }]
    """.trimIndent()

    private val newFormat = """
        [{ "id": "KR-42-GN-017", "name": "남항진해변", "latitude": 37.75, "longitude": 128.95,
           "signal": "yellow",
           "stay": { "car": { "signal": "yellow", "reason": "1박만" },
                     "tent": { "signal": "red", "reason": "단속" },
                     "cook": { "signal": "unknown", "reason": "" } },
           "evidence": ["공식 안내"],
           "site": { "type": "해변 주차장", "heightLimit": { "state": "limited", "meters": 2.1 } },
           "seasonRules": ["7~8월 단속 강화"], "notes": ["텐트 적발 시 과태료", "밤 10시 이후 소음 주의"],
           "demo": true }]
    """.trimIndent()

    @Test
    fun legacyFormatTreatsSignalAsCarAndTentUnknown() {
        val place = PlaceParser.parse(legacy).single()
        assertEquals(Signal.RED, place.signalFor(CampingStyle.STEALTH))
        assertEquals(Signal.UNKNOWN, place.signalFor(CampingStyle.DOCKING))
        assertEquals(Signal.UNKNOWN, place.rule(StayRule.COOK).signal)
        assertEquals(true, place.amenities.toilet)
        assertNull(place.amenities.water)
    }

    @Test
    fun newFormatReadsStayAndSite() {
        val place = PlaceParser.parse(newFormat).single()
        assertEquals(Signal.YELLOW, place.signalFor(CampingStyle.STEALTH))
        assertEquals(Signal.RED, place.signalFor(CampingStyle.ROOFTOP))
        assertEquals("1박만", place.reasonFor(CampingStyle.STEALTH))
        assertEquals(2.1, place.site?.heightLimitMeters ?: 0.0, 0.001)
        assertTrue(place.isDemo)
    }

    @Test
    fun seasonRulesAndNotes() {
        val place = PlaceParser.parse(newFormat).single()
        assertEquals(listOf("7~8월 단속 강화"), place.seasonRules)
        assertEquals(listOf("텐트 적발 시 과태료", "밤 10시 이후 소음 주의"), place.notes)
        assertEquals(emptyList<String>(), PlaceParser.parse(legacy).single().notes)
    }

    /** 2026-11-04 이후 서버 형식: 도킹형 · 루프탑형이 따로 오고, `tent`는 옛 앱용으로 더 엄격한 쪽 */
    private val split = """
        [{ "id": "KR-42-PC-001", "name": "육백마지기", "latitude": 37.40, "longitude": 128.51,
           "signal": "yellow",
           "stay": { "car": { "signal": "yellow", "reason": "잠만 자는 스텔스" },
                     "docking": { "signal": "red", "reason": "야영 금지" },
                     "rooftop": { "signal": "green", "reason": "높이 제한 없음" },
                     "tent": { "signal": "red", "reason": "도킹형 금지(야영 금지) · 루프탑형 허용(높이 제한 없음)" },
                     "cook": { "signal": "red", "reason": "취사 금지" } } }]
    """.trimIndent()

    /** 나누기 전에 받아 둔 데이터: `tent`만 있다 */
    private val tentOnly = """
        [{ "id": "KR-42-GN-020", "name": "옛 데이터", "latitude": 37.7, "longitude": 128.9,
           "signal": "green",
           "stay": { "car": { "signal": "green", "reason": "허용" },
                     "tent": { "signal": "yellow", "reason": "주차면 안에서만" } } }]
    """.trimIndent()

    @Test
    fun dockingAndRooftopAreReadSeparately() {
        val place = PlaceParser.parse(split).single()
        assertEquals(Signal.RED, place.signalFor(CampingStyle.DOCKING))
        assertEquals("야영 금지", place.reasonFor(CampingStyle.DOCKING))
        assertEquals(Signal.GREEN, place.signalFor(CampingStyle.ROOFTOP))
        assertEquals("높이 제한 없음", place.reasonFor(CampingStyle.ROOFTOP))
        // 스텔스는 car 그대로, tent는 방식 신호에 섞이지 않는다
        assertEquals(Signal.YELLOW, place.signalFor(CampingStyle.STEALTH))
        assertEquals("잠만 자는 스텔스", place.reasonFor(CampingStyle.STEALTH))
    }

    @Test
    fun tentOnlyFallsBackForBothStyles() {
        val place = PlaceParser.parse(tentOnly).single()
        for (style in listOf(CampingStyle.DOCKING, CampingStyle.ROOFTOP)) {
            assertEquals(Signal.YELLOW, place.signalFor(style))
            assertEquals("주차면 안에서만", place.reasonFor(style))
        }
        assertEquals(Signal.GREEN, place.signalFor(CampingStyle.STEALTH))
    }

    @Test
    fun unknownDockingDoesNotBorrowStricterTent() {
        // docking이 unknown이면 미확인이다. tent(루프탑 금지가 섞인 값)로 돌아가지 않는다
        val json = """
            [{ "id": "KR-42-GN-021", "name": "x", "latitude": 37.7, "longitude": 128.9, "signal": "green",
               "stay": { "car": { "signal": "green", "reason": "" },
                         "docking": { "signal": "unknown", "reason": "" },
                         "rooftop": { "signal": "red", "reason": "높이 제한 1.9m" },
                         "tent": { "signal": "red", "reason": "도킹형 미확인 · 루프탑형 금지" } } }]
        """.trimIndent()
        val place = PlaceParser.parse(json).single()
        assertEquals(Signal.UNKNOWN, place.signalFor(CampingStyle.DOCKING))
        assertEquals(Signal.RED, place.signalFor(CampingStyle.ROOFTOP))
    }

    @Test
    fun unknownStyleSignalValueIsUnknown() {
        val json = split.replace("\"signal\": \"green\", \"reason\": \"높이 제한 없음\"", "\"signal\": \"maybe\", \"reason\": \"높이 제한 없음\"")
        val place = PlaceParser.parse(json).single()
        assertEquals(Signal.UNKNOWN, place.signalFor(CampingStyle.ROOFTOP))
        assertEquals(Signal.RED, place.signalFor(CampingStyle.DOCKING))
    }

    @Test
    fun bundledDemoPlacesHaveOneSplitPlace() {
        val file = listOf("../automotive/src/main/assets/demo-places.json", "automotive/src/main/assets/demo-places.json")
            .map { File(it) }.first { it.exists() }
        val places = PlaceParser.parse(file.readText())
        val split = places.filter { it.signalFor(CampingStyle.DOCKING) != it.signalFor(CampingStyle.ROOFTOP) }
        assertEquals(listOf("KR-42-PC-001"), split.map { it.id })
    }

    @Test
    fun invalidIdIsSkipped() {
        val json = """[{ "id": "bad", "name": "x", "latitude": 1, "longitude": 2 }]"""
        assertTrue(PlaceParser.parse(json).isEmpty())
    }

    @Test
    fun ratingsParse() {
        val ratings = RatingsParser.parse("""{ "places": { "KR-42-GN-007": { "count": 12, "average": 4.4 } } }""")
        assertEquals(Rating(12, 4.4), ratings["KR-42-GN-007"])
    }
}
