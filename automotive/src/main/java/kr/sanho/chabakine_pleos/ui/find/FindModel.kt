package kr.sanho.chabakine_pleos.ui.find

import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.home.PlaceItem

/** 신호 칩(01 · 02 · 22): 셋 중 하나만 고른다. 기본은 허용 + 조건부 */
enum class SignalScope(val label: String, val chipLabel: String, val hint: String) {
    ALLOWED("허용만", "허용만", "초록만"),
    WITH_CONDITIONAL("허용 + 조건부", "허용·조건부", "기본"),
    ALL("전부 보기", "전부 보기", "미확인·금지도"),
}

/**
 * 찾기 필터(화면설계서 01 · 02 칩 · 22 필터 더 보기, 10-30 정리).
 * 신호는 찾기 화면 위에서 고른 대표 방식 기준이고, [SignalScope] 셋 중 하나로 고른다.
 * 기본은 허용 + 조건부(미확인 · 금지는 "전부 보기"에서만).
 */
data class FindFilter(
    val allowed: Boolean = true,
    val conditional: Boolean = true,
    val unknown: Boolean = false,
    val red: Boolean = false,
    /** null이면 거리 제한 없음 */
    val maxDistanceKm: Int? = null,
    val toilet: Boolean = false,
    val water: Boolean = false,
    val electricity: Boolean = false,
    val shower: Boolean = false,
    val cookAllowed: Boolean = false,
    /** 비어 있으면 모든 종류 */
    val siteTypes: Set<String> = emptySet(),
    val noHeightLimit: Boolean = false,
    val officialOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    /** 02-1에서 "검색"을 누른 검색어. 비어 있지 않으면 이름·지역에 이 말이 든 곳만 */
    val query: String = "",
) {
    /** 프리셋 "허용만" */
    val allowedOnly: Boolean get() = allowed && !conditional && !unknown && !red

    fun withAllowedOnly() = withSignal(SignalScope.ALLOWED)

    /** 지금 신호 선택. 예전 저장값처럼 셋에 딱 맞지 않으면 가장 가까운 것으로 본다 */
    val signalScope: SignalScope
        get() = when {
            allowedOnly -> SignalScope.ALLOWED
            unknown || red -> SignalScope.ALL
            else -> SignalScope.WITH_CONDITIONAL
        }

    fun withSignal(scope: SignalScope) = when (scope) {
        SignalScope.ALLOWED -> copy(allowed = true, conditional = false, unknown = false, red = false)
        SignalScope.WITH_CONDITIONAL -> copy(allowed = true, conditional = true, unknown = false, red = false)
        SignalScope.ALL -> copy(allowed = true, conditional = true, unknown = true, red = true)
    }

    /** "필터 n ›"에 보일 개수: 칩 줄에 없는 22 전용 조건만 센다 */
    val moreCount: Int
        get() = listOf(
            maxDistanceKm != null, water, electricity, shower, siteTypes.isNotEmpty(),
            noHeightLimit, officialOnly, favoritesOnly,
        ).count { it }

    /** 기본과 다른 게 하나라도 있으면 칩 줄에 "초기화"를 보인다(검색어는 따로 지운다) */
    val anyOn: Boolean get() = signalScope != SignalScope.WITH_CONDITIONAL || toilet || cookAllowed || moreCount > 0

    fun reset() = FindFilter(query = query)
}

/** 공식 근거로 치는 근거 유형(후기·블로그가 아닌 것) */
private val OFFICIAL_EVIDENCE = setOf("공식 안내", "현지 표지", "전화 확인", "현장 확인")

fun Place.hasOfficialEvidence(): Boolean = evidence.any { it in OFFICIAL_EVIDENCE }

fun List<PlaceItem>.applyFilter(
    filter: FindFilter,
    favorites: Set<String>,
): List<PlaceItem> = filter { item ->
    val p = item.place
    val signalOk = when (item.signal) {
        Signal.GREEN -> filter.allowed
        Signal.YELLOW -> filter.conditional
        Signal.UNKNOWN -> filter.unknown
        Signal.RED -> filter.red
    }
    signalOk &&
        (filter.maxDistanceKm == null || item.distanceKm <= filter.maxDistanceKm) &&
        (!filter.toilet || p.amenities.toilet == true) &&
        (!filter.water || p.amenities.water == true) &&
        (!filter.electricity || p.amenities.electricity == true) &&
        (!filter.shower || p.amenities.shower == true) &&
        (!filter.cookAllowed || p.rule(StayRule.COOK).signal.let { it == Signal.GREEN || it == Signal.YELLOW }) &&
        (filter.siteTypes.isEmpty() || p.site?.type in filter.siteTypes) &&
        (!filter.noHeightLimit || p.site.let { it?.heightLimitKnown == true && it.heightLimitMeters == null }) &&
        (!filter.officialOnly || p.hasOfficialEvidence()) &&
        (!filter.favoritesOnly || p.id in favorites) &&
        (filter.query.isBlank() || item.matchesQuery(filter.query))
}

/** 02-1 검색: 이름이나 지역에 검색어가 들어 있다(띄어쓰기 무시) */
fun PlaceItem.matchesQuery(q: String): Boolean {
    val compact = q.replace(" ", "")
    return listOf(place.name, place.region).any { it.replace(" ", "").contains(compact, ignoreCase = true) }
}

/** 방식 하나의 이유([Place.ruleFor]: 도킹형 · 루프탑형은 각자 값, 없으면 옛 `tent`). 비면 미확인 안내나 대표 이유 */
fun Place.styleReason(style: CampingStyle): String {
    val r = ruleFor(style)
    return when {
        r.reason.isNotBlank() -> r.reason
        r.signal == Signal.UNKNOWN -> "아직 확인하지 못했어요. 현장 안내를 먼저 확인하세요."
        else -> reason
    }
}
