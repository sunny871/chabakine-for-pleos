package kr.sanho.chabakine_pleos.util

import java.time.LocalTime

/**
 * "아침" 문구를 쓸 시간인지(QA-08: 저녁 6시 알람에도 "좋은 아침이에요"가 나왔다).
 * 오전 4시 ~ 오전 10시 59분이면 아침 문구, 그 밖은 시간과 상관없는 문구를 쓴다.
 */
object DayPart {

    fun isMorning(time: LocalTime = LocalTime.now()): Boolean = time.hour in 4..10

    /** 05-6 기상 알람 화면 큰 글씨 */
    fun wakeTitle(time: LocalTime = LocalTime.now()): String = if (isMorning(time)) "좋은 아침이에요" else "일어날 시간이에요"

    /** 05-6 기상 알람 음성(알림음 뒤 되풀이) */
    fun wakeSpeech(time: LocalTime = LocalTime.now()): String =
        if (isMorning(time)) "좋은 아침이에요. 일어날 시간이에요" else "일어날 시간이에요. 천천히 일어나세요"

    /** 05-6 "일어났어요" 뒤 음성 */
    fun wokeSpeech(time: LocalTime = LocalTime.now()): String =
        if (isMorning(time)) "좋은 아침이에요. 정리하고 떠날 준비를 해요" else "잘 쉬었어요? 정리하고 떠날 준비를 해요"

    /** 10 철수 안내 제목 */
    fun leaveTitle(time: LocalTime = LocalTime.now()): String = if (isMorning(time)) "좋은 아침, 정리하고 떠나요" else "정리하고 떠나요"

    /** 10-1 철수 완료 제목 */
    fun leaveDoneTitle(time: LocalTime = LocalTime.now()): String = if (isMorning(time)) "철수 완료 · 오늘 아침, 주변" else "철수 완료 · 오늘, 주변"
}
