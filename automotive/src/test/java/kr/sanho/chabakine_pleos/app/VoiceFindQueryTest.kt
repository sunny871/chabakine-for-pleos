package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.ui.find.SignalScope
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceFindQueryTest {

    @Test
    fun styleOnlyKeepsQuery() {
        // QA-14: "평창 …" 뒤 "루프탑으로 바꿔 줘"는 평창을 남긴다
        assertEquals("평창", voiceFindQuery("평창", VoiceCommand.Find(style = CampingStyle.ROOFTOP)))
    }

    @Test
    fun namedPlaceReplacesQuery() {
        assertEquals("속초", voiceFindQuery("평창", VoiceCommand.Find(query = "속초", style = CampingStyle.ROOFTOP)))
    }

    @Test
    fun plainNearbySearchClearsQuery() {
        assertEquals("", voiceFindQuery("평창", VoiceCommand.Find(scope = SignalScope.ALLOWED)))
        assertEquals("", voiceFindQuery("평창", VoiceCommand.Find(toilet = true, style = CampingStyle.STEALTH)))
    }
}
