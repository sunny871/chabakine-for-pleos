package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.util.DayPart
import kr.sanho.chabakine_pleos.util.Josa
import kr.sanho.chabakine_pleos.data.BatteryLog
import kr.sanho.chabakine_pleos.data.BatterySample
import kr.sanho.chabakine_pleos.vehicle.BatteryForecast
import kr.sanho.chabakine_pleos.vehicle.Forecast
import kr.sanho.chabakine_pleos.vehicle.Rates
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.car.drivingstate.CarUxRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.sanho.chabakine_pleos.data.FavoritesStore
import kr.sanho.chabakine_pleos.data.Festival
import kr.sanho.chabakine_pleos.data.FeedRepository
import kr.sanho.chabakine_pleos.data.PlaceCatalog
import kr.sanho.chabakine_pleos.data.PlacesRepository
import kr.sanho.chabakine_pleos.data.Server
import kr.sanho.chabakine_pleos.data.ServerEnv
import kr.sanho.chabakine_pleos.data.ReviewsRepository
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.UserPrefs
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.platform.NaviLauncher
import kr.sanho.chabakine_pleos.platform.NaviStatus
import kr.sanho.chabakine_pleos.platform.isPointId
import kr.sanho.chabakine_pleos.platform.AlarmSound
import kr.sanho.chabakine_pleos.platform.SpeakResult
import kr.sanho.chabakine_pleos.platform.Speaker
import kr.sanho.chabakine_pleos.platform.VoiceSearch
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.distanceKmFrom
import kr.sanho.chabakine_pleos.ui.components.RailItem
import kr.sanho.chabakine_pleos.ui.find.FindFilter
import kr.sanho.chabakine_pleos.ui.find.applyFilter
import kr.sanho.chabakine_pleos.ui.find.SignalScope
import kr.sanho.chabakine_pleos.ui.find.summarySpeech
import kr.sanho.chabakine_pleos.util.sunrise
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.vehicle.CampControl
import kr.sanho.chabakine_pleos.vehicle.CampPreset
import kr.sanho.chabakine_pleos.vehicle.ControlResult
import kr.sanho.chabakine_pleos.vehicle.DrivingState
import kr.sanho.chabakine_pleos.vehicle.VehicleMonitor

/** 11 메뉴 줄 "차박 모드" 상태 */
enum class CampRailState { OFF, ON, DRIVING }

/** 05 위에 덮는 화면: 잠들기 전 알림 묻기 → 잠든 화면(어둡게), 05-5 기상 알람 설정 · 05-6 기상 알람 울림 · 05-7 배터리 부족 */
enum class CampOverlay { NONE, ASK_SLEEP, SLEEP, WAKE_SET, WAKE_RING, LOW_BATTERY }

/**
 * 05-7 배터리 부족 알림 내용. 잠든 화면이 잠들 때 배터리([startPercent])와 시각을 넘겨 준다.
 */
data class LowBatteryAlert(
    val percent: Int,
    val startPercent: Float?,
    val startMs: Long?,
    val atMs: Long,
    /** 알릴 때 공조가 켜져 있었는지. false면 "이미 꺼져 있어요"로 바꿔 알린다. null은 모름(켜진 것으로 본다) */
    val climateOn: Boolean? = null,
) {
    /** 05-7에서 읽어 주고 화면에도 보여 주는 말 */
    val speech: String
        get() = if (climateOn == false) "배터리가 $percent%예요. 공조는 이미 꺼져 있어요" else "배터리가 $percent%예요. 공조를 줄여 주세요"

    /**
     * 잠든 뒤 줄어든 양과 10%까지 어림 시간. 15분이 안 됐거나 줄지 않았으면 null(설계서 05-7 메모).
     * "잠든 뒤 3시간 동안 6% 줄었어요 · 이대로면 약 4시간 뒤 10%"
     */
    val drainText: String?
        get() {
            val start = startPercent ?: return null
            val since = startMs ?: return null
            val hours = (atMs - since) / 3_600_000f
            val drop = start - percent
            if (hours < 0.25f || drop <= 0f) return null
            val perHour = drop / hours
            val dropText = "${drop.toInt().coerceAtLeast(1)}%"
            val left = if (percent > FLOOR) " · 이대로면 약 ${hoursText((percent - FLOOR) / perHour)} 뒤 $FLOOR%" else ""
            return "잠든 뒤 ${hoursText(hours)} 동안 $dropText 줄었어요$left"
        }

    private fun hoursText(h: Float): String = if (h < 1f) "${(h * 60).toInt().coerceAtLeast(1)}분" else "${kotlin.math.round(h).toInt()}시간"

    companion object {
        /** 어림 시간의 기준(이 아래로 내려가기 전에 움직이게) */
        const val FLOOR = 10
    }
}

/**
 * 잠든 동안 화면을 켜서 알릴 일(설계서 05 "잠든 동안 알려 드릴까요?").
 * 기상 알람은 잠든 화면이 켜져 있는 동안만 울린다. 시동을 끈 뒤 동작은 [Features.WAKE_ALARM](다음 단계).
 */
data class SleepAlerts(
    val vent: Boolean = true,
    val battery: Boolean = true,
    val wake: Boolean = false,
    /** 기상 시각(자정부터 분). 기본 오전 7:00 */
    val wakeMinutes: Int = 7 * 60,
    /** 05-5 소리: false면 알림음 + 음성, true면 음성만(옆 사람이 깨지 않게) */
    val wakeVoiceOnly: Boolean = false,
    /** 05-6 "n분 뒤 다시"의 n. 0이면 다시 알림 없음 */
    val snoozeMinutes: Int = 5,
) {
    /** "오전 7:00" */
    val wakeLabel: String
        get() {
            val h = wakeMinutes / 60
            val m = wakeMinutes % 60
            return "${if (h < 12) "오전" else "오후"} ${(h % 12).let { if (it == 0) 12 else it }}:${"%02d".format(m)}"
        }

    /** 기상 시각을 [deltaMinutes]만큼 옮긴다(하루를 넘으면 돈다) */
    fun shiftWake(deltaMinutes: Int) = copy(wakeMinutes = ((wakeMinutes + deltaMinutes) % DAY_MINUTES + DAY_MINUTES) % DAY_MINUTES)

    /** 기상 시각을 [minutesOfDay]로 정한다(하루 안으로 맞춤) */
    fun wakeAt(minutesOfDay: Int) = copy(wakeMinutes = (minutesOfDay % DAY_MINUTES + DAY_MINUTES) % DAY_MINUTES)

    private companion object {
        const val DAY_MINUTES = 24 * 60
    }
}

/**
 * 앱 전체 상태와 화면 이동(화면설계서 F1~F6).
 * 주행 상태는 [VehicleMonitor] 한 곳에서 읽고, 화면은 [onDrivingState]로 전달된 값만 본다.
 */
class AppState(
    private val scope: CoroutineScope,
    val vehicle: VehicleMonitor,
    val places: PlacesRepository,
    val reviews: ReviewsRepository,
    val feeds: FeedRepository,
    val prefs: UserPrefs,
    val favorites: FavoritesStore,
    val batteryLog: BatteryLog,
    val navi: NaviLauncher,
    val campControl: CampControl,
    private val speaker: Speaker,
    private val alarmSound: AlarmSound,
    private val voice: VoiceSearch,
) {
    private val stack = mutableStateListOf<Route>(Route.Find())
    val route: Route get() = stack.last()

    var filter by mutableStateOf(FindFilter())
    var selectedPlaceId by mutableStateOf<String?>(null)

    /**
     * 찾기 화면(목록)이 지금 상세로 보여 주는 장소. 고른 장소가 없으면 첫 장소를 대신 보여 주므로,
     * 음성 명령("요약 읽어 줘" 등)은 [selectedPlaceId] 다음에 이 값을 본다(QA-03). 화면을 떠나면 null
     */
    var shownPlaceId: String? = null

    /** 음성 명령이 가리키는 장소: 고른 장소 → 찾기 화면에 보이는 장소 */
    private fun voiceTargetPlace(): Place? = (selectedPlaceId ?: shownPlaceId)?.let(::place)

    /**
     * 거리 기준: 내비가 알려 준 차의 현재 위치. 받기 전에는 마지막으로 알던 위치, 그것도 없으면 국토 가운데.
     * 위치는 이 기기에만 두고 서버로 보내지 않는다.
     */
    var origin by mutableStateOf(prefs.lastLocation ?: COUNTRY_CENTER)
        private set

    /** 지금 [origin]이 이번에 켠 뒤 내비에서 받은 위치인지 */
    var originFresh by mutableStateOf(false)
        private set

    private val hadLastLocation = prefs.lastLocation != null

    /** 제목 아래 기준 문구: "현재 위치 근처" · 받기 전이면 "마지막 위치 근처" · 처음이면 "위치 확인 중" */
    val originText: String
        get() = when {
            originFresh -> "현재 위치 근처"
            hadLastLocation -> "마지막 위치 근처"
            else -> "위치 확인 중"
        }

    init {
        scope.launch {
            navi.location.collect { loc ->
                if (loc == null) return@collect
                origin = loc.latLng
                originFresh = true
                prefs.lastLocation = loc.latLng
            }
        }
        // 내비는 물어볼 때만 위치를 알려 주므로 주기적으로 묻는다. 처음 받기 전(내비 연결 전 포함)에는 짧게 다시 묻는다
        scope.launch {
            while (true) {
                val got = navi.currentLocation()
                delay(if (got == null && !originFresh) LOCATION_RETRY_MS else LOCATION_REFRESH_MS)
            }
        }
    }

    /** 차박 모드 장소. null이면 차박 모드가 꺼져 있다. 앱을 다시 열어도 이어진다. */
    var campPlaceId by mutableStateOf(prefs.activeCampPlaceId)
        private set

    /** 내비로 안내한 목적지. 도착해 주차하면 이 장소로 차박 모드를 묻는다. */
    var arrivalPlaceId by mutableStateOf<String?>(null)
        private set

    /** 06-A "주차했어요". 차량 값을 못 읽을 때만 쓰이고 시스템 주행 제한은 풀지 않는다. */
    var manualParked by mutableStateOf(false)
        private set

    /** 04 차박 모드를 켤까요? 대상 장소 */
    var campPromptPlaceId by mutableStateOf<String?>(null)
        private set

    var listening by mutableStateOf(false)
        private set

    /** 화면이 보는 주행 상태(모르면 주행으로 본다) */
    var drivingState by mutableStateOf(DrivingState.UNKNOWN)
        private set
    private var startHandled = false

    // 이동

    /**
     * 시스템이 내려 준 주행 제한(CarUxRestrictions 활성 항목). 지역마다 다르게 내려오므로 앱은 그대로 따른다
     * (Pleos 주행 규제 개발 가이드). 정차 전용 화면 규칙([Route.parkedOnly])과 함께 쓴다.
     */
    var uxActiveRestrictions by mutableStateOf(0)

    fun uxRestricted(flag: Int): Boolean = uxActiveRestrictions and flag != 0

    fun navigate(to: Route) {
        if (to == route) return
        blockedByUx(to)?.let { return toast(it) }
        stack.add(to)
    }

    /** 시스템 제한 때문에 열 수 없는 화면이면 알릴 문구 */
    private fun blockedByUx(to: Route): String? = when {
        to == Route.Search && uxRestricted(CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD) -> "지금은 키보드를 쓸 수 없어요 · 음성으로 찾아 주세요"
        (to == Route.Settings || to == Route.VehicleLink) && uxRestricted(CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP) -> "지금은 설정을 바꿀 수 없어요"
        else -> null
    }

    fun replace(to: Route) {
        stack.clear()
        stack.add(to)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else replace(defaultRoute())
    }

    private fun defaultRoute(): Route = Route.Home

    /**
     * 차박 모드를 쓸 수 있는 차인지: 순수 전기차만. 아니면(또는 차종을 아직 못 읽었으면) 메뉴 줄 · 02 버튼 · 04 묻기 ·
     * 03 안내 문구 · 설정 항목 등 차박 모드 입구를 모두 숨긴다.
     */
    var campModeAvailable by mutableStateOf(vehicle.snapshot.value.isElectric == true)
        private set

    init {
        // 차종 값은 차에 연결된 뒤에 온다(이 속성이 만들어진 다음에 구독해야 한다)
        scope.launch { vehicle.snapshot.collect { campModeAvailable = it.isElectric == true } }
    }

    /** 메뉴 줄 "차박 모드"의 상태(11 campState) */
    val campRailState: CampRailState
        get() = when {
            drivingState != DrivingState.PARKED -> CampRailState.DRIVING
            campPlaceId != null -> CampRailState.ON
            else -> CampRailState.OFF
        }

    fun selectRail(item: RailItem) {
        val parked = drivingState == DrivingState.PARKED
        when (item) {
            // 홈은 주행·정차 모두(주행이면 14-1로 그린다)
            RailItem.HOME -> replace(Route.Home)
            RailItem.MAP -> replace(Route.Find(map = true))
            RailItem.FIND -> replace(Route.Find())
            RailItem.FAVORITES -> replace(Route.Favorites)
            RailItem.SETTINGS -> when {
                !parked -> toast("설정은 정차 중에만 열려요")
                else -> blockedByUx(Route.Settings)?.let(::toast) ?: replace(Route.Settings)
            }
            // 켜짐 → 05, 꺼짐+정차 → 04, 주행 중 → 누를 수 없음(값을 못 읽을 때만 06-A)
            RailItem.CAMP -> when {
                !campModeAvailable -> Unit
                parked && campPlaceId != null -> replace(Route.CampMode)
                parked -> askCampMode(campCandidate()?.id, force = true)
                vehicle.snapshot.value.drivingState == DrivingState.UNKNOWN -> navigate(Route.ParkedConfirm)
                else -> Unit
            }
        }
    }

    /** 02 오른쪽 시트(방식 칸 · 취사·불 · 계절·시간 · 주의사항 · 리뷰). 정차 중에만 열리고 출발하면 닫힌다 */
    var reasonSheet by mutableStateOf<ReasonSheet?>(null)
        private set

    fun openSheet(placeId: String, topic: SheetTopic) {
        if (drivingState == DrivingState.PARKED) reasonSheet = ReasonSheet(placeId, topic)
    }

    fun closeReasonSheet() {
        reasonSheet = null
    }

    /**
     * 13 폰 계정 연결(시연). 서버 차량 연결 API가 없어 앱 안에서만 흉내 내고, 앱을 다시 켜면 풀린다.
     * 연결된 닉네임(가명). null이면 연결 안 됨
     */
    var linkedAccount by mutableStateOf<String?>(null)
        private set

    /** 13 "시연: 폰에서 승인" */
    fun demoLinkAccount() {
        linkedAccount = DEMO_NICKNAME
        toast("폰에서 승인했어요 · 계정이 연결됐어요(시연)")
    }

    /** 13 "연결 끊기" */
    fun unlinkAccount() {
        linkedAccount = null
        toast("연결을 끊었어요 · 이 기기에만 저장해요")
    }

    /** 10-1 축제 상세(오른쪽 시트, 02 이유 시트와 같은 틀). 정차 중에만 열리고 출발하면 닫힌다 */
    var festivalSheet by mutableStateOf<FestivalSheet?>(null)
        private set

    /** [km]는 10-1 카드와 같은 기준(차박지에서 축제까지) */
    fun openFestival(festival: Festival, km: Double) {
        if (drivingState == DrivingState.PARKED) festivalSheet = FestivalSheet(festival, km)
    }

    fun closeFestival() {
        festivalSheet = null
    }

    /** 숨은 "만든 사람" 화면. 정차(P단) 중에만 열리고 출발하면 닫힌다 */
    var creditsOpen by mutableStateOf(false)
        private set

    fun openCredits() {
        if (drivingState == DrivingState.PARKED) creditsOpen = true
    }

    fun closeCredits() {
        creditsOpen = false
    }

    // 주행 상태(F1 시작 · F4 전환)

    fun onDrivingState(state: DrivingState, settings: Settings) {
        val previous = drivingState
        drivingState = state
        if (state != DrivingState.PARKED) {
            // 출발하면 좌석 움직임을 바로 멈춘다(시트 제어는 주행 중 금지)
            campControl.stopSeat()
            // 주행 중에는 차량을 움직이지 않고 시선을 끄는 경보도 띄우지 않는다(Pleos 주행 안전 가이드).
            // 차박 모드는 켜 둔 채 환기만 멈추고, 다시 정차하면 이어 간다
            stopVentilation()
            coDemoPpm = null
            creditsOpen = false
            reasonSheet = null
            festivalSheet = null
            // F5: 정차 전용 화면·04는 닫고 01로, 첫 실행 중이면 20-1로([departTarget])
            val target = departTarget(stack, state, campPromptOpen = campPromptPlaceId != null)
            campPromptPlaceId = null
            if (target != null) replace(target)
        }
        if (!startHandled) {
            if (state == DrivingState.UNKNOWN) return
            startHandled = true
            replace(startRoute(state, settings))
            return
        }
        // 20-1: 첫 실행을 마치지 않았으면 차를 세웠을 때 16부터 다시 안내한다
        if (state == DrivingState.PARKED && previous != DrivingState.PARKED && !settings.onboardingDone) {
            replace(Route.Onboarding(0))
            return
        }
        if (state == DrivingState.PARKED && previous != DrivingState.PARKED) {
            // 환기 중에 출발했으면 열어 둔 창문을 닫고, 차박 모드면 환기를 이어 간다
            closeVentWindows()
            if (campPlaceId != null) appliedPreset?.let(::startVentilation)
        }
        if (state == DrivingState.PARKED && previous != DrivingState.PARKED) {
            when {
                !settings.onboardingDone -> replace(Route.Onboarding(0))
                campModeAvailable && campPlaceId == null && settings.askCampModeOnPark -> askOnPark()
            }
        }
    }

    /** F1 앱 시작: 첫 실행이면 16(주행이면 20-1), 차박 모드를 켜 둔 채 열면 05, 아니면 홈(주행이면 14-1) */
    private fun startRoute(state: DrivingState, settings: Settings): Route {
        val parked = state == DrivingState.PARKED
        return when {
            !settings.onboardingDone -> if (parked) Route.Onboarding(0) else Route.OnboardingDriving
            parked && campModeAvailable && campPlaceId != null && place(campPlaceId!!) != null -> Route.CampMode
            else -> Route.Home
        }
    }

    /** 차량 값이 끝내 오지 않을 때 시작 화면을 정한다(모르면 주행으로 본다). */
    fun onStartTimeout(settings: Settings) {
        if (startHandled) return
        startHandled = true
        replace(startRoute(DrivingState.UNKNOWN, settings))
    }

    /**
     * 06-A "주차했어요"는 차량 값을 못 읽는 동안만 쓴다.
     * 값을 다시 읽거나 시스템이 주행 중이라고 하면 풀어서, 다음에 또 못 읽을 때는 "모르면 주행"으로 돌아간다.
     */
    fun syncManualParked(vehicleState: DrivingState, uxRestricted: Boolean) {
        if (manualParked && (vehicleState != DrivingState.UNKNOWN || uxRestricted)) manualParked = false
    }

    /** 06-A "주차했어요": 시스템 주행 제한이 "제한 없음"일 때만 버튼이 켜지고 05-0으로 간다 */
    fun confirmParkedManually() {
        manualParked = true
        // 차박 모드를 쓸 수 없는 차면 정차 화면(02)으로만 간다
        if (!campModeAvailable) return replace(Route.Find())
        val placeId = campCandidate()?.id ?: return replace(Route.Home)
        beginCampMode(placeId)
    }

    // 차박 모드

    /**
     * 정차 감지(F4): 차가 차박지 근처에 섰을 때만 04를 묻는다([campPromptPlace]).
     * 위치는 내비에 물어 받는다. 받는 동안 다시 출발했거나 차박 모드를 켰으면 묻지 않는다.
     */
    private fun askOnPark() {
        scope.launch {
            val location = navi.currentLocation()
            if (drivingState != DrivingState.PARKED || campPlaceId != null || campPromptPlaceId != null) return@launch
            val arrivedByNavi = arrivalPlaceId != null && (navi.status.value as? NaviStatus.Arrived)?.placeId == arrivalPlaceId
            val place = campPromptPlace(places.state.value.places, location, arrivalPlace(), arrivedByNavi) ?: return@launch
            askCampMode(place.id)
        }
    }

    /**
     * 04 차박 모드를 켤까요? 정차 감지로는 장소당 한 번만 묻는다(답하지 않고 출발해도 물은 것으로 친다).
     * [force]는 메뉴·02에서 직접 요청한 것이라 언제든 띄운다.
     */
    private fun askCampMode(placeId: String?, force: Boolean = false) {
        if (placeId == null) {
            toast("근처에 차박지가 없어요")
            return
        }
        if (!force) {
            val asked = prefs.askedCampPlaceIds
            if (placeId in asked) return
            prefs.askedCampPlaceIds = asked + placeId
        }
        campPromptPlaceId = placeId
        // 정차 감지로 띄울 때만 먼저 말을 건다(메뉴·02에서 직접 연 것은 사용자가 이미 보고 있다)
        if (!force) place(placeId)?.let { speakQuietly(campPromptSpeech(it.name, arrived = arrivalPlaceId == it.id, canAnswer = voice.isAvailable)) }
    }

    /** 앱이 먼저 꺼내는 말: 읽을 수 없거나 내비 안내 중이면 토스트 없이 넘어간다(화면 카드가 같은 내용을 보여 준다) */
    private fun speakQuietly(text: String) {
        speaker.speak(text)
    }

    /** 04 "나중에": 홈으로 가고 이 장소에서는 다시 묻지 않는다 */
    fun dismissCampPrompt() {
        campPromptPlaceId?.let { prefs.askedCampPlaceIds = prefs.askedCampPlaceIds + it }
        campPromptPlaceId = null
        replace(Route.Home)
    }

    /** 차박 체크리스트에서 확인한 항목(이번 차박 동안만) */
    var checkedItems by mutableStateOf(emptySet<String>())

    fun toggleChecked(item: String) {
        checkedItems = if (item in checkedItems) checkedItems - item else checkedItems + item
    }

    var campOverlay by mutableStateOf(CampOverlay.NONE)
    var sleepAlerts by mutableStateOf(SleepAlerts())

    /** 05-6 "n분 뒤 다시"를 누른 뒤 다시 울릴 때(밀리초). null이면 정한 기상 시각에 울린다 */
    var wakeSnoozeUntil by mutableStateOf<Long?>(null)
        private set

    /** 잠든 화면에서 기상 시각(또는 다시 알림 시각)이 되면: 화면을 밝게 켜고 05-6을 덮는다. 소리는 05-6이 보이는 동안만 */
    fun ringWake() {
        wakeSnoozeUntil = null
        campOverlay = CampOverlay.WAKE_RING
    }

    fun startWakeSound() = alarmSound.playWake(sleepAlerts.wakeVoiceOnly)

    /** 05-6이 닫히면(버튼 · 출발 · 일산화탄소 경보) 기상 소리만 멈춘다. 경보 소리는 건드리지 않는다 */
    fun stopWakeSound() = alarmSound.stopWake()

    /** 05-7에 보여 줄 배터리 부족 내용. 창이 닫혀도 남겨 두고 다음 알림 때 바꾼다 */
    var lowBattery by mutableStateOf<LowBatteryAlert?>(null)
        private set

    /** 잠든 화면에서 배터리가 새로 20% 아래로: 화면을 켜고 05-7을 덮는다. 소리는 05-7이 보이는 동안만 */
    fun ringLowBattery(alert: LowBatteryAlert) {
        // 공조가 이미 꺼져 있으면 "줄여 주세요"가 맞지 않아, 띄우기 전에 차의 공조 전원을 읽는다(2초 안에 못 읽으면 그대로)
        if (!campControl.state.value.ready) return showLowBattery(alert)
        var shown = false
        val show: (Boolean?) -> Unit = { on -> scope.launch { if (!shown) { shown = true; showLowBattery(alert.copy(climateOn = on)) } } }
        campControl.readClimateOn(show)
        scope.launch { delay(2_000); show(null) }
    }

    private fun showLowBattery(alert: LowBatteryAlert) {
        lowBattery = alert
        campOverlay = CampOverlay.LOW_BATTERY
    }

    fun startLowBatterySound() {
        val alert = lowBattery ?: return
        alarmSound.playLowBattery(alert.speech)
    }

    /** 05-7 "공조 약하게" · "공조 끄기": 승인된 차에서만 보내고, 결과를 알린 뒤 다시 잠든 화면으로 */
    fun easeClimateForBattery(off: Boolean) {
        val report: (Boolean) -> Unit = { ok ->
            scope.launch {
                toast(
                    when {
                        ok && off -> "공조를 껐어요"
                        ok -> "풍량을 가장 낮게 했어요"
                        else -> "공조를 바꾸지 못했어요 · 차에서 직접 줄여 주세요"
                    },
                )
            }
        }
        if (off) campControl.climateOff(report) else campControl.lowerFan(report)
        campOverlay = CampOverlay.SLEEP
    }

    /**
     * 05-7에서 버튼으로 공조를 줄일 수 있는지: 차량 연동(공조 제어 권한)이 승인된 차.
     * 23에서 공조를 맞추지 않았어도 지금 켜진 공조의 풍량 · 전원을 바꾼다(실패하면 "직접 줄여 주세요" 알림)
     */
    val canEaseClimate: Boolean
        get() = Features.VEHICLE_CONTROL && campControl.state.value.linked

    /** 05-6 "n분 뒤 다시": 다시 잠든 화면으로, n분 뒤 또 울린다 */
    fun snoozeWake(nowMs: Long = System.currentTimeMillis()) {
        val minutes = sleepAlerts.snoozeMinutes.takeIf { it > 0 } ?: return dismissWake()
        wakeSnoozeUntil = nowMs + minutes * 60_000L
        campOverlay = CampOverlay.SLEEP
    }

    /** 05-6 "일어났어요": 알람을 끄고 10 철수 안내로 */
    fun dismissWake() {
        wakeSnoozeUntil = null
        campOverlay = CampOverlay.NONE
        navigate(Route.CampLeave)
        speak(DayPart.wokeSpeech())
    }

    /** 차박 모드를 켤 때 신호. 지금 신호와 다르면 05에서 "신호가 바뀌었어요" */
    val campStartSignal: Signal?
        get() = prefs.campStartSignal?.let { runCatching { Signal.valueOf(it) }.getOrNull() }

    /** 05 "확인했어요": 바뀐 신호를 기준으로 삼는다 */
    fun acknowledgeSignal(signal: Signal) {
        prefs.campStartSignal = signal.name
    }

    /** 23에서 마지막으로 맞춘 값. 05 차량 상태 카드에 보여 준다. */
    var appliedPreset by mutableStateOf<CampPreset?>(null)

    private var ventJob: Job? = null

    /** 23 "차박 시작": 값을 맞추고, 차박 모드 동안 고른 간격마다 환기한다 */
    fun applyCampPreset(preset: CampPreset, retry: Boolean) {
        if (retry) campControl.retryFailed(preset) else campControl.applyPreset(preset)
        appliedPreset = preset
        startVentilation(preset)
    }

    /** 정해진 환기로 창문을 열어 둔 상태. 닫기 전에 차박 모드를 끄거나 출발하면 정차한 뒤 닫는다 */
    private var ventOpen = false

    /** 05 차량 카드 표시용: 다음 환기 시각(ms). 환기가 멈춰 있거나 창문을 연 동안은 null */
    var ventNextAt by mutableStateOf<Long?>(null)
        private set

    /** 05 차량 카드 표시용: 환기로 연 창문을 닫을 시각(ms). 열려 있지 않으면 null */
    var ventCloseAt by mutableStateOf<Long?>(null)
        private set

    /** 환기 작업을 멈춘다(열어 둔 창문은 [closeVentWindows]가 닫는다) */
    private fun stopVentilation() {
        ventJob?.cancel()
        ventNextAt = null
        sleepVentAt = null
    }

    /**
     * 잠든 화면의 환기 알림 시각(ms). 차량 맞추기를 하지 않아 [ventNextAt]이 없을 때만 쓴다.
     * 다시 잠들어도 이어 쓰도록 여기 둔다(QA-04: 잠들 때마다 1시간 뒤로 밀리던 문제)
     */
    private var sleepVentAt: Long? = null

    /** 잠든 화면이 보여 주고 깨울 다음 환기 시각: 실제 환기 일정이 있으면 그 값, 없으면 이어 쓰는 알림 시각 */
    fun nextSleepVentAt(intervalMin: Long, nowMs: Long = System.currentTimeMillis()): Long {
        ventNextAt?.let { return it }
        val kept = sleepVentAt?.takeIf { it > nowMs }
        return kept ?: (nowMs + intervalMin * 60_000L).also { sleepVentAt = it }
    }

    /** 잠든 화면의 환기 알림이 울렸다: 다음 잠은 그때부터 다시 센다 */
    fun sleepVentFired() {
        sleepVentAt = null
    }

    /**
     * 창문은 닫아 두고, 고른 간격마다 고른 폭으로 [CampPreset.VENT_OPEN_MINUTES]분 열었다가 다시 닫는다.
     * 창문을 움직일 수 없거나 "알림만"이면 열어 달라는 알림만 띄운다. 차박 모드를 끄거나 출발하면 멈춘다(정차하면 다시 시작).
     */
    private fun startVentilation(preset: CampPreset) {
        stopVentilation()
        // 열어 둔 채 다시 시작하면(23에서 다시 맞춤) 먼저 닫는다
        closeVentWindows()
        ventJob = scope.launch {
            while (true) {
                ventNextAt = System.currentTimeMillis() + preset.ventIntervalMin * 60_000L
                delay(preset.ventIntervalMin * 60_000L)
                ventNextAt = null
                if (campPlaceId == null) break
                val width = preset.windowOpenPercent
                if (width != null && campControl.state.value.windows == ControlResult.Done) {
                    toast("환기할게요 · 창문을 ${preset.windowOpenCm} cm ${CampPreset.VENT_OPEN_MINUTES}분 열어요")
                    ventOpen = true
                    ventCloseAt = System.currentTimeMillis() + CampPreset.VENT_OPEN_MINUTES * 60_000L
                    campControl.ventWindows(width)
                    delay(CampPreset.VENT_OPEN_MINUTES * 60_000L)
                    closeVentWindows()
                } else {
                    toast("환기할 시간이에요 · 창문을 ${CampPreset.VENT_OPEN_MINUTES}분쯤 열어 주세요")
                }
            }
        }
    }

    /** 환기로 열어 둔 창문을 닫는다. 주행 중에는 차량을 움직이지 않으므로 정차한 뒤에 닫는다 */
    private fun closeVentWindows() {
        if (!ventOpen || drivingState != DrivingState.PARKED) return
        ventOpen = false
        ventCloseAt = null
        campControl.ventWindows(0)
    }

    /** 04 "차박 모드 켜기" → 05-0 도착 체크 */
    fun beginCampMode(placeId: String) {
        campPromptPlaceId = null
        checkedItems = emptySet()
        appliedPreset = null
        stopVentilation()
        campControl.clearResults()
        prefs.campStartSignal = place(placeId)?.signalFor(prefs.settings.value.primaryStyle)?.name
        setCampPlace(placeId)
        replace(Route.CampArrive)
    }

    /** 05-0 "다음": 차량 연동이 승인된 차면 23, 아니면 05 */
    fun finishArrival() {
        replace(if (Features.VEHICLE_CONTROL && campControl.state.value.linked) Route.VehicleLink else Route.CampMode)
    }

    /** 10 "철수 완료" → 10-1, 차박 모드를 끈다 */
    fun finishCamp() {
        val placeId = campPlaceId ?: return replace(Route.Find())
        // 체크 상태는 다음 차박을 켤 때 지운다(10-1 꺾쇠는 홈으로)
        setCampPlace(null)
        replace(Route.CampLeaveDone(placeId))
    }

    /** 05 "가까운 다른 곳 찾기"는 찾기(01·02)로, 05 "차박 모드 종료"는 홈(14)으로 */
    fun exitCampMode(to: Route = Route.Find()) {
        setCampPlace(null)
        replace(to)
    }

    private fun setCampPlace(placeId: String?) {
        if (placeId == null) {
            stopVentilation()
            closeVentWindows()
            coDemoPpm = null
            closeBatteryNight()
        } else if (campPlaceId == null) {
            markCampStartBattery(force = true)
        }
        campPlaceId = placeId
        prefs.activeCampPlaceId = placeId
    }

    // ── 밤사이 배터리(11-10): 시작 % · 10분마다 기록 · 예상 ──

    /** 05 "밤사이 n% 썼어요"의 시작 %와 시각. 차박을 켤 때 배터리를 못 읽었으면 처음 읽을 때 정한다 */
    var campStartBattery by mutableStateOf(prefs.campStartBattery)
        private set
    var campStartMs by mutableStateOf(prefs.campStartMs)
        private set

    private fun markCampStartBattery(force: Boolean) {
        if (!force && campStartBattery != null) return
        val pct = vehicle.snapshot.value.energy?.batteryPercent
        campStartBattery = pct
        campStartMs = pct?.let { System.currentTimeMillis() }
        prefs.campStartBattery = campStartBattery
        prefs.campStartMs = campStartMs
    }

    /** 차박 중이고 정차 중일 때만 기록한다. 기록은 이 기기 안에만 둔다 */
    private fun recordBatterySample() {
        if (campPlaceId == null || drivingState != DrivingState.PARKED) return
        val snap = vehicle.snapshot.value
        val pct = snap.energy?.batteryPercent ?: return
        markCampStartBattery(force = false)
        batteryLog.addSample(BatterySample(System.currentTimeMillis(), pct, snap.outsideTempC, appliedPreset != null))
    }

    private fun closeBatteryNight() {
        recordBatterySample()
        batteryLog.closeNight()
        campStartBattery = null
        campStartMs = null
        prefs.campStartBattery = null
        prefs.campStartMs = null
    }

    private val batteryWatch = scope.launch {
        while (true) {
            recordBatterySample()
            delay(BATTERY_SAMPLE_MS)
        }
    }

    /**
     * [hours] 뒤 배터리 예상. [useMeasured]면 그날 밤 실측을 먼저 쓴다(05). 23은 맞추기 전이라 지난 기록 → 어림값만.
     * 배터리 값을 모르면 null.
     */
    fun batteryForecast(preset: CampPreset?, hours: Float, useMeasured: Boolean): Forecast? {
        val snap = vehicle.snapshot.value
        val now = snap.energy?.batteryPercent ?: return null
        val capacity = snap.evCapacityWh ?: return null
        val measured = if (useMeasured) BatteryForecast.measuredRate(campStartBattery, campStartMs, now, System.currentTimeMillis()) else null
        val history = batteryLog.historyRate(BatteryForecast.tempBand(snap.outsideTempC), preset != null)
        val model = BatteryForecast.model(capacity, preset, snap.outsideTempC).total
        return BatteryForecast.forecast(now, hours, measured, history, model)
    }

    /** 23 아랫줄 항목별 몫(어림값 기준) */
    fun batteryRates(preset: CampPreset?): Rates? {
        val snap = vehicle.snapshot.value
        val capacity = snap.evCapacityWh ?: return null
        return BatteryForecast.model(capacity, preset, snap.outsideTempC)
    }

    /** 설정 12 "배터리 기록 지우기" */
    fun clearBatteryLog() {
        batteryLog.clear()
        toast("배터리 기록을 지웠어요")
    }

    /** 02 "차박 모드 켜기" */
    fun requestCampMode(placeId: String) {
        if (!campModeAvailable) return toast("차박 모드는 전기차에서만 쓸 수 있어요")
        when (drivingState) {
            DrivingState.PARKED -> askCampMode(placeId, force = true)
            DrivingState.UNKNOWN -> navigate(Route.ParkedConfirm)
            DrivingState.DRIVING -> toast("차박 모드는 정차 중에만 켤 수 있어요")
        }
    }

    fun arrivalPlace(): Place? = arrivalPlaceId?.let(::place)

    private fun campCandidate(): Place? {
        return selectedPlaceId?.let(::place) ?: places.state.value.places.minByOrNull { it.distanceKmFrom(origin) }
    }

    fun place(id: String): Place? = places.state.value.places.firstOrNull { it.id == id }

    // 내비

    /** 03: 목적지를 보내고 결과는 [NaviLauncher.status]로 받는다 */
    fun navigateTo(place: Place) {
        arrivalPlaceId = place.id
        navi.navigate(place)
        navigate(Route.NaviSent(place.id))
    }

    /** 내비가 도착을 알려 주면 그 장소를 도착 장소로 둔다(정차하면 04에서 이 장소로 묻는다). 장소 밖 목적지는 제외 */
    fun onNaviArrived(placeId: String) {
        if (!isPointId(placeId)) arrivalPlaceId = placeId
    }

    /** 10-1 축제 같은 장소 밖의 목적지 → 03. 차박 모드 도착 확인에는 쓰지 않는다. */
    fun navigateToPoint(name: String, location: LatLng) {
        navi.navigatePoint(name, location.latitude, location.longitude)
        navigate(Route.NaviSentPoint(name, location.latitude, location.longitude))
    }

    /** 길안내 중인 차박지(03-1·"안내 중" 띠). 내비가 안내를 시작했거나 내비 앱만 열린 동안 */
    fun guidingPlace(status: NaviStatus): Place? = when (status) {
        is NaviStatus.Started -> status.placeId
        is NaviStatus.Opened -> status.placeId
        else -> null
    }?.takeUnless(::isPointId)?.let(::place)

    /** 03-1 "내비 화면으로" */
    fun showNavigationScreen() {
        if (!navi.showNavigationScreen()) toast("내비 화면을 열지 못했어요 · 화면 아래 내비 아이콘을 눌러 주세요")
    }

    /** 03 "안내 취소" → 찾기(주행 중 01 · 정차 중 02) */
    fun cancelGuidance() {
        arrivalPlaceId = null
        navi.cancel()
        toast("길안내를 취소했어요")
        replace(Route.Find())
    }

    fun notice(message: String) = toast(message)

    // 일산화탄소 경보(05-2 ~ 05-4)

    /**
     * 시연 값(ppm). 실내 공기 센서(BLE) 연결이 없어서([Features.INDOOR_AIR_SENSOR]) 05 "BLE 연결하기"에서 고른다.
     * null이면 값 없음. 차박 모드를 끄면 지운다.
     */
    var coDemoPpm by mutableStateOf<Int?>(null)

    var coAlarm by mutableStateOf(CoAlarmState())
        private set

    private val coWatch = scope.launch {
        while (true) {
            val before = coAlarm
            // 차박 모드가 아니거나 주행 중이면 경보를 판정하지 않는다
            val watching = campPlaceId != null && drivingState == DrivingState.PARKED
            coAlarm = if (!watching) CoAlarmState() else nextCoAlarm(before, coDemoPpm, System.currentTimeMillis())
            val ringing = coAlarm.level.takeIf { coAlarm.ringing }
            if (ringing != before.level.takeIf { before.ringing }) {
                if (before.ringing && before.level == CoLevel.DANGER) speaker.stop()
                if (ringing == null) {
                    alarmSound.stop()
                } else {
                    // 잠든 화면이면 깨워서 경보를 보여 준다
                    campOverlay = CampOverlay.NONE
                    alarmSound.play(ringing)
                }
            }
            delay(CO_TICK_MS)
        }
    }

    /** 경보 "알림 끄기"(3단계는 화면에서 3초 길게 눌러야 여기로 온다) */
    fun dismissCoAlarm() {
        coAlarm = coAlarm.copy(dismissed = true)
    }

    /** 경보 "환기 시키기": 창문을 움직일 수 있으면 크게 열고, 아니면 직접 열어 달라고 알린다 */
    fun ventForCoAlarm() {
        if (Features.VEHICLE_CONTROL && campControl.state.value.linked) {
            campControl.ventWindows(CampPreset.VENT_OPEN_PERCENT)
            toast("창문을 크게 열어요")
        } else {
            toast("창문을 직접 열어 주세요")
        }
    }

    /**
     * 화면 아래에 잠깐 띄우는 알림 한 줄. 차 화면(Pleos)에서는 시스템 토스트가 보이지 않아 앱이 직접 그린다(AppRoot).
     * 같은 글을 다시 띄워도 새로 보이게 [Notice.id]를 바꾼다.
     */
    var currentNotice by mutableStateOf<Notice?>(null)
        private set

    private fun toast(message: String) {
        currentNotice = Notice(message)
    }

    fun clearNotice(shown: Notice) {
        if (currentNotice == shown) currentNotice = null
    }

    // 음성

    fun speak(text: String) {
        when (speaker.speak(text)) {
            SpeakResult.SPOKEN -> Unit
            SpeakResult.UNAVAILABLE -> toast("이 차에서는 읽어 주기를 쓸 수 없어요")
            SpeakResult.BUSY -> toast("다른 안내가 나오는 중이라 지금은 읽지 않아요")
        }
    }

    fun stopSpeaking() = speaker.stop()

    fun startVoiceSearch() {
        if (!voice.isAvailable) {
            toast("이 차에서는 음성 검색을 쓸 수 없어요")
            return
        }
        // 기상 알람이 울리는 중이면 소리를 멈춰야 말을 알아듣는다(명령이 없으면 다시 울린다)
        val ringing = campOverlay == CampOverlay.WAKE_RING
        if (ringing) stopWakeSound()
        stopSpeaking()
        // 마이크는 Pleos 음성 서비스가 다룬다(앱은 STT_SERVICE 권한만 선언, 문서대로)
        listening = true
        voice.listen { text ->
            listening = false
            if (text == null) {
                toast("잘 듣지 못했어요. 다시 말해 주세요")
                if (ringing && campOverlay == CampOverlay.WAKE_RING) startWakeSound()
            } else {
                runVoiceCommand(text)
                if (ringing && campOverlay == CampOverlay.WAKE_RING) startWakeSound()
            }
        }
    }

    /** 마지막으로 읽어 준 음성 후보(장소 번호). "두 번째로 안내해 줘"가 이 목록을 본다 */
    private var voiceCandidates: List<String> = emptyList()

    /** 말로 앱 조작: 인식한 문장을 명령으로 바꿔 실행하고 결과를 소리로 알려 준다 */
    fun runVoiceCommand(text: String) {
        val command = VoiceCommands.parse(text)
        if (!command.drivingAllowed && drivingState != DrivingState.PARKED) {
            speak("정차한 뒤 말해 주세요")
            return
        }
        val style = prefs.settings.value.primaryStyle
        when (command) {
            is VoiceCommand.Find -> voiceFind(command)
            VoiceCommand.ClearFilter -> {
                // 후보까지 비우지 않고, 지운 조건으로 가까운 곳을 다시 읽어 준다(QA-17)
                filter = filter.reset().copy(query = "")
                replace(Route.Find())
                readVoiceCandidates(filter, prefix = "필터를 지웠어요. ")
            }
            is VoiceCommand.NavigateNth -> {
                val id = voiceCandidates.getOrNull(command.index - 1)
                when {
                    voiceCandidates.isEmpty() -> speak("먼저 \"근처 차박지 찾아 줘\"라고 말해 주세요")
                    id == null -> speak("후보가 ${voiceCandidates.size}곳뿐이에요")
                    else -> place(id)?.let(::voiceNavigate)
                }
            }
            VoiceCommand.NavigateSelected -> {
                val target = voiceTargetPlace() ?: voiceCandidates.firstOrNull()?.let(::place)
                if (target == null) speak("어디로 안내할지 먼저 골라 주세요") else voiceNavigate(target)
            }
            is VoiceCommand.NavigateNamed -> {
                val match = voiceItems(FindFilter().withSignal(SignalScope.ALL).copy(query = command.query)).firstOrNull()
                if (match == null) speak("${Josa.eulReul(command.query)} 찾지 못했어요") else voiceNavigate(match.place)
            }
            VoiceCommand.ReadSummary -> {
                val p = voiceTargetPlace()
                if (p == null) speak("먼저 장소를 골라 주세요")
                else speak(summarySpeech(PlaceItem(p, p.signalFor(style), p.distanceKmFrom(origin), null), style))
            }
            VoiceCommand.CancelGuidance -> {
                cancelGuidance()
                speak("길안내를 취소했어요")
            }
            VoiceCommand.ShowNavigation -> showNavigationScreen()
            VoiceCommand.AddFavorite -> {
                val p = voiceTargetPlace()
                when {
                    p == null -> speak("먼저 장소를 골라 주세요")
                    p.id in favorites.ids.value -> speak("${p.name}은 이미 즐겨찾기에 있어요")
                    else -> {
                        favorites.toggle(p.id)
                        speak("${Josa.eulReul(p.name)} 즐겨찾기에 넣었어요")
                    }
                }
            }
            is VoiceCommand.Open -> {
                selectRail(command.item)
                speak("${command.item.title} 화면이에요")
            }
            is VoiceCommand.WakeAt -> voiceWakeAt(command)
            VoiceCommand.Snooze ->
                if (campOverlay == CampOverlay.WAKE_RING && sleepAlerts.snoozeMinutes > 0) {
                    snoozeWake()
                    speak("${sleepAlerts.snoozeMinutes}분 뒤에 다시 깨워 드릴게요")
                } else {
                    speak("지금 울리는 알람이 없어요")
                }
            VoiceCommand.WakeUp ->
                if (campOverlay == CampOverlay.WAKE_RING) dismissWake() else speak("지금 울리는 알람이 없어요")
            VoiceCommand.CampModeOn -> {
                val prompt = campPromptPlaceId
                when {
                    prompt != null -> {
                        beginCampMode(prompt)
                        speak("차박 모드를 켰어요")
                    }
                    !campModeAvailable -> speak("차박 모드는 전기차에서만 쓸 수 있어요")
                    campPlaceId != null -> {
                        replace(Route.CampMode)
                        speak("이미 차박 모드예요")
                    }
                    else -> askCampMode(campCandidate()?.id, force = true)
                }
            }
            VoiceCommand.CampModeLater ->
                if (campPromptPlaceId != null) {
                    dismissCampPrompt()
                    speak("알겠어요. 이곳에서는 다시 묻지 않을게요")
                } else {
                    speak("지금 묻고 있는 게 없어요")
                }
            VoiceCommand.Unknown -> speak("잘 모르겠어요. 예를 들어 \"근처 차박지 찾아 줘\", \"첫 번째로 안내해 줘\"처럼 말해 주세요")
        }
    }

    /** 찾기 명령: 말한 조건만 바꾸고(이름은 새로 말하지 않으면 지움) 가까운 3곳을 읽어 준다 */
    private fun voiceFind(command: VoiceCommand.Find) {
        command.style?.let { s -> prefs.update { it.copy(primaryStyle = s, styles = it.styles + s) } }
        var f = filter.copy(query = voiceFindQuery(filter.query, command))
        command.scope?.let { f = f.withSignal(it) }
        if (command.toilet) f = f.copy(toilet = true)
        filter = f
        replace(Route.Find())
        readVoiceCandidates(f)
    }

    /** 조건에 맞는 가까운 3곳을 음성 후보로 잡고 읽어 준다. "첫 번째로 안내해 줘"가 이 목록을 쓴다 */
    private fun readVoiceCandidates(f: FindFilter, prefix: String = "") {
        val style = prefs.settings.value.primaryStyle
        // 음성으로 고르는 후보에는 금지 장소를 넣지 않는다
        val top = voiceItems(f.copy(red = false)).take(3)
        voiceCandidates = top.map { it.place.id }
        selectedPlaceId = top.firstOrNull()?.place?.id
        if (top.isEmpty()) {
            speak(prefix + if (f == FindFilter()) "가까운 차박지가 없어요"
                else "조건에 맞는 곳을 찾지 못했어요. \"필터 지워 줘\"라고 말하면 다시 찾을 수 있어요")
        } else {
            val ordinal = listOf("첫째", "둘째", "셋째")
            speak(
                prefix + "가까운 ${top.size}곳을 찾았어요. " + top.mapIndexed { i, item ->
                    "${ordinal[i]}, ${item.place.name}, ${style.label} ${item.signal.label}, ${formatKm(item.distanceKm)}"
                }.joinToString(". ") + ". \"첫 번째로 안내해 줘\"처럼 말해 주세요",
            )
        }
    }

    private fun voiceItems(f: FindFilter): List<PlaceItem> {
        val style = prefs.settings.value.primaryStyle
        return places.state.value.places
            .map { PlaceItem(it, it.signalFor(style), it.distanceKmFrom(origin), null) }
            .applyFilter(f, favorites.ids.value)
            .sortedBy { it.distanceKm }
    }

    private fun voiceNavigate(place: Place) {
        selectedPlaceId = place.id
        navigateTo(place)
        speak("${Josa.euroRo(place.name)} 길안내를 시작할게요")
    }

    /** "7시에 깨워 줘" · "일출에 깨워 줘": 차박 모드에서만. 알람을 켜고 시각을 맞춘다 */
    private fun voiceWakeAt(command: VoiceCommand.WakeAt) {
        val camp = campPlaceId?.let(::place)
        if (camp == null) {
            speak("기상 알람은 차박 모드에서 맞출 수 있어요")
            return
        }
        val korea = java.time.ZoneId.of("Asia/Seoul")
        val minutes = command.minutesOfDay ?: run {
            val now = java.time.LocalDateTime.now(korea)
            val day = if (now.hour < 12) now.toLocalDate() else now.toLocalDate().plusDays(1)
            val rise = sunrise(day, camp.latitude, camp.longitude, korea) ?: return speak("이곳 일출 시각을 계산하지 못했어요")
            (rise.hour * 60 + rise.minute) / 5 * 5 + command.sunriseOffsetMin
        }
        sleepAlerts = sleepAlerts.wakeAt(minutes).copy(wake = true)
        speak("${sleepAlerts.wakeLabel}에 깨워 드릴게요. 잠들기를 누르면 알람이 켜져요")
    }

    // 데이터

    /** 개발자 모드: 서버를 바꾸면 장소·별점·축제를 새 서버에서 다시 받는다 */
    fun selectServer(env: ServerEnv) {
        if (env == prefs.settings.value.server) return
        prefs.update { it.copy(server = env) }
        Server.env = env
        scope.launch {
            reviews.clear()
            feeds.clear()
            places.switchServer()
            reviews.refreshRatings(useFallbackOnFailure = places.state.value.source == PlaceCatalog.Source.BUNDLE)
            feeds.refresh()
        }
        toast("${env.label} 서버로 바꿨어요 · 데이터를 다시 받아요")
    }

    /** 설정 "버전"을 7번 누르면 개발자 모드. 끄면 서버도 기본으로 돌린다 */
    fun setDeveloperMode(on: Boolean) {
        prefs.update { it.copy(developerMode = on) }
        if (!on) selectServer(Server.DEFAULT)
        toast(if (on) "개발자 모드를 켰어요" else "개발자 모드를 껐어요")
    }

    fun refreshData(force: Boolean = false) {
        scope.launch {
            places.refresh(force)
            reviews.refreshRatings(useFallbackOnFailure = places.state.value.source == PlaceCatalog.Source.BUNDLE)
            feeds.refresh()
        }
    }
}

/** 오른쪽 시트 하나: 장소 + 무엇을 보여 줄지 */
data class ReasonSheet(val placeId: String, val topic: SheetTopic)

sealed interface SheetTopic {
    /** 방식 칸의 이유. [style]이 null이면 취사·불 */
    data class Rule(val style: CampingStyle?) : SheetTopic
    data object Season : SheetTopic
    data object Caution : SheetTopic
    data object Reviews : SheetTopic
}

/** 차 위치를 한 번도 받지 못했을 때의 거리 기준(국토 가운데) */
private val COUNTRY_CENTER = LatLng(36.35, 127.85)

/** 내비에 차 위치를 다시 묻는 간격(받은 뒤 · 아직 못 받았을 때) */
private const val LOCATION_REFRESH_MS = 60_000L

/** 차박 중 배터리 기록 간격(10분) */
private const val BATTERY_SAMPLE_MS = 10 * 60_000L
private const val LOCATION_RETRY_MS = 3_000L

data class Notice(val text: String, val id: Long = System.nanoTime())

private const val CO_TICK_MS = 500L

data class FestivalSheet(val festival: Festival, val km: Double)

/** 13 시연용 닉네임(가명) */
private const val DEMO_NICKNAME = "새벽 차박러"
