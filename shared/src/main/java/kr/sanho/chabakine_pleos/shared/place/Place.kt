package kr.sanho.chabakine_pleos.shared.place

/** 차박 가능 신호. 미확인은 허용이 아니다. */
enum class Signal {
    GREEN, YELLOW, RED, UNKNOWN;

    companion object {
        fun from(value: String?): Signal = when (value) {
            "green" -> GREEN
            "yellow" -> YELLOW
            "red" -> RED
            else -> UNKNOWN
        }
    }
}

/**
 * 장소 규칙 (server-api.md 4장 `stay`). 도킹형·루프탑형은 2026-11-04부터 따로 온다.
 * [TENT]는 옛 데이터 호환용(서버가 두 방식 중 더 엄격한 쪽으로 만든다)이라 방식을 고를 때 대신 쓰는 값으로만 읽는다.
 */
enum class StayRule { CAR, DOCKING, ROOFTOP, TENT, COOK }

/** 사용자의 차박 방식. 신호는 [Place.ruleFor]로 고른다. */
enum class CampingStyle(val rule: StayRule) {
    STEALTH(StayRule.CAR),
    DOCKING(StayRule.DOCKING),
    ROOFTOP(StayRule.ROOFTOP),
}

data class RuleSignal(val signal: Signal, val reason: String)

/** 편의시설. null은 미확인이다(없음이 아님). */
data class Amenities(
    val toilet: Boolean? = null,
    val water: Boolean? = null,
    val electricity: Boolean? = null,
    val shower: Boolean? = null,
)

data class Site(
    val type: String? = null,
    /** large / medium / small / unknown */
    val parkingSize: String = "unknown",
    /** 높이 제한(m). null이면 제한 없음 또는 미확인, [heightLimitKnown]으로 구분한다. */
    val heightLimitMeters: Double? = null,
    val heightLimitKnown: Boolean = false,
    /** exposed / moderate / sheltered / unknown */
    val wind: String = "unknown",
)

data class Place(
    val id: String,
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    /** 대표 신호. 항상 `stay.car`와 같다. */
    val signal: Signal,
    val reason: String,
    val stay: Map<StayRule, RuleSignal>,
    val evidence: List<String>,
    val site: Site?,
    val amenities: Amenities,
    val photoUrls: List<String>,
    val sourceName: String?,
    val verifiedAt: String?,
    /** 1~3 */
    val confidence: Int,
    val verification: String?,
    val seasonRules: List<String>,
    /** 시연용으로 만든 값이 들어 있는 장소 */
    val isDemo: Boolean,
    /** 주의사항(과태료 · 견인 · 출입 제한 · 소음 등). 데이터 `notes` */
    val notes: List<String> = emptyList(),
) {
    fun rule(rule: StayRule): RuleSignal = stay[rule] ?: RuleSignal(Signal.UNKNOWN, "")

    /**
     * 방식에 맞는 규칙: 스텔스 → `car`, 도킹형 → `docking ?? tent`, 루프탑형 → `rooftop ?? tent`, 그것도 없으면 미확인.
     * `tent`로 돌아가는 건 서버가 나누기 전에 받은 데이터뿐이다. 항목이 있으면 `unknown`이어도 `tent`로 돌아가지 않는다
     * (`tent`는 두 방식 중 더 엄격한 쪽이라 다른 방식의 금지가 섞일 수 있다).
     */
    fun ruleFor(style: CampingStyle): RuleSignal =
        stay[style.rule] ?: (if (style == CampingStyle.STEALTH) null else stay[StayRule.TENT]) ?: RuleSignal(Signal.UNKNOWN, "")

    /** 방식에 맞는 규칙의 신호 ([ruleFor]). */
    fun signalFor(style: CampingStyle): Signal = ruleFor(style).signal

    fun reasonFor(style: CampingStyle): String = ruleFor(style).reason.ifBlank { reason }
}
