package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.ui.components.RailItem

/**
 * 화면(화면설계서 번호). [parkedOnly]인 화면은 출발하면 닫고 01로 간다(F5 목록이 기준).
 * 홈·즐겨찾기처럼 주행·정차 모두 있는 화면은 같은 경로에서 주행용(14-1·09)으로 바꿔 그린다.
 */
sealed interface Route {
    val parkedOnly: Boolean
    val rail: RailItem?

    /** 14 홈 · 정차 중 / 14-1 홈 · 주행 중 */
    data object Home : Route {
        override val parkedOnly = false
        override val rail = RailItem.HOME
    }

    /** 01 · 02 찾기(주행·정차에 따라), 15 지도는 [map] */
    data class Find(val map: Boolean = false) : Route {
        override val parkedOnly = false
        override val rail = if (map) RailItem.MAP else RailItem.FIND
    }

    /** 02-1 검색 키보드(정차 중) */
    data object Search : Route {
        override val parkedOnly = true
        override val rail = RailItem.FIND
    }

    /** 09 즐겨찾기 · 주행 중 / 09-1 즐겨찾기 · 정차 중 */
    data object Favorites : Route {
        override val parkedOnly = false
        override val rail = RailItem.FAVORITES
    }

    /** 21 리뷰 전체 */
    data class Reviews(val placeId: String) : Route {
        override val parkedOnly = true
        override val rail = RailItem.FIND
    }

    /** 22 필터 더 보기 */
    data object FilterMore : Route {
        override val parkedOnly = true
        override val rail = RailItem.FIND
    }

    /** 03 내비 길안내 전달 */
    data class NaviSent(val placeId: String) : Route {
        override val parkedOnly = false
        override val rail = RailItem.FIND
    }

    /** 03-1 내비 안내 중(주행 중에도 보이고, 출발해도 닫히지 않는다) */
    data class NaviGuiding(val placeId: String) : Route {
        override val parkedOnly = false
        override val rail = RailItem.FIND
    }

    /** 02-2 사진 크게 보기(정차 중). [index]부터 보여 준다 */
    data class PhotoViewer(val placeId: String, val index: Int = 0) : Route {
        override val parkedOnly = true
        override val rail = RailItem.FIND
    }

    /** 03 내비 길안내 전달 · 장소 밖 목적지(10-1 축제). 도착해도 04는 묻지 않는다 */
    data class NaviSentPoint(val name: String, val latitude: Double, val longitude: Double) : Route {
        override val parkedOnly = false
        override val rail = RailItem.FIND
    }

    /** 05-0 도착 체크: 시동 끄기 → 유틸리티 모드 */
    data object CampArrive : Route {
        override val parkedOnly = true
        override val rail = RailItem.CAMP
    }

    /** 05 차박 모드 */
    data object CampMode : Route {
        override val parkedOnly = true
        override val rail = RailItem.CAMP
    }

    /** 10 철수 안내 */
    data object CampLeave : Route {
        override val parkedOnly = true
        override val rail = RailItem.CAMP
    }

    /** 10-1 철수 완료 · 오늘 아침 주변 */
    data class CampLeaveDone(val placeId: String) : Route {
        override val parkedOnly = true
        override val rail = RailItem.CAMP
    }

    /** 23 차량 맞추기(승인 시) */
    data object VehicleLink : Route {
        override val parkedOnly = true
        override val rail = RailItem.CAMP
    }

    /** 06-A 차량 정보를 못 읽을 때 */
    data object ParkedConfirm : Route {
        override val parkedOnly = false
        override val rail = RailItem.CAMP
    }

    /** 12 설정 */
    data object Settings : Route {
        override val parkedOnly = true
        override val rail = RailItem.SETTINGS
    }

    /** 13 폰으로 계정 연결(시연: 서버 API 전이라 앱 안에서만 연결, [Features.ACCOUNT_LINK]) */
    data object AccountLink : Route {
        override val parkedOnly = true
        override val rail = RailItem.SETTINGS
    }

    /** 16~20 첫 실행. [step] 0~4. [replay]면 설정에서 다시 보기 */
    data class Onboarding(val step: Int, val replay: Boolean = false) : Route {
        override val parkedOnly = true
        override val rail: RailItem? = null
    }

    /** 20-1 첫 실행인데 주행 중 */
    data object OnboardingDriving : Route {
        override val parkedOnly = false
        override val rail: RailItem? = null
    }
}
