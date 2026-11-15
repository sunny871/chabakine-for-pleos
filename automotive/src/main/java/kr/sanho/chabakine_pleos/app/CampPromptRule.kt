package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.distanceKmFrom

/** 정차했을 때 04를 묻는 거리: 차가 차박지에서 이만큼 안에 섰을 때만(휴게소·신호 대기에서는 묻지 않는다) */
const val CAMP_PROMPT_RADIUS_KM = 1.0

/**
 * 정차 감지로 04를 물을 장소. 없으면 묻지 않는다.
 * - 차 위치를 알면: 안내한 목적지가 [CAMP_PROMPT_RADIUS_KM] 안이면 그곳, 아니면 그 안에서 가장 가까운 차박지.
 * - 위치를 모르면: 내비가 목적지 도착을 알려 준 경우에만 그 목적지(도착 = 근처에 있다).
 */
fun campPromptPlace(places: List<Place>, location: LatLng?, arrivalPlace: Place?, arrivedByNavi: Boolean): Place? {
    if (location == null) return arrivalPlace?.takeIf { arrivedByNavi }
    fun near(p: Place) = p.distanceKmFrom(location) <= CAMP_PROMPT_RADIUS_KM
    return arrivalPlace?.takeIf(::near) ?: places.filter(::near).minByOrNull { it.distanceKmFrom(location) }
}

/**
 * 정차 감지로 04를 띄울 때 앱이 먼저 읽어 주는 말. 화면 카드 첫 줄과 같은 내용이다.
 * [canAnswer]가 true면(음성 인식을 쓸 수 있는 차) 마이크로 답하는 법을 덧붙인다.
 */
fun campPromptSpeech(placeName: String, arrived: Boolean, canAnswer: Boolean): String {
    val where = if (arrived) "${placeName}에 도착했어요." else "${placeName} 근처에 주차했어요."
    val answer = if (canAnswer) " 마이크를 누르고 \"켜 줘\" 또는 \"나중에\"라고 답해 주세요." else ""
    return "$where 차박 모드를 켤까요?$answer"
}
