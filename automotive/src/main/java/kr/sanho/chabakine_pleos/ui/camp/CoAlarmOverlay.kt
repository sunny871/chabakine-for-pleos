package kr.sanho.chabakine_pleos.ui.camp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.CoLevel
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.withHaptic

/** 단계별 색 · 문구 · 깜빡임 간격(화면설계서 05-2 · 05-3 · 05-4) */
private class CoStyle(
    val accent: Color,
    val text: Color,
    val panel: Color,
    val onAccent: Color,
    val blinkMs: Int,
    val title: String,
    val sound: String,
    val soundSub: String,
    val autoClear: String,
)

private fun styleOf(level: CoLevel): CoStyle = when (level) {
    CoLevel.CAUTION -> CoStyle(
        Color(0xFFF2B630), Color(0xFFFFD98A), Color(0xFF3A2F12), Color(0xFF1A1405), 2_000,
        "1단계 주의 · 일산화탄소 50 ppm 이상 · 환기하세요",
        "소리 1단계 · 알림음 · 부드럽게 2번", "보통 음량 · 반복 없음 · 알려 주는 정도",
        "수치가 50 ppm 아래로 10초 넘게 유지되면 경보가 저절로 꺼져요",
    )
    CoLevel.WARNING -> CoStyle(
        Color(0xFFFF8A1F), Color(0xFFFFC48A), Color(0xFF3D2108), Color(0xFF1A0F05), 1_200,
        "[2단계 경고]\n일산화탄소 100 ppm 이상 · 창문을 여세요",
        "소리 2단계 · 경고음 · 5초마다 반복", "큰 음량 · 볼륨이 작아도 70% 이상 · 자다가도 알아차리는 정도",
        "수치가 내려가면 1단계로 낮아지고, 50 ppm 아래로 10초 넘게 유지되면 저절로 꺼져요",
    )
    else -> CoStyle(
        Color(0xFFE5484D), Color(0xFFFFB3B5), Color(0xFF3A1416), Color(0xFF1A0508), 700,
        "[3단계 위험]\n일산화탄소 200 ppm 이상 · 즉시 차 밖으로",
        "소리 3단계 · 사이렌 + 음성 \"즉시 차 밖으로\"", "최대 음량 · 끌 때까지 계속 · 바로 움직이게 하는 소리",
        "수치가 내려가면 2단계 → 1단계로 낮아지고, 50 ppm 아래로 10초 넘게 유지되면 저절로 꺼져요",
    )
}

/**
 * 일산화탄소 경보: 화면 가장자리가 단계 색으로 깜빡이고 경보 창을 띄운다(앱 전체 위, 차박 모드 중).
 * 1단계는 위쪽 작은 창이라 05를 계속 쓸 수 있고, 2 · 3단계는 가운데 큰 창이다.
 */
@Composable
fun CoAlarmOverlay(app: AppState, level: CoLevel) {
    val style = styleOf(level)
    Box(Modifier.fillMaxSize()) {
        BlinkingEdge(style)
        if (level == CoLevel.CAUTION) {
            AlarmCard(app, level, style, Modifier.align(Alignment.TopCenter).padding(top = 14.dp).width(780.dp), big = false)
        } else {
            AlarmCard(
                app, level, style,
                Modifier.align(Alignment.Center).width(if (level == CoLevel.DANGER) 1332.dp else 1235.dp)
                    .shadow(48.dp, RoundedCornerShape(56.dp), ambientColor = style.accent, spotColor = style.accent),
                big = true,
            )
        }
    }
}

/** 가장자리 안쪽 빛: 단계 간격으로 옅어졌다 진해진다(누르는 것은 막지 않는다) */
@Composable
private fun BoxScope.BlinkingEdge(style: CoStyle) {
    val alpha by rememberInfiniteTransition(label = "coBlink").animateFloat(
        0.25f, 1f, infiniteRepeatable(tween(style.blinkMs / 2, easing = LinearEasing), RepeatMode.Reverse), label = "coAlpha",
    )
    Box(
        Modifier.matchParentSize().drawBehind {
            // 바깥에서 안쪽으로 겹겹이 옅어지는 테두리
            val layers = 12
            val step = 10.dp.toPx()
            for (i in 0 until layers) {
                val inset = step * i + step / 2
                drawRect(
                    style.accent.copy(alpha = alpha * (1f - i / layers.toFloat()) * 0.6f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(step),
                )
            }
        },
    )
}

@Composable
private fun AlarmCard(app: AppState, level: CoLevel, style: CoStyle, modifier: Modifier, big: Boolean) {
    val shape = RoundedCornerShape(if (big) 56.dp else 36.dp)
    Column(
        modifier.clip(shape).background(style.panel).border(if (big) 6.dp else 3.dp, style.accent, shape)
            .clickable(enabled = false) {}
            .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = if (big) 64.dp else 28.dp, vertical = if (big) 56.dp else 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (big) 40.dp else 18.dp),
    ) {
        // 줄마다 가운데 정렬(Label은 정렬을 받지 않아 줄을 나눠 그린다)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            style.title.lines().forEach { Label(it, if (level == CoLevel.DANGER) 64 else if (big) 60 else 26, FontWeight.ExtraBold, style.text, maxLines = 2) }
        }
        SoundRow(level, style, big)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (big) 40.dp else 16.dp)) {
            val h = if (big) 120.dp else 96.dp
            val size = if (big) 40 else 32
            Row(
                Modifier.weight(1f).height(h).clip(RoundedCornerShape(if (big) 32.dp else 28.dp)).background(style.accent)
                    .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.Confirm, app::ventForCoAlarm)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            ) {
                LineIconView(LineIcon.Fan, (size + 4).dp, style.onAccent, strokeWidth = 2.2f)
                Label("환기 시키기", size, FontWeight.ExtraBold, style.onAccent)
            }
            if (level == CoLevel.DANGER) {
                HoldToDismiss(style, h, size, Modifier.weight(1f), app::dismissCoAlarm)
            } else {
                Row(
                    Modifier.weight(1f).height(h).clip(RoundedCornerShape(if (big) 32.dp else 28.dp))
                        .border(3.dp, style.accent, RoundedCornerShape(if (big) 32.dp else 28.dp))
                        .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.Confirm, app::dismissCoAlarm)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                ) {
                    LineIconView(LineIcon.BellOff, (size + 4).dp, style.text, strokeWidth = 2.2f)
                    Label("알림 끄기", size, FontWeight.ExtraBold, style.text)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LineIconView(LineIcon.TrendDown, 30.dp, style.text, strokeWidth = 2.2f)
            Label(style.autoClear, if (big) 28 else 24, FontWeight.SemiBold, style.text.copy(alpha = 0.9f), maxLines = 2)
        }
    }
}

@Composable
private fun SoundRow(level: CoLevel, style: CoStyle, big: Boolean) {
    // 스피커 물결: 단계만큼 칠한다
    val waves = listOf(LineIcon.SoundWave1, LineIcon.SoundWave2, LineIcon.SoundWave3)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(style.accent.copy(alpha = 0.14f))
            .border(2.dp, style.accent, RoundedCornerShape(24.dp)).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val icon = if (big) 48.dp else 38.dp
        Box {
            LineIconView(LineIcon.Speaker, icon, style.accent, strokeWidth = 2.2f)
            waves.forEachIndexed { i, w -> LineIconView(w, icon, if (i < level.ordinal) style.accent else Color(0xFF4A505A), strokeWidth = 2.2f) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(style.sound, if (big) 30 else 24, FontWeight.ExtraBold, style.text)
            Label(style.soundSub, if (big) 26 else 22, FontWeight.SemiBold, style.text.copy(alpha = 0.85f))
        }
    }
}

/** 3단계 "길게 눌러 끄기": 3초 누르는 동안 아래 막대가 차오르고, 다 차면 끈다. 손을 떼면 처음부터 */
@Composable
private fun HoldToDismiss(style: CoStyle, height: Dp, textSize: Int, modifier: Modifier, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var done by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(done) {
        if (done) {
            // 3초를 다 채웠을 때 한 번 길게 알린다
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDismiss()
        }
    }
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier.height(height).clip(shape).border(3.dp, style.accent, shape)
            .semantics { contentDescription = "3초 길게 눌러 알림 끄기" }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val fill = scope.launch {
                        progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                        done = true
                    }
                    tryAwaitRelease()
                    if (!done) {
                        fill.cancel()
                        progress.snapTo(0f)
                    }
                })
            },
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            LineIconView(LineIcon.BellOff, (textSize + 4).dp, style.text, strokeWidth = 2.2f)
            Column {
                Label("길게 눌러 끄기", textSize, FontWeight.ExtraBold, style.text)
                Label("3초 누르고 있기", 24, FontWeight.Bold, style.text.copy(alpha = 0.85f))
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.value).height(10.dp).background(style.accent))
    }
}

private const val HOLD_MS = 3_000
