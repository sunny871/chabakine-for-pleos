package kr.sanho.chabakine_pleos.data

/** 장소·리뷰·피드를 받는 서버. 설정의 개발자 모드에서만 바꿀 수 있다. */
enum class ServerEnv(val label: String, val baseUrl: String) {
    TEST("테스트", "https://chabakine-api.sanhokr.com"),
    LIVE("라이브", "https://chabakine-api-live.sanhokr.com"),
}

/** 지금 요청을 보낼 서버. 앱 시작 때 설정 값으로 맞추고, 개발자 모드에서 바꾸면 바로 따라간다. */
object Server {
    /** 개발자 모드를 켠 적이 없는 기기의 서버 */
    val DEFAULT = ServerEnv.LIVE

    @Volatile
    var env: ServerEnv = DEFAULT

    val baseUrl: String get() = env.baseUrl
}
