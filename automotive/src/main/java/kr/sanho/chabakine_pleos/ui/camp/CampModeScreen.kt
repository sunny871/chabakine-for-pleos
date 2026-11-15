package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.ui.theme.LightColors
import kr.sanho.chabakine_pleos.ui.theme.DarkColors
import kr.sanho.chabakine_pleos.ui.components.TrustIcon
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import kr.sanho.chabakine_pleos.ui.components.PleosSwitch
import kr.sanho.chabakine_pleos.ui.components.pressShrink
import kr.sanho.chabakine_pleos.ui.components.ChipRadius
import kr.sanho.chabakine_pleos.ui.components.ButtonRadius
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import kr.sanho.chabakine_pleos.vehicle.BatteryForecast
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.CampOverlay
import kr.sanho.chabakine_pleos.app.Features
import kr.sanho.chabakine_pleos.app.LowBatteryAlert
import kr.sanho.chabakine_pleos.app.LowBatteryRule
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.app.SleepAlerts
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.PhotoTextShadow
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.VoiceButton
import kr.sanho.chabakine_pleos.ui.components.TrustLevel
import kr.sanho.chabakine_pleos.ui.components.TrustShield
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.components.withHaptic
import kr.sanho.chabakine_pleos.ui.find.AmenityGrid
import kr.sanho.chabakine_pleos.ui.find.styleReason
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.lampColor
import kr.sanho.chabakine_pleos.ui.theme.textColor
import kr.sanho.chabakine_pleos.util.sunrise
import kr.sanho.chabakine_pleos.vehicle.CampControlState
import kr.sanho.chabakine_pleos.vehicle.CampPreset
import kr.sanho.chabakine_pleos.vehicle.ControlResult
import kr.sanho.chabakine_pleos.vehicle.Energy
import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val NextStepBg = Color(0xFF2D2A4A)
private val NextStepFg = Color(0xFFC9B8FF)
private val RedPanel = Color(0xFF3A1416)
private val RedSoft = Color(0xFFFFB3B5)
private val RedSofter = Color(0xFFFFD7D8)
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val KOREA = ZoneId.of("Asia/Seoul")

/**
 * 화면설계서 05 차박 모드(정차 중, 사용자가 켬).
 * 메뉴 줄 없이 화면 전체를 쓰고, 오른쪽 위 "차박 모드 종료"로 나간다.
 * 왼쪽: 오늘 밤 머무는 곳(사진·편의시설·잠들기·철수). 오른쪽: 신호 · 실내 공기 · 근처 축제 · 차량 · 배터리.
 * 금지 · 신호 바뀜 알림이 뜨면 신호 자리에 알림, 아래는 내일 아침(일출) 하나.
 * 23 차량 맞추기는 차량 연동이 승인된 차에서만: 아직 맞추지 않았으면 "차량 맞추기", 맞춘 뒤에는 차량 상태 카드(누르면 23).
 */
@Composable
fun CampModeScreen(
    app: AppState,
    place: Place,
    settings: Settings,
    snapshot: VehicleSnapshot,
    control: CampControlState,
    modifier: Modifier = Modifier,
) {
    val style = settings.primaryStyle
    val signal = place.signalFor(style)
    val startSignal = app.campStartSignal
    var showEvidence by remember(place.id) { mutableStateOf(false) }
    var coDemoOpen by remember { mutableStateOf(false) }
    // 차박 중에는 30분마다 서버 목록을 다시 확인해 신호가 바뀌었는지 본다.
    // 6시간 규칙을 건너뛴다(manifest 한 번만 받고, 버전이 같으면 장소 파일은 받지 않는다)
    LaunchedEffect(place.id) {
        while (true) {
            delay(RECHECK_MS)
            app.refreshData(force = true)
        }
    }
    val vehicleCard = Features.VEHICLE_CONTROL && control.linked
    Box(modifier.fillMaxSize()) {
        // 11-08: 보드 전체가 장소 사진(위아래로 어둡게), 카드는 유리판
        CampPhotoBackdrop(place.photoUrls.firstOrNull())
        Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 44.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // 머리 줄은 64, 버튼은 누르는 칸 96을 지키려고 위아래로 넘친다(requiredHeight)
            Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.weight(1f).height(56.dp).background(Palette.Accent, RoundedCornerShape(28.dp)).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LineIconView(LineIcon.Tent, 32.dp, Palette.OnAccent, strokeWidth = 2.2f)
                    Label("차박 모드 중", 26, FontWeight.ExtraBold, Palette.OnAccent)
                }
                if (vehicleCard && control.notApplied) {
                    PrimaryButton("차량 맞추기", { app.navigate(Route.VehicleLink) }, Modifier.requiredHeight(96.dp), height = 96.dp, textSize = 26, icon = null, trailingChevron = false)
                }
                // 말로 조작("7시에 깨워 줘" 등). 머리 줄 64 안에서 누르는 칸 88을 지키려고 위아래로 넘친다
                Box(Modifier.requiredHeight(88.dp), contentAlignment = Alignment.Center) { VoiceButton(app.listening) { app.startVoiceSearch() } }
                // 메뉴 줄을 숨기므로 05에서 나가는 길은 여기 하나
                Row(
                    Modifier.pressShrink().requiredHeight(96.dp).clip(RoundedCornerShape(ButtonRadius))
                        .clickable(role = Role.Button) { app.exitCampMode(Route.Home) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.height(56.dp).glassButton(ChipRadius).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LineIconView(LineIcon.Exit, 28.dp, ExitRed, strokeWidth = 2.4f)
                        Label("차박 모드 종료", 26, FontWeight.ExtraBold)
                    }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TonightCard(app, place, Modifier.width(600.dp).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    val changed = startSignal != null && startSignal != signal
                    when {
                        changed -> ChangedAlert(startSignal!!, signal) { app.acknowledgeSignal(signal) }
                        signal == Signal.RED -> ForbiddenAlert(place, style) { app.exitCampMode() }
                        else -> SignalCard(place, style, signal) { showEvidence = true }
                    }
                    if (changed || signal == Signal.RED) {
                        // 알림이 떠 있으면 나머지 칸은 내일 아침 하나로 줄인다
                        MorningCard(place, Modifier.fillMaxWidth().weight(1f))
                    } else {
                        // 설계서 05: 실내 공기 · 근처 축제 보기 / 차량(승인 시) · 배터리
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                IndoorAirCard(app, Modifier.weight(1f).fillMaxWidth()) { coDemoOpen = true }
                                FestivalButton { app.navigate(Route.CampLeaveDone(place.id)) }
                            }
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (vehicleCard) {
                                    VehicleCard(app.appliedPreset.takeUnless { control.notApplied }, app.ventNextAt, app.ventCloseAt, control.windows == ControlResult.Done, Modifier.weight(1f).fillMaxWidth()) { app.navigate(Route.VehicleLink) }
                                }
                                snapshot.energy?.let { BatteryCard(app, it, Modifier.weight(1f).fillMaxWidth()) }
                            }
                        }
                    }
                }
            }
        }
        if (showEvidence) EvidenceDialog(place, style, signal) { showEvidence = false }
        if (coDemoOpen) CoDemoDialog(app) { coDemoOpen = false }
    }
}

/**
 * 05 차량 카드(승인 시, 설계서 05): 23에서 맞춘 창문 · 공조 · 조명. 카드를 누르면 23에서 바꾼다.
 * 아직 맞추지 않았으면 값 자리에 "-"와 "23에서 맞춰 주세요"를 보여 준다.
 */
@Composable
private fun VehicleCard(preset: CampPreset?, ventNextAt: Long?, ventCloseAt: Long?, windowsMovable: Boolean, modifier: Modifier, onClick: () -> Unit) {
    // 남은 시간을 분 단위로 보여 주려고 15초마다 다시 그린다
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }
    val window = preset?.let { p -> ventWindowText(p, ventNextAt, ventCloseAt, now, windowsMovable) } ?: "-"
    val climate = preset?.let { "${it.temperatureC.roundToInt()}°C" } ?: "-"
    val light = preset?.let { p ->
        when (p.ambientColor) {
            null -> "끔"
            CampPreset.WARM_ORANGE -> "주황 ${p.ambientBrightness}%"
            else -> "흰색 ${p.ambientBrightness}%"
        }
    } ?: "-"
    Column(
        modifier.pressShrink().panel().clickable(role = Role.Button, onClickLabel = "차량 맞추기", onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineIconView(LineIcon.Car, 30.dp, CarOrange)
            Label("차량", 26, FontWeight.Bold, Palette.SubText, modifier = Modifier.weight(1f))
            Label(if (preset == null) "맞추기 ›" else "›", if (preset == null) 26 else 30, FontWeight.Bold, Palette.Accent)
        }
        if (preset == null) Label("아직 안 맞췄어요 · 눌러서 23에서 맞춰 주세요", 24, FontWeight.Medium, Palette.MutedText, maxLines = 2)
        // 창문은 평소 닫혀 있고 환기 때만 이 폭으로 연다
        VehicleRow(LineIcon.Window, "창문 환기", window, Modifier.weight(1f), valueSize = if (window.length > 6) 34 else 40)
        VehicleRow(LineIcon.Fan, "공조", climate, Modifier.weight(1f))
        VehicleRow(LineIcon.Bulb, "조명", light, Modifier.weight(1f))
    }
}

@Composable
private fun VehicleRow(icon: LineIcon, title: String, value: String, modifier: Modifier, valueSize: Int = 40) {
    Row(
        modifier.fillMaxWidth().background(GlassInner, RoundedCornerShape(22.dp)).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LineIconView(icon, 32.dp, Palette.SubText)
        // 이름은 줄이지 않는다(QA-06: 값이 길면 "창…"으로 잘렸다). 남는 폭은 가운데 빈칸이 받는다
        Label(title, 26, FontWeight.Bold, Palette.SubText)
        Spacer(Modifier.weight(1f))
        Label(value, valueSize, FontWeight.ExtraBold)
    }
}

/**
 * 05 차량 카드 "창문 환기" 값: 환기 중이면 "열림 · 2분 뒤 닫힘"(줄 이름이 "창문 환기"라 짧게, QA-06), 기다리는 중이면 "23분 뒤 · 2 cm"("알림만"이면 "23분 뒤 알림"),
 * 환기가 멈춰 있으면(주행 뒤 등) 고른 폭만. 창문을 움직일 수 없으면([windowsMovable] false) 알림만 하므로 "23분 뒤 알림".
 */
internal fun ventWindowText(preset: CampPreset, nextAt: Long?, closeAt: Long?, now: Long, windowsMovable: Boolean = true): String {
    fun minutesLeft(at: Long) = ((at - now + 59_999) / 60_000).coerceAtLeast(1)
    return when {
        closeAt != null -> "열림 · ${minutesLeft(closeAt)}분 뒤 닫힘"
        nextAt != null -> preset.windowOpenCm?.takeIf { windowsMovable }?.let { "${minutesLeft(nextAt)}분 뒤 · $it cm" } ?: "${minutesLeft(nextAt)}분 뒤 알림"
        else -> preset.windowOpenCm?.let { "$it cm" } ?: "알림만"
    }
}

@Composable
private fun TonightCard(app: AppState, place: Place, modifier: Modifier) {
    // 설계서 05(11-08 개선): 사진은 화면 전체 바탕으로 옮기고, 장소 판은 카드 없이 사진 위에.
    // 장소 이름 76 · 편의시설 · 버튼은 아래로
    Box(modifier) {
        Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LineIconView(LineIcon.Pin, 52.dp, PinBlue)
                    Label(place.name, 76, FontWeight.ExtraBold, maxLines = 2, lineHeight = 1.1.em, shadow = TitleShadow)
                }
                val meta = listOfNotNull(
                    place.site?.type,
                    place.verifiedAt?.let { "확인 $it" },
                    (place.evidence.firstOrNull() ?: place.sourceName)?.let { "근거 $it" },
                ).joinToString(" · ")
                if (meta.isNotEmpty()) Label(meta, 26, FontWeight.SemiBold, MetaOnPhoto, maxLines = 2, lineHeight = 1.45.em, shadow = PhotoTextShadow)
            }
            Spacer(Modifier.weight(1f))
            AmenityGrid(place, labeled = false, tileSize = 96.dp, tileRadius = 24.dp, gap = 14.dp)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // 설계서 05: 잠들기를 더 넓게(3 : 2)
                PrimaryButton(
                    "잠들기", { app.campOverlay = CampOverlay.ASK_SLEEP }, Modifier.weight(3f),
                    height = 128.dp, textSize = 32, icon = LineIcon.Moon, trailingChevron = false, glow = true,
                )
                // 사진이 비치는 반투명 회색 + 얇은 흰 테두리
                Box(
                    Modifier.weight(2f).pressShrink().height(128.dp).glassButton()
                        .clickable(role = Role.Button) { app.navigate(Route.CampLeave) }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Label("철수 준비", 28, FontWeight.ExtraBold, maxLines = 1) }
            }
        }
    }
}

/** 오늘 밤 신호: 신뢰도 고리 + 내 방식 신호 + 이유 + 다른 방식·계절 규칙 */
@Composable
private fun SignalCard(place: Place, style: CampingStyle, signal: Signal, onEvidence: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(240.dp).panel().padding(horizontal = 30.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        ConfidenceTile(place.confidence)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SignalLamp(signal, 24.dp)
                Label(style.label, 36, FontWeight.ExtraBold)
                Label(signal.label, 36, FontWeight.ExtraBold, signal.textColor)
            }
            Label(place.styleReason(style), 26, FontWeight.SemiBold, Palette.SoftText, maxLines = 2)
            val others = othersLine(place, style)
            if (others.isNotEmpty()) Label(others, 24, FontWeight.SemiBold, Palette.MutedText, maxLines = 2)
        }
        SecondaryButton("근거 보기", onEvidence, height = 96.dp, textSize = 26)
    }
}

/** 다른 방식의 신호와 첫 계절 규칙 한 줄(예: "도킹형·루프탑 금지 · 7~8월 단속 강화") */
private fun othersLine(place: Place, style: CampingStyle): String {
    val byStatus = CampingStyle.entries.filter { it != style }.groupBy { place.signalFor(it) }
        .map { (s, styles) -> "${styles.joinToString("·") { it.label }} ${s.label}" }
    return (byStatus + place.seasonRules.take(1)).joinToString(" · ")
}

/** 신뢰도: 26 부품과 같은 등급 색 칸 + 방패(1 비공식 ! · 2 간접 확인 빈 방패 · 3 공식 확인 ✓)와 등급 이름 */
@Composable
private fun ConfidenceTile(confidence: Int) {
    val level = TrustLevel.of(confidence)
    Column(
        Modifier.size(150.dp).background(level.background, RoundedCornerShape(40.dp))
            .semantics(mergeDescendants = true) { contentDescription = "신뢰도 ${confidence}단계 · ${level.title}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        TrustShield(level, 76.dp)
        Label(level.title, 22, FontWeight.ExtraBold, level.foreground)
    }
}

@Composable
private fun ForbiddenAlert(place: Place, style: CampingStyle, onFindOther: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(32.dp)).background(RedPanel)
            .border(3.dp, Signal.RED.lampColor, RoundedCornerShape(32.dp)).padding(34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            SignalLamp(Signal.RED, 52.dp, showSymbol = true)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label("내 방식 ${style.label}", 24, FontWeight.Bold, RedSoft)
                Label("이곳은 차박 금지예요", 42, FontWeight.ExtraBold)
            }
        }
        val detail = listOfNotNull(
            place.styleReason(style),
            (place.evidence.firstOrNull() ?: place.sourceName)?.let { "근거 $it" },
            place.verifiedAt?.let { "확인 $it" },
        ).joinToString(" · ")
        Label(detail, 26, FontWeight.SemiBold, RedSofter, maxLines = 3)
        Spacer(Modifier.weight(1f))
        PrimaryButton("가까운 다른 곳 찾기", onFindOther, Modifier.fillMaxWidth(), height = 112.dp, textSize = 32, icon = null, trailingChevron = false)
    }
}

@Composable
private fun ChangedAlert(from: Signal, to: Signal, onAcknowledge: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(32.dp)).background(Palette.BannerBackground)
            .border(3.dp, Signal.YELLOW.lampColor, RoundedCornerShape(32.dp)).padding(34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Label("새 정보를 받았어요", 24, FontWeight.Bold, Palette.BannerText)
        Label("신호가 바뀌었어요 · ${from.label} → ${to.label}", 42, FontWeight.ExtraBold, maxLines = 2)
        Label(
            if (to == Signal.RED || to == Signal.UNKNOWN) "가까운 다른 곳을 찾아보세요 · 근거는 근거 보기에서 확인해요" else "바뀐 이유는 근거 보기에서 확인해요",
            26, FontWeight.SemiBold, Color(0xFFF2E2B8), maxLines = 2,
        )
        Spacer(Modifier.weight(1f))
        SecondaryButton("확인했어요", onAcknowledge, Modifier.fillMaxWidth(), height = 112.dp, textSize = 32, background = Palette.Text, foreground = Palette.Background)
    }
}

/**
 * 실내 공기(BLE 센서): 일산화탄소 · 온도 · 습도. 센서 연결은 아직 없어서([Features.INDOOR_AIR_SENSOR])
 * 설계서 05-1처럼 "BLE 연결하기"와 "--"를 보여 준다. 버튼을 누르면 경보 시연 값을 고르는 창이 뜬다.
 */
@Composable
private fun IndoorAirCard(app: AppState, modifier: Modifier, onConnect: () -> Unit) {
    val demo = app.coDemoPpm
    Column(modifier.panel().padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 머리 줄은 56, 버튼은 누르는 칸 96을 지키려고 위아래로 넘친다(requiredHeight)
        Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Label("실내 공기", 26, FontWeight.Bold, Palette.SubText, modifier = Modifier.weight(1f))
            Box(
                Modifier.pressShrink().requiredHeight(96.dp).clip(RoundedCornerShape(ButtonRadius)).clickable(role = Role.Button, onClick = onConnect),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier.height(56.dp).background(if (demo == null) Palette.Accent else NextStepBg, RoundedCornerShape(28.dp)).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LineIconView(LineIcon.Bluetooth, 24.dp, if (demo == null) Palette.OnAccent else NextStepFg, strokeWidth = 2.4f)
                    Label(if (demo == null) "BLE 연결하기" else "시연 값", 24, FontWeight.ExtraBold, if (demo == null) Palette.OnAccent else NextStepFg)
                }
            }
        }
        AirRow(LineIcon.Fire, CarbonRed, "일산화탄소", "${demo ?: "--"} ppm", Modifier.weight(1f))
        AirRow(LineIcon.Thermometer, Palette.SubText, "온도", "--°C", Modifier.weight(1f))
        AirRow(LineIcon.Water, HumidityBlue, "습도", "--%", Modifier.weight(1f))
    }
}

/** "BLE 연결하기": 센서가 없으니 일산화탄소 시연 값을 골라 05-2 ~ 05-4 경보를 확인한다 */
@Composable
private fun CoDemoDialog(app: AppState, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC08090B)).clickable(role = Role.Button, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(1000.dp).card(44.dp, Palette.CardInner).clickable(enabled = false) {}.padding(horizontal = 44.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Label("실내 공기 센서 연결은 준비 중이에요", 40, FontWeight.ExtraBold)
            Label("시연: 일산화탄소 값을 고르면 경보가 울려요 · 단계는 값이 5초 넘게 이어질 때 바뀌어요", 26, FontWeight.Medium, Palette.SubText, maxLines = 2)
            val choices = listOf(CO_DEMO_NORMAL to "정상", CO_DEMO_CAUTION to "1단계 주의", CO_DEMO_WARNING to "2단계 경고", CO_DEMO_DANGER to "3단계 위험")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                choices.forEach { (ppm, label) ->
                    val on = app.coDemoPpm == ppm
                    Column(
                        Modifier.weight(1f).pressShrink().height(128.dp).clip(RoundedCornerShape(ButtonRadius))
                            .background(if (on) Palette.Text else Palette.Sub)
                            .clickable(role = Role.Button) { app.coDemoPpm = ppm },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val fg = if (on) Palette.Background else Palette.Text
                        Label(label, 28, FontWeight.ExtraBold, fg)
                        Label("$ppm ppm", 24, FontWeight.SemiBold, fg)
                    }
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SecondaryButton("시연 끄기", { app.coDemoPpm = null }, Modifier.weight(1f), height = 96.dp, textSize = 28)
                PrimaryButton("닫기", onClose, Modifier.weight(1f), height = 96.dp, textSize = 28, icon = null, trailingChevron = false)
            }
        }
    }
}

@Composable
private fun AirRow(icon: LineIcon, tint: Color, title: String, value: String, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().background(GlassInner, RoundedCornerShape(22.dp)).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LineIconView(icon, 34.dp, tint)
        Label(title, 26, FontWeight.Bold, Palette.SubText, modifier = Modifier.weight(1f))
        Label(value, 44, FontWeight.ExtraBold)
    }
}

/** 근처 축제 보기 → 10-1(차박 모드는 켠 채로, 돌아오면 05) */
@Composable
private fun FestivalButton(onClick: () -> Unit) {
    Row(
        Modifier.pressShrink().fillMaxWidth().height(112.dp).clip(RoundedCornerShape(ButtonRadius)).background(GlassSecondary).border(1.dp, GlassSecondaryLine, RoundedCornerShape(ButtonRadius))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        LineIconView(LineIcon.Flag, 40.dp, MoonYellow)
        Label("근처 축제 보기", 32, FontWeight.Bold)
    }
}

/**
 * 전기차 배터리(11-10 설계서 05): 머리 줄 "알림까지 n%", 배터리 모양 게이지(지금 % · 밤사이 쓴 몫 빗금 · 20% 노란 눈금),
 * "밤사이 n% 썼어요 · 시작 → 지금", 주행 가능 거리, "기상 7:00쯤 약 n%"(켠 뒤 30분부터).
 */
@Composable
private fun BatteryCard(app: AppState, energy: Energy, modifier: Modifier) {
    val nowMs by produceState(System.currentTimeMillis()) { while (true) { delay(60_000); value = System.currentTimeMillis() } }
    val pct = energy.batteryPercent
    val start = app.campStartBattery
    val used = start?.let { (it - pct).coerceAtLeast(0f) } ?: 0f
    Column(modifier.panel().padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("배터리 · 전기차", 26, FontWeight.Bold, Palette.SubText, modifier = Modifier.weight(1f))
            val margin = (pct - LOW_BATTERY).roundToInt()
            Label(if (margin > 0) "알림까지 $margin%" else "알림 기준 아래", 24, FontWeight.Bold, if (margin > 0) Palette.SubText else MoonYellow)
        }
        Spacer(Modifier.weight(1f))
        BatteryGauge(pct, used)
        if (start != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(28.dp, 20.dp).clip(RoundedCornerShape(6.dp)).background(UsedStripes))
                Label(if (used >= 1f) "밤사이 ${used.roundToInt()}% 썼어요" else "아직 거의 안 썼어요", 26, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Label("${start.roundToInt()}% → ${pct.roundToInt()}%", 24, FontWeight.SemiBold, Palette.MutedText)
            }
        }
        energy.rangeKm?.let { Label("${it.roundToInt()} km 주행 가능", 26, FontWeight.Bold) }
        // 기상 시각: 05-5 알람을 켰으면 그 시각, 아니면 오전 7시
        val wakeMinutes = app.sleepAlerts.takeIf { it.wake }?.wakeMinutes ?: BatteryForecast.MORNING_MINUTES
        val wakeLabel = "%d:%02d".format(wakeMinutes / 60, wakeMinutes % 60)
        val started = app.campStartMs?.let { nowMs - it >= BatteryForecast.MEASURE_MIN_MS } == true
        val forecast = if (started) {
            val hours = BatteryForecast.hoursUntil(java.time.Instant.ofEpochMilli(nowMs).atZone(KOREA), wakeMinutes)
            app.batteryForecast(app.appliedPreset, hours, useMeasured = true)
        } else null
        Row(
            Modifier.fillMaxWidth().background(GlassInner, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LineIconView(LineIcon.Moon, 28.dp, Palette.Accent)
            Label(
                forecast?.let { "기상 ${wakeLabel}쯤 약 ${it.leftPercent}%" } ?: "기상 ${wakeLabel} 예상은 30분 뒤부터",
                26, FontWeight.Bold, if ((forecast?.leftPercent ?: 100) < LOW_BATTERY) MoonYellow else Palette.Text,
            )
        }
    }
}

/** 배터리 모양 게이지: 초록 테두리 몸체 + 오른쪽 꼭지, 안에 지금 %(진한 글씨), 쓴 몫은 빗금, 부족 알림 기준 자리 노란 눈금 */
@Composable
private fun BatteryGauge(percent: Float, used: Float) {
    val green = Color(0xFF4CC38A)
    Row(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = "배터리 ${percent.roundToInt()}% · 노란 눈금은 부족 알림 기준 ${LOW_BATTERY.roundToInt()}%" }, verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxHeight().border(5.dp, green, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16231D)).padding(12.dp),
        ) {
            val inner = maxWidth
            val fill = (percent / 100f).coerceIn(0f, 1f)
            val usedFrac = (used / 100f).coerceIn(0f, 1f - fill)
            // 줄 전체를 둥글게 잘라, 100%처럼 초록이 오른쪽 끝까지 차도 끝이 둥글다(가운데 경계는 곧게)
            Row(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                Box(Modifier.width(inner * fill).fillMaxHeight().background(green))
                Box(Modifier.width(inner * usedFrac).fillMaxHeight().background(UsedStripes))
            }
            // 눈금은 % 글씨 뒤에
            Box(Modifier.offset(x = inner * (LOW_BATTERY / 100f) - 2.dp, y = (-17).dp).size(4.dp, 106.dp).background(MoonYellow, RoundedCornerShape(2.dp)))
            Row(Modifier.fillMaxHeight().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("${percent.roundToInt()}", 56, FontWeight.ExtraBold, Color(0xFF08140E))
                Label("%", 26, FontWeight.ExtraBold, Color(0xFF08140E))
            }
        }
        Box(Modifier.size(14.dp, 48.dp).background(green, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)))
    }
}

/** 밤사이 쓴 몫 빗금 */
private val UsedStripes = Brush.linearGradient(
    0f to Color(0x73F4EFE6), 0.5f to Color(0x73F4EFE6), 0.5f to Color(0x1FF4EFE6), 1f to Color(0x1FF4EFE6),
    start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(12f, 12f),
    tileMode = androidx.compose.ui.graphics.TileMode.Repeated,
)

/** 내일 아침: 일출은 기기에서 계산. 날씨는 다음 단계([Features.WEATHER]) */
@Composable
private fun MorningCard(place: Place, modifier: Modifier) {
    // 장소 데이터가 국내라 한국 시간으로 계산한다(차 시간대 설정과 무관)
    val rise = remember(place.id) { sunrise(LocalDate.now(KOREA).plusDays(1), place.latitude, place.longitude, KOREA) }
    Column(modifier.panel().padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("내일 아침", 26, FontWeight.Bold, Palette.SubText, modifier = Modifier.weight(1f))
            if (!Features.WEATHER) {
                Box(Modifier.height(40.dp).background(NextStepBg, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Label("날씨는 다음 단계", 24, FontWeight.ExtraBold, NextStepFg)
                }
            }
        }
        PanelRow("일출", rise?.format(HHMM) ?: "-", 40)
    }
}

@Composable
private fun PanelRow(title: String, value: String, valueSize: Int) {
    Row(
        Modifier.fillMaxWidth().background(GlassInner, RoundedCornerShape(22.dp)).padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Label(title, 26, FontWeight.Bold, Palette.SubText, modifier = Modifier.weight(1f))
        Label(value, valueSize, FontWeight.ExtraBold)
    }
}

@Composable
private fun Modifier.panel(): Modifier = glass(28.dp)

/**
 * 근거: 단속이나 문의가 오면 보여 줄 한 장(11-11 개선).
 * 맨 위 결론(신호 · 방식 · 이유)을 크게, 아래에 출처 · 확인한 날 · 신뢰도 칸, 계절 · 기간 규칙(자르지 않음), 다른 방식 칩.
 * 바탕은 비치지 않는 카드, 닫기는 오른쪽 위와 바깥 누르기.
 */
@Composable
private fun EvidenceDialog(place: Place, style: CampingStyle, signal: Signal, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC08090B)).clickable(role = Role.Button, onClickLabel = "닫기", onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(1120.dp).heightIn(max = 1040.dp)
                .card(36.dp, if (Palette.isDark) DarkColors.card else LightColors.card)
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 머리: 신뢰도 방패 · 제목 · 닫기
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TrustIcon(place.confidence, 72.dp, hero = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label("근거", 40, FontWeight.ExtraBold)
                    Label("단속이나 문의가 오면 이 화면을 보여 주세요", 26, FontWeight.Medium, Palette.SubText)
                }
                SecondaryButton("닫기", onClose, height = 96.dp, textSize = 28)
            }
            // 결론: 신호 기호 + "스텔스(차 안 숙박) · 허용" + 이유
            val what = if (style == CampingStyle.STEALTH) "스텔스(차 안 숙박)" else style.label
            Row(
                Modifier.fillMaxWidth().background(signal.lampColor.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                    .border(2.dp, signal.lampColor.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 32.dp, vertical = 28.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(Modifier.size(72.dp).background(signal.lampColor, CircleShape), contentAlignment = Alignment.Center) {
                    LineIconView(signal.symbol, 44.dp, Color(0xFF08090B), strokeWidth = 3f)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label("$what · ${signal.label}", 44, FontWeight.ExtraBold, signal.textColor, maxLines = 2)
                    Label(place.styleReason(style), 28, FontWeight.SemiBold, Palette.SoftText, maxLines = 3, lineHeight = 1.4.em)
                }
            }
            // 출처 · 확인한 날 · 신뢰도
            val source = place.evidence.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: place.sourceName ?: "근거 정보 없음"
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                EvidenceFact(LineIcon.Info, "출처", source, Modifier.weight(1.4f))
                EvidenceFact(LineIcon.Clock, "확인한 날", place.verifiedAt ?: "확인 날짜 없음", Modifier.weight(1f))
                EvidenceFact(LineIcon.Shield, "신뢰도", "${TrustLevel.of(place.confidence).title} · ${place.confidence}/3", Modifier.weight(1f))
            }
            // 계절 · 기간 규칙: 자르지 않고 한 줄씩
            if (place.seasonRules.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().background(Palette.Caution.copy(alpha = 0.12f), RoundedCornerShape(24.dp)).padding(horizontal = 28.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LineIconView(LineIcon.Alert, 32.dp, Palette.Caution, strokeWidth = 2.6f)
                        Label("계절 · 기간 규칙", 26, FontWeight.ExtraBold, Palette.Caution)
                    }
                    place.seasonRules.forEach { Label(it, 28, FontWeight.SemiBold, Palette.Text, maxLines = 4, lineHeight = 1.45.em) }
                }
            }
            // 다른 방식 · 취사
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("다른 방식", 24, FontWeight.ExtraBold, Palette.SubText)
                val others = CampingStyle.entries.filter { it != style }.map { it.label to place.signalFor(it) } + ("취사·불" to place.rule(StayRule.COOK).signal)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    others.forEach { (name, sig) ->
                        Row(
                            Modifier.height(64.dp).background(Palette.CardInner, RoundedCornerShape(ChipRadius)).padding(horizontal = 22.dp)
                                .semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(28.dp).background(sig.lampColor, CircleShape), contentAlignment = Alignment.Center) {
                                LineIconView(sig.symbol, 18.dp, Color(0xFF08090B), strokeWidth = 3f)
                            }
                            Label("$name ${sig.label}", 26, FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** 근거 칸 하나: 아이콘 · 작은 제목 · 값 */
@Composable
private fun EvidenceFact(icon: LineIcon, title: String, value: String, modifier: Modifier) {
    Column(
        modifier.fillMaxHeight().background(Palette.CardInner, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineIconView(icon, 28.dp, Palette.SubText)
            Label(title, 22, FontWeight.ExtraBold, Palette.SubText)
        }
        Label(value, 28, FontWeight.Bold, maxLines = 3, lineHeight = 1.4.em)
    }
}

/** 색만으로 가르지 않게 신호마다 기호 */
private val Signal.symbol: LineIcon
    get() = when (this) {
        Signal.GREEN -> LineIcon.Check
        Signal.YELLOW -> LineIcon.Alert
        Signal.RED -> LineIcon.Cross
        Signal.UNKNOWN -> LineIcon.Question
    }

/** 잠들기 전: 잠든 동안 화면을 켜서 알릴 일을 고른다(앱 전체를 덮는다) */
@Composable
fun SleepAskOverlay(app: AppState) {
    val alerts = app.sleepAlerts
    Box(Modifier.fillMaxSize().background(Color(0xCC08090B)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.width(1040.dp).card(44.dp).padding(52.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Label("잠든 동안 알려 드릴까요?", 52, FontWeight.ExtraBold)
            Label("화면을 어둡게 하고, 고른 일이 생길 때만 화면을 켜서 알려요", 26, FontWeight.Medium, Palette.SubText)
            val interval = intervalText(app.appliedPreset?.ventIntervalMin ?: VENT_INTERVAL_MIN.toInt())
            // 이미 20% 아래로 잠들면 알림 기준을 10% · 5%로 낮추고, 미리 경고한다(LowBatteryRule)
            val battery = app.vehicle.snapshot.collectAsState().value.energy?.batteryPercent
            val limit = LowBatteryRule.threshold(battery)
            if (battery != null && battery < LowBatteryRule.STEPS.first()) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0x33E0A100), RoundedCornerShape(24.dp)).padding(horizontal = 28.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LineIconView(LineIcon.Battery, 40.dp, MoonYellow)
                    Label("배터리가 지금 ${battery.roundToInt()}%예요 · 아침까지 버티기 어려울 수 있어요. 공조를 줄여 두세요", 26, FontWeight.Bold, MoonYellow, maxLines = 2)
                }
            }
            AlertToggle("환기 알림", "${interval}마다 · 창문 제어가 되면 자동으로 열고 닫아요", alerts.vent) { app.sleepAlerts = alerts.copy(vent = it) }
            val batterySub = when {
                limit == null -> "지금 ${battery?.roundToInt()}%라 더 낮아져도 알릴 단계가 없어요"
                battery != null && battery < LowBatteryRule.STEPS.first() -> "지금 ${battery.roundToInt()}%라 ${limit.roundToInt()}% 아래로 내려가면 알려요"
                else -> "${limit.roundToInt()}% 아래로 내려가면 · 공조를 줄이라고 알려요"
            }
            AlertToggle("배터리 부족 알림", batterySub, alerts.battery) { app.sleepAlerts = alerts.copy(battery = it) }
            AlertToggle("기상 알람", "정한 시각에 화면을 켜고 알람을 울려요 · 기본 오전 7:00", alerts.wake) { app.sleepAlerts = alerts.copy(wake = it) }
            if (alerts.wake) WakeTimeRow(alerts) { app.campOverlay = CampOverlay.WAKE_SET }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryButton("취소", { app.campOverlay = CampOverlay.NONE }, Modifier.weight(1f), height = 128.dp, textSize = 36)
                PrimaryButton("잠들기", { app.campOverlay = CampOverlay.SLEEP }, Modifier.weight(1f), height = 128.dp, textSize = 38, icon = null, trailingChevron = false)
            }
        }
    }
}

/** 기상 시각 한 줄: 누르면 05-5 기상 알람(시각 · 소리 · 다시 알림)으로 */
@Composable
private fun WakeTimeRow(alerts: SleepAlerts, onOpen: () -> Unit) {
    Row(
        Modifier.pressShrink().fillMaxWidth().height(96.dp).card(24.dp, Palette.CardInner)
            .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.ContextClick, onOpen))
            .semantics { contentDescription = "기상 시각 ${alerts.wakeLabel} 바꾸기" }
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Label("기상 시각", 26, FontWeight.Bold, Palette.SubText, Modifier.weight(1f))
        Label(alerts.wakeLabel, 30, FontWeight.ExtraBold, WakeGreen)
        Label("바꾸기 ›", 26, FontWeight.Bold)
    }
}

@Composable
private fun AlertToggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.pressShrink().fillMaxWidth().height(108.dp).card(24.dp, Palette.CardInner).clickable(role = Role.Switch, onClick = withHaptic(if (on) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn) { onChange(!on) }).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, 30, FontWeight.ExtraBold)
            Label(sub, 24, FontWeight.Medium, Palette.SubText)
        }
        // 11-11 Pleos 가이드: 설정과 같은 공용 스위치
        PleosSwitch(on)
    }
}

/**
 * 잠든 화면: 검은 화면 + 시계, 창 밝기를 최저로 낮춘다. 누르면 05로 돌아온다.
 * 고른 일(환기 시간·배터리 20% 아래·기상 시각)이 되면 스스로 깨어나 알린다.
 */
@Composable
fun SleepOverlay(app: AppState, snapshot: VehicleSnapshot) {
    val alerts: SleepAlerts = app.sleepAlerts
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val window = activity?.window
        val before = window?.attributes?.screenBrightness
        window?.attributes = window?.attributes?.apply { screenBrightness = 0.01f }
        onDispose { window?.attributes = window?.attributes?.apply { screenBrightness = before ?: -1f } }
    }
    val now by produceState(LocalTime.now()) {
        while (true) {
            delay(15_000)
            value = LocalTime.now()
        }
    }
    val ventMin = app.appliedPreset?.ventIntervalMin?.toLong() ?: VENT_INTERVAL_MIN
    // 다음 환기: 실제 환기 일정(ventNextAt)이 있으면 그 시각, 없으면 다시 잠들어도 이어 쓰는 시각(QA-04)
    val keptVentAt = remember(alerts.vent) { if (alerts.vent) app.nextSleepVentAt(ventMin) else 0L }
    val ventAtMs = app.ventNextAt ?: keptVentAt
    val nextVent = Instant.ofEpochMilli(ventAtMs).atZone(ZoneId.systemDefault()).toLocalTime()
    fun wake(message: String) {
        app.campOverlay = CampOverlay.NONE
        app.notice(message)
    }
    if (alerts.vent) {
        LaunchedEffect(ventAtMs) {
            delay((ventAtMs - System.currentTimeMillis()).coerceAtLeast(0))
            app.sleepVentFired()
            wake("환기할 시간이에요 · 창문을 조금 열어 주세요")
        }
    }
    // 기상 알람: 정한 시각(또는 "n분 뒤 다시" 시각)에 화면을 켜고 05-6을 덮는다
    val snoozeUntil = app.wakeSnoozeUntil
    if (alerts.wake || snoozeUntil != null) {
        LaunchedEffect(alerts.wakeMinutes, snoozeUntil) {
            delay(snoozeUntil?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) } ?: millisUntil(alerts.wakeMinutes))
            app.ringWake()
        }
    }
    // 잠들 때 배터리로 이번 알림 기준을 정한다: 20% 이상이면 20%, 이미 아래면 10% · 5%(LowBatteryRule)
    val battery = snapshot.energy?.batteryPercent
    // 05-7에 "잠든 뒤 n% 줄었어요"를 보여 주려고 잠들 때 배터리와 시각을 기억한다
    val startBattery = remember { battery }
    val startMs = remember { System.currentTimeMillis() }
    val batteryLimit = LowBatteryRule.threshold(startBattery)
    LaunchedEffect(battery) {
        if (alerts.battery && battery != null && LowBatteryRule.shouldAlert(startBattery, battery)) {
            app.ringLowBattery(LowBatteryAlert(battery.roundToInt(), startBattery, startMs, System.currentTimeMillis()))
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color.Black).clickable(role = Role.Button) { app.campOverlay = CampOverlay.NONE }
            .semantics { contentDescription = "화면 켜기" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        val dim = Color(0xFF6B7080)
        Label(now.format(HHMM), 160, FontWeight.Bold, dim)
        val width = app.appliedPreset?.windowOpenCm?.let { " · 창문 $it cm" } ?: ""
        Label(if (alerts.vent) "다음 환기 ${nextVent.format(HHMM)}$width" else "알림을 켜 둔 일만 화면을 켜요", 30, FontWeight.SemiBold, dim)
        // 칩은 "켜 둔 알림"이다. "배터리 부족"만 쓰면 지금 배터리가 부족하다는 뜻으로 읽혀, 언제 알리는지로 쓴다
        val chips = listOfNotNull(
            "환기".takeIf { alerts.vent },
            batteryLimit?.let { "배터리 ${it.roundToInt()}% 아래" }?.takeIf { alerts.battery },
            snoozeUntil?.let { "다시 알림 ${Instant.ofEpochMilli(it).atZone(KOREA).format(HHMM)}" }
                ?: "기상 ${alerts.wakeLabel}".takeIf { alerts.wake },
        )
        if (chips.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("켜 둔 알림", 24, FontWeight.SemiBold, Color(0xFF4A4F59), Modifier.padding(end = 4.dp))
                chips.forEach { SleepChip(it, dim) }
            }
        }
        // 5% 아래로 잠들면 배터리 칩이 빠진다. 왜 빠졌는지 한 줄로 알린다(QA-05)
        LowBatteryRule.noStepNote(startBattery)?.takeIf { alerts.battery }?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LineIconView(LineIcon.Battery, 32.dp, MoonYellow.copy(alpha = 0.7f))
                Label(it, 24, FontWeight.SemiBold, MoonYellow.copy(alpha = 0.7f))
            }
        }
        Label("화면을 누르면 다시 켜져요", 24, FontWeight.Medium, Color(0xFF4A4F59), Modifier.padding(top = 60.dp))
    }
}

@Composable
private fun SleepChip(text: String, color: Color) {
    Box(Modifier.height(52.dp).border(2.dp, Palette.Sub, RoundedCornerShape(26.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        Label(text, 24, FontWeight.Bold, color)
    }
}

/** 지금부터 다음 [minutesOfDay](오늘 지났으면 내일)까지 남은 시간 */
internal fun millisUntil(minutesOfDay: Int, now: LocalTime = LocalTime.now()): Long {
    val nowSec = now.toSecondOfDay().toLong()
    val target = minutesOfDay * 60L
    val diff = if (target > nowSec) target - nowSec else target + 24 * 3600 - nowSec
    return diff * 1000
}

private val WakeGreen = Color(0xFF8EE0B4)
private val ExitRed = Color(0xFFFF7A7E)
private val CarbonRed = Color(0xFFFF7A7E)
private val MoonYellow = Color(0xFFFFD98A)
private val CarOrange = Color(0xFFE58A3A)
private val HumidityBlue = Color(0xFFB9C4B8)
private val PinBlue = Color(0xFFDAD1BF)
private val MetaOnPhoto = Color(0xFFE3E7EE)

/** 05 장소 이름 그림자(0 2 18 · 검정 70%): 사진 가운데가 밝아져서 넓게 */
private val TitleShadow = Shadow(Color(0xB3000000), Offset(0f, 2f), blurRadius = 18f)

// 시연 값: 단계마다 기준값보다 조금 위(정상은 50 ppm 아래)
private const val CO_DEMO_NORMAL = 20
private const val CO_DEMO_CAUTION = 60
private const val CO_DEMO_WARNING = 150
private const val CO_DEMO_DANGER = 250

private const val RECHECK_MS = 30 * 60_000L
private const val VENT_INTERVAL_MIN = 60L
internal const val LOW_BATTERY = 20f
