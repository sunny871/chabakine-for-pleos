package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.ui.layout.layout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.ui.theme.Palette

/** 화면 위쪽 제목 줄(화면설계서 01·02·09·12 등). [trailing]에 방식 선택·상태·음성 버튼을 둔다. 높이 104(10-31 낮춤) */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleSize: Int = 40,
    height: Dp = 104.dp,
    onSubtitleClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(height).padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, titleSize, FontWeight.ExtraBold)
            if (subtitle != null) {
                val m = if (onSubtitleClick != null) Modifier.clickable(role = Role.Button, onClick = onSubtitleClick) else Modifier
                Label(subtitle, 26, FontWeight.Medium, Palette.SubText, m)
            }
        }
        trailing()
    }
}

/** 정차 중(초록) · 주행 중(회색) 상태 알약 */
@Composable
fun StatusPill(parked: Boolean, text: String = if (parked) "정차 중" else "주행 중") {
    Box(
        Modifier
            .height(60.dp)
            .background(if (parked) Palette.ParkedPill else Palette.Sub, RoundedCornerShape(30.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 24, FontWeight.Bold, if (parked) Color.White else Palette.Text) }
}

@Composable
fun VoiceButton(listening: Boolean, size: Dp = 88.dp, onClick: () -> Unit) {
    RoundIcon(
        icon = LineIcon.Mic,
        description = if (listening) "듣는 중" else "음성으로 찾기",
        size = size,
        iconSize = size * 0.46f,
        background = if (listening) Palette.Accent.copy(alpha = 0.7f) else Palette.Accent,
        onClick = onClick,
    )
}

/** 스텔스 · 도킹형 · 루프탑 선택(대표 방식) */
@Composable
fun StyleSegment(selected: CampingStyle, itemHeight: Dp = 80.dp, onSelect: (CampingStyle) -> Unit) {
    Row(
        Modifier.background(Palette.Card, RoundedCornerShape(ButtonRadius)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CampingStyle.entries.forEach { style ->
            val on = style == selected
            Box(
                Modifier
                    .pressShrink()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(SmallRadius))
                    .background(if (on) Palette.Text else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = withHaptic(HapticFeedbackType.SegmentTick) { onSelect(style) })
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Label(style.label, 30, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) Palette.Background else Palette.Text)
            }
        }
    }
}

/** 프리셋 칩(주행 중에도 쓰는 큰 칩, 높이 96) */
/** 지도 위처럼 칩이 떠 있을 때 고르지 않은 칩 바탕(평소엔 투명). 15 지도에서 반투명 바탕으로 깐다 */
val LocalChipBackdrop = androidx.compose.runtime.staticCompositionLocalOf { Color.Transparent }

@Composable
fun PresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: LineIcon? = null,
    height: Dp = 96.dp,
) {
    Row(
        modifier = Modifier
            .pressShrink()
            .height(height)
            .clip(RoundedCornerShape(ChipRadius))
            .background(if (selected) Palette.Text else LocalChipBackdrop.current)
            .border(2.dp, if (selected) Palette.Text else Palette.Gauge, RoundedCornerShape(ChipRadius))
            .clickable(role = Role.Checkbox, onClick = withHaptic(HapticFeedbackType.ContextClick, onClick))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val fg = if (selected) Palette.Background else Palette.Text
        icon?.let { LineIconView(it, 30.dp, fg) }
        Label(text, 26, if (selected) FontWeight.ExtraBold else FontWeight.Bold, fg)
    }
}

/** 테두리만 있는 정보 칩(높이 64) */
@Composable
fun InfoChip(text: String, borderColor: Color = Palette.Gauge) {
    Box(
        Modifier
            .height(64.dp)
            .border(2.dp, borderColor, RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 26) }
}

/** 노란 경고 칩(현장 조건) */
@Composable
fun WarnChip(text: String) {
    Box(
        Modifier
            .height(56.dp)
            .background(Palette.BannerBackground, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 24, FontWeight.Bold, Palette.BannerText) }
}

/**
 * 버튼 모서리(11-11 "너무 둥글둥글" → 완만하게). 큰 버튼 · 보조 버튼 20, 칩 18, 칩 안 작은 칸 14.
 * 동그란 아이콘 버튼(음성 · 즐겨찾기 등)은 Pleos Icon button Circle 스타일이라 그대로 둔다.
 */
val ButtonRadius = 20.dp
val ChipRadius = 18.dp
val SmallRadius = 14.dp

/** 큰 파란 버튼(여기로 안내 등) */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 136.dp,
    textSize: Int = 42,
    icon: LineIcon? = LineIcon.Navigate,
    trailingChevron: Boolean = true,
    enabled: Boolean = true,
    radius: Dp = ButtonRadius,
    /** 사진 위(05)처럼 버튼을 띄울 때: 파란 빛 그림자(0 12 32 · 강조색 55%) */
    glow: Boolean = false,
    /** Pleos Basic button Loading: 아이콘 자리에 도는 표시, 누를 수 없음 */
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(radius)
    val active = enabled && !loading
    Row(
        modifier = modifier
            .pressShrink(active)
            .height(height)
            .then(if (glow && enabled) Modifier.shadow(16.dp, shape, ambientColor = Palette.Accent, spotColor = Palette.Accent) else Modifier)
            .clip(shape)
            .background(if (enabled) Palette.Accent else Palette.CardInner)
            .clickable(enabled = active, role = Role.Button, onClick = withHaptic(HapticFeedbackType.Confirm, onClick))
            // 가운데 정렬 버튼은 좁은 칸에서도 글자가 잘리지 않게 여백을 줄인다
            .padding(horizontal = if (trailingChevron) 40.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp, if (trailingChevron) Alignment.Start else Alignment.CenterHorizontally),
    ) {
        val fg = if (enabled) Palette.OnAccent else Palette.DisabledText
        if (loading) LoadingMark(fg, (textSize - 4).dp) else icon?.let { LineIconView(it, (textSize + 8).dp, fg, strokeWidth = 2.2f) }
        Label(text, textSize, FontWeight.ExtraBold, fg, if (trailingChevron) Modifier.weight(1f) else Modifier)
        if (trailingChevron) LineIconView(LineIcon.ChevronRight, (textSize + 2).dp, fg, strokeWidth = 2.4f)
    }
}

/** 회색 버튼 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 108.dp,
    textSize: Int = 32,
    icon: LineIcon? = null,
    background: Color = Palette.Sub,
    foreground: Color = Palette.Text,
    /** Pleos Basic button Loading: 아이콘 자리에 도는 표시, 누를 수 없음 */
    loading: Boolean = false,
) {
    Row(
        modifier = modifier
            .pressShrink(!loading)
            .height(height)
            .clip(RoundedCornerShape(ButtonRadius))
            .background(background)
            .clickable(enabled = !loading, role = Role.Button, onClick = withHaptic(HapticFeedbackType.ContextClick, onClick))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        if (loading) LoadingMark(foreground, (textSize - 4).dp) else icon?.let { LineIconView(it, (textSize + 10).dp, foreground) }
        Label(text, textSize, FontWeight.Bold, foreground)
    }
}

@Composable
fun RoundIcon(
    icon: LineIcon,
    description: String,
    size: Dp,
    iconSize: Dp,
    background: Color,
    onClick: () -> Unit,
    // 회색(보조) 바탕이면 테마 글자색, 강조색 바탕이면 흰색
    tint: Color = when (background) {
        Palette.Sub -> Palette.Text
        Palette.Accent -> Palette.OnAccent
        else -> Color.White
    },
) {
    Box(
        modifier = Modifier
            .pressShrink()
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.ContextClick, onClick))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { LineIconView(icon, iconSize, tint) }
}

/**
 * Pleos Page Navigation(Depth)의 뒤로가기: 바탕 없는 꺾쇠 하나. 누르는 칸은 96을 지키고,
 * 그림이 머리 줄 왼쪽 여백에 맞도록 양옆을 20씩 겹쳐 놓는다(차지하는 폭 56).
 */
@Composable
fun BackChevron(description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .layout { m, c ->
                val p = m.measure(c)
                val inset = 20.dp.roundToPx()
                layout(p.width - inset * 2, p.height) { p.place(-inset, 0) }
            }
            .pressShrink()
            .size(96.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.ContextClick, onClick))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { LineIconView(LineIcon.ChevronLeft, 48.dp, Palette.Text, strokeWidth = 2.6f) }
}

/** 즐겨찾기 별 버튼 */
@Composable
fun FavoriteButton(saved: Boolean, onClick: () -> Unit, background: Color = Palette.Sub) {
    RoundIcon(
        icon = LineIcon.Star,
        description = if (saved) "즐겨찾기에서 빼기" else "즐겨찾기에 추가",
        size = 96.dp,
        iconSize = 44.dp,
        background = background,
        onClick = onClick,
        tint = if (saved) Palette.Star else Palette.Text,
    )
}

/** 카드 바탕 (테마를 따른다) */
@Composable
fun Modifier.card(radius: Dp = 36.dp, color: Color = Palette.Card): Modifier =
    this.clip(RoundedCornerShape(radius)).background(color)

/**
 * 누를 때 화면 햅틱을 한 번 낸다(Pleos 차 화면 터치 피드백). 시스템 "터치 진동" 설정을 따르고,
 * 진동 장치가 없는 화면에서는 아무 일도 일어나지 않는다. 확정 동작은 Confirm, 그 밖은 ContextClick · SegmentTick.
 */
@Composable
fun withHaptic(type: HapticFeedbackType, onClick: () -> Unit): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return {
        haptic.performHapticFeedback(type)
        onClick()
    }
}

/** 버튼 안 기다림 표시(Pleos Basic button Loading) */
@Composable
fun LoadingMark(color: Color, size: Dp) {
    androidx.compose.material3.CircularProgressIndicator(Modifier.size(size), color = color, strokeWidth = 3.dp, trackColor = color.copy(alpha = 0.2f))
}

/**
 * 켜기 · 끄기 스위치(Pleos Selections › Switch). 켜짐은 강조색 트랙 + 손잡이 오른쪽.
 * 줄 전체가 누르는 곳이라 이건 그림만 그린다.
 */
@Composable
fun PleosSwitch(checked: Boolean) {
    val offset by androidx.compose.animation.core.animateDpAsState(if (checked) 44.dp else 4.dp, androidx.compose.animation.core.tween(150), label = "switch")
    Box(
        Modifier.size(88.dp, 48.dp).clip(RoundedCornerShape(24.dp)).background(if (checked) Palette.Accent else Palette.Gauge),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.padding(start = offset).size(40.dp).clip(CircleShape).background(if (checked) Palette.OnAccent else Palette.SubText))
    }
}
