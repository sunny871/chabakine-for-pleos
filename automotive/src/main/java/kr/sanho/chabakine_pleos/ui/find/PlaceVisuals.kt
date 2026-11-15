package kr.sanho.chabakine_pleos.ui.find

import kr.sanho.chabakine_pleos.ui.components.pressShrink
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.ui.components.DrivingPhotoDim
import kr.sanho.chabakine_pleos.ui.components.TrustShield
import kr.sanho.chabakine_pleos.ui.components.TrustLevel
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.lampColor

private val SelectedBorder: Color @Composable @ReadOnlyComposable get() = Palette.Selected
private val CellLine: Color @Composable @ReadOnlyComposable get() = Palette.Gauge
private val RedText: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0xFFFF9A9D) else Palette.Danger
private val RedIcon: Color @Composable @ReadOnlyComposable get() = Palette.Danger
private val RedCell: Color @Composable @ReadOnlyComposable get() = Palette.DangerFill

// 편의시설 "있음"은 파란색(신호등 초록·노랑·빨강과 겹치지 않게)
private val BlueCell: Color @Composable @ReadOnlyComposable get() = Palette.GuideFill
private val BlueIcon: Color @Composable @ReadOnlyComposable get() = Palette.GuideIcon

/** 사진 + 글자 쪽(왼쪽)을 어둡게 덮는 그라데이션. 사진이 없으면 아무것도 깔지 않는다. [driving]이면 한 겹 더 어둡게 */
@Composable
fun BoxScope.RowPhoto(url: String?, driving: Boolean = false) {
    if (url == null) return
    AsyncImage(url, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    Box(
        Modifier.matchParentSize().background(
            Brush.horizontalGradient(0f to Color(0xD908090B), 0.55f to Color(0x9908090B), 1f to Color(0x5908090B)),
        ),
    )
    DrivingPhotoDim(driving)
}

/**
 * 목록 한 줄(화면설계서 01·02). 사진이 있으면 줄 배경으로 꽉 채우고, 신호등은 세로.
 * 주행 중은 크게(184) 이름과 거리·별점만, 정차 중은 작게(150) 신호·거리·별점 한 줄을 더한다.
 */
@Composable
fun PlaceRow(item: PlaceItem, selected: Boolean, parked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (parked) 24.dp else 28.dp)
    DarkScope(item.place.photoUrls.isNotEmpty()) {
    Box(
        Modifier
            .pressShrink()
            .fillMaxWidth()
            .height(if (parked) 150.dp else 184.dp)
            .clip(shape)
            .background(Palette.Card)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        RowPhoto(item.place.photoUrls.firstOrNull(), driving = !parked)
        if (selected) Box(Modifier.matchParentSize().border(4.dp, SelectedBorder, shape))
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (parked) 24.dp else 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (parked) 20.dp else 24.dp),
        ) {
            SignalLamp(item.signal, if (parked) 30.dp else 40.dp, showSymbol = true, vertical = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(item.place.name, if (parked) 32 else 36, FontWeight.ExtraBold, maxLines = 2)
                if (parked) {
                    val sub = listOfNotNull(item.signal.label, formatKm(item.distanceKm), item.rating?.let { "★ ${it.average}" })
                    Label(sub.joinToString(" · "), 24, FontWeight.Medium, Palette.SubText)
                }
            }
            if (!parked) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label(formatKm(item.distanceKm), 32, FontWeight.ExtraBold)
                    item.rating?.let { Label("★ ${it.average}", 24, FontWeight.Bold, Palette.SubText) }
                }
            }
        }
    }
    }
}

private enum class CellState { YES, NO, UNKNOWN, CAUTION, FORBIDDEN }

private class AmenityCell(val name: String, val icon: LineIcon, val state: CellState, val stateText: String, val isCook: Boolean = false)

private fun Boolean?.cell(): CellState = when (this) {
    true -> CellState.YES
    false -> CellState.NO
    null -> CellState.UNKNOWN
}

/**
 * 편의시설 4칸 + 취사·불 1칸. 있음 파란색 · 없음 회색 · 확인 안 됨 점선 · 금지 빨간색(취사 조건부는 노란색).
 * null은 "확인 안 됨"(점선)이지 "없음"이 아니다.
 */
private fun amenityCells(place: Place): List<AmenityCell> {
    val a = place.amenities
    fun text(s: CellState) = when (s) {
        CellState.YES -> "있음"
        CellState.NO -> "없음"
        CellState.UNKNOWN -> "확인 안 됨"
        CellState.CAUTION -> "조건부"
        CellState.FORBIDDEN -> "금지"
    }
    val cook = when (place.rule(StayRule.COOK).signal) {
        Signal.GREEN -> CellState.YES
        Signal.YELLOW -> CellState.CAUTION
        Signal.RED -> CellState.FORBIDDEN
        Signal.UNKNOWN -> CellState.UNKNOWN
    }
    return listOf(
        Triple("화장실", LineIcon.Toilet, a.toilet.cell()),
        Triple("수도", LineIcon.Water, a.water.cell()),
        Triple("전기", LineIcon.Power, a.electricity.cell()),
        Triple("샤워", LineIcon.Shower, a.shower.cell()),
        Triple("취사·불", LineIcon.Fire, cook),
    ).map { (name, icon, state) ->
        val cook = name == "취사·불"
        AmenityCell(name, icon, state, if (cook && state == CellState.YES) "가능" else text(state), isCook = cook)
    }
}

/** "확인 안 됨"이 아닌 편의시설이 하나라도 있다(01 주행 중에는 확인된 것만 보여 준다) */
fun hasKnownAmenity(place: Place): Boolean = amenityCells(place).any { it.state != CellState.UNKNOWN }

/**
 * 편의시설 5칸(화면설계서 02·17 범례). [labeled]면 아이콘 아래 이름(02),
 * 아니면 88 정사각 아이콘만(01). 사진 위(05 차박 모드)에서는 [tileSize] 96 · [tileRadius] 24 · [raised] 그림자.
 */
@Composable
fun AmenityGrid(
    place: Place,
    labeled: Boolean,
    modifier: Modifier = Modifier,
    hideUnknown: Boolean = false,
    onCookClick: (() -> Unit)? = null,
    tileSize: Dp = 88.dp,
    tileRadius: Dp = 18.dp,
    gap: Dp = 10.dp,
    raised: Boolean = false,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (labeled) 6.dp else gap)) {
        amenityCells(place).filterNot { hideUnknown && it.state == CellState.UNKNOWN }.forEach { cell ->
            val radius = if (labeled) 20.dp else tileRadius
            val size = if (labeled) Modifier.weight(1f).height(112.dp) else Modifier.size(tileSize)
            // 설계서 05: 사진 위 칸은 그림자(0 6 18 · 검정 40%)로 띄운다
            val lift = if (raised) Modifier.shadow(10.dp, RoundedCornerShape(radius), ambientColor = Color.Black, spotColor = Color.Black) else Modifier
            // 취사·불 칸은 [onCookClick]이 있으면 눌러서 이유 시트를 연다(정차 중 02)
            val click = if (cell.isCook && onCookClick != null) {
                Modifier.clip(RoundedCornerShape(radius)).clickable(role = Role.Button, onClickLabel = "취사·불 이유 보기", onClick = onCookClick)
            } else Modifier
            AmenityCellView(cell, labeled, radius, size.then(lift).then(click))
        }
    }
}

@Composable
private fun AmenityCellView(cell: AmenityCell, labeled: Boolean, radius: Dp, modifier: Modifier) {
    val (bg, line, fg) = when (cell.state) {
        CellState.YES -> Triple(BlueCell, Palette.Accent, BlueIcon)
        CellState.NO -> Triple(Palette.Sub, CellLine, Palette.DisabledText)
        CellState.UNKNOWN -> Triple(Palette.Card, Palette.ChipBorder, Palette.SubText)
        CellState.CAUTION -> Triple(Palette.BannerBackground, Signal.YELLOW.lampColor, Palette.BannerText)
        CellState.FORBIDDEN -> Triple(RedCell, Signal.RED.lampColor, RedIcon)
    }
    val dashed = cell.state == CellState.UNKNOWN
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .drawBehind {
                val w = 2.dp.toPx()
                drawRoundRect(
                    color = line,
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(w, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null),
                )
            }
            .semantics { contentDescription = "${cell.name} ${cell.stateText}" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (labeled) 2.dp else 4.dp)) {
            LineIconView(cell.icon, 34.dp, fg, strokeWidth = 1.8f)
            if (labeled) {
                val textColor = if (cell.state == CellState.FORBIDDEN) RedText else fg
                Label(cell.name, 22, FontWeight.Bold, textColor)
                // 이름 아래 상태를 작게(있음 · 없음 · 확인 안 됨 · 조건부 · 금지)
                Label(cell.stateText, 18, FontWeight.Medium, textColor.copy(alpha = 0.8f))
            }
        }
    }
}

/** 화면설계서 17 편의시설 아이콘 보는 법(파란색 있음 · 점선 확인 못 함 · 회색 없음 · 빨간색 금지) */
@Composable
fun AmenityLegend(modifier: Modifier = Modifier) {
    val rows = listOf(
        AmenityCell("화장실", LineIcon.Toilet, CellState.YES, "있음") to "파란색 · 있음",
        AmenityCell("수도", LineIcon.Water, CellState.UNKNOWN, "확인 안 됨") to "점선 · 아직 확인 못 함",
        AmenityCell("전기", LineIcon.Power, CellState.NO, "없음") to "회색 · 없음",
        AmenityCell("취사·불", LineIcon.Fire, CellState.FORBIDDEN, "금지") to "빨간색 · 금지",
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label("편의시설 아이콘 보는 법", 28, FontWeight.ExtraBold)
        rows.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                pair.forEach { (cell, text) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AmenityCellView(cell, labeled = false, 18.dp, Modifier.size(72.dp))
                        Label(text, 24, FontWeight.SemiBold, Palette.SoftText, maxLines = 2)
                    }
                }
            }
        }
    }
}

/** 현장 조건 타일(높이 제한·주차면 좁음·강풍). 확인된 것만 보여 준다. */
fun siteTiles(place: Place): List<Pair<LineIcon, String>> {
    val site = place.site ?: return emptyList()
    return buildList {
        site.heightLimitMeters?.let { add(LineIcon.HeightLimit to "${"%.1f".format(it)} m") }
        if (site.parkingSize == "small") add(LineIcon.Narrow to "좁음")
        if (site.wind == "exposed") add(LineIcon.Wind to "강풍")
    }
}

@Composable
fun SiteConditionTiles(place: Place) {
    val tiles = siteTiles(place)
    if (tiles.isEmpty()) {
        Label("확인된 현장 조건이 없어요", 24, FontWeight.Medium, Palette.SubText)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        tiles.forEach { (icon, text) ->
            Column(
                Modifier
                    .width(132.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Palette.BannerBackground)
                    .padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LineIconView(icon, 52.dp, Palette.BannerText, strokeWidth = 1.8f)
                Label(text, 24, FontWeight.Bold, Palette.BannerText)
            }
        }
    }
}

/** 신뢰도 1~3을 iOS와 같은 방패로(공식 확인 ✓ · 간접 확인 빈 방패 · 비공식 !). 사진 위에서는 [shadow] */
@Composable
fun ConfidenceShield(confidence: Int, size: Dp = 44.dp, shadow: Shadow? = null) {
    val level = TrustLevel.of(confidence)
    TrustShield(level, size, shadow = shadow, modifier = Modifier.semantics { contentDescription = "신뢰도 ${level.title}" })
}

/**
 * 사진 한 장을 [modifier] 영역에 깔고 위는 또렷하게, 아래로 갈수록 흐리게(02 상세 · 07-1, 11-08).
 * 흐린 사진 위에 또렷한 사진을 겹치고, 또렷한 쪽을 [sharpUntil]부터 [fadeEnd]까지 투명하게 지운다(경계선 없음).
 */
@Composable
fun ProgressiveBlurPhoto(url: String, modifier: Modifier, sharpUntil: Float = 0.2f, fadeEnd: Float = 0.55f, blurRadius: Dp = 28.dp) {
    Box(modifier) {
        AsyncImage(url, null, Modifier.matchParentSize().blur(blurRadius), contentScale = ContentScale.Crop)
        AsyncImage(
            url, null,
            Modifier.matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(0f to Color.Black, sharpUntil to Color.Black, fadeEnd to Color.Transparent), blendMode = BlendMode.DstIn)
                },
            contentScale = ContentScale.Crop,
        )
    }
}
