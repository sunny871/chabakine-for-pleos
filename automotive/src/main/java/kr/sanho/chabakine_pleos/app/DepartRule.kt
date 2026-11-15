package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.vehicle.DrivingState

/**
 * F5: 정차가 아니게 됐을 때 갈 화면. null이면 지금 화면을 그대로 둔다(홈·즐겨찾기는 같은 경로에서 주행용으로 바뀐다).
 * - 첫 실행(16~20) 중이면 20-1. 설정에서 다시 보던 중이면 다른 정차 전용 화면처럼 01.
 * - 정차 전용 화면이나 04 "켤까요?"가 열려 있으면 01.
 * - 06-A는 상태를 모를 때 뜨는 화면이라, 시스템이 주행 중이라고 할 때만 닫는다.
 */
fun departTarget(stack: List<Route>, state: DrivingState, campPromptOpen: Boolean): Route? = when {
    state == DrivingState.PARKED -> null
    stack.any { it is Route.Onboarding && !it.replay } -> Route.OnboardingDriving
    campPromptOpen || stack.any { it.parkedOnly } -> Route.Find()
    state == DrivingState.DRIVING && Route.ParkedConfirm in stack -> Route.Find()
    else -> null
}
