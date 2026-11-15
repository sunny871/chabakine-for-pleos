package kr.sanho.chabakine_pleos.ui.find

import kr.sanho.chabakine_pleos.ui.components.ChipRadius
import android.car.drivingstate.CarUxRestrictions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kr.sanho.chabakine_pleos.ui.theme.lampColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.clickable
import kr.sanho.chabakine_pleos.ui.home.GuidingBand
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import kr.sanho.chabakine_pleos.ui.components.DrivingPhotoDim
import kr.sanho.chabakine_pleos.ui.components.icon
import androidx.compose.material3.Icon
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import kr.sanho.chabakine_pleos.ui.components.LocalChipBackdrop
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.SheetTopic
import kr.sanho.chabakine_pleos.app.Features
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.ReviewPage
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.Amenities
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.components.DataStatus
import kr.sanho.chabakine_pleos.ui.components.DataStatusStrip
import kr.sanho.chabakine_pleos.ui.components.DataBanner
import kr.sanho.chabakine_pleos.ui.components.DemoBanner
import kr.sanho.chabakine_pleos.ui.components.FavoriteButton
import kr.sanho.chabakine_pleos.ui.components.InfoChip
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.PhotoTextShadow
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.pressShrink
import kr.sanho.chabakine_pleos.ui.components.PresetChip
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.RoundIcon
import kr.sanho.chabakine_pleos.ui.components.ScreenHeader
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.StyleSegment
import kr.sanho.chabakine_pleos.ui.components.VoiceButton
import kr.sanho.chabakine_pleos.ui.components.WarnChip
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.split.SplitFindDetail
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.vehicle.DrivingState
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.textColor

/** 화면 폭이 이보다 좁으면 분할 모드(07)로 그린다 */
const val SPLIT_MODE_MAX_WIDTH_DP = 1100

/** 주행 중 목록 개수. 차가 UX 제한 개수를 알려 주면 그 값을 쓴다 */
const val DRIVING_LIST_LIMIT = 6

/**
 * 화면설계서 01 찾기·주행 중(목록 왼쪽 · 요약 오른쪽) / 02 찾기·정차 중(상세 왼쪽 · 목록 오른쪽) / 07 분할 모드.
 * 주행 중에는 신호·이름·거리·별점·편의시설 아이콘과 안내·읽어 주기만 보여 준다.
 */
@Composable
fun FindScreen(
    app: AppState,
    items: List<PlaceItem>,
    parked: Boolean,
    settings: Settings,
    favorites: Set<String>,
    bannerText: String,
    dataStatus: DataStatus,
    mapMode: Boolean,
    drivingListLimit: Int,
    modifier: Modifier = Modifier,
    /** 15 지도(11-08): 지도가 메뉴 줄 뒤까지 깔릴 때 메뉴 줄 폭. 떠 있는 줄 · 카드는 그만큼 오른쪽에서 시작한다 */
    railInset: Dp = 0.dp,
) {
    val filtered = items.applyFilter(app.filter, favorites)
    val shown = if (parked) filtered else filtered.take(drivingListLimit)
    val selected = shown.firstOrNull { it.place.id == app.selectedPlaceId } ?: shown.firstOrNull()
    // 목록 화면이 대신 보여 주는 첫 장소도 음성 명령이 알 수 있게 알려 둔다(지도는 처음에 선택 없이 열려 빼다)
    val shownId = selected?.place?.id?.takeUnless { mapMode }
    DisposableEffect(shownId) {
        app.shownPlaceId = shownId
        onDispose { if (app.shownPlaceId == shownId) app.shownPlaceId = null }
    }
    val style = settings.primaryStyle
    val select: (PlaceItem) -> Unit = { item ->
        app.selectedPlaceId = item.place.id
        if (settings.readSummaryOnSelect) app.speak(summarySpeech(item, style))
    }
    var splitDetail by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth.value < SPLIT_MODE_MAX_WIDTH_DP) {
            // 07: 정차 중에 고르면 07-1 상세로 넘어간다(주행 중에는 목록과 안내만)
            if (parked && splitDetail && selected != null) {
                SplitFindDetail(app, selected, style) { splitDetail = false }
            } else {
                SplitFind(app, shown, selected, parked, style, app.originText) { item ->
                    select(item)
                    if (parked) splitDetail = true
                }
            }
            return@BoxWithConstraints
        }
        // 15 지도(11-08): 메뉴 줄 오른쪽 전체가 지도, 나머지는 지도 위에 떠 있다
        if (mapMode) {
            MapScreen(app, filtered, parked, settings, dataStatus, select, railInset)
            return@BoxWithConstraints
        }
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "주변 차박지",
                subtitle = "${app.originText} · " + (app.filter.maxDistanceKm?.let { "$it km 안 · " } ?: "") + if (mapMode) "지도" else "가까운 순",
            ) {
                StyleSegment(style) { s -> app.prefs.update { it.copy(primaryStyle = s, styles = it.styles + s) } }
                StatusPill(parked)
                if (parked) RoundIcon(LineIcon.Search, "검색", 88.dp, 42.dp, Palette.Sub, { app.navigate(Route.Search) })
                VoiceButton(app.listening) { app.startVoiceSearch() }
            }
            DemoBanner(bannerText, Modifier.padding(horizontal = 36.dp))
            DataStatusStrip(
                dataStatus,
                onRetry = if (parked) ({ app.refreshData(force = true) }) else null,
                modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 12.dp),
            )
            PresetRow(app, parked, mapMode, settings)
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(start = 36.dp, end = 36.dp, top = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                when {
                    mapMode -> FindMap(app, filtered, Modifier.fillMaxSize(), select)
                    shown.isEmpty() -> EmptyResult(app, Modifier.fillMaxSize())
                    parked -> {
                        selected?.let { ParkedDetail(app, it, style, favorites, Modifier.weight(1f).fillMaxHeight()) }
                        PlaceList(shown, selected, parked = true, more = false, Modifier.width(480.dp).fillMaxHeight(), select)
                    }
                    else -> {
                        PlaceList(shown, selected, parked = false, more = filtered.size > shown.size, Modifier.width(660.dp).fillMaxHeight(), select)
                        selected?.let { DrivingSummary(app, it, style, favorites, Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }
        }
    }
}

/**
 * 화면설계서 15 지도(11-08): 지도가 메뉴 줄 오른쪽을 꽉 채우고, 제목 · 내 방식 · 정차 표시 · 음성,
 * 신호 칩 · 화장실 · 취사 가능 · 필터, 데이터 띠, 확대 · 축소 · 내 위치가 그 위에 떠 있다.
 */
@Composable
private fun MapScreen(
    app: AppState,
    items: List<PlaceItem>,
    parked: Boolean,
    settings: Settings,
    dataStatus: DataStatus,
    onSelect: (PlaceItem) -> Unit,
    railInset: Dp,
) {
    val style = settings.primaryStyle
    val float = Palette.Background.copy(alpha = 0.92f)
    Box(Modifier.fillMaxSize()) {
        FindMap(app, items, Modifier.fillMaxSize(), onSelect, fullBleed = true, insetStart = railInset)
        Column(Modifier.fillMaxWidth().padding(start = railInset + 28.dp, end = 28.dp, top = 44.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    Modifier.height(108.dp).background(float, RoundedCornerShape(30.dp)).padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Label("주변 차박지 · 지도", 32, FontWeight.ExtraBold)
                    val km = app.filter.maxDistanceKm?.let { "$it km 안 " } ?: ""
                    Label("${app.originText} · $km${items.size}곳", 22, FontWeight.SemiBold, Palette.SubText)
                }
                Box(Modifier.background(float, RoundedCornerShape(30.dp))) {
                    StyleSegment(style) { s -> app.prefs.update { it.copy(primaryStyle = s, styles = it.styles + s) } }
                }
                Spacer(Modifier.weight(1f))
                StatusPill(parked)
                VoiceButton(app.listening, size = 108.dp) { app.startVoiceSearch() }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompositionLocalProvider(LocalChipBackdrop provides float) {
                    Box(Modifier.weight(1f)) { PresetRow(app, parked, mapMode = true, settings = settings, floating = true) }
                }
                if (dataStatus != DataStatus.Hidden) {
                    DataStatusStrip(dataStatus, onRetry = null, modifier = Modifier.width(620.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaceList(items: List<PlaceItem>, selected: PlaceItem?, parked: Boolean, more: Boolean, modifier: Modifier, onSelect: (PlaceItem) -> Unit) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items, key = { it.place.id }) { item ->
            PlaceRow(item, selected = item.place.id == selected?.place?.id, parked = parked) { onSelect(item) }
        }
        if (more) {
            item { Label("더 많은 곳은 정차하면 보여요", 24, FontWeight.Medium, Palette.MutedText, Modifier.padding(top = 8.dp, bottom = 4.dp)) }
        }
    }
}

/**
 * 칩 줄(화면설계서 01 · 02, 10-30 정리).
 * 주행 중(01): 목록·지도 · 신호 ▾ · 화장실. 정차 중(02): 여기에 취사 가능 · 초기화(걸린 게 있을 때) · 필터 n ›(22).
 */
@Composable
private fun PresetRow(app: AppState, parked: Boolean, mapMode: Boolean, settings: Settings, floating: Boolean = false) {
    val f = app.filter
    // 왼쪽 칩들은 넘겨 볼 수 있고, 초기화 · 필터 n은 오른쪽 끝에 붙인다
    Row(
        Modifier.fillMaxWidth().padding(start = if (floating) 0.dp else 36.dp, end = if (floating) 0.dp else 36.dp, top = if (floating) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 11-08: 목록 · 지도 토글은 메뉴 줄(지도 · 목록)로 옮겼다
            // 02-1 "검색"으로 거른 목록. 누르면 검색을 푼다
            if (f.query.isNotBlank()) PresetChip("'${f.query}' 검색 결과 · 지우기 ✕", true, { app.filter = f.copy(query = "") }, LineIcon.Search, CHIP_ROW_HEIGHT)
            // 시스템이 거르기를 막으면(UX_RESTRICTIONS_NO_FILTERING) 칩을 숨긴다
            if (!app.uxRestricted(CarUxRestrictions.UX_RESTRICTIONS_NO_FILTERING)) {
                SignalChip(f.signalScope) { app.filter = app.filter.withSignal(it) }
                PresetChip("화장실", f.toilet, { app.filter = f.copy(toilet = !f.toilet) }, if (f.toilet) LineIcon.Check else null, CHIP_ROW_HEIGHT)
                if (parked) PresetChip("취사 가능", f.cookAllowed, { app.filter = f.copy(cookAllowed = !f.cookAllowed) }, if (f.cookAllowed) LineIcon.Check else null, CHIP_ROW_HEIGHT)
            }
        }
        if (parked) {
            if (f.anyOn) {
                Box(
                    Modifier.height(CHIP_ROW_HEIGHT).clip(RoundedCornerShape(ChipRadius)).clickable(role = Role.Button) { app.filter = f.reset() }.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Label("초기화", 26, FontWeight.Bold, Palette.SubText) }
            }
            FilterMoreChip(f.moreCount) { app.navigate(Route.FilterMore) }
        }
    }
}

/**
 * 신호 칩: 작은 신호등(켜진 불 = 보이는 신호) + 이름. 누르면 같은 화면에서 셋 중 하나를 고른다.
 * 11-11 Pleos 가이드 Dropdown: 꺾쇠 아이콘(열리면 위로), 둥근 사각 상자, 고른 줄은 옅은 바탕 + 오른쪽 ✓(Suffix),
 * 앞쪽 신호등(Prefix). 설명은 이름 아래 작은 글로.
 */
@Composable
private fun SignalChip(scope: SignalScope, onPick: (SignalScope) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.pressShrink().height(CHIP_ROW_HEIGHT).clip(RoundedCornerShape(ChipRadius)).background(Palette.Text)
                .clickable(role = Role.Button, onClickLabel = "신호 고르기") { open = true }
                .padding(start = 14.dp, end = 20.dp)
                .semantics { contentDescription = "신호 ${scope.label}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScopeLamp(scope, 22.dp)
            Label(scope.chipLabel, 26, FontWeight.ExtraBold, Palette.Background)
            LineIconView(if (open) LineIcon.ChevronUp else LineIcon.ChevronDown, 30.dp, Palette.Background, strokeWidth = 2.6f)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.width(460.dp).padding(vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            containerColor = Palette.CardInner,
            offset = DpOffset(0.dp, 10.dp),
        ) {
            SignalScope.entries.forEach { s ->
                val on = s == scope
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ScopeLamp(s, 20.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Label(s.label, 28, if (on) FontWeight.ExtraBold else FontWeight.Bold)
                                Label(s.hint, 22, FontWeight.Medium, Palette.MutedText)
                            }
                            if (on) LineIconView(LineIcon.Check, 32.dp, Palette.Text, strokeWidth = 2.6f)
                        }
                    },
                    onClick = {
                        open = false
                        onPick(s)
                    },
                    modifier = Modifier.height(104.dp).padding(horizontal = 10.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp)).background(if (on) Palette.Sub else Color.Transparent),
                )
            }
        }
    }
}

/** 가로 신호등 3칸. 허용만이면 초록만, 허용 + 조건부면 초록·노랑, 전부 보기면 셋 다 켠다 */
@Composable
private fun ScopeLamp(scope: SignalScope, dot: Dp) {
    val lit = when (scope) {
        SignalScope.ALLOWED -> setOf(Signal.GREEN)
        SignalScope.WITH_CONDITIONAL -> setOf(Signal.GREEN, Signal.YELLOW)
        SignalScope.ALL -> setOf(Signal.GREEN, Signal.YELLOW, Signal.RED)
    }
    Row(
        Modifier.background(Palette.LampBody, RoundedCornerShape(dot)).padding(horizontal = dot / 3, vertical = dot / 4),
        horizontalArrangement = Arrangement.spacedBy(dot / 4),
    ) {
        listOf(Signal.GREEN, Signal.YELLOW, Signal.RED).forEach { sig ->
            Box(Modifier.size(dot).background(if (sig in lit) sig.lampColor else Palette.LampOff, CircleShape))
        }
    }
}

/** "필터 n ›": 22에서 건 조건 수(0이면 숫자 없이) */
@Composable
private fun FilterMoreChip(count: Int, onClick: () -> Unit) {
    Row(
        Modifier.height(CHIP_ROW_HEIGHT).clip(RoundedCornerShape(ChipRadius)).background(LocalChipBackdrop.current).border(2.dp, Palette.Gauge, RoundedCornerShape(ChipRadius))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 28.dp)
            .semantics { contentDescription = "필터 더 보기 · ${count}개 걸림" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LineIconView(LineIcon.Sliders, 30.dp, Palette.Text)
        Label("필터", 26, FontWeight.Bold)
        if (count > 0) {
            Box(Modifier.size(40.dp).background(Palette.Accent, CircleShape), contentAlignment = Alignment.Center) {
                Label(count.toString(), 24, FontWeight.ExtraBold, Palette.OnAccent)
            }
        }
        Label("›", 30, FontWeight.Bold)
    }
}

/** 장소 사진 + 위에서 아래로 어둡게 덮는 그라데이션(01 요약 카드 배경). 주행 중 화면이라 한 겹 더 어둡게 */
@Composable
private fun BoxScope.CardPhoto(url: String?) {
    if (url == null) return
    AsyncImage(url, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x8C08090B), 0.55f to Color(0xC708090B), 1f to Color(0xEB08090B))))
    DrivingPhotoDim(driving = true)
}

/** 01 오른쪽 요약 카드 */
@Composable
private fun DrivingSummary(app: AppState, item: PlaceItem, style: CampingStyle, favorites: Set<String>, modifier: Modifier) {
    DarkScope(item.place.photoUrls.isNotEmpty()) {
    Box(modifier.card()) {
        CardPhoto(item.place.photoUrls.firstOrNull())
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SignalLamp(item.signal, 48.dp, showSymbol = true)
                Label(item.signal.label, 34, FontWeight.ExtraBold, item.signal.textColor)
                Spacer(Modifier.weight(1f))
                FavoriteButton(item.place.id in favorites, { app.favorites.toggle(item.place.id) })
            }
            Label("리뷰 글과 방식별 상세는 정차하면 펼쳐져요", 24, FontWeight.Medium, Palette.MutedText)
            Label(item.place.name, 56, FontWeight.ExtraBold, maxLines = 2)
            // 시스템이 글 길이를 줄이라고 하면(UX_RESTRICTIONS_LIMIT_STRING_LENGTH) 한 줄만
            val shortText = app.uxRestricted(CarUxRestrictions.UX_RESTRICTIONS_LIMIT_STRING_LENGTH)
            Label(item.place.styleReason(style), 32, FontWeight.Medium, Palette.SoftText, maxLines = if (shortText) 1 else 2, lineHeight = 1.4.em)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(formatKm(item.distanceKm))
                item.rating?.let { InfoChip("★ ${it.average} · 리뷰 ${it.count}개") }
            }
            // 01 주행 중: 한눈에 보도록 "확인 안 됨"은 빼고 확인된 것만. 하나도 없으면 칸째 숨긴다
            if (hasKnownAmenity(item.place)) {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Label("편의시설", 28, FontWeight.ExtraBold)
                    Box(Modifier.width(48.dp).height(3.dp).background(Palette.ChipBorder, RoundedCornerShape(2.dp)))
                }
                AmenityGrid(item.place, labeled = false, hideUnknown = true)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // 설계서 01: 화살표 없이 아이콘 + 글자(오른쪽 읽어 주기 버튼과 나란히라 글자가 잘리지 않게)
                PrimaryButton("여기로 안내", { app.navigateTo(item.place) }, Modifier.weight(1f), height = 114.dp, textSize = 36, trailingChevron = false)
                SecondaryButton(
                    "요약·리뷰 읽어 주기",
                    { app.speak(summarySpeech(item, style)) },
                    height = 114.dp,
                    textSize = 26,
                    icon = LineIcon.Volume,
                )
            }
        }
    }
    }
}

/**
 * 02 왼쪽 상세: 사진 머리 + 방식별 가능 여부·현장 조건·편의시설 | 계절·시간·주의사항·리뷰 요약, 아래 버튼.
 * 방식 칸 · 취사·불 칸 · 계절·시간 · 주의사항을 누르면 오른쪽 이유 시트(정차 중).
 */
@Composable
private fun ParkedDetail(app: AppState, item: PlaceItem, style: CampingStyle, favorites: Set<String>, modifier: Modifier) {
    val place = item.place
    val reviews by produceState<ReviewPage?>(null, place.id) { value = app.reviews.reviews(place.id) }
    val photos = place.photoUrls
    val pager = key(place.id) { rememberPagerState { photos.size } }
    Box(modifier.card()) {
        // 카드 배경(11-08): 사진 한 장을 카드 전체에 깔고, 위는 또렷하게 · 아래로 갈수록 흐리게.
        // 흐린 사진 위에 또렷한 사진을 겹치고, 또렷한 쪽을 아래로 갈수록 투명하게 지운다(경계선 없음)
        photos.getOrNull(pager.currentPage)?.let { url ->
            ProgressiveBlurPhoto(url, Modifier.matchParentSize())
            // 글이 잘 보이게: 위에서 아래로 한 번만 차츰 짙게(제목 줄에 따로 어두운 띠를 두지 않는다 · 제목은 글자 그림자로)
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Palette.Background.copy(alpha = 0.1f), 0.4f to Palette.Background.copy(alpha = 0.66f),
                        1f to Palette.Background.copy(alpha = 0.9f),
                    ),
                ),
            )
        }
        Column(Modifier.fillMaxSize()) {
            PhotoHeader(
                place, item, pager, favorites.contains(place.id),
                onOpenPhotos = { app.navigate(Route.PhotoViewer(place.id, it)) },
            ) { app.favorites.toggle(place.id) }
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(start = 36.dp, end = 36.dp, top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("방식별 가능 여부", 28, FontWeight.ExtraBold)
                    val parked = app.drivingState == DrivingState.PARKED
                    StyleTiles(place, style, parked) { s -> app.openSheet(place.id, SheetTopic.Rule(s)) }
                    Label("현장 조건", 28, FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                    SiteConditionTiles(place)
                    Label("편의시설", 28, FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                    AmenityGrid(place, labeled = true, Modifier.fillMaxWidth(), onCookClick = { app.openSheet(place.id, SheetTopic.Rule(null)) }.takeIf { parked })
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val parked = app.drivingState == DrivingState.PARKED
                    if (place.seasonRules.isNotEmpty()) {
                        NoteCard("계절·시간", place.seasonRules, NOTE_SEASON, Palette.SoftText,
                            onOpen = { app.openSheet(place.id, SheetTopic.Season) }.takeIf { parked })
                    }
                    // 과태료 · 견인 · 출입 제한 · 소음 같은 주의사항(데이터 notes)
                    if (place.notes.isNotEmpty()) {
                        NoteCard("주의사항", place.notes, NOTE_CAUTION, Palette.BannerText, Modifier.padding(top = 4.dp),
                            onOpen = { app.openSheet(place.id, SheetTopic.Caution) }.takeIf { parked })
                    }
                    ReviewSummary(app, place, reviews, item)
                }
            }
            Row(Modifier.padding(start = 36.dp, end = 36.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryButton("여기로 안내", { app.navigateTo(place) }, Modifier.weight(1f), height = 108.dp, textSize = 32, trailingChevron = false)
                if (app.campModeAvailable) SecondaryButton("차박 모드 켜기", { app.requestCampMode(place.id) }, Modifier.weight(1f), textSize = 30, icon = LineIcon.Tent)
            }
        }
    }
}

/** 방식 칸 바탕: 신호 색을 옅게(내 방식 칸은 조금 더 진하게) */
@Composable
private fun Signal.tileColor(selected: Boolean): Color =
    if (this == Signal.UNKNOWN) Palette.Text.copy(alpha = if (selected) 0.12f else 0.06f)
    else lampColor.copy(alpha = if (selected) 0.34f else 0.22f)


@Composable
private fun PhotoHeader(place: Place, item: PlaceItem, pager: PagerState, saved: Boolean, onOpenPhotos: (Int) -> Unit, onFavorite: () -> Unit) {
    val photos = place.photoUrls
    // 사진 여러 장: 좌우로 밀어 넘기고, 누르거나 "크게 보기"로 02-2. 정차 중 화면이라 넘기기를 허용한다
    DarkScope(photos.isNotEmpty()) {
    Box(Modifier.fillMaxWidth().height(200.dp).background(if (photos.isEmpty()) Palette.Sub else Color.Transparent)) {
        if (photos.isNotEmpty()) {
            // 사진은 카드 바탕(한 장)으로 그린다. 여기는 좌우로 밀어 넘기기 · 눌러서 크게 보기만
            HorizontalPager(pager, Modifier.matchParentSize(), key = { it }) { page ->
                Box(Modifier.fillMaxSize().clickable(role = Role.Button, onClickLabel = "사진 크게 보기") { onOpenPhotos(page) })
            }
            Row(
                Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (photos.size > 1) {
                    Box(Modifier.height(52.dp).background(Color(0xD9101A16), RoundedCornerShape(26.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        Label("${pager.currentPage + 1} / ${photos.size}", 24, FontWeight.ExtraBold)
                    }
                }
                Box(
                    Modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xD9101A16))
                        .clickable(role = Role.Button) { onOpenPhotos(pager.currentPage) }.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Label("크게 보기", 24, FontWeight.Bold) }
            }
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Label(place.name, 50, FontWeight.ExtraBold, modifier = Modifier.weight(1f, fill = false), shadow = PhotoTextShadow)
                    ConfidenceShield(place.confidence, shadow = PhotoTextShadow)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LineIconView(LineIcon.Pin, 28.dp, Palette.SoftText, shadow = PhotoTextShadow)
                        Label(formatKm(item.distanceKm), 26, FontWeight.Bold, Palette.SoftText, shadow = PhotoTextShadow)
                    }
                    item.rating?.let { Label("★ ${it.average}", 26, FontWeight.Bold, Palette.SoftText, shadow = PhotoTextShadow) }
                }
                val meta = listOfNotNull(
                    place.site?.type,
                    item.rating?.let { "리뷰 ${it.count}개" },
                    place.verifiedAt,
                    place.evidence.takeIf { it.isNotEmpty() }?.joinToString("·") ?: place.sourceName,
                ).joinToString(" · ")
                Label(meta, 24, FontWeight.Bold, Palette.SoftText, shadow = PhotoTextShadow)
            }
            FavoriteButton(saved, onFavorite, background = Palette.Background)
        }
    }
    }
}

/**
 * 방식별 가능 여부 세 칸(스텔스 · 도킹형 · 루프탑형): 아이콘 · 이름 · 신호 점과 글자만.
 * 칸 바탕은 신호 색(허용 초록 · 조건부 노랑 · 금지 빨강)을 옅게, 미확인은 회색.
 * 내 방식 칸은 색을 조금 더 진하게 하고 오른쪽 위에 파란 체크를 단다. 이유는 칸을 눌러 시트에서 본다.
 * 누르면 그 방식의 이유 시트를 연다. 주행 중에는 누를 수 없고 "주차 후에 볼 수 있어요"를 작게 보여 준다.
 */
@Composable
private fun StyleTiles(place: Place, mine: CampingStyle, parked: Boolean, onOpen: (CampingStyle) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CampingStyle.entries.forEach { s ->
            val signal = place.signalFor(s)
            val on = s == mine
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                    .background(signal.tileColor(on))
                    .clickable(enabled = parked, role = Role.Button, onClickLabel = "${s.label} 이유 보기") { onOpen(s) }
                    .semantics { selected = on },
            ) {
                if (on) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Palette.Accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { LineIconView(LineIcon.Check, 18.dp, Palette.OnAccent, strokeWidth = 3f) }
                }
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(s.icon, null, Modifier.size(36.dp), tint = if (on) Palette.Text else Palette.SubText)
                    Label(if (s == CampingStyle.ROOFTOP) "루프탑형" else s.label, 24, FontWeight.ExtraBold, if (on) Palette.Text else Palette.SoftText)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(12.dp).background(signal.lampColor, CircleShape))
                        Label(signal.label, 24, FontWeight.Bold, signal.textColor)
                    }
                }
            }
        }
    }
    if (!parked) Label("주차 후에 볼 수 있어요", 24, FontWeight.Medium, Palette.MutedText)
}

/**
 * 계절·시간 · 주의사항 카드(이유 시트의 이유 카드와 같은 꼴): 왼쪽에 색 띠, 제목 · 개수, 첫 항목 두 줄 미리 보기.
 * 누르면([onOpen]) 오른쪽 시트에 항목 전체를 자르지 않고 보여 준다. 정차 중에만 누를 수 있다.
 */
@Composable
private fun NoteCard(title: String, lines: List<String>, accent: Color, titleColor: Color, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).card(20.dp, Palette.CardInner.copy(alpha = 0.86f))
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClickLabel = "$title 전체 보기", onClick = onOpen) else Modifier)
            .semantics(mergeDescendants = true) {},
    ) {
        // 11-11 설계서 02: 왼쪽 색 띠는 6 폭으로 카드 위아래 끝까지(모서리는 카드가 자른다)
        Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(title, 24, FontWeight.ExtraBold, titleColor, Modifier.weight(1f))
                Label(if (onOpen != null) "${lines.size}가지 ›" else "${lines.size}가지", 24, FontWeight.Bold, Palette.SubText)
            }
            Label(lines.first(), 24, FontWeight.Medium, Palette.Text, maxLines = 2, lineHeight = 1.45.em)
        }
    }
}

@Composable
private fun ReviewSummary(app: AppState, place: Place, page: ReviewPage?, item: PlaceItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
        val avg = page?.takeIf { it.count > 0 }?.average ?: item.rating?.average
        Label("리뷰 요약", 28, FontWeight.ExtraBold)
        avg?.let { Label("★ ${"%.1f".format(it)}", 28, FontWeight.ExtraBold, Palette.BannerText) }
        Spacer(Modifier.weight(1f))
        SecondaryButton("리뷰 보기", { app.openSheet(place.id, SheetTopic.Reviews) }, height = 72.dp, textSize = 24)
    }
    val items = page?.items.orEmpty()
    if (items.isEmpty()) {
        Label("아직 리뷰가 없어요 · 리뷰 쓰기는 폰 앱에서 해요", 24, FontWeight.Medium, Palette.SubText, maxLines = 2)
    } else {
        items.take(2).forEach { r ->
            Label("\"${r.text}\"" + if (r.visited) " · 방문 확인" else "", 24, FontWeight.Medium, Palette.SoftText, maxLines = 2)
        }
    }
}

@Composable
private fun EmptyResult(app: AppState, modifier: Modifier) {
    Column(modifier.card().padding(64.dp), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        Label("조건에 맞는 차박지가 없어요", 44, FontWeight.ExtraBold)
        Label("필터를 줄이거나 거리 제한을 풀어 보세요", 30, FontWeight.Medium, Palette.SubText)
        SecondaryButton("필터 초기화", { app.filter = app.filter.reset() }, Modifier.width(360.dp))
    }
}

/** 07 분할 모드(앱 영역 840): 메뉴 줄 없이 목록과 안내 버튼만 */
@Composable
private fun SplitFind(
    app: AppState,
    items: List<PlaceItem>,
    selected: PlaceItem?,
    parked: Boolean,
    style: CampingStyle,
    originText: String,
    onSelect: (PlaceItem) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(128.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label("주변 차박지", 40, FontWeight.ExtraBold)
                Label("${if (parked) "정차 중" else "주행 중"} · $originText", 24, FontWeight.Medium, Palette.SubText)
            }
            RoundIcon(LineIcon.Mic, "음성으로 찾기", 104.dp, 48.dp, Palette.Accent, { app.startVoiceSearch() })
        }
        // 07: 내비로 안내 중이면 "안내 중" 띠(누르면 03-1)
        val naviStatus by app.navi.status.collectAsState()
        app.guidingPlace(naviStatus)?.let { g ->
            GuidingBand(g.name, { app.navigate(Route.NaviGuiding(g.id)) }, Modifier.padding(start = 28.dp, end = 28.dp, bottom = 12.dp))
        }
        Row(Modifier.padding(horizontal = 28.dp).fillMaxWidth().card(28.dp).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CampingStyle.entries.forEach { s ->
                val on = s == style
                Box(
                    Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (on) Palette.Text else Color.Transparent)
                        .clickable(role = Role.Tab) { app.prefs.update { it.copy(primaryStyle = s, styles = it.styles + s) } },
                    contentAlignment = Alignment.Center,
                ) { Label(s.label, 30, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) Palette.Background else Palette.Text) }
            }
        }
        DataBanner(app, Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
        ) {
            items(items, key = { it.place.id }) { item ->
                PlaceRow(item, item.place.id == selected?.place?.id, parked = true) { onSelect(item) }
            }
        }
        if (selected != null) {
            Column(Modifier.fillMaxWidth().background(Palette.Card).padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("선택: ${selected.place.name}", 24, FontWeight.Bold, Palette.SubText)
                PrimaryButton("여기로 안내", { app.navigateTo(selected.place) }, Modifier.fillMaxWidth(), height = 124.dp, textSize = 38)
            }
        }
    }
}

// 글자 만들기

/** 편의시설 한 줄. null은 "미확인"이다(없음이 아님). */
fun amenityText(a: Amenities): String {
    fun f(name: String, v: Boolean?) = "$name ${when (v) { true -> "있음"; false -> "없음"; null -> "미확인" }}"
    return listOf(f("화장실", a.toilet), f("수도", a.water), f("전기", a.electricity), f("샤워", a.shower)).joinToString(" · ")
}

fun evidenceText(place: Place): String =
    place.evidence.takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { "근거: $it" }
        ?: place.sourceName?.let { "출처: $it" }
        ?: "근거 정보가 없어요"

fun siteWarnings(place: Place): List<String> {
    val site = place.site ?: return emptyList()
    return buildList {
        site.heightLimitMeters?.let { add("높이 제한 ${"%.1f".format(it)} m") }
        if (site.parkingSize == "small") add("주차면 좁음")
        if (site.wind == "exposed") add("바람에 노출")
    }
}

fun summarySpeech(item: PlaceItem, style: CampingStyle): String =
    listOfNotNull(
        item.place.name,
        "${style.label} ${item.signal.label}",
        item.place.styleReason(style),
        formatKm(item.distanceKm).replace("km", "킬로미터"),
        item.rating?.let { "별점 ${it.average}, 리뷰 ${it.count}개" },
    ).joinToString(". ")

fun reviewSpeech(place: Place, page: ReviewPage?): String {
    val items = page?.items.orEmpty()
    if (items.isEmpty()) return "${place.name}에는 아직 리뷰가 없어요"
    return "${place.name} 리뷰 ${page?.count ?: items.size}개. " +
        items.take(3).joinToString(" ") { "별 ${it.rating}개, ${it.text}" }
}

/** 01 · 02 · 15 칩 줄 높이(목록·지도 · 신호 · 화장실 …). 설계 최소 터치 96보다 낮춘 값 */
private val CHIP_ROW_HEIGHT = 76.dp
