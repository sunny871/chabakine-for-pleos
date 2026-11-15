package kr.sanho.chabakine_pleos.shared.place

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceDataRulesTest {

    private val base = "https://chabakine-api.sanhokr.com"

    @Test
    fun parsesManifest() {
        val m = PlaceDataRules.parseManifest(
            """{ "version": 14, "updatedAt": "2026-10-25T07:22:38Z", "url": "/static/places/places-v14.json", "sha256": "ABC", "count": 77 }""",
        )
        assertEquals(PlaceManifest(14, "2026-10-25T07:22:38Z", "/static/places/places-v14.json", "abc", 77), m)
        assertNull(PlaceDataRules.parseManifest("""{ "version": 1 }"""))
        assertNull(PlaceDataRules.parseManifest("not json"))
    }

    @Test
    fun serverPathKeepsSameHostOnly() {
        assertEquals("/static/places/p.json", PlaceDataRules.serverPath("/static/places/p.json", base))
        assertEquals("/static/places/p.json?v=2", PlaceDataRules.serverPath("$base/static/places/p.json?v=2", base))
        assertNull(PlaceDataRules.serverPath("https://evil.example.com/static/places/p.json", base))
        assertNull(PlaceDataRules.serverPath("https://chabakine-api.sanhokr.com:8443/p.json", base))
        assertNull(PlaceDataRules.serverPath("static/places/p.json", base))
    }

    @Test
    fun acceptsOnlyMatchingCountAndUniqueIds() {
        val one = PlaceParser.parse("""[{ "id": "KR-42-GN-001", "name": "a", "latitude": 1, "longitude": 2, "signal": "green" }]""")
        val dup = one + one
        assertTrue(PlaceDataRules.isAcceptable(one, 1))
        assertFalse(PlaceDataRules.isAcceptable(one, 2))
        assertFalse(PlaceDataRules.isAcceptable(dup, 2))
        assertFalse(PlaceDataRules.isAcceptable(emptyList(), 0))
    }

    @Test
    fun droppedPlaceFailsCountCheck() {
        val json = """[{ "id": "KR-42-GN-001", "name": "a", "latitude": 1, "longitude": 2 },
                       { "id": "bad", "name": "b", "latitude": 1, "longitude": 2 }]"""
        assertFalse(PlaceDataRules.isAcceptable(PlaceParser.parse(json), 2))
    }

    @Test
    fun sha256Hex() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            PlaceDataRules.sha256Hex(ByteArray(0)),
        )
    }
}
