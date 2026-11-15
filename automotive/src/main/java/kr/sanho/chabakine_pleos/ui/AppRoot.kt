package kr.sanho.chabakine_pleos.ui

import kr.sanho.chabakine_pleos.ui.components.PressOverlayIndication
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.LocalIndication
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import kr.sanho.chabakine_pleos.app.Notice
import kr.sanho.chabakine_pleos.ui.components.Label
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.CampOverlay
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.PlaceCatalog
import kr.sanho.chabakine_pleos.data.PlacesState
import kr.sanho.chabakine_pleos.ui.components.dataBannerText
import kr.sanho.chabakine_pleos.ui.components.DataStatus
import kr.sanho.chabakine_pleos.ui.components.DataStatusStrip
import kr.sanho.chabakine_pleos.ui.components.RailItem
import kr.sanho.chabakine_pleos.ui.components.Rail
import kr.sanho.chabakine_pleos.ui.credits.CreditsScreen
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.distanceKmFrom
import kr.sanho.chabakine_pleos.ui.camp.CampArriveScreen
import kr.sanho.chabakine_pleos.ui.camp.CampLeaveDoneScreen
import kr.sanho.chabakine_pleos.ui.camp.CampLeaveScreen
import kr.sanho.chabakine_pleos.ui.camp.CampModeScreen
import kr.sanho.chabakine_pleos.ui.camp.CampPromptOverlay
import kr.sanho.chabakine_pleos.ui.camp.NaviGuidingScreen
import kr.sanho.chabakine_pleos.ui.camp.NaviSentScreen
import kr.sanho.chabakine_pleos.ui.find.PhotoViewerScreen
import kr.sanho.chabakine_pleos.ui.find.ReasonPanel
import kr.sanho.chabakine_pleos.ui.camp.naviPointTarget
import kr.sanho.chabakine_pleos.ui.camp.naviTarget
import kr.sanho.chabakine_pleos.ui.camp.ParkedConfirmScreen
import kr.sanho.chabakine_pleos.ui.camp.CoAlarmOverlay
import kr.sanho.chabakine_pleos.ui.camp.FestivalPanel
import kr.sanho.chabakine_pleos.ui.camp.SleepAskOverlay
import kr.sanho.chabakine_pleos.ui.camp.CampGlassLayer
import kr.sanho.chabakine_pleos.ui.camp.CampPhotoBackdrop
import kr.sanho.chabakine_pleos.ui.camp.LowBatteryOverlay
import kr.sanho.chabakine_pleos.ui.camp.glassColors
import kr.sanho.chabakine_pleos.ui.camp.SleepOverlay
import kr.sanho.chabakine_pleos.ui.camp.WakeRingOverlay
import kr.sanho.chabakine_pleos.ui.camp.WakeSetOverlay
import kr.sanho.chabakine_pleos.ui.camp.VehicleLinkScreen
import kr.sanho.chabakine_pleos.ui.find.FavoritesScreen
import kr.sanho.chabakine_pleos.ui.onboarding.OnboardingDrivingScreen
import kr.sanho.chabakine_pleos.ui.onboarding.OnboardingScreen
import kr.sanho.chabakine_pleos.ui.settings.AccountLinkScreen
import kr.sanho.chabakine_pleos.ui.settings.SettingsScreen
import kr.sanho.chabakine_pleos.ui.find.FilterMoreScreen
import kr.sanho.chabakine_pleos.ui.find.DRIVING_LIST_LIMIT
import kr.sanho.chabakine_pleos.ui.find.FindScreen
import kr.sanho.chabakine_pleos.ui.find.ReviewListScreen
import kr.sanho.chabakine_pleos.ui.find.SPLIT_MODE_MAX_WIDTH_DP
import kr.sanho.chabakine_pleos.ui.find.SearchScreen
import kr.sanho.chabakine_pleos.ui.home.HomeActions
import kr.sanho.chabakine_pleos.ui.split.SplitCampLeave
import kr.sanho.chabakine_pleos.ui.split.SplitCampMode
import kr.sanho.chabakine_pleos.ui.home.HomeScreen
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.buildHomeData
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkColors
import kr.sanho.chabakine_pleos.ui.theme.LightColors
import kr.sanho.chabakine_pleos.ui.theme.ThemeMode
import kr.sanho.chabakine_pleos.ui.theme.ThemeScope
import kr.sanho.chabakine_pleos.ui.theme.resolveTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import kr.sanho.chabakine_pleos.vehicle.DrivingState

/** 앱 화면 전체: 왼쪽 메뉴 줄 + 현재 화면 + 04 차박 모드 묻기 */
@Composable
fun AppRoot(app: AppState) {
    val snapshot by app.vehicle.snapshot.collectAsState()
    val settings by app.prefs.settings.collectAsState()
    val favorites by app.favorites.ids.collectAsState()
    val placesState by app.places.state.collectAsState()
    val ratings by app.reviews.ratings.collectAsState()
    val festivals by app.feeds.festivals.collectAsState()
    val naviStatus by app.navi.status.collectAsState()
    val naviRemaining by app.navi.remaining.collectAsState()
    val guiding = app.guidingPlace(naviStatus)
    val campControl by app.campControl.state.collectAsState()

    val state = snapshot.effectiveState(app.manualParked)
    val parked = state == DrivingState.PARKED
    LaunchedEffect(state) { app.onDrivingState(state, app.prefs.settings.value) }
    LaunchedEffect(snapshot.drivingState, snapshot.uxRequiresDistractionOptimization) {
        app.syncManualParked(snapshot.drivingState, snapshot.uxRequiresDistractionOptimization == true)
    }
    LaunchedEffect(snapshot.uxActiveRestrictions) { app.uxActiveRestrictions = snapshot.uxActiveRestrictions }
    LaunchedEffect(Unit) {
        delay(START_TIMEOUT_MS)
        app.onStartTimeout(app.prefs.settings.value)
    }

    val homeData = remember(settings.primaryStyle, app.origin, placesState.places, ratings) {
        buildHomeData(placesState.places, ratings, settings.primaryStyle, app.origin)
    }
    val banner = dataBannerText(placesState)
    val dataStatus = dataStatus(placesState)
    val route = app.route

    // 화면 테마(08-1): 차박 모드 쪽 화면은 늘 야간, 나머지는 설정 12 "화면 테마"를 따른다
    val alwaysDark = route == Route.CampMode || route is Route.PhotoViewer ||
        app.campOverlay != CampOverlay.NONE || app.coAlarm.ringing || (app.creditsOpen && parked)
    val themeMode = resolveTheme(settings.theme, snapshot.nightMode, isSystemInDarkTheme(), alwaysDark)
    val colors = if (themeMode == ThemeMode.DAY) LightColors else DarkColors
    val view = LocalView.current
    SideEffect { view.rootView.setBackgroundColor(colors.background.toArgb()) }

    // 11-11 Pleos 가이드 Button interaction: 누르는 동안 바탕색이 바뀐다(기본 물결 대신, 모든 clickable)
    val pressOverlay = remember(colors.isDark) { PressOverlayIndication(if (colors.isDark) Color(0x14FFFFFF) else Color(0x0F16241D)) }
    ThemeScope(colors) { CompositionLocalProvider(LocalIndication provides pressOverlay) { BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Background)) {
        val split = maxWidth.value < SPLIT_MODE_MAX_WIDTH_DP
        // 11-08 차박 구역(05-0 · 23 · 10 · 10-1 · 06-A): 메뉴 줄 뒤까지 장소 사진을 깔고, 카드는 유리판으로.
        // 분할(07-3 · 07-5 · 07-6)도 같다. 넓은 05는 화면이 사진을 직접 깐다
        val splitCamp = split && route == Route.CampMode
        val campPhotoPlace = when {
            route.rail != RailItem.CAMP || (route == Route.CampMode && !split) -> null
            route is Route.CampLeaveDone -> app.place(route.placeId)
            else -> app.campPlaceId?.let(app::place)
        }
        val campGlass = route.rail == RailItem.CAMP && (route != Route.CampMode || splitCamp)
        if (campGlass) CampPhotoBackdrop(campPhotoPlace?.photoUrls?.firstOrNull())
        // 15 지도(11-08): 지도가 떠 있는 메뉴 줄 뒤까지 깔린다. 메뉴 줄은 지도 위에 따로 그린다
        val mapUnderRail = !split && route is Route.Find && route.map
        val railView: @Composable () -> Unit = {
            Rail(
                active = route.rail,
                onSelect = app::selectRail,
                // 값을 못 읽을 때(UNKNOWN)는 눌러서 06-A로 간다
                campLocked = state == DrivingState.DRIVING,
                onLogoSecret = app::openCredits,
                showCamp = app.campModeAvailable,
            )
        }
        ThemeScope(if (campGlass) glassColors(colors) else colors) { Row(Modifier.fillMaxSize()) {
            // 05 차박 모드 중에는 메뉴 줄을 숨기고 화면 전체를 쓴다(나가기는 05 오른쪽 위 "차박 모드 종료")
            if (!split && route.rail != null && route != Route.CampMode && route !is Route.NaviGuiding && !mapUnderRail) railView()
            // 24: 차박 화면(05-0 · 05 · 10 · 10-1 · 23 · 06-A)에도 데이터 상태 띠. 14 · 01 · 02는 화면 안에서 그린다
            val campStrip = route.rail == RailItem.CAMP && dataStatus != DataStatus.Hidden
            // 11-08: 넓은 화면에서 머리 줄 버튼이 화면 위 끝에 붙지 않게 조금 띄운다(사진·지도가 꽉 차는 화면은 제외)
            val fullBleed = mapUnderRail || route is Route.NaviGuiding || route == Route.CampMode || route is Route.PhotoViewer
            val topGap = if (!split && !fullBleed) TOP_GAP else 0.dp
            Column(Modifier.weight(1f).padding(top = topGap)) {
                if (campStrip) {
                    DataStatusStrip(
                        dataStatus,
                        onRetry = if (parked) ({ app.refreshData(force = true) }) else null,
                        modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 20.dp),
                    )
                }
                val content = Modifier.weight(1f)
                when (route) {
                    Route.Home -> HomeScreen(
                        data = homeData,
                        parked = parked,
                        style = settings.primaryStyle,
                        originText = app.originText,
                        favorites = favorites,
                        bannerText = banner,
                        dataStatus = dataStatus,
                        actions = HomeActions(
                            onVoice = app::startVoiceSearch,
                            onSearch = { app.navigate(Route.Search) },
                            onNavigate = { app.navigateTo(it.place) },
                            onToggleFavorite = { app.favorites.toggle(it.place.id) },
                            onShowAll = { app.replace(Route.Find()) },
                            onShowFavorites = { app.replace(Route.Favorites) },
                            onOpenPlace = {
                                app.selectedPlaceId = it.place.id
                                app.replace(Route.Find())
                            },
                            onRetryData = { app.refreshData(force = true) },
                        ),
                        modifier = content,
                        drivingListLimit = snapshot.uxMaxListItems ?: DRIVING_LIST_LIMIT,
                        guidingName = guiding?.name,
                        onOpenGuiding = { guiding?.let { app.navigate(Route.NaviGuiding(it.id)) } },
                    )
                    is Route.Find -> FindScreen(
                        app, homeData.nearby, parked, settings, favorites, banner, dataStatus, route.map,
                        drivingListLimit = snapshot.uxMaxListItems ?: DRIVING_LIST_LIMIT, modifier = content,
                        railInset = if (mapUnderRail) RAIL_WIDTH else 0.dp,
                    )
                    Route.Search -> SearchScreen(app, homeData.nearby, content)
                    Route.Favorites -> FavoritesScreen(app, homeData.nearby, favorites, parked, settings, banner, content)
                    is Route.Reviews -> ReviewListScreen(app, route.placeId, content)
                    Route.FilterMore -> FilterMoreScreen(app, homeData.nearby, favorites, settings, content)
                    is Route.NaviSent -> app.place(route.placeId)?.let { NaviSentScreen(app, naviTarget(app, it, settings), naviStatus, content) }
                    is Route.NaviGuiding -> app.place(route.placeId)?.let {
                        NaviGuidingScreen(app, it, naviStatus, naviRemaining, parked, settings, content, split = split)
                    } ?: FindFallback(app)
                    is Route.PhotoViewer -> app.place(route.placeId)?.let { PhotoViewerScreen(app, it, route.index, content) } ?: FindFallback(app)
                    is Route.NaviSentPoint -> NaviSentScreen(app, naviPointTarget(app, route.name, route.latitude, route.longitude, settings), naviStatus, content)
                    Route.CampArrive -> app.campPlaceId?.let(app::place)?.let {
                        CampArriveScreen(app, it, campControl.linked, banner, content)
                    } ?: FindFallback(app)
                    Route.CampMode -> app.campPlaceId?.let(app::place)?.let {
                        if (split) SplitCampMode(app, it, settings.primaryStyle, snapshot) else CampModeScreen(app, it, settings, snapshot, campControl, content)
                    } ?: FindFallback(app)
                    Route.CampLeave -> app.campPlaceId?.let(app::place)?.let { place ->
                        if (split) return@let SplitCampLeave(app, place)
                        val origin = LatLng(place.latitude, place.longitude)
                        val next = placesState.places
                            .filter { it.id != place.id }
                            .map { PlaceItem(it, it.signalFor(settings.primaryStyle), it.distanceKmFrom(origin), ratings[it.id]) }
                            .filter { it.signal == Signal.GREEN || it.signal == Signal.YELLOW }
                            .sortedBy { it.distanceKm }
                            .take(2)
                        CampLeaveScreen(app, place, next, banner, content)
                    } ?: FindFallback(app)
                    is Route.CampLeaveDone -> app.place(route.placeId)?.let {
                        CampLeaveDoneScreen(app, it, festivals, banner, content)
                    } ?: FindFallback(app)
                    Route.VehicleLink -> VehicleLinkScreen(app, app.campPlaceId?.let(app::place), campControl, snapshot.outsideTempC, content)
                    Route.ParkedConfirm -> ParkedConfirmScreen(app, uxUnrestricted = snapshot.uxRequiresDistractionOptimization != true, modifier = content)
                    Route.Settings -> SettingsScreen(app, settings, placesState, snapshot, content)
                    Route.AccountLink -> AccountLinkScreen(app, favorites.size, content)
                    is Route.Onboarding -> OnboardingScreen(app, route.step, route.replay, settings, content)
                    Route.OnboardingDriving -> OnboardingDrivingScreen(app, content)
                }
            }
        } }
        if (mapUnderRail) railView()
        // 04 차박 모드를 켤까요? (정차 중 한 번)
        app.campPromptPlaceId?.let(app::place)?.let { CampPromptOverlay(app, it, snapshot) }
        // 05 잠들기: 메뉴 줄까지 덮는다. 출발하면 닫는다
        LaunchedEffect(parked) { if (!parked) app.campOverlay = CampOverlay.NONE }
        val campPlace = app.campPlaceId?.let(app::place)
        when (app.campOverlay) {
            CampOverlay.NONE -> Unit
            CampOverlay.ASK_SLEEP -> SleepAskOverlay(app)
            CampOverlay.SLEEP -> SleepOverlay(app, snapshot)
            CampOverlay.WAKE_SET -> CampGlassLayer(campPlace) { WakeSetOverlay(app, campPlace) }
            CampOverlay.WAKE_RING -> WakeRingOverlay(app, campPlace, snapshot)
            CampOverlay.LOW_BATTERY -> CampGlassLayer(campPlace) { LowBatteryOverlay(app, snapshot) }
        }
        // 05-2 ~ 05-4 일산화탄소 경보: 차박 모드 중 어느 화면에서든 맨 위에
        app.coAlarm.takeIf { it.ringing }?.let { CoAlarmOverlay(app, it.level) }
        // 02 이유 시트: 오른쪽에서 밀려 나온다. 정차 중에만
        ReasonPanel(app, app.reasonSheet.takeIf { parked })
        // 10-1 축제 상세: 같은 오른쪽 시트
        FestivalPanel(app, app.festivalSheet.takeIf { parked })
        // 숨은 "만든 사람" 화면: 모든 것 위에. 정차 중에만(출발하면 닫힌다)
        if (app.creditsOpen && parked) CreditsScreen(onClose = app::closeCredits)
        app.currentNotice?.let { NoticeBar(it) { app.clearNotice(it) } }
    } } }
}

/**
 * 알림 한 줄: 화면 아래 가운데에 3초 보였다가 사라진다(시스템 토스트 대신).
 * 11-11 Pleos 가이드 Toast: 앱 화면 안 옆 · 아래 여백 24, 전체 최대 폭 792, 글 최대 폭 712
 */
@Composable
private fun BoxScope.NoticeBar(notice: Notice, onDone: () -> Unit) {
    LaunchedEffect(notice) {
        delay(NOTICE_MS)
        onDone()
    }
    Box(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
            .widthIn(max = 792.dp)
            .background(Palette.Text, RoundedCornerShape(40.dp))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 40.dp, vertical = 24.dp),
    ) {
        Label(notice.text, 30, FontWeight.Bold, Palette.Background, Modifier.widthIn(max = 712.dp), maxLines = 2)
    }
}

private const val NOTICE_MS = 3_000L

/** 왼쪽 메뉴 줄 자리 폭(설계서 11) */
private val RAIL_WIDTH = 132.dp

/** 차박 모드 장소가 없어진 경우 찾기로 돌아간다 */
@Composable
private fun FindFallback(app: AppState) {
    LaunchedEffect(Unit) { app.replace(Route.Find()) }
}

private const val START_TIMEOUT_MS = 2_000L

private fun dataStatus(state: PlacesState): DataStatus = when {
    state.refreshing -> DataStatus.Loading
    state.source == PlaceCatalog.Source.BUNDLE -> DataStatus.Bundled
    state.offline -> DataStatus.Offline(
        state.checkedAtMillis?.let { DateFormat.format("yyyy-MM-dd", it).toString() } ?: "이전",
    )
    else -> DataStatus.Hidden
}

/** 넓은 화면 내용 위 여백(머리 줄 버튼을 위 끝에서 띄움) */
private val TOP_GAP = 20.dp
