package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.util.Josa
import kr.sanho.chabakine_pleos.ui.components.SmallRadius
import kr.sanho.chabakine_pleos.ui.components.ButtonRadius
import kr.sanho.chabakine_pleos.ui.components.pressShrink
import kr.sanho.chabakine_pleos.ui.components.ChipRadius
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.semantics.semantics
import kr.sanho.chabakine_pleos.ui.camp.LOW_BATTERY
import kr.sanho.chabakine_pleos.vehicle.RateSource
import kr.sanho.chabakine_pleos.vehicle.BatteryForecast
import java.time.ZoneId
import java.time.Instant
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.foundation.background
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.BuildConfig
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.vehicle.DrivingState
import kr.sanho.chabakine_pleos.platform.NaviStatus
import kr.sanho.chabakine_pleos.platform.pointId
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.vehicle.CampControlState
import kr.sanho.chabakine_pleos.vehicle.CampPreset
import kr.sanho.chabakine_pleos.vehicle.ControlResult
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.distanceKm
import kr.sanho.chabakine_pleos.shared.place.distanceKmFrom
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.find.BackHeader
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkColors
import kr.sanho.chabakine_pleos.ui.theme.LightColors

/** 03에 보여 줄 목적지. 차박지([isPlace])면 도착해 주차할 때 04를 묻고, 장소 밖 목적지면 묻지 않는다. */
data class NaviTarget(val id: String, val name: String, val km: String, val isPlace: Boolean, val resend: () -> Unit)

fun naviTarget(app: AppState, place: Place, settings: Settings) = NaviTarget(
    place.id, place.name, formatKm(place.distanceKmFrom(app.origin)), isPlace = true,
) { app.navigateTo(place) }

fun naviPointTarget(app: AppState, name: String, latitude: Double, longitude: Double, settings: Settings) = NaviTarget(
    pointId(name), name, formatKm(distanceKm(app.origin, LatLng(latitude, longitude))), isPlace = false,
) { app.navigateToPoint(name, LatLng(latitude, longitude)) }

/** 화면설계서 03 내비 길안내 전달: 보내는 중 → 길안내 시작 · 내비만 열림 · 실패 · 도착 */
@Composable
fun NaviSentScreen(app: AppState, target: NaviTarget, status: NaviStatus, modifier: Modifier = Modifier) {
    val km = target.km
    val current = status.takeIf { it.placeIdOrNull() == target.id } ?: NaviStatus.Requested(target.id)
    // QA-09: 이 목적지 상태를 한 번 받은 뒤 내비에서 안내를 끝내면 상태가 Idle로 돌아온다.
    // 그때 "보내는 중"에 머물지 않고 목록으로 돌아간다(장소면 안내 중 화면도 끝났으므로 목록)
    var seenOwn by remember(target.id) { mutableStateOf(false) }
    if (status.placeIdOrNull() == target.id) seenOwn = true
    LaunchedEffect(target.id, seenOwn, status is NaviStatus.Idle) {
        if (seenOwn && status is NaviStatus.Idle) {
            app.notice("길안내가 끝났어요")
            app.replace(Route.Find())
        }
    }
    // 내비가 20초 안에 답하지 않으면 실패로 본다(에뮬레이터에서 경로 탐색에 10초 넘게 걸렸다)
    LaunchedEffect(target.id, current is NaviStatus.Requested) {
        if (current is NaviStatus.Requested) {
            delay(20_000)
            if (app.navi.status.value is NaviStatus.Requested) app.navi.timeout(target.id)
        }
    }
    var seconds by remember(target.id, current::class) { mutableIntStateOf(5) }
    if (current is NaviStatus.Started || current is NaviStatus.Opened) {
        LaunchedEffect(target.id, current::class) {
            while (seconds > 0) {
                delay(1_000)
                seconds--
            }
            // 차박지는 03-1 안내 중으로, 장소 밖 목적지는 목록으로
            app.replace(if (target.isPlace) Route.NaviGuiding(target.id) else Route.Find())
        }
    }
    Box(modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxSize().card(40.dp).padding(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        ) {
            val failed = current is NaviStatus.Failed
            val iconBg = if (failed) Color(0xFFC42B2B) else Palette.Accent
            Box(Modifier.size(if (failed) 120.dp else 160.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
                LineIconView(if (failed) LineIcon.Alert else LineIcon.Navigate, if (failed) 64.dp else 84.dp, if (failed) Color.White else Palette.OnAccent, strokeWidth = if (failed) 2.4f else 2f)
            }
            when (current) {
                is NaviStatus.Failed -> {
                    Label("내비에 목적지를 보내지 못했어요", 52, FontWeight.ExtraBold)
                    Label(
                        "${current.message} 다시 시도하거나 내비에서 \"${target.name}\"${Josa.eulReul(target.name).removePrefix(target.name)} 찾아 주세요.",
                        30, FontWeight.Medium, Palette.SoftText, maxLines = 3, lineHeight = 1.5.em,
                    )
                    Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SecondaryButton("목록으로", { app.replace(Route.Find()) }, Modifier.width(320.dp), height = 120.dp, textSize = 34)
                        PrimaryButton(
                            "다시 시도", {
                                app.back()
                                target.resend()
                            },
                            Modifier.width(360.dp), height = 120.dp, textSize = 34, icon = null, trailingChevron = false,
                        )
                    }
                }
                is NaviStatus.Requested -> {
                    Label("내비에 목적지를 보내는 중이에요", 56, FontWeight.ExtraBold)
                    Label("${target.name} · $km", 36, FontWeight.Bold, Palette.SoftText)
                    SecondaryButton("목록으로", { app.replace(Route.Find()) }, Modifier.padding(top = 24.dp).width(320.dp), height = 120.dp, textSize = 34)
                }
                is NaviStatus.Arrived -> {
                    Label("도착했어요", 56, FontWeight.ExtraBold)
                    Label(if (target.isPlace && app.campModeAvailable) "${target.name} · 주차하면 차박 모드를 켤지 물어볼게요" else target.name, 32, FontWeight.Bold, Palette.SoftText)
                    SecondaryButton("목록으로", { app.replace(Route.Find()) }, Modifier.padding(top = 24.dp).width(320.dp), height = 120.dp, textSize = 34)
                }
                else -> {
                    val started = current is NaviStatus.Started
                    Label(if (started) "내비로 길안내를 시작했어요" else "내비를 열었어요", 56, FontWeight.ExtraBold)
                    Label("${target.name} · $km", 36, FontWeight.Bold, Palette.SoftText)
                    if (!started) {
                        Label("목적지가 보이지 않으면 내비에서 \"${target.name}\"${Josa.eulReul(target.name).removePrefix(target.name)} 찾아 주세요", 28, FontWeight.Bold, Palette.BannerText)
                    }
                    if (target.isPlace && app.campModeAvailable) Label("도착해서 주차하면 차박 모드를 켤지 물어볼게요", 28, FontWeight.Medium, Palette.SubText)
                    Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SecondaryButton("안내 취소", app::cancelGuidance, Modifier.width(320.dp), height = 120.dp, textSize = 34)
                        if (target.isPlace) {
                            SecondaryButton("안내 화면으로", { app.replace(Route.NaviGuiding(target.id)) }, Modifier.width(320.dp), height = 120.dp, textSize = 34, background = Palette.Text, foreground = Palette.Background)
                        } else {
                            SecondaryButton("목록으로", { app.replace(Route.Find()) }, Modifier.width(320.dp), height = 120.dp, textSize = 34, background = Palette.Text, foreground = Palette.Background)
                        }
                    }
                    Label(if (target.isPlace) "${seconds}초 뒤 안내 중 화면으로 넘어가요" else "${seconds}초 뒤 목록으로 돌아가요", 26, FontWeight.Medium, Palette.MutedText)
                }
            }
        }
    }
}

private fun NaviStatus.placeIdOrNull(): String? = when (this) {
    is NaviStatus.Requested -> placeId
    is NaviStatus.Started -> placeId
    is NaviStatus.Opened -> placeId
    is NaviStatus.Failed -> placeId
    is NaviStatus.Arrived -> placeId
    NaviStatus.Idle -> null
}

/** 화면설계서 06-A 차량 정보(속도·기어)를 못 읽을 때. "주차했어요"는 시스템 주행 제한을 풀지 않는다. */
@Composable
fun ParkedConfirmScreen(app: AppState, uxUnrestricted: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.width(1000.dp).fillMaxHeight().card().padding(48.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Label("차량 정보(속도·기어)를 읽을 수 없어요", 26, FontWeight.ExtraBold, Palette.SubText)
            Label("지금 주차했나요?", 52, FontWeight.ExtraBold)
            Label("속도·기어를 읽을 수 없어요. 차박 모드는 주차했을 때 직접 켤 수 있어요.", 30, FontWeight.Medium, Palette.SoftText, maxLines = 2)
            Column(Modifier.fillMaxWidth().card(24.dp, Palette.CardInner).padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("상태를 모르면 주행 중으로 간주", 28, FontWeight.Bold, Palette.SoftText)
                Label(
                    if (uxUnrestricted) "이 버튼은 주행 제한을 풀지 않아요. 시스템이 주행 중이라고 알려 주면 버튼을 끄고, 긴 글·입력은 계속 잠가요."
                    else "시스템이 아직 주행 제한 중이라고 알려 와서 버튼을 켤 수 없어요. 안전한 곳에 세우면 켜져요.",
                    24, FontWeight.Medium, Palette.SubText, maxLines = 3,
                )
            }
            Spacer(Modifier.weight(1f))
            // 시스템 주행 제한(CarUxRestrictions)이 "제한 없음"일 때만 켜진다
            PrimaryButton(
                if (app.campModeAvailable) "주차했어요 · 차박 모드 켜기" else "주차했어요", app::confirmParkedManually, Modifier.fillMaxWidth(),
                height = 132.dp, textSize = 38, icon = null, trailingChevron = false, enabled = uxUnrestricted,
            )
            SecondaryButton("아직 주행 중이에요", { app.replace(Route.Find()) }, Modifier.fillMaxWidth(), height = 112.dp, textSize = 34)
        }
    }
}

/**
 * 화면설계서 23 차박 시작 · 차량 맞추기. Pleos Vehicle SDK로 앰비언트 조명 · 공조 · 앞좌석 · 창문을 한 번에 맞춘다.
 * 정차 중에만 열리고, 출발하면 닫힌다. 아래 줄은 고르기 → 맞추는 중(항목별 칩) → 결과(실패한 항목 다시 시도)로 바뀐다.
 * 모두 적용되면 3초 뒤 05로 간다. 차종이 지원하지 않거나 권한이 없는 항목은 "미지원"으로 보여 준다.
 */
@Composable
fun VehicleLinkScreen(app: AppState, place: Place?, control: CampControlState, outsideTempC: Float?, modifier: Modifier = Modifier) {
    var preset by remember { mutableStateOf(app.appliedPreset ?: CampPreset()) }
    val temperature = if (preset.autoClimate) CampPreset.comfortTemperature(outsideTempC) else preset.temperatureC
    var applied by remember { mutableStateOf(false) }
    val allDone = applied && control.allDone
    LaunchedEffect(allDone) {
        if (allDone) {
            delay(RESULT_HOLD_MS)
            app.replace(Route.CampMode)
        }
    }
    val start = { retry: Boolean ->
        val p = preset.copy(temperatureC = temperature)
        app.applyCampPreset(p, retry)
        if (BuildConfig.DEBUG) app.campControl.logCurrentValuesLater()
        applied = true
    }
    var lightSheet by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        BackHeader(app, "차박 시작 · 차량 맞추기", "한 번에 잠자리 준비" + (place?.let { " · ${it.name}" } ?: ""), onBack = { app.replace(Route.CampMode) }) {
            StatusPill(parked = true, text = "정차 중 · P단")
        }
        // 11-10 설계서 23: 안내 띠 없음. 차량 연결 전에만 알린다
        if (!control.ready) {
            Box(
                Modifier.padding(horizontal = 36.dp).fillMaxWidth().height(52.dp).background(Palette.InfoFill, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Label("차량 연결 중이에요 · 연결되지 않으면 차량은 바뀌지 않아요", 24, FontWeight.Bold, Palette.InfoText)
            }
        }
        // 11-10: 아래 "오전 7시까지" 카드가 들어가도록 가운데만 위아래로 넘긴다(아래 버튼 줄은 고정)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // 11-11 설계서 23: 카드엔 자주 쓰는 두 색 · 끄기 · 더보기만, 나머지 색과 밝기는 팝업
                SettingCard("앰비언트 조명", LineIcon.Bulb, Modifier.weight(1f)) {
                    val color = preset.ambientColor
                    Label(if (color == null) "꺼짐" else "${CampPreset.ambientName(color)} · ${preset.ambientBrightness}%", 36, FontWeight.Bold, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CampPreset.AMBIENT_COLORS.take(2).forEach { (name, value) ->
                            Box(
                                Modifier.pressShrink().size(96.dp).clip(CircleShape).background(rgb(value))
                                    .then(if (color == value) Modifier.border(4.dp, Palette.Text, CircleShape) else Modifier)
                                    .semantics { contentDescription = name }
                                    .clickable(role = Role.RadioButton) { preset = preset.copy(ambientColor = value) },
                            )
                        }
                        Box(
                            Modifier.pressShrink().size(96.dp).clip(CircleShape).border(if (color == null) 4.dp else 2.dp, if (color == null) Palette.Text else Palette.Gauge, CircleShape)
                                .clickable(role = Role.RadioButton) { preset = preset.copy(ambientColor = null) },
                            contentAlignment = Alignment.Center,
                        ) { Label("끄기", 24, FontWeight.Bold) }
                        // 팝업에서 두 색 밖의 색을 골랐으면 그 색 테두리
                        val custom = color != null && CampPreset.AMBIENT_COLORS.take(2).none { it.second == color }
                        Box(
                            Modifier.pressShrink().size(96.dp).clip(CircleShape).background(Palette.CardInner)
                                .then(if (custom) Modifier.border(4.dp, rgb(color!!), CircleShape) else Modifier)
                                .semantics { contentDescription = "조명 색 · 밝기 더보기" }
                                .clickable(role = Role.Button) { lightSheet = true },
                            contentAlignment = Alignment.Center,
                        ) { LineIconView(LineIcon.Sliders, 40.dp, Palette.Text, strokeWidth = 2.2f) }
                    }
                }
                // 11-11 설계서 23: 이름 줄에 자동 · 외기 유입 토글, 바람 약 · 중 · 강, 온도는 Pleos Stepper
                SettingCard(
                    "공조", LineIcon.Fan, Modifier.weight(1f), gap = 6,
                    trailing = {
                        SmallToggle("자동", preset.autoClimate) { preset = preset.copy(autoClimate = true) }
                        SmallToggle("외기 유입", preset.outsideAir) { preset = preset.copy(outsideAir = !preset.outsideAir) }
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Label("바람", 26, FontWeight.Bold, Palette.SubText)
                        Row(
                            Modifier.weight(1f).background(Palette.Background.copy(alpha = 0.5f), RoundedCornerShape(ChipRadius)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            CampPreset.FAN_STEPS.forEach { (name, speed) ->
                                val on = CampPreset.fanName(preset.fanSpeed) == name
                                Box(
                                    Modifier.weight(1f).pressShrink().height(64.dp).clip(RoundedCornerShape(SmallRadius))
                                        .background(if (on) Palette.Text else Color.Transparent)
                                        .clickable(role = Role.RadioButton) { preset = preset.copy(fanSpeed = speed) },
                                    contentAlignment = Alignment.Center,
                                ) { Label(name, 26, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) Palette.Background else Palette.Text) }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Stepper(
                        text = formatTemp(temperature),
                        description = "설정 온도",
                        canDown = temperature > CampPreset.TEMP_MIN,
                        canUp = temperature < CampPreset.TEMP_MAX,
                        onDown = { preset = preset.copy(autoClimate = false, temperatureC = (temperature - CampPreset.TEMP_STEP).coerceAtLeast(CampPreset.TEMP_MIN)) },
                        onUp = { preset = preset.copy(autoClimate = false, temperatureC = (temperature + CampPreset.TEMP_STEP).coerceAtMost(CampPreset.TEMP_MAX)) },
                    )
                }
                SettingCard("앞좌석 세팅", LineIcon.Seat, Modifier.weight(1f)) {
                    val on = preset.seatForward
                    // 11-10 설계서 23: 남은 높이를 채우는 테두리 버튼(켜면 크림 테두리)
                    Row(
                        Modifier.fillMaxWidth().weight(1f).heightIn(min = 96.dp).clip(RoundedCornerShape(ButtonRadius))
                            .border(2.dp, if (on) Palette.Accent else Palette.Gauge, RoundedCornerShape(ButtonRadius))
                            .clickable(role = Role.Switch, onClickLabel = if (on) "앞좌석 그대로 두기" else "앞좌석 앞으로 밀기") { preset = preset.copy(seatForward = !on) },
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineIconView(if (on) LineIcon.ArrowRight else LineIcon.Minus, 36.dp, if (on) Palette.Text else Palette.SubText, strokeWidth = 2.4f)
                        Label(if (on) "앞으로 쭉 밀기" else "그대로 두기", 26, FontWeight.Bold, if (on) Palette.Text else Palette.SubText)
                    }
                }
            }
            VentCard(preset) { preset = it }
            MorningBatteryCard(app, preset.copy(temperatureC = temperature))
        }
        Row(Modifier.fillMaxWidth().height(176.dp).padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val items = listOfNotNull(
                "조명" to control.lights,
                "공조" to control.climate,
                ("좌석" to control.seat).takeIf { preset.seatForward },
                "창문" to control.windows,
            )
            when {
                !applied -> {
                    SecondaryButton("차박 모드로", { app.replace(Route.CampMode) }, height = 128.dp, textSize = 32)
                    PrimaryButton(
                        "차박 시작 · 조명·공조 맞추기", { start(false) }, Modifier.weight(1f),
                        height = 128.dp, textSize = 40, icon = null, trailingChevron = false, enabled = control.ready,
                    )
                }
                control.working -> Row(
                    Modifier.weight(1f).height(128.dp).card(32.dp).padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LineIconView(LineIcon.Refresh, 48.dp, Palette.Selected, strokeWidth = 2.4f)
                    Label("맞추는 중…", 36, FontWeight.ExtraBold, modifier = Modifier.padding(end = 12.dp))
                    items.forEach { (name, result) -> ResultChip(name, result) }
                }
                else -> {
                    val failed = items.filter { it.second != ControlResult.Done && it.second != ControlResult.Idle }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Label(if (failed.isEmpty()) "차량을 맞췄어요" else "일부만 맞췄어요", 32, FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
                            items.forEach { (name, result) -> ResultChip(name, result) }
                        }
                        Label(
                            if (failed.isEmpty()) "곧 차박 모드로 가요" else "안 된 항목은 다시 시도하거나, 그대로 차박 모드로 가도 돼요",
                            24, FontWeight.Medium, Palette.MutedText,
                        )
                    }
                    if (failed.isNotEmpty()) {
                        SecondaryButton("${failed.joinToString("·") { it.first }} 다시 시도", { start(true) }, height = 128.dp, textSize = 30)
                    }
                    PrimaryButton(
                        "차박 모드로", { app.replace(Route.CampMode) },
                        height = 128.dp, textSize = 34, icon = null, trailingChevron = false,
                    )
                }
            }
        }
    }
    if (lightSheet) {
        AmbientLightSheet(preset.ambientColor ?: CampPreset.WARM_ORANGE, preset.ambientBrightness, onClose = { lightSheet = false }) { color, brightness ->
            preset = preset.copy(ambientColor = color, ambientBrightness = brightness)
            lightSheet = false
        }
    }
    }
}

private fun rgb(value: Int) = Color(0xFF000000.toInt() or value)

private fun formatTemp(c: Float): String = if (c % 1f == 0f) "${c.roundToInt()}°C" else "%.1f°C".format(c)

/** 23 공조 이름 줄의 작은 토글 버튼(Pleos Toggle button: 켜짐 = 채움 + ✓) */
@Composable
private fun SmallToggle(text: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.pressShrink().height(56.dp).shadow(if (on) 4.dp else 0.dp, RoundedCornerShape(ChipRadius)).clip(RoundedCornerShape(ChipRadius))
            .background(if (on) Palette.Accent else Palette.CardInner)
            .toggleable(value = on, role = Role.Switch) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Label(text, 24, FontWeight.ExtraBold, if (on) Palette.OnAccent else Palette.Text, maxLines = 1)
        if (on) LineIconView(LineIcon.Check, 24.dp, Palette.OnAccent, strokeWidth = 3f)
    }
}

/**
 * Pleos Controllers › Stepper: 한 덩어리 상자 안에 − | 지금 값 | +. 끝에 닿으면 그쪽 버튼을 흐리게(누를 수 없음).
 * [middle]이 있으면 값 대신 그것을 가운데에 둔다(밝기 막대 등).
 */
@Composable
private fun Stepper(
    text: String, description: String, canDown: Boolean, canUp: Boolean, onDown: () -> Unit, onUp: () -> Unit,
    modifier: Modifier = Modifier, buttonWidth: Dp = 104.dp, middle: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ButtonRadius)
    Row(
        modifier.fillMaxWidth().height(96.dp).clip(shape).background(Palette.CardInner).border(1.dp, Palette.Gauge, shape)
            .semantics { contentDescription = "$description $text" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(LineIcon.Minus, "$description 내리기", canDown, buttonWidth, onDown)
        Box(Modifier.width(1.dp).height(48.dp).background(Palette.Gauge))
        if (middle != null) middle() else Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Label(text, 36, FontWeight.ExtraBold) }
        Box(Modifier.width(1.dp).height(48.dp).background(Palette.Gauge))
        StepButton(LineIcon.Plus, "$description 올리기", canUp, buttonWidth, onUp)
    }
}

@Composable
private fun StepButton(icon: LineIcon, label: String, enabled: Boolean, width: Dp, onClick: () -> Unit) {
    Box(
        Modifier.width(width).fillMaxHeight().clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { LineIconView(icon, 40.dp, if (enabled) Palette.Text else Palette.DisabledText, strokeWidth = 2.6f) }
}

/** 23 앰비언트 조명 더보기: 색 8가지 + 밝기(10~100%, 10 단위). 적용을 눌러야 바뀐다(11-11) */
@Composable
private fun AmbientLightSheet(color: Int, brightness: Int, onClose: () -> Unit, onApply: (Int, Int) -> Unit) {
    var draftColor by remember { mutableIntStateOf(color) }
    var draftBright by remember { mutableIntStateOf(brightness.coerceIn(10, 100)) }
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(role = Role.Button, onClickLabel = "닫기", onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            // 유리 카드는 뒤가 비쳐 팝업에선 테마의 불투명 카드 색을 쓴다
            Modifier.width(1000.dp).card(32.dp, if (Palette.isDark) DarkColors.card else LightColors.card).clickable(enabled = false) {}.padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.size(72.dp).shadow(16.dp, CircleShape, ambientColor = rgb(draftColor), spotColor = rgb(draftColor)).background(rgb(draftColor), CircleShape))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label("앰비언트 조명", 40, FontWeight.ExtraBold)
                    Label("${CampPreset.ambientName(draftColor)} · $draftBright% · 적용을 누르면 차에 보내요", 26, FontWeight.SemiBold, Palette.SubText)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Label("색", 28, FontWeight.ExtraBold)
                CampPreset.AMBIENT_COLORS.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { (name, value) ->
                            val on = draftColor == value
                            val shape = RoundedCornerShape(ButtonRadius)
                            Column(
                                Modifier.weight(1f).pressShrink().height(132.dp).clip(shape)
                                    .background(if (on) Palette.CardInner else Color.Transparent)
                                    .border(if (on) 4.dp else 2.dp, if (on) Palette.Text else Palette.Gauge, shape)
                                    .clickable(role = Role.RadioButton) { draftColor = value },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            ) {
                                Box(Modifier.size(56.dp).background(rgb(value), CircleShape))
                                Label(name, 24, FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Label("밝기", 28, FontWeight.ExtraBold)
                Stepper(
                    text = "$draftBright%", description = "밝기",
                    canDown = draftBright > 10, canUp = draftBright < 100,
                    onDown = { draftBright = (draftBright - 10).coerceAtLeast(10) }, onUp = { draftBright = (draftBright + 10).coerceAtMost(100) },
                    modifier = Modifier.weight(1f), buttonWidth = 112.dp,
                ) {
                    Row(Modifier.weight(1f).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.weight(1f).height(12.dp).background(Palette.Gauge, RoundedCornerShape(6.dp))) {
                            Box(Modifier.fillMaxWidth(draftBright / 100f).fillMaxHeight().background(Palette.Accent, RoundedCornerShape(6.dp)))
                        }
                        Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) { Label("$draftBright%", 34, FontWeight.ExtraBold) }
                    }
                }
            }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryButton("취소", onClose, Modifier.width(260.dp), height = 112.dp, textSize = 32)
                PrimaryButton("적용", { onApply(draftColor, draftBright) }, Modifier.width(360.dp), height = 112.dp, textSize = 34, icon = null, trailingChevron = false)
            }
        }
    }
}

/** 디버그 빌드 시험용 환기 간격(분) */
private const val TEST_VENT_INTERVAL_MIN = 2

internal fun intervalText(min: Int) = if (min < 60) "${min}분" else "${min / 60}시간"

/** 23 설정 카드: 제목 · 내용 · 맨 아래 안내 글 */
@Composable
private fun SettingCard(
    title: String, icon: LineIcon, modifier: Modifier, note: String? = null, gap: Int = 12,
    trailing: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxHeight().card(32.dp).padding(horizontal = 32.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(gap.dp)) {
        // 11-11: 이름이 잘 안 보여서 28 · 굵게 · 본문색 + 아이콘
        Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineIconView(icon, 32.dp, Palette.Text, strokeWidth = 2f)
                Label(title, 28, FontWeight.ExtraBold, maxLines = 1)
            }
            trailing?.invoke(this)
        }
        content()
        if (note != null) {
            Spacer(Modifier.weight(1f))
            Label(note, 24, FontWeight.Medium, Palette.MutedText, maxLines = 4)
        }
    }
}

/** 고른 것은 흰 바탕. [recommended]면 아래에 "권장" */
@Composable
private fun OptionButton(
    text: String, selected: Boolean, modifier: Modifier, textSize: Int = 24, recommended: Boolean = false,
    height: Dp = 96.dp, quiet: Boolean = false, onClick: () -> Unit,
) {
    // [quiet]: 설계서 23 공조 줄처럼 고르지 않은 칸은 옅은 바탕(유리 칸 색)
    Column(
        modifier.pressShrink().height(height).clip(RoundedCornerShape(SmallRadius + 2.dp)).background(if (selected) Palette.Text else if (quiet) Palette.CardInner else Palette.Sub)
            .clickable(role = Role.RadioButton, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(text, if (recommended) 26 else textSize, FontWeight.ExtraBold, if (selected) Palette.Background else Palette.Text, maxLines = 1)
        if (recommended) Label("권장", 24, FontWeight.Bold, if (selected) Palette.SuccessOnInverse else Palette.Success)
    }
}

/** 23 맞추는 중 · 결과 칩: 적용 ✓ 초록, 보내는 중 … 회색, 일부 · 미지원 노랑 */
@Composable
private fun ResultChip(name: String, result: ControlResult) {
    val (text, bg, fg) = when (result) {
        ControlResult.Done -> Triple("$name ✓", Palette.SuccessFill, Palette.Success)
        ControlResult.Idle, ControlResult.Working -> Triple("$name …", Palette.CardInner, Palette.SubText)
        is ControlResult.Partial -> Triple("$name · 일부만", Palette.BannerBackground, Palette.BannerText)
        is ControlResult.Unsupported -> Triple("$name · 미지원", Palette.BannerBackground, Palette.BannerText)
    }
    Box(Modifier.height(64.dp).clip(RoundedCornerShape(32.dp)).background(bg).padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
        Label(text, 26, FontWeight.Bold, fg, maxLines = 1)
    }
}

/** 첫 실행 안내 등에서 쓰는 가운데 정렬 글 */
@Composable
fun CenterText(text: String, size: Int, weight: FontWeight = FontWeight.Medium, color: Color = Palette.SoftText) {
    androidx.compose.material3.Text(
        text,
        color = color,
        fontSize = androidx.compose.ui.unit.TextUnit(size.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = weight,
        textAlign = TextAlign.Center,
        lineHeight = 1.5.em,
    )
}

/** 23에서 적용 결과를 보여 주는 시간 */
private const val RESULT_HOLD_MS = 3_000L

/**
 * 23 "이렇게 맞추면 내일 오전 7시까지"(11-10): 기준 시각은 오전 7시로 고정(기상 알람과 무관).
 * 지난 차박 기록(같은 조건 2밤 이상) → 차 정보로 만든 어림값 순서. 고르는 값이 바뀌면 바로 다시 계산한다.
 * 배터리 값을 모르면 숨긴다.
 */
@Composable
private fun MorningBatteryCard(app: AppState, preset: CampPreset) {
    val nowMs by produceState(System.currentTimeMillis()) { while (true) { delay(60_000); value = System.currentTimeMillis() } }
    val snapshot by app.vehicle.snapshot.collectAsState()
    val now = snapshot.energy?.batteryPercent ?: return
    val hours = BatteryForecast.hoursUntil(Instant.ofEpochMilli(nowMs).atZone(ZoneId.of("Asia/Seoul")))
    val forecast = app.batteryForecast(preset, hours, useMeasured = false) ?: return
    val rates = app.batteryRates(preset)
    val low = forecast.leftPercent < LOW_BATTERY
    Column(
        Modifier.fillMaxWidth().card(32.dp).padding(horizontal = 32.dp, vertical = 18.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LineIconView(LineIcon.Moon, 28.dp, Palette.Accent)
                    Label("이렇게 맞추면 내일 오전 7시까지", 24, FontWeight.Bold, Palette.SubText)
                }
                Label("약 ${forecast.usePercent}% 쓰고 ${forecast.leftPercent}% 남아요", 40, FontWeight.ExtraBold, if (low) Palette.Caution else Palette.Text)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Gauge)) {
                    Box(Modifier.fillMaxWidth((now / 100f).coerceIn(0f, 1f)).fillMaxHeight().background(Palette.GaugeFill.copy(alpha = 0.35f)))
                    Box(Modifier.fillMaxWidth((forecast.leftPercent / 100f).coerceIn(0f, 1f)).fillMaxHeight().background(Palette.GaugeFill))
                }
                Row {
                    Label("오전 7시 ${forecast.leftPercent}%", 24, FontWeight.Bold, Palette.SoftText, modifier = Modifier.weight(1f))
                    Label("지금 ${now.roundToInt()}%", 24, FontWeight.Bold, Palette.SoftText)
                }
            }
        }
        val parts = rates?.let {
            listOf("대기" to it.idle, "공조" to it.climate, "조명" to it.light, "환기" to it.vent)
                .filter { (_, r) -> r > 0f }
                .joinToString(" · ") { (name, r) -> "$name 약 ${(r * hours).roundToInt().coerceAtLeast(1)}%" }
        }
        val source = when (forecast.source) {
            RateSource.HISTORY -> "지난 차박 ${forecast.nights}밤 기준"
            else -> "어림값 · 차박할수록 정확해져요"
        }
        Label(
            listOfNotNull(if (low) "공조를 한 단계 낮춰 보세요" else null, parts.takeIf { forecast.source == RateSource.ESTIMATE }, source).joinToString(" · "),
            24, FontWeight.SemiBold, if (low) Palette.Caution else Palette.MutedText, maxLines = 1,
        )
    }
}

/**
 * 23 환기 · 창문(11-10 설계서): 글자 대신 그림으로. 머리에 창문 아이콘 + 지금 설정, 오른쪽에 "밤 10시 → 아침 7시" 줄 위
 * 환기하는 때를 점으로(간격을 바꾸면 점 수가 바뀜). 여는 폭은 창문이 열린 정도 그림, 알림만은 종. 간격은 시계 + 밤새 몇 번.
 */
@Composable
private fun VentCard(preset: CampPreset, onChange: (CampPreset) -> Unit) {
    val width = preset.windowOpenCm
    val interval = preset.ventIntervalMin
    val green = Palette.Success
    Column(Modifier.fillMaxWidth().card(32.dp).padding(horizontal = 32.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Box(Modifier.size(72.dp).background(green.copy(alpha = 0.14f), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                LineIconView(LineIcon.VentWindow, 44.dp, green)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label("환기 · 창문", 28, FontWeight.ExtraBold)
                Label(
                    if (width == null) "창문 그대로 · ${intervalText(interval)}마다 알림"
                    else "${intervalText(interval)}마다 $width cm · ${CampPreset.VENT_OPEN_MINUTES}분 환기",
                    40, FontWeight.ExtraBold, green,
                )
            }
            NightVentTimeline(interval, notifyOnly = width == null, Modifier.weight(1f).padding(start = 24.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("여는 폭 · 대각선 두 창문", 24, FontWeight.Bold, Palette.SoftText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, null).forEach { cm ->
                        val on = width == cm
                        IconOption(on, cm == 2, Modifier.weight(1f), onClick = { onChange(preset.copy(windowOpenCm = cm)) }) { fg ->
                            if (cm == null) LineIconView(LineIcon.Bell, 40.dp, fg) else WindowGapIcon(cm, fg)
                            Label(cm?.let { "$it cm" } ?: "알림만", 28, FontWeight.ExtraBold, fg, maxLines = 1)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("간격 · ${CampPreset.VENT_OPEN_MINUTES}분 열었다 닫기", 24, FontWeight.Bold, Palette.SoftText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 디버그 빌드에만 2분 간격(환기 열기 · 닫기를 바로 확인하는 시험용)
                    (if (BuildConfig.DEBUG) listOf(TEST_VENT_INTERVAL_MIN, 30, 60, 120) else listOf(30, 60, 120)).forEach { min ->
                        val on = interval == min
                        IconOption(on, min == 60, Modifier.weight(1f), horizontal = true, onClick = { onChange(preset.copy(ventIntervalMin = min)) }) { fg ->
                            LineIconView(LineIcon.Clock, 40.dp, fg)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Label(intervalText(min), 30, FontWeight.ExtraBold, fg, maxLines = 1)
                                Label("밤새 ${ventCount(min)}번", 20, FontWeight.Bold, fg.copy(alpha = 0.75f), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 밤 10시 → 아침 7시(9시간)에 환기하는 횟수(아침 7시 정각은 빼고) */
private fun ventCount(intervalMin: Int): Int = (NIGHT_MINUTES + intervalMin - 1) / intervalMin - 1

private const val NIGHT_MINUTES = 9 * 60

/** "밤 10시 → 아침 7시" 줄 위에 환기(알림만이면 알림) 때를 점으로 */
@Composable
private fun NightVentTimeline(intervalMin: Int, notifyOnly: Boolean, modifier: Modifier) {
    val count = ventCount(intervalMin)
    val dot = if (notifyOnly) Palette.Accent else Palette.Success
    val ring = Palette.Background
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label("밤새 ${if (notifyOnly) "알림" else "환기"} ${count}번", 24, FontWeight.ExtraBold, modifier = Modifier.align(Alignment.End))
        val track = Palette.Gauge
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val y = size.height / 2
            drawRoundRect(track, topLeft = androidx.compose.ui.geometry.Offset(0f, y - 3.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            for (i in 1..count) {
                val x = size.width * (i * intervalMin) / NIGHT_MINUTES
                drawCircle(ring, 14.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
                drawCircle(dot, 11.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Row {
            Label("밤 10시", 24, FontWeight.SemiBold, Palette.MutedText, modifier = Modifier.weight(1f))
            Label("아침 7시", 24, FontWeight.SemiBold, Palette.MutedText)
        }
    }
}

/** 창문 그림: 틀 안에서 유리가 [cm]만큼 내려가 위에 틈(점선 바람)이 보인다 */
@Composable
private fun WindowGapIcon(cm: Int, color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(44.dp, 40.dp)) {
        val u = size.width / 44f
        drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(3 * u, 3 * u), size = androidx.compose.ui.geometry.Size(38 * u, 34 * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6 * u), style = androidx.compose.ui.graphics.drawscope.Stroke(3 * u))
        val gap = cm * 4f
        drawRoundRect(color.copy(alpha = 0.4f), topLeft = androidx.compose.ui.geometry.Offset(9 * u, (8 + gap) * u), size = androidx.compose.ui.geometry.Size(26 * u, (24 - gap) * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2 * u))
        drawLine(color, androidx.compose.ui.geometry.Offset(12 * u, (8 + gap / 2) * u), androidx.compose.ui.geometry.Offset(32 * u, (8 + gap / 2) * u), strokeWidth = 2.5f * u,
            cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3 * u, 4 * u)))
    }
}

/** 아이콘이 있는 고르기 칸(112). 고른 것은 밝은 바탕. [recommended]면 오른쪽 위 작은 "권장" 표 */
@Composable
private fun IconOption(selected: Boolean, recommended: Boolean, modifier: Modifier, horizontal: Boolean = false, onClick: () -> Unit, content: @Composable (Color) -> Unit) {
    val fg = if (selected) Palette.Background else Palette.Text
    Box(modifier.pressShrink().height(112.dp).clip(RoundedCornerShape(ButtonRadius)).background(if (selected) Palette.Text else Palette.Sub).clickable(role = Role.RadioButton, onClick = onClick)) {
        if (horizontal) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { content(fg) }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) { content(fg) }
        }
        if (recommended) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp)
                    .background(if (selected) Color(0xFF157347) else Palette.Success, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp),
            ) { Label("권장", 18, FontWeight.ExtraBold, if (selected) Color.White else Palette.Background) }
        }
    }
}
