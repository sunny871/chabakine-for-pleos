package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.app.VoiceCommand.CancelGuidance
import kr.sanho.chabakine_pleos.app.VoiceCommand.ClearFilter
import kr.sanho.chabakine_pleos.app.VoiceCommand.Find
import kr.sanho.chabakine_pleos.app.VoiceCommand.NavigateNamed
import kr.sanho.chabakine_pleos.app.VoiceCommand.NavigateNth
import kr.sanho.chabakine_pleos.app.VoiceCommand.NavigateSelected
import kr.sanho.chabakine_pleos.app.VoiceCommand.Open
import kr.sanho.chabakine_pleos.app.VoiceCommand.WakeAt
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.ui.components.RailItem
import kr.sanho.chabakine_pleos.ui.find.SignalScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandsTest {

    private fun p(text: String) = VoiceCommands.parse(text)

    @Test
    fun 첫_실행_시연_문장() {
        assertEquals(Find(query = "속초", scope = SignalScope.ALLOWED), p("속초 근처 차박 가능한 곳"))
        assertEquals(NavigateNth(1), p("첫 번째로 안내해 줘"))
    }

    @Test
    fun 찾기_조건() {
        assertEquals(Find(scope = SignalScope.ALLOWED, toilet = true), p("화장실 있는 허용만"))
        assertEquals(Find(query = "강릉", toilet = true), p("강릉에 화장실 있는 곳 찾아 줘"))
        assertEquals(Find(scope = SignalScope.ALL), p("금지도 전부 보여 줘"))
        assertEquals(Find(scope = SignalScope.WITH_CONDITIONAL), p("조건부도 보여 줘"))
        assertEquals(Find(style = CampingStyle.ROOFTOP), p("루프탑으로 바꿔 줘"))
        assertEquals(Find(), p("근처 차박지 찾아 줘"))
        assertEquals(Find(query = "해운대"), p("해운대 근처 차박지 찾아 줘"))
    }

    @Test
    fun 길안내() {
        assertEquals(NavigateNth(2), p("두번째 꺼로 안내해줘"))
        assertEquals(NavigateNth(3), p("3번으로 가 줘"))
        assertEquals(NavigateNth(1), p("제일 가까운 데로 가자"))
        assertEquals(NavigateSelected, p("여기로 안내해 줘"))
        assertEquals(NavigateSelected, p("안내해 줘"))
        assertEquals(NavigateNamed("남항진해변"), p("남항진해변으로 안내해 줘"))
        assertEquals(CancelGuidance, p("길안내 취소"))
        assertEquals(VoiceCommand.ShowNavigation, p("내비 화면 보여 줘"))
    }

    @Test
    fun 그_밖의_명령() {
        assertEquals(VoiceCommand.ReadSummary, p("요약 읽어 줘"))
        assertEquals(VoiceCommand.AddFavorite, p("즐겨찾기에 추가해 줘"))
        assertEquals(ClearFilter, p("필터 지워 줘"))
        assertEquals(Open(RailItem.HOME), p("홈"))
        assertEquals(Open(RailItem.FAVORITES), p("즐겨찾기 열어 줘"))
        assertEquals(VoiceCommand.Unknown, p("음악 틀어 줘"))
    }

    @Test
    fun 기상_알람() {
        assertEquals(WakeAt(7 * 60), p("내일 7시에 깨워 줘"))
        assertEquals(WakeAt(7 * 60 + 30), p("일곱 시 반에 깨워 줘"))
        assertEquals(WakeAt(6 * 60 + 45), p("여섯 시 사십오 분에 깨워 줘"))
        assertEquals(WakeAt(11 * 60), p("열한시에 알람 맞춰 줘"))
        assertEquals(WakeAt(14 * 60), p("오후 2시에 깨워 줘"))
        assertEquals(WakeAt(null, 0), p("일출에 깨워 줘"))
        assertEquals(WakeAt(null, 30), p("해 뜨고 30분 뒤에 깨워 줘"))
        assertEquals(VoiceCommand.Snooze, p("5분 더 잘래"))
        assertEquals(VoiceCommand.WakeUp, p("일어났어"))
    }

    @Test
    fun 주행_중_허용() {
        assertTrue(p("첫 번째로 안내해 줘").drivingAllowed)
        assertTrue(p("속초 근처 차박 가능한 곳").drivingAllowed)
        assertFalse(p("7시에 깨워 줘").drivingAllowed)
        assertFalse(p("5분 더").drivingAllowed)
        assertFalse(p("켜 줘").drivingAllowed)
        assertFalse(p("나중에").drivingAllowed)
    }

    @Test
    fun 차박_모드_묻기에_답하기() {
        for (yes in listOf("켜 줘", "켜 줘요", "네", "응", "좋아요", "시작해 줘", "차박 모드 켜 줘", "차박 모드 시작")) {
            assertEquals(yes, VoiceCommand.CampModeOn, p(yes))
        }
        for (no in listOf("나중에", "나중에 할게", "아니요", "괜찮아요", "됐어", "안 켤래")) {
            assertEquals(no, VoiceCommand.CampModeLater, p(no))
        }
        // 짧은 대답 낱말이 들어 있어도 다른 명령은 그대로
        assertEquals(NavigateNth(4), p("네 번째로 안내해 줘"))
        assertEquals(Find(), p("근처 차박지 찾아 줘"))
        assertEquals(VoiceCommand.WakeUp, p("알람 그만"))
    }
}
