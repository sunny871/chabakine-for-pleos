package kr.sanho.chabakine_pleos.ui.split

import kr.sanho.chabakine_pleos.ui.components.BackChevron
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import kr.sanho.chabakine_pleos.ui.camp.LOW_BATTERY
import kr.sanho.chabakine_pleos.vehicle.BatteryForecast
import java.time.ZoneId
import java.time.Instant
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import kr.sanho.chabakine_pleos.R
import kr.sanho.chabakine_pleos.app.SheetTopic
import kr.sanho.chabakine_pleos.ui.camp.glass
import kr.sanho.chabakine_pleos.ui.camp.glassButton
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.find.AmenityGrid
import kr.sanho.chabakine_pleos.ui.find.ConfidenceShield
import kr.sanho.chabakine_pleos.ui.find.ProgressiveBlurPhoto
import kr.sanho.chabakine_pleos.ui.find.siteTiles
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.CampOverlay
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.Amenities
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.camp.CheckRow
import kr.sanho.chabakine_pleos.ui.camp.LEAVE_CHECKS
import kr.sanho.chabakine_pleos.ui.components.DataBanner
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.RoundIcon
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.find.siteWarnings
import kr.sanho.chabakine_pleos.ui.find.styleReason
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.lampColor
import kr.sanho.chabakine_pleos.ui.theme.textColor
import kr.sanho.chabakine_pleos.vehicle.Energy
import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot
import kotlin.math.roundToInt

/** 분할 화면 머리(120): 왼쪽 버튼 + 제목 + 오른쪽 */
@Composable
private fun SplitHeader(title: String, subtitle: String, onBack: (() -> Unit)?, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        onBack?.let { BackChevron("목록으로", it) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, 40, FontWeight.ExtraBold)
            Label(subtitle, 24, FontWeight.Medium, Palette.SubText)
        }
        trailing()
    }
}

/** 편의시설 한 줄(분할용): 있음·없음만 묶어 짧게. 미확인은 빼고 취사·불 금지는 붙인다 */
private fun amenityShort(a: Amenities, cook: Signal): String {
    val names = listOf("화장실" to a.toilet, "수도" to a.water, "전기" to a.electricity, "샤워" to a.shower)
    val yes = names.filter { it.second == true }.joinToString("·") { it.first }
    val no = names.filter { it.second == false }.joinToString("·") { it.first }
    return listOfNotNull(
        yes.takeIf { it.isNotEmpty() }?.let { "$it 있음" },
        no.takeIf { it.isNotEmpty() }?.let { "$it 없음" },
        "취사·불 금지".takeIf { cook == Signal.RED },
    ).joinToString(" · ").ifEmpty { "편의시설 확인 안 됨" }
}

/** 편의시설 · 현장 조건(07-1): 02 편의시설 칸 그대로 + 현장 조건은 아이콘 붙은 한 줄 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitySiteCard(app: AppState, place: Place) {
    Column(Modifier.fillMaxWidth().card(24.dp).padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label("편의시설 · 현장 조건", 26, FontWeight.ExtraBold)
        AmenityGrid(place, labeled = true, Modifier.fillMaxWidth(), onCookClick = { app.openSheet(place.id, SheetTopic.Rule(null)) })
        val tiles = siteTiles(place)
        if (tiles.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.forEach { (icon, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LineIconView(icon, 34.dp, Palette.BannerText)
                        Label(text, 24, FontWeight.Bold, Palette.BannerText)
                    }
                }
            }
        }
    }
}

/** 07-3 차박 모드 카드: 편의시설은 짧은 한 줄 */
@Composable
private fun AmenityShortCard(place: Place) {
    Column(Modifier.fillMaxWidth().glass(28.dp).padding(horizontal = 26.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("편의시설 · 현장 조건", 24, FontWeight.Bold, Palette.SubText)
        Label(amenityShort(place.amenities, place.rule(StayRule.COOK).signal), 26, FontWeight.SemiBold, maxLines = 2)
        val warnings = siteWarnings(place)
        if (warnings.isNotEmpty()) Label(warnings.joinToString(" · "), 24, FontWeight.Bold, Palette.BannerText, maxLines = 2)
    }
}

/**
 * 화면설계서 07-1 분할 · 02 찾기 정차 중(상세만, 11-08 개선). 840에서는 목록과 상세를 한 화면에 두지 않고,
 * 목록(07)에서 고르면 이 상세로 넘어온다. 02처럼 사진 한 장이 화면 위에서부터 깔리고 아래로 갈수록 흐려지며
 * 바탕으로 이어진다(사진 머리 경계선 · 머리 카드 없음).
 */
@Composable
fun SplitFindDetail(app: AppState, item: PlaceItem, style: CampingStyle, onBack: () -> Unit) {
    val place = item.place
    val signal = item.signal
    val photo = place.photoUrls.firstOrNull()
    val shadow = if (Palette.isDark) Shadow(Color(0x99000000), blurRadius = 20f) else null
    Box(Modifier.fillMaxSize()) {
        photo?.let {
            ProgressiveBlurPhoto(it, Modifier.matchParentSize(), sharpUntil = 0.22f, fadeEnd = 0.5f)
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Palette.Background.copy(alpha = 0.35f), 0.3f to Palette.Background.copy(alpha = 0.55f),
                        0.5f to Palette.Background.copy(alpha = 0.78f), 1f to Palette.Background.copy(alpha = 0.9f),
                    ),
                ),
            )
        }
        Column(Modifier.fillMaxSize()) {
            val sub = listOfNotNull(place.site?.type, formatKm(item.distanceKm), item.rating?.let { "★ ${it.average}" }).joinToString(" · ")
            SplitHeader(place.name, sub, onBack) {
                StatusPill(parked = true)
                RoundIcon(LineIcon.Search, "검색", 96.dp, 44.dp, Palette.Sub, { app.navigate(Route.Search) })
            }
            DataBanner(app, Modifier.padding(horizontal = 28.dp))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 사진 위 요약(카드 없음): 신호 · 이유 한 줄 · 확인 날짜와 근거
                Column(
                    Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 4.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SignalLamp(signal, 34.dp, showSymbol = true)
                        Label("${style.label} ${signal.label}", 30, FontWeight.ExtraBold, signal.textColor, shadow = shadow)
                    }
                    Label(place.styleReason(style), 26, FontWeight.SemiBold, maxLines = 2, shadow = shadow)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ConfidenceShield(place.confidence, 52.dp)
                        val meta = listOfNotNull(
                            place.verifiedAt?.let { "확인 $it" },
                            (place.evidence.firstOrNull() ?: place.sourceName)?.let { "근거 $it" },
                        ).joinToString(" · ")
                        Label(meta, 24, FontWeight.Medium, Palette.SoftText, maxLines = 2, shadow = shadow)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampingStyle.entries.forEach { s ->
                        StyleCell(s, place.signalFor(s), Modifier.weight(1f)) { app.openSheet(place.id, SheetTopic.Rule(s)) }
                    }
                }
                AmenitySiteCard(app, place)
                Row(
                    Modifier.fillMaxWidth().height(96.dp).card(24.dp).clickable(role = Role.Button) { app.navigate(Route.Reviews(place.id)) }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Label(item.rating?.let { "리뷰 ★ ${it.average} · ${it.count}개" } ?: "리뷰", 26, FontWeight.Bold, modifier = Modifier.weight(1f))
                    Label("보기 ›", 26, FontWeight.Bold, Palette.Selected)
                }
                if (place.seasonRules.isNotEmpty()) {
                    Label("계절·시간: ${place.seasonRules.joinToString(" · ")}", 24, FontWeight.Medium, Palette.SubText, maxLines = 4)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryButton("여기로 안내", { app.navigateTo(place) }, Modifier.weight(1f), height = 120.dp, textSize = 34, icon = null, trailingChevron = false)
                if (app.campModeAvailable) SecondaryButton("차박 모드 켜기", { app.requestCampMode(place.id) }, Modifier.weight(1f), height = 120.dp, textSize = 30)
            }
        }
    }
}

/** 방식별 가능 여부 칸(07-1): 신호 색을 옅게 깐 84 높이 칸. 누르면 그 방식 이유 시트 */
@Composable
private fun StyleCell(style: CampingStyle, signal: Signal, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (signal == Signal.UNKNOWN) Palette.Sub else signal.lampColor.copy(alpha = 0.18f)
    val mark = when (signal) {
        Signal.GREEN -> "✓"
        Signal.YELLOW -> "!"
        Signal.RED -> "✕"
        Signal.UNKNOWN -> "?"
    }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(96.dp).clip(shape).background(tint).border(1.dp, Palette.Gauge, shape)
            .clickable(role = Role.Button, onClickLabel = "${style.label} 이유 보기", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Label(style.label, 24, FontWeight.Medium, Palette.SubText)
        Label("$mark ${signal.label}", 24, FontWeight.Bold, signal.textColor)
    }
}

/**
 * 화면설계서 07-3 분할 · 05 차박 모드(11-08 개선). 05처럼 화면 전체가 장소 사진(AppRoot가 깐다), 카드는 유리판.
 * 840에서는 핵심 카드(내 방식 신호 · 배터리 · 편의시설)만 남긴다. 잠들기가 주 버튼(크림), 철수 준비는 유리 버튼.
 * 내일 아침 · 근거 카드는 넓은 화면(05)에서만, 차량 맞추기는 승인 시에만.
 */
@Composable
fun SplitCampMode(app: AppState, place: Place, style: CampingStyle, snapshot: VehicleSnapshot) {
    val signal = place.signalFor(style)
    Column(Modifier.fillMaxSize()) {
        val temp = snapshot.outsideTempC?.let { " · 바깥 ${it.roundToInt()}°C" }.orEmpty()
        Row(
            Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Image(painterResource(R.drawable.chabakine_logo), "차박이네", Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(place.name, 48, FontWeight.ExtraBold, shadow = Shadow(Color(0xB3000000), blurRadius = 36f))
                Label("차박 중$temp", 24, FontWeight.Medium, Palette.SubText)
            }
            StatusPill(parked = true)
        }
        DataBanner(app, Modifier.padding(horizontal = 28.dp))
        Column(Modifier.weight(1f).padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth().glass(28.dp).padding(26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                SignalLamp(signal, 44.dp, showSymbol = true, vertical = true)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("내 방식 ${style.label}", 24, FontWeight.Bold, Palette.SubText)
                    Label(signalHeadline(signal), 40, FontWeight.ExtraBold)
                    Label(place.styleReason(style), 26, FontWeight.Medium, Palette.SoftText, maxLines = 2)
                    val forbidden = CampingStyle.entries.filter { it != style && place.signalFor(it) == Signal.RED }.map { it.label } +
                        listOfNotNull("취사·불".takeIf { place.rule(StayRule.COOK).signal == Signal.RED })
                    if (forbidden.isNotEmpty()) Label("${forbidden.joinToString("·")}은 금지", 24, FontWeight.SemiBold, Palette.Danger)
                }
            }
            snapshot.energy?.let { SplitEnergy(app, it) }
            AmenityShortCard(place)
            Spacer(Modifier.weight(1f))
            PrimaryButton("잠들기", { app.campOverlay = CampOverlay.ASK_SLEEP }, Modifier.fillMaxWidth(), height = 120.dp, textSize = 34, icon = LineIcon.Moon, trailingChevron = false, glow = true)
            SecondaryButton("철수 준비", { app.navigate(Route.CampLeave) }, Modifier.fillMaxWidth().glassButton(), height = 112.dp, textSize = 32, background = Color.Transparent)
            SecondaryButton("가까운 다른 곳 찾기", { app.navigate(Route.Find()) }, Modifier.fillMaxWidth().glass(26.dp), height = 96.dp, textSize = 28, background = Color.Transparent)
        }
    }
}

private fun signalHeadline(signal: Signal) = when (signal) {
    Signal.GREEN -> "가능해요"
    Signal.YELLOW -> "조건부로 가능해요"
    Signal.RED -> "이곳은 금지예요"
    Signal.UNKNOWN -> "아직 확인 못 했어요"
}

/** 07-3 배터리(11-10): 05와 같은 계산을 한 줄로 · 게이지에 부족 알림 기준 눈금 */
@Composable
private fun SplitEnergy(app: AppState, energy: Energy) {
    val nowMs by produceState(System.currentTimeMillis()) { while (true) { delay(60_000); value = System.currentTimeMillis() } }
    val pct = energy.batteryPercent
    Column(Modifier.fillMaxWidth().glass(28.dp).padding(horizontal = 26.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label("배터리 · 전기차", 24, FontWeight.Bold, Palette.SubText)
        Label("${pct.roundToInt()}%" + (energy.rangeKm?.let { " · ${it.roundToInt()} km 주행 가능" } ?: ""), 38, FontWeight.ExtraBold)
        BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
            Box(Modifier.fillMaxSize().background(Palette.Gauge, RoundedCornerShape(9.dp))) {
                Box(Modifier.fillMaxWidth(pct.coerceIn(0f, 100f) / 100f).fillMaxHeight().background(Palette.GaugeFill, RoundedCornerShape(9.dp)))
            }
            Box(Modifier.offset(x = maxWidth * (LOW_BATTERY / 100f) - 2.dp, y = (-6).dp).size(4.dp, 30.dp).background(Palette.Caution, RoundedCornerShape(2.dp)))
        }
        val used = app.campStartBattery?.let { (it - pct).coerceAtLeast(0f) }
        val wakeMinutes = app.sleepAlerts.takeIf { it.wake }?.wakeMinutes ?: BatteryForecast.MORNING_MINUTES
        val started = app.campStartMs?.let { nowMs - it >= BatteryForecast.MEASURE_MIN_MS } == true
        val morning = if (started) {
            app.batteryForecast(app.appliedPreset, BatteryForecast.hoursUntil(Instant.ofEpochMilli(nowMs).atZone(ZoneId.of("Asia/Seoul")), wakeMinutes), useMeasured = true)
        } else null
        val line = listOfNotNull(
            used?.let { if (it >= 1f) "밤사이 ${it.roundToInt()}% 썼어요" else "아직 거의 안 썼어요" },
            morning?.let { "기상 %d:%02d쯤 약 %d%%".format(wakeMinutes / 60, wakeMinutes % 60, it.leftPercent) },
        ).joinToString(" · ")
        if (line.isNotEmpty()) Label(line, 24, FontWeight.SemiBold, Palette.SoftText)
    }
}

/** 화면설계서 07-5 분할 · 10 철수 안내(11-08: 장소 사진 바탕 + 유리 카드, AppRoot가 깐다). 다음 후보는 840에서 10-1에만 보여 준다. */
@Composable
fun SplitCampLeave(app: AppState, place: Place) {
    val done = LEAVE_CHECKS.count { it in app.checkedItems }
    val left = LEAVE_CHECKS.size - done
    Column(Modifier.fillMaxSize()) {
        SplitHeader("정리하고 떠나요", "철수 안내 · ${place.name}", onBack = { app.replace(Route.CampMode) })
        DataBanner(app, Modifier.padding(horizontal = 28.dp))
        Column(Modifier.weight(1f).padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("철수 체크리스트", 32, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Label("$done / ${LEAVE_CHECKS.size}", 26, FontWeight.Bold, Palette.SubText)
            }
            LEAVE_CHECKS.forEach { CheckRow(app, it, null, titleWidth = false) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = if (left == 0) "철수 완료" else "철수 완료 · ${left}개 남음",
                onClick = app::finishCamp,
                modifier = Modifier.fillMaxWidth(),
                height = 128.dp,
                textSize = 38,
                icon = if (left == 0) LineIcon.Check else null,
                trailingChevron = false,
                enabled = left == 0,
            )
            Label("모두 체크하면 버튼이 켜지고, 누르면 오늘 아침 주변을 보여 드려요", 22, FontWeight.Medium, Palette.MutedText, maxLines = 2)
        }
    }
}
