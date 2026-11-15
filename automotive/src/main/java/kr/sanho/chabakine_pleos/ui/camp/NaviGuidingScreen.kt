package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.ui.components.ButtonRadius
import android.car.drivingstate.CarUxRestrictions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.platform.NaviStatus
import kr.sanho.chabakine_pleos.platform.RouteRemaining
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.shared.place.distanceKmFrom
import kr.sanho.chabakine_pleos.ui.components.DrivingPhotoDim
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.find.siteTiles
import kr.sanho.chabakine_pleos.ui.find.summarySpeech
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.textColor
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 화면설계서 03-1 내비 안내 중(주행 중에도 보인다, 출발해도 닫히지 않는다).
 * 왼쪽: 목적지(사진 첫 장 · 신호 · 조건 한 줄 · 남은 거리) + 가기 전에 알아 둘 것. 오른쪽: 큰 버튼 3개.
 * 남은 거리·도착 예정은 내비가 알려 줄 때만(onDrivingInfo), 아니면 차 위치에서 잰 직선 거리만 보여 준다.
 * 분할(840 · 07-4)도 같은 사진 화면이고 버튼만 위아래로 쌓는다(큰 버튼 하나 → 버튼 두 개 한 줄).
 */
@Composable
fun NaviGuidingScreen(
    app: AppState,
    place: Place,
    status: NaviStatus,
    remaining: RouteRemaining?,
    parked: Boolean,
    settings: Settings,
    modifier: Modifier = Modifier,
    split: Boolean = false,
) {
    // 시스템이 글 길이를 줄이라고 하면(UX_RESTRICTIONS_LIMIT_STRING_LENGTH) 이유 문장은 한 줄만
    val limitText = app.uxRestricted(CarUxRestrictions.UX_RESTRICTIONS_LIMIT_STRING_LENGTH)
    val style = settings.primaryStyle
    val signal = place.signalFor(style)
    val item = PlaceItem(place, signal, place.distanceKmFrom(app.origin), null)
    val arrived = status is NaviStatus.Arrived && status.placeId == place.id
    FullGuiding(app, place, item, style, remaining, arrived, parked, limitText, modifier, split)
}

/**
 * 03-1(11-08 개선): 메뉴 줄 없이 화면 전체가 목적지 사진(위 0.45 → 가운데 0.8 → 아래 0.95로 어둡게).
 * 위에 떠 있는 줄(안내 아이콘 · 제목 · 주행 표시), 가운데 신호 · 이름 · 조건 한 줄 · 남은 거리,
 * 맨 아래 버튼 한 줄(내비 화면으로 1.3 : 요약 읽어 주기 1 : 안내 취소 1). 주행 중이라 사진은 첫 장만.
 */
@Composable
private fun FullGuiding(
    app: AppState,
    place: Place,
    item: PlaceItem,
    style: CampingStyle,
    remaining: RouteRemaining?,
    arrived: Boolean,
    parked: Boolean,
    limitText: Boolean,
    modifier: Modifier,
    split: Boolean,
) {
    DarkScope {
    Box(modifier.fillMaxSize().background(Palette.Background)) {
        place.photoUrls.firstOrNull()?.let {
            AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            DrivingPhotoDim(!parked)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x7308090B), 0.5f to Color(0xCC08090B), 1f to Color(0xF208090B))))
        val pad = if (split) 32.dp else 56.dp
        Column(Modifier.fillMaxSize().padding(start = pad, end = pad, top = if (split) 188.dp else 184.dp, bottom = if (split) 32.dp else 56.dp), verticalArrangement = Arrangement.spacedBy(if (split) 18.dp else 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SignalLamp(item.signal, if (split) 44.dp else 48.dp, showSymbol = true)
                Label(item.signal.label, if (split) 32 else 34, FontWeight.ExtraBold, item.signal.textColor)
            }
            Label(place.name, if (split) 56 else 64, FontWeight.ExtraBold, maxLines = 2)
            Label(place.reasonFor(style), if (split) 30 else 34, FontWeight.Bold, maxLines = if (limitText) 1 else 2)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Label(remaining?.let { formatKm(it.meters / 1000.0) } ?: formatKm(item.distanceKm), if (split) 52 else 56, FontWeight.ExtraBold)
                Label(remaining?.let { "남음 · 도착 ${arrivalTime(it.seconds)}" } ?: "직선 거리", 26, FontWeight.SemiBold, Palette.SubText)
            }
            Spacer(Modifier.weight(1f))
            val read: () -> Unit = { app.speak(guidingSpeech(item, style, cautions(place))) }
            val cancelModifier = Modifier.border(2.dp, Palette.Gauge, RoundedCornerShape(ButtonRadius))
            val cancelBackground = Palette.Background.copy(alpha = 0.7f)
            if (split) {
                // 07-4: 840이라 버튼은 위아래로
                PrimaryButton("내비 화면으로", app::showNavigationScreen, Modifier.fillMaxWidth(), height = 124.dp, textSize = 34, trailingChevron = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SecondaryButton("요약 읽어 주기", read, Modifier.weight(1f), height = 108.dp, textSize = 28, icon = LineIcon.Volume)
                    SecondaryButton("안내 취소", app::cancelGuidance, Modifier.weight(1f).then(cancelModifier), height = 108.dp, textSize = 28, background = cancelBackground)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrimaryButton("내비 화면으로", app::showNavigationScreen, Modifier.weight(1.3f), height = 128.dp, textSize = 32, trailingChevron = false)
                    SecondaryButton("요약 읽어 주기", read, Modifier.weight(1f), height = 128.dp, textSize = 30, icon = LineIcon.Volume)
                    SecondaryButton("안내 취소", app::cancelGuidance, Modifier.weight(1f).then(cancelModifier), height = 128.dp, textSize = 30, background = cancelBackground)
                }
            }
        }
        // 위에 떠 있는 줄
        Row(
            Modifier.fillMaxWidth().padding(start = if (split) 32.dp else 48.dp, end = if (split) 32.dp else 48.dp, top = if (split) 32.dp else 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (split) 18.dp else 20.dp),
        ) {
            Box(Modifier.size(if (split) 64.dp else 72.dp).background(Palette.Accent, CircleShape), contentAlignment = Alignment.Center) {
                LineIconView(LineIcon.Navigate, if (split) 34.dp else 40.dp, Palette.OnAccent)
            }
            Label(if (arrived) "도착했어요" else "내비로 안내 중", if (split) 40 else 46, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            StatusPill(parked)
        }
    }
    }
}

/** 도착하기 전에 알아 둘 것: 현장 조건(높이·좁음·바람) · 화장실 · 취사 금지 */
fun cautions(place: Place): List<Pair<LineIcon, String>> = buildList {
    siteTiles(place).forEach { (icon, text) ->
        add(
            icon to when (icon) {
                LineIcon.HeightLimit -> "입구 높이 제한 $text"
                LineIcon.Narrow -> "주차면이 좁아요"
                LineIcon.Wind -> "바람이 세요"
                else -> text
            },
        )
    }
    when (place.amenities.toilet) {
        true -> add(LineIcon.Toilet to "화장실 있음")
        false -> add(LineIcon.Toilet to "화장실 없음")
        null -> Unit
    }
    if (place.rule(StayRule.COOK).signal == Signal.RED) add(LineIcon.Fire to "취사·불 금지")
}

private fun guidingSpeech(item: PlaceItem, style: CampingStyle, notes: List<Pair<LineIcon, String>>): String =
    summarySpeech(item, style) + notes.takeIf { it.isNotEmpty() }?.joinToString(". ", prefix = ". 알아 둘 것. ") { it.second }.orEmpty()

private fun arrivalTime(seconds: Int): String =
    LocalTime.now(ZoneId.of("Asia/Seoul")).plusSeconds(seconds.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))
