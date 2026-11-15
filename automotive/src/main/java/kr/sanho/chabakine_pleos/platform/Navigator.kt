package kr.sanho.chabakine_pleos.platform

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kr.sanho.chabakine_pleos.BuildConfig
import kr.sanho.chabakine_pleos.MainActivity
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place

/** 내비에 목적지를 넘긴 뒤의 상태(화면설계서 03) */
/** 내비가 알려 준 차 위치와 받은 시각 */
data class CarLocation(val latLng: LatLng, val atMillis: Long)

/** 내비가 알려 준 목적지까지 남은 거리(m)·시간(초) */
data class RouteRemaining(val meters: Int, val seconds: Int)

/** 장소 밖 목적지의 상태 id. 차박지 id와 겹치지 않게 앞에 붙인다. */
fun pointId(name: String) = "point:$name"

fun isPointId(id: String) = id.startsWith("point:")

sealed interface NaviStatus {
    data object Idle : NaviStatus

    /** 목적지를 보냈고 내비의 응답을 기다리는 중 */
    data class Requested(val placeId: String) : NaviStatus

    /** 내비가 경로를 찾아 길안내를 시작했다 */
    data class Started(val placeId: String) : NaviStatus

    /** SDK가 없어 표준 인텐트로 내비만 열었다(목적지 설정은 보장되지 않음) */
    data class Opened(val placeId: String) : NaviStatus

    data class Failed(val placeId: String, val message: String) : NaviStatus

    data class Arrived(val placeId: String) : NaviStatus
}

/**
 * 목적지를 Pleos 내비에 넘긴다. 길안내는 내비가 하고 앱은 직접 하지 않는다.
 * Pleos NaviHelper SDK(`requestRoute`)로 목적지를 설정하고 경로 이벤트를 받는다.
 * SDK 초기화에 실패하면 AAOS 표준 내비 인텐트로 내비 앱만 연다.
 */
class NaviLauncher(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    private val helper = NaviHelper(context.applicationContext)

    @Volatile
    private var sdkReady = false

    private val _status = MutableStateFlow<NaviStatus>(NaviStatus.Idle)
    val status: StateFlow<NaviStatus> = _status.asStateFlow()

    /** 내비가 알려 준 도착. 앱은 이 장소로 차박 모드를 묻는다. */
    var onArrived: (placeId: String) -> Unit = {}

    /** 03-1에 보여 줄 남은 거리·시간. 내비가 알려 줄 때만 있다(에뮬레이터·SDK 없음이면 null) */
    private val _remaining = MutableStateFlow<RouteRemaining?>(null)
    val remaining: StateFlow<RouteRemaining?> = _remaining.asStateFlow()

    /** 내비가 알려 준 차 위치(04 "차박지 근처에 섰는지"에 쓴다). 서버로 보내지 않는다 */
    private val _location = MutableStateFlow<CarLocation?>(null)
    val location: StateFlow<CarLocation?> = _location.asStateFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            // 디버그 빌드에서만 좌표를 남긴다(정차 감지 확인용)
            Log.i(TAG, if (BuildConfig.DEBUG) "차 위치 받음 ${info.latitude},${info.longitude}" else "차 위치 받음")
            _location.value = CarLocation(LatLng(info.latitude, info.longitude), System.currentTimeMillis())
        }

        override fun onRouteStarted(info: RouteStartInfo) {
            Log.i(TAG, "경로 시작: ${info.name}, ${info.distance} m")
            _remaining.value = RouteRemaining(info.distance, info.duration)
            update { NaviStatus.Started(it) }
        }

        override fun onDrivingInfo(info: DrivingInfo) {
            _remaining.value = RouteRemaining(info.destination.distance, info.destination.duration)
        }

        override fun onRouteEnded() {
            _remaining.value = null
        }

        override fun onRoutingCompleted() = update { NaviStatus.Started(it) }

        override fun onError(code: NaviErrorCode) {
            Log.w(TAG, "내비 오류: $code")
            update { NaviStatus.Failed(it, errorMessage(code)) }
        }

        override fun onRouteCancelled() {
            // 새 목적지를 보내면 내비가 이전 경로를 먼저 취소한다. 보내는 중에 온 취소는 무시한다.
            if (_status.value !is NaviStatus.Requested) {
                _status.value = NaviStatus.Idle
                _remaining.value = null
            }
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            _remaining.value = null
            update { NaviStatus.Arrived(it) }
            currentPlaceId()?.let(onArrived)
        }
    }

    fun start() {
        runCatching {
            helper.initialize(
                {
                    sdkReady = true
                    helper.addListener(listener)
                    Log.i(TAG, "NaviHelper 준비됨")
                },
                { e ->
                    sdkReady = false
                    Log.w(TAG, "NaviHelper 초기화 실패: ${e.javaClass.simpleName} ${e.message}")
                },
            )
        }.onFailure { Log.w(TAG, "NaviHelper를 시작하지 못했다", it) }
    }

    fun stop() {
        if (!sdkReady) return
        runCatching {
            helper.removeListener(listener)
            helper.release()
        }
        sdkReady = false
    }

    /** 목적지를 보낸다. 결과는 [status]로 알린다. */
    fun navigate(place: Place) = send(place.id, place.name, place.latitude, place.longitude)

    /** 장소 밖의 목적지. 상태는 [pointId]로 알리고, 도착해도 차박 모드는 묻지 않는다. */
    fun navigatePoint(name: String, latitude: Double, longitude: Double) = send(pointId(name), name, latitude, longitude)

    private fun send(id: String, name: String, latitude: Double, longitude: Double) {
        if (sdkReady && requestRoute(id, name, latitude, longitude)) return
        _status.value = if (openNaviApp(name, latitude, longitude)) {
            NaviStatus.Opened(id)
        } else {
            NaviStatus.Failed(id, "내비가 응답하지 않아요.")
        }
    }

    /**
     * 길안내 취소. Pleos 내비는 취소를 받으면 스스로 앞으로 나와(QA-12) 앱을 떠나게 되므로,
     * 앱에서 취소했으면([bringAppBack]) 잠시 뒤 앱을 다시 앞으로 가져온다. 사용자가 앱에서 누른 취소에만 쓴다.
     */
    fun cancel(bringAppBack: Boolean = true) {
        if (sdkReady) runCatching { helper.cancelRoute() }
        _status.value = NaviStatus.Idle
        _remaining.value = null
        if (bringAppBack && sdkReady) {
            // 내비가 앞으로 나오는 때가 조금씩 달라 두 번 돌려놓는다(이미 앞이면 아무 일도 없다)
            BRING_BACK_DELAYS_MS.forEach { handler.postDelayed(::bringAppToFront, it) }
        }
    }

    private fun bringAppToFront() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { context.startActivity(intent) }.onFailure { Log.w(TAG, "앱을 앞으로 가져오지 못함", it) }
    }

    /**
     * 지금 차 위치를 내비에 묻고 [timeoutMs] 안에 온 새 값을 돌려준다. SDK가 없거나 답이 없으면 null.
     */
    suspend fun currentLocation(timeoutMs: Long = 3_000): LatLng? {
        if (!sdkReady) return null
        val since = System.currentTimeMillis()
        if (runCatching { helper.getCurrentLocationInfo() }.isFailure) return null
        return withTimeoutOrNull(timeoutMs) { location.first { it != null && it.atMillis >= since } }?.latLng
    }

    /** 03-1 "내비 화면으로": 내비 앱을 앞으로 띄운다. 못 띄우면 false */
    fun showNavigationScreen(): Boolean {
        if (sdkReady && runCatching { helper.requestNavigationScreen(NavigationScreenRequest.Foreground) }.isSuccess) return true
        return runCatching {
            context.startActivity(Intent(ACTION_NAVIGATE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    /** 내비가 정해진 시간 안에 답하지 않았다 */
    fun timeout(placeId: String) {
        if (currentPlaceId() == placeId) _status.value = NaviStatus.Failed(placeId, "내비가 응답하지 않아요.")
    }

    fun clearStatus() {
        _status.value = NaviStatus.Idle
    }

    private fun requestRoute(id: String, name: String, latitude: Double, longitude: Double): Boolean {
        _status.value = NaviStatus.Requested(id)
        return runCatching { helper.requestRoute(routeInfo(name, latitude, longitude)) }
            .onFailure { Log.w(TAG, "requestRoute 실패", it) }
            .isSuccess
    }

    private fun routeInfo(name: String, latitude: Double, longitude: Double) = RouteInfo(
        latitude = latitude,
        longitude = longitude,
        poiName = name,
        // 내비 제공자의 POI 번호는 없으므로 좌표와 이름만 보낸다
        poiId = "",
        address = "",
        poiSubId = "",
        routeOption = RouteOption.RECOMMENDED,
    )

    private fun openNaviApp(name: String, latitude: Double, longitude: Double): Boolean {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})")
        val intent = Intent(ACTION_NAVIGATE, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "내비 앱을 찾지 못했다")
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "내비 앱을 열 수 없다", e)
            false
        }
    }

    private fun currentPlaceId(): String? = when (val s = _status.value) {
        is NaviStatus.Requested -> s.placeId
        is NaviStatus.Started -> s.placeId
        is NaviStatus.Opened -> s.placeId
        is NaviStatus.Failed -> s.placeId
        is NaviStatus.Arrived -> s.placeId
        NaviStatus.Idle -> null
    }

    /** 지금 보낸 목적지가 있을 때만 상태를 바꾼다 */
    private fun update(next: (String) -> NaviStatus) {
        currentPlaceId()?.let { _status.value = next(it) }
    }

    private fun errorMessage(code: NaviErrorCode): String = when (code) {
        NaviErrorCode.CAUSE_LOW_BATTERY -> "배터리가 부족해 경로를 찾지 못했어요."
        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "너무 멀어서 경로를 찾지 못했어요."
        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "목적지가 너무 가까워요."
        else -> "내비가 경로를 찾지 못했어요."
    }

    private companion object {
        const val ACTION_NAVIGATE = "android.intent.action.NAVIGATE"
        const val TAG = "NaviLauncher"
        val BRING_BACK_DELAYS_MS = listOf(500L, 1_500L)
    }
}
