package kr.sanho.chabakine_pleos.app

/**
 * 시연 범위(화면설계서 10-26 결정). false인 기능은 코드는 두되 앱과 시연 영상에서 숨긴다.
 */
object Features {
    /** 23 차량 맞추기와 05 차량 맞추기 버튼. Vehicle Control 승인 전이면 false로 숨긴다(시연 범위 확인 필요) */
    const val VEHICLE_CONTROL = true

    /**
     * 13 폰으로 계정 연결(12 설정의 "폰으로 연결" 포함). 서버에 차량 연결 API가 아직 없어
     * 시연용으로만 켠다: 코드 · QR은 앱이 만들고 "폰에서 승인"을 흉내 낸다(실제 계정은 연결되지 않음)
     */
    const val ACCOUNT_LINK = true

    /** 15 지도 보기(카카오맵 SDK 확정). 01·02에 목록/지도 전환 버튼을 보여 준다 */
    const val MAP_VIEW = true

    /** 10 "이곳 어땠나요?"(차 안 제보). 기획서 3.3에서 제외, 로그인 필요 */
    const val PLACE_REPORT = false

    /** 05 기상 알람. 시동을 끈 뒤 앱 동작이 확인되지 않음 */
    const val WAKE_ALARM = false

    /** 05 내일 아침 날씨. 데이터 출처 없음(일출만 기기에서 계산) */
    const val WEATHER = false

    /** 05 실내 공기(BLE 센서: 일산화탄소 · 온도 · 습도). 센서 기종 · 연결 방식 확인 전이라 카드는 "연결 안 됨"으로만 보인다 */
    const val INDOOR_AIR_SENSOR = false
}
