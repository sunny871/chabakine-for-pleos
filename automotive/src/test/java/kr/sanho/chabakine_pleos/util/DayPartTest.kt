package kr.sanho.chabakine_pleos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DayPartTest {

    @Test
    fun morningIsFourToElevenAm() {
        assertFalse(DayPart.isMorning(LocalTime.of(3, 59)))
        assertTrue(DayPart.isMorning(LocalTime.of(4, 0)))
        assertTrue(DayPart.isMorning(LocalTime.of(7, 0)))
        assertTrue(DayPart.isMorning(LocalTime.of(10, 59)))
        assertFalse(DayPart.isMorning(LocalTime.of(11, 0)))
        assertFalse(DayPart.isMorning(LocalTime.of(18, 26)))
    }

    @Test
    fun wordingFollowsTime() {
        assertEquals("좋은 아침이에요", DayPart.wakeTitle(LocalTime.of(7, 0)))
        assertEquals("일어날 시간이에요", DayPart.wakeTitle(LocalTime.of(18, 26)))
        assertEquals("일어날 시간이에요. 천천히 일어나세요", DayPart.wakeSpeech(LocalTime.of(14, 0)))
        assertEquals("잘 쉬었어요? 정리하고 떠날 준비를 해요", DayPart.wokeSpeech(LocalTime.of(14, 0)))
        assertEquals("좋은 아침, 정리하고 떠나요", DayPart.leaveTitle(LocalTime.of(6, 30)))
        assertEquals("정리하고 떠나요", DayPart.leaveTitle(LocalTime.of(20, 0)))
        assertEquals("철수 완료 · 오늘, 주변", DayPart.leaveDoneTitle(LocalTime.of(20, 0)))
    }
}
