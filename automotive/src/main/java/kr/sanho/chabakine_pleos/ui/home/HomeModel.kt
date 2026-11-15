package kr.sanho.chabakine_pleos.ui.home

import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Rating
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.distanceKmFrom

/** 화면에 쓰는 장소 한 줄: 내 방식 기준 신호와 거리 */
data class PlaceItem(
    val place: Place,
    val signal: Signal,
    val distanceKm: Double,
    val rating: Rating?,
)

data class HomeData(
    /** 오늘의 추천: 내 방식 기준 허용 → 조건부, 각각 가까운 순으로 최대 3곳 */
    val recommendations: List<PlaceItem>,
    /** 가까운 순 전체 */
    val nearby: List<PlaceItem>,
    /** 인기 장소: 리뷰가 있는 곳을 리뷰 많은 순 → 별점 높은 순(금지 제외, 최대 [POPULAR_LIMIT]곳) */
    val popular: List<PlaceItem> = emptyList(),
)

const val POPULAR_LIMIT = 10

/** 홈 "인기 장소" 순서. 거리와 상관없이 리뷰가 많이 쌓인 곳부터, 같으면 별점이 높은 곳 */
fun popularPlaces(items: List<PlaceItem>): List<PlaceItem> = items
    .filter { (it.rating?.count ?: 0) > 0 && it.signal != Signal.RED }
    .sortedWith(compareByDescending<PlaceItem> { it.rating!!.count }.thenByDescending { it.rating!!.average }.thenBy { it.distanceKm })
    .take(POPULAR_LIMIT)

fun buildHomeData(
    places: List<Place>,
    ratings: Map<String, Rating>,
    style: CampingStyle,
    origin: LatLng,
): HomeData {
    val items = places
        .map { PlaceItem(it, it.signalFor(style), it.distanceKmFrom(origin), ratings[it.id]) }
        .sortedBy { it.distanceKm }
    val recommendations = (items.filter { it.signal == Signal.GREEN } + items.filter { it.signal == Signal.YELLOW })
        .take(3)
    return HomeData(recommendations, items, popularPlaces(items))
}

fun formatKm(km: Double): String = if (km < 10) "%.1f km".format(km) else "%.0f km".format(km)
