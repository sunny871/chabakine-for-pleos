package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.ui.components.RailItem
import kr.sanho.chabakine_pleos.ui.find.SignalScope

/**
 * 말로 앱 조작(음성 명령). 인식한 문장을 기기 안에서 정해진 규칙으로 해석한다(서버 · AI 없음).
 * [drivingAllowed]가 false인 명령은 정차 중에만 실행한다(터치와 같은 규칙).
 */
sealed interface VoiceCommand {
    val drivingAllowed: Boolean get() = true

    /** 찾기: 장소·지역 이름, 신호 범위, 화장실, 내 방식. 말하지 않은 조건은 null(그대로 둠) */
    data class Find(
        val query: String? = null,
        val scope: SignalScope? = null,
        val toilet: Boolean = false,
        val style: CampingStyle? = null,
    ) : VoiceCommand

    data object ClearFilter : VoiceCommand

    /** 방금 읽어 준 후보 중 [index]번째(1부터)로 길안내 */
    data class NavigateNth(val index: Int) : VoiceCommand

    /** 지금 고른 장소로 길안내 */
    data object NavigateSelected : VoiceCommand

    /** 이름으로 말한 장소로 길안내("남항진해변으로 안내해 줘") */
    data class NavigateNamed(val query: String) : VoiceCommand

    data object ReadSummary : VoiceCommand
    data object CancelGuidance : VoiceCommand
    data object ShowNavigation : VoiceCommand
    data object AddFavorite : VoiceCommand
    data class Open(val item: RailItem) : VoiceCommand

    /** 기상 알람 시각. [minutesOfDay]가 null이면 일출 + [sunriseOffsetMin] */
    data class WakeAt(val minutesOfDay: Int?, val sunriseOffsetMin: Int = 0) : VoiceCommand {
        override val drivingAllowed get() = false
    }

    /** 울리는 기상 알람: n분 뒤 다시 */
    data object Snooze : VoiceCommand {
        override val drivingAllowed get() = false
    }

    /** 울리는 기상 알람: 일어났어요 */
    data object WakeUp : VoiceCommand {
        override val drivingAllowed get() = false
    }

    /** 04 "차박 모드를 켤까요?"에 말로 답하기: 켜 줘. 열려 있지 않으면 근처 차박지로 04를 띄운다 */
    data object CampModeOn : VoiceCommand {
        override val drivingAllowed get() = false
    }

    /** 04 "차박 모드를 켤까요?"에 말로 답하기: 나중에 */
    data object CampModeLater : VoiceCommand {
        override val drivingAllowed get() = false
    }

    data object Unknown : VoiceCommand
}

object VoiceCommands {

    fun parse(raw: String): VoiceCommand {
        val text = raw.trim()
        val c = text.replace(" ", "")
        if (c.isEmpty()) return VoiceCommand.Unknown

        // 기상 알람(울릴 때)
        if (c.containsAny("일어났", "알람꺼", "알람끄", "알람그만", "알람멈춰", "알람취소")) return VoiceCommand.WakeUp
        if (c.containsAny("더잘", "분더", "조금만더", "다시알려", "다시깨워", "스누즈")) return VoiceCommand.Snooze
        // 기상 알람 맞추기
        if (c.containsAny("깨워", "알람")) {
            parseWakeTime(c)?.let { return it }
        }

        // 04 차박 모드를 켤까요?: 짧은 대답은 문장 전체가 같을 때만("네 번째로 안내해 줘"를 대답으로 읽지 않게)
        val answer = c.trimEnd('.', '!', '?', '요')
        if (answer in CAMP_YES || (c.contains("차박모드") && c.containsAny("켜", "시작", "들어가"))) return VoiceCommand.CampModeOn
        if (answer in CAMP_NO || c.startsWith("나중에") || c.containsAny("안켜", "안켤")) return VoiceCommand.CampModeLater

        // 길안내
        if (c.contains("취소") || c.containsAny("안내그만", "안내멈춰", "안내꺼", "안내끝")) return VoiceCommand.CancelGuidance
        if (c.containsAny("내비화면", "내비보여", "내비열어", "내비로가", "지도화면")) return VoiceCommand.ShowNavigation
        val wantsGo = c.containsAny("안내", "가자", "가줘", "가주", "갈래", "길찾", "출발")
        if (wantsGo) {
            ordinal(c)?.let { return VoiceCommand.NavigateNth(it) }
            if (c.containsAny("여기", "거기", "이곳", "그곳", "이리", "그리")) return VoiceCommand.NavigateSelected
            // "남항진해변으로 안내해 줘" → 그 이름의 장소, 이름이 없으면 고른 장소
            return placeQuery(text)?.let { VoiceCommand.NavigateNamed(it) } ?: VoiceCommand.NavigateSelected
        }

        if (c.containsAny("읽어", "요약")) return VoiceCommand.ReadSummary
        if (c.contains("즐겨찾기") && c.containsAny("추가", "저장", "넣어", "담아")) return VoiceCommand.AddFavorite
        if (c.containsAny("저장해", "찜해")) return VoiceCommand.AddFavorite
        if (c.containsAny("필터지워", "필터초기화", "필터풀어", "필터없애", "조건지워", "조건초기화", "초기화")) return VoiceCommand.ClearFilter

        // 화면 이동(단어 하나만 말했을 때)
        val bare = c.removeSuffixes("화면", "으로가줘", "로가줘", "열어줘", "보여줘", "열어", "가줘", "으로", "로")
        when (bare) {
            "홈", "처음" -> return VoiceCommand.Open(RailItem.HOME)
            "즐겨찾기" -> return VoiceCommand.Open(RailItem.FAVORITES)
            "찾기" -> return VoiceCommand.Open(RailItem.FIND)
        }

        // 찾기
        val scope = when {
            c.containsAny("허용만", "허용된", "허용되는", "가능한곳만") -> SignalScope.ALLOWED
            c.containsAny("전부", "모두", "다보여", "금지도", "전체") -> SignalScope.ALL
            c.contains("조건부") -> SignalScope.WITH_CONDITIONAL
            c.containsAny("허용", "가능") -> SignalScope.ALLOWED
            else -> null
        }
        val style = when {
            c.contains("스텔스") -> CampingStyle.STEALTH
            c.contains("도킹") -> CampingStyle.DOCKING
            c.containsAny("루프탑", "루프톱", "루프") -> CampingStyle.ROOFTOP
            else -> null
        }
        val toilet = c.contains("화장실")
        val query = placeQuery(text)
        val findWords = c.containsAny("찾아", "찾기", "검색", "어디", "근처", "주변", "차박지", "차박", "곳", "보여", "알려")
        // 이름만 말하고 찾는 말이 없으면(예: "음악 틀어 줘") 찾기로 보지 않는다
        if (scope != null || style != null || toilet || findWords) {
            return VoiceCommand.Find(query = query, scope = scope, toilet = toilet, style = style)
        }
        return VoiceCommand.Unknown
    }

    /** "첫 번째", "둘째", "3번", "세 번째 거" → 1부터 */
    internal fun ordinal(c: String): Int? {
        val digit = Regex("([1-9])번").find(c)?.groupValues?.get(1)?.toInt()
        if (digit != null) return digit
        return when {
            c.containsAny("첫번", "첫째", "첫곳", "맨위", "제일가까운", "가장가까운", "하나") -> 1
            c.containsAny("두번", "둘째", "두째", "두곳째") -> 2
            c.containsAny("세번", "셋째", "세째") -> 3
            c.containsAny("네번", "넷째") -> 4
            c.containsAny("다섯번", "다섯째") -> 5
            else -> null
        }
    }

    /** "7시 반", "일곱 시 삼십 분", "오후 2시", "일출", "일출 30분 뒤" (공백을 뺀 문장) */
    internal fun parseWakeTime(c: String): VoiceCommand.WakeAt? {
        if (c.containsAny("일출", "해뜰", "해뜨", "해돋")) {
            val after = Regex("(\\d+)분(뒤|후)").find(c)?.groupValues?.get(1)?.toInt()
                ?: if (c.containsAny("삼십분뒤", "삼십분후", "30분")) 30 else 0
            return VoiceCommand.WakeAt(null, after)
        }
        val hour = Regex("(\\d{1,2})시").find(c)?.groupValues?.get(1)?.toInt()
            ?: NATIVE_HOURS.entries.sortedByDescending { it.key.length }.firstOrNull { c.contains(it.key + "시") }?.value
            ?: return null
        val minute = when {
            c.contains("시반") -> 30
            else -> Regex("시(\\d{1,2})분").find(c)?.groupValues?.get(1)?.toInt()
                ?: SINO_MINUTES.entries.sortedByDescending { it.key.length }.firstOrNull { c.contains("시" + it.key + "분") }?.value
                ?: 0
        }
        if (hour !in 0..24 || minute !in 0..59) return null
        val pm = c.containsAny("오후", "저녁", "밤") && hour in 1..11
        val h = (if (pm) hour + 12 else hour) % 24
        return VoiceCommand.WakeAt(h * 60 + minute)
    }

    /**
     * 장소·지역 이름만 남긴다: 명령 낱말과 조사를 걷어 내고 2글자 이상 남으면 검색어.
     * "속초 근처 차박 가능한 곳" → "속초", "강릉에 화장실 있는 곳 찾아 줘" → "강릉"
     */
    internal fun placeQuery(text: String): String? {
        val words = text.split(' ', ',', '.', '?', '!').map { it.trim() }.filter { it.isNotEmpty() }
        val kept = words.mapNotNull { w ->
            val stem = w.removeSuffixes(*PARTICLES)
            stem.takeIf { s -> s.length >= 2 && STOP_WORDS.none { stop -> isStopWord(s, stop) } }
        }
        return kept.joinToString(" ").takeIf { it.isNotBlank() }
    }

    /** 한 글자 낱말("해" · "밤")은 똑같을 때만 걷어 낸다("해운대"를 지우지 않게) */
    private fun isStopWord(word: String, stop: String): Boolean =
        if (stop.length == 1) word == stop else word.startsWith(stop) || stop.startsWith(word)

    private fun String.containsAny(vararg words: String) = words.any { contains(it) }

    private fun String.removeSuffixes(vararg suffixes: String): String {
        var s = this
        for (suffix in suffixes) if (s.length > suffix.length && s.endsWith(suffix)) return s.removeSuffix(suffix)
        return s
    }

    private val NATIVE_HOURS = mapOf(
        "한" to 1, "두" to 2, "세" to 3, "네" to 4, "다섯" to 5, "여섯" to 6,
        "일곱" to 7, "여덟" to 8, "아홉" to 9, "열" to 10, "열한" to 11, "열두" to 12,
    )
    private val SINO_MINUTES = mapOf(
        "오" to 5, "십" to 10, "십오" to 15, "이십" to 20, "이십오" to 25, "삼십" to 30,
        "삼십오" to 35, "사십" to 40, "사십오" to 45, "오십" to 50, "오십오" to 55,
    )
    /** 끝의 "요" · 문장부호를 뗀 뒤 비교한다("켜 줘요" → "켜줘", "네" · "예") */
    private val CAMP_YES = setOf("응", "네", "예", "좋아", "그래", "켜", "켜줘", "켜자", "켜기", "켜주세", "켤래", "시작", "시작해", "시작해줘")
    private val CAMP_NO = setOf("아니", "아니오", "아니야", "아뇨", "나중", "나중에", "다음에", "괜찮아", "됐어", "싫어", "그만")
    private val PARTICLES = arrayOf("에서", "으로", "에", "로", "을", "를", "은", "는", "이", "가", "만", "도", "의")
    private val STOP_WORDS = listOf(
        "근처", "주변", "차박", "가능", "허용", "조건부", "금지", "전부", "모두", "전체", "곳", "데", "장소",
        "찾아", "찾기", "검색", "보여", "알려", "있는", "있어", "화장실", "수도", "캠핑", "스텔스", "도킹", "루프",
        "가까운", "좀", "해줘", "해", "줘", "어디", "근방", "쪽", "지역", "오늘", "밤", "자고", "잘만한", "잘",
        "안내", "가자", "가줘", "갈래", "길찾", "출발", "데려", "내비",
        "바꿔", "바꾸", "맞춰", "깨워", "켜", "꺼", "틀어", "해봐",
    )
}

/**
 * 음성 찾기 뒤 검색어(QA-14). "루프탑으로 바꿔 줘"처럼 방식만 말하면 지금 검색어(예: 평창)를 그대로 두고,
 * 장소 · 지역 이름을 말하면 그 이름으로, 그 밖의 찾기("근처 차박 가능한 곳")는 내 주변 새 검색이라 검색어를 지운다.
 */
internal fun voiceFindQuery(current: String, command: VoiceCommand.Find): String = when {
    command.query != null -> command.query
    command.style != null && command.scope == null && !command.toilet -> current
    else -> ""
}
