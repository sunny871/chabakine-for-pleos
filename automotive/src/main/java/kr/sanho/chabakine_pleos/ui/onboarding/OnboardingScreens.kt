package kr.sanho.chabakine_pleos.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.ui.components.TrustLevel
import kr.sanho.chabakine_pleos.ui.components.TrustIcon
import kr.sanho.chabakine_pleos.R
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.components.rememberReduceMotion
import kr.sanho.chabakine_pleos.ui.components.riseIn
import kr.sanho.chabakine_pleos.ui.settings.CheckBox
import kr.sanho.chabakine_pleos.ui.settings.Segment
import kr.sanho.chabakine_pleos.ui.find.AmenityLegend
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.textColor

private const val STEPS = 5

// 사용법(17~19) 움직임. 동작 줄이기면 모두 멈춘 채 한 번에 보인다
private const val DOT_MS = 360
private const val HIGHLIGHT_MS = 300
private const val SIGNAL_FIRST_MS = 1_100L
private const val SIGNAL_STEP_MS = 1_600L
private const val MODES_FIRST_MS = 900L
private const val MODES_STEP_MS = 2_600L
private const val BREATH_MS = 1_200
private const val CHAT_START_MS = 500L
private const val CHAT_STEP_MS = 1_300L
private const val CHAT_HOLD_MS = 3_000L
private const val BUBBLE_IN_MS = 380
private const val BUBBLE_OUT_MS = 260

/** 화면설계서 16~20 첫 실행(정차 중에만). [step] 0: 시작하기 전에 · 1~3: 사용법 · 4: 내 차박 방식 */
@Composable
fun OnboardingScreen(app: AppState, step: Int, replay: Boolean, settings: Settings, modifier: Modifier = Modifier) {
    fun go(to: Int) = app.replace(Route.Onboarding(to, replay))
    fun finish(styles: Set<CampingStyle>, primary: CampingStyle) {
        app.prefs.update { it.copy(styles = styles, primaryStyle = primary, onboardingDone = true) }
        if (replay) app.replace(Route.Settings) else app.replace(Route.Home)
    }
    Column(modifier.fillMaxSize()) {
        OnboardHeader(step) {
            if (step in 1..3) SecondaryButton(if (replay) "닫기" else "건너뛰기", { if (replay) app.replace(Route.Settings) else go(4) }, height = 96.dp, textSize = 28)
        }
        when (step) {
            0 -> NoticeStep(Modifier.weight(1f)) { go(1) }
            1 -> GuideStep(Modifier.weight(1f), step, "신호등으로 한눈에", "차박이 되는 곳인지 신호등으로 알려 줘요. 색과 기호를 함께 보여 줘요.",
                "방식마다 신호가 달라요. 스텔스는 차 안 숙박, 도킹형은 도킹 텐트, 루프탑은 지붕 텐트 규칙을 따로 봐요.", { go(0) }, { go(2) },
                extra = { TrustLegend(it) }) { SignalGuide() }
            // 차박 모드는 전기차에서만: 아니면 "차를 세우면 · 차박 모드" 카드와 차박 모드 문장을 뺀다
            2 -> GuideStep(Modifier.weight(1f), step, "달릴 때와 멈출 때", "차 상태에 맞춰 화면이 알아서 바뀌어요.",
                if (app.campModeAvailable) "차박 모드는 자동으로 켜지 않아요. 마트나 휴게소에서도 주차하니까, 켤지 꼭 물어봐요."
                else "차를 세우면 방식별 가능 여부와 현장 조건, 사진을 자세히 볼 수 있어요.",
                { go(1) }, { go(3) }) { ModesGuide(showCamp = app.campModeAvailable) }
            3 -> GuideStep(Modifier.weight(1f), step, "말로 찾고\n내비로 안내", "손을 떼지 않고 고를 수 있어요. 고른 곳은 Pleos 내비가 길안내해요.",
                "마이크는 음성 버튼을 눌렀을 때만 써요. 위치는 가까운 곳을 고르는 데만 쓰고 서버로 보내지 않아요.",
                { go(2) }, { if (replay) app.replace(Route.Settings) else go(4) }, nextText = if (replay) "다 봤어요" else "다음",
                badge = "시연에서 실제 동작 · Pleos 음성 인식 · 정해진 말을 알아들어요") { VoiceGuide() }
            else -> StyleStep(Modifier.weight(1f), settings, ::finish)
        }
    }
}

@Composable
private fun OnboardHeader(step: Int?, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(104.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Image(painterResource(R.drawable.chabakine_logo), null, Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)))
        Label("차박이네", 32, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        step?.let { Label("${it + 1} / $STEPS", 26, FontWeight.Bold, Palette.SubText) }
        trailing()
    }
}

@Composable
private fun NoticeStep(modifier: Modifier, onNext: () -> Unit) {
    Column(modifier) {
        Row(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Label("시작하기 전에", 72, FontWeight.ExtraBold)
                Label("차박이네는 합법·안전한 차박지를 찾도록 돕는 참고 정보예요.", 32, FontWeight.Medium, Palette.SoftText, maxLines = 3, lineHeight = 1.5.em)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf(
                    Triple(LineIcon.Info, "참고용 정보예요", "공개된 자료와 이용자 제보를 바탕으로 차박 가능 여부를 안내해요. 실제와 다를 수 있어요."),
                    Triple(LineIcon.Alert, "확인 전인 장소가 많아요", "대부분 좌표와 출처만 확인했어요. 전화·현장 확인은 아직이에요."),
                    Triple(LineIcon.Check, "현장 안내가 먼저예요", "출처와 최종 확인일을 보고, 현장 안내판과 관계 기관의 지시를 항상 우선해 주세요."),
                    Triple(LineIcon.Navigate, "운전 중에는 짧고 크게", "달리는 동안에는 신호와 거리만 보여 주고, 자세한 내용은 차를 세우면 펼쳐져요."),
                ).forEach { (icon, title, body) ->
                    Row(Modifier.fillMaxWidth().card(32.dp).padding(horizontal = 36.dp, vertical = 28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        LineIconView(icon, 44.dp, Palette.BannerText)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Label(title, 34, FontWeight.ExtraBold)
                            Label(body, 28, FontWeight.Medium, Palette.SoftText, maxLines = 2)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(184.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            PrimaryButton("확인했어요", onNext, Modifier.width(520.dp), height = 128.dp, textSize = 40, icon = null, trailingChevron = false)
        }
    }
}

@Composable
private fun GuideStep(
    modifier: Modifier,
    step: Int,
    title: String,
    body: String,
    note: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    nextText: String = "다음",
    /** 시연에서 실제로 되는 범위(19) */
    badge: String? = null,
    /** 왼쪽 글 아래에 덧붙일 것(17: 신뢰도 방패 보는 법) */
    extra: (@Composable (Modifier) -> Unit)? = null,
    visual: @Composable (Modifier) -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    Column(modifier) {
        Row(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Label(title, 72, FontWeight.ExtraBold, modifier = Modifier.riseIn(0, reduceMotion), maxLines = 2)
                badge?.let {
                    Box(Modifier.riseIn(1, reduceMotion).background(Palette.SuccessFill, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 10.dp)) {
                        Label(it, 24, FontWeight.Bold, Palette.Success, maxLines = 2)
                    }
                }
                Label(body, 32, FontWeight.Medium, Palette.SoftText, modifier = Modifier.riseIn(2, reduceMotion), maxLines = 3, lineHeight = 1.5.em)
                Label(note, 26, FontWeight.Medium, Palette.SubText, modifier = Modifier.riseIn(3, reduceMotion).padding(top = 16.dp), maxLines = 4, lineHeight = 1.5.em)
                extra?.invoke(Modifier.riseIn(4, reduceMotion).padding(top = 12.dp))
            }
            visual(Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(184.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 지금 쪽 점은 들어올 때 20 → 48로 늘어난다
                val grow = remember { Animatable(if (reduceMotion) 1f else 0f) }
                LaunchedEffect(Unit) { grow.animateTo(1f, tween(DOT_MS, easing = FastOutSlowInEasing)) }
                repeat(STEPS) { i ->
                    val width = if (i == step) 20.dp + 28.dp * grow.value else 20.dp
                    Box(Modifier.width(width).height(20.dp).background(if (i == step) Palette.Text else Palette.ChipBorder, RoundedCornerShape(10.dp)))
                }
            }
            SecondaryButton("이전", onPrev, Modifier.width(240.dp), height = 128.dp, textSize = 36)
            PrimaryButton(nextText, onNext, Modifier.width(420.dp), height = 128.dp, textSize = 40, icon = null, trailingChevron = false)
        }
    }
}

@Composable
private fun SignalGuide(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    // 다 나타난 뒤 허용 → 조건부 → 금지 → 미확인 순으로 한 줄씩 밝혀 짚어 준다
    var lit by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(SIGNAL_FIRST_MS)
        while (true) {
            repeat(4) { lit = it; delay(SIGNAL_STEP_MS) }
        }
    }
    Column(modifier.card().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        listOf(
            Triple(Signal.GREEN, "허용", "관리 주체가 차박·야영을 허용한 곳이에요."),
            Triple(Signal.YELLOW, "조건부", "시즌·시간·시설에 따라 달라요. 이유를 꼭 확인하세요."),
            Triple(Signal.RED, "금지·위험", "금지되었거나 위험이 확인된 곳이에요."),
            Triple(Signal.UNKNOWN, "아직 확인 못 함", "허용이 아니에요. 현장 안내를 먼저 확인하세요."),
        ).forEachIndexed { i, (signal, title, body) ->
            val bg by animateColorAsState(if (i == lit) Palette.CardInner else Palette.Card, tween(HIGHLIGHT_MS), label = "signalRow")
            val lampScale by animateFloatAsState(if (i == lit) 1.08f else 1f, tween(HIGHLIGHT_MS), label = "signalLamp")
            Row(
                Modifier.riseIn(i, reduceMotion, startMs = 200).fillMaxWidth().background(bg, RoundedCornerShape(28.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Box(Modifier.width(212.dp).scale(lampScale), contentAlignment = Alignment.Center) { SignalLamp(signal, 48.dp, showSymbol = true) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label(title, 36, FontWeight.ExtraBold, signal.textColor)
                    Label(body, 28, FontWeight.Medium, Palette.SoftText, maxLines = 2)
                }
            }
        }
        AmenityLegend(Modifier.riseIn(4, reduceMotion, startMs = 200).padding(start = 12.dp, top = 8.dp))
    }
}

/** 17 신뢰도 방패 보는 법(iOS 안내서 '신뢰도'와 같은 문구 · 아이콘) */
@Composable
private fun TrustLegend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label("신뢰도", 26, FontWeight.ExtraBold, Palette.SubText)
        Column(Modifier.fillMaxWidth().card(28.dp).padding(horizontal = 24.dp, vertical = 8.dp)) {
            listOf(
                3 to "지자체나 관리 기관의 공식 자료를 직접 확인했어요.",
                2 to "공식 자료를 간접으로 확인했거나 여러 이용자의 제보가 일치해요.",
                1 to "비공식 정보만 있어요. 방문 전에 꼭 확인하세요.",
            ).forEachIndexed { i, (confidence, detail) ->
                if (i > 0) Box(Modifier.padding(start = 72.dp).fillMaxWidth().height(1.dp).background(Palette.Gauge))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    TrustIcon(confidence, 52.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Label(TrustLevel.of(confidence).title, 26, FontWeight.ExtraBold)
                        Label(detail, 22, FontWeight.Medium, Palette.SubText, maxLines = 2, lineHeight = 1.35.em)
                    }
                }
            }
        }
        Label("신호는 공개된 자료를 근거로 한 안내예요. 현장 안내판과 관계 기관의 지시가 항상 우선해요.", 22, FontWeight.Medium, Palette.MutedText, maxLines = 2, lineHeight = 1.4.em)
    }
}

@Composable
private fun ModesGuide(modifier: Modifier = Modifier, showCamp: Boolean = true) {
    val reduceMotion = rememberReduceMotion()
    // 0 달리는 동안 ↔ 1 차를 세우면: 지금 상태인 카드에 테두리를 두르고 아이콘이 숨 쉰다(차박 모드 카드가 없으면 번갈지 않는다)
    var active by remember { mutableIntStateOf(-1) }
    LaunchedEffect(showCamp) {
        if (reduceMotion || !showCamp) return@LaunchedEffect
        delay(MODES_FIRST_MS)
        while (true) {
            active = 0; delay(MODES_STEP_MS)
            active = 1; delay(MODES_STEP_MS)
        }
    }
    val breath by rememberInfiniteTransition(label = "modes").animateFloat(
        1f, 1.1f, infiniteRepeatable(tween(BREATH_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf(
            Triple("달리는 동안", "찾기 모드", listOf("신호·이름·거리만 크게", "말로 찾고, 요약을 읽어 줘요", "버튼 한 번에 내비 안내")),
            Triple("차를 세우면", "차박 모드", listOf("속도 0 · P단을 알아채요", "방식별 가능 여부와 현장 조건", "체크리스트, 전기차는 배터리")),
        ).take(if (showCamp) 2 else 1).forEachIndexed { i, (title, sub, lines) ->
            val on = i == active
            val accent = if (i == 0) Palette.Selected else Palette.ParkedPill
            val border by animateColorAsState(if (on) accent else Color.Transparent, tween(HIGHLIGHT_MS), label = "modeBorder")
            Column(
                Modifier.riseIn(i, reduceMotion, startMs = 200, stepMs = 180).weight(1f).fillMaxHeight().card()
                    .border(4.dp, border, RoundedCornerShape(36.dp)).padding(40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(Modifier.size(96.dp).scale(if (on) breath else 1f).background(if (i == 0) Palette.Sub else Palette.ParkedPill, CircleShape), contentAlignment = Alignment.Center) {
                    if (i == 0) LineIconView(LineIcon.Navigate, 48.dp, Palette.Text) else Label("P", 48, FontWeight.ExtraBold, Color.White)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label(title, 40, FontWeight.ExtraBold)
                    Label(sub, 30, FontWeight.Bold, Palette.SubText)
                }
                lines.forEachIndexed { j, line ->
                    Label(line, 28, FontWeight.SemiBold, Palette.SoftText, modifier = Modifier.riseIn(i * 3 + j, reduceMotion, startMs = 500), maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun VoiceGuide(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val lines = listOf(
        "\"속초 근처 차박 가능한 곳\"" to true,
        "가까운 세 곳을 찾았어요. 첫째, 스텔스 허용, 3.6 km…" to false,
        "\"첫 번째로 안내해 줘\"" to true,
        "내비로 길안내를 시작했어요" to false,
    )
    var shown by remember { mutableIntStateOf(if (reduceMotion) lines.size else 0) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(CHAT_START_MS)
            for (k in 1..lines.size) { shown = k; delay(CHAT_STEP_MS) }
            delay(CHAT_HOLD_MS)
            shown = 0
        }
    }
    Column(modifier.card().padding(44.dp), verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)) {
        // 자리는 처음부터 잡아 두고(덜컹이지 않게) 보이기만 바꾼다
        lines.forEachIndexed { i, (text, mine) ->
            Bubble(text, mine, visible = i < shown, speaking = !reduceMotion && i == shown - 1)
        }
    }
}

@Composable
private fun Bubble(text: String, mine: Boolean, visible: Boolean = true, speaking: Boolean = false) {
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) BUBBLE_IN_MS else BUBBLE_OUT_MS, easing = FastOutSlowInEasing), label = "bubble")
    val pulse by rememberInfiniteTransition(label = "speak").animateFloat(
        1f, 1.12f, infiniteRepeatable(tween(BREATH_MS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    Row(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = shown
            translationX = (1f - shown) * (if (mine) 48.dp else (-48).dp).toPx()
        },
        horizontalArrangement = Arrangement.spacedBy(18.dp, if (mine) Alignment.End else Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
        val icon: @Composable () -> Unit = {
            Box(Modifier.size(88.dp).scale(if (speaking) pulse else 1f).background(if (mine) Palette.Accent else Palette.Sub, CircleShape), contentAlignment = Alignment.Center) {
                LineIconView(if (mine) LineIcon.Mic else LineIcon.Volume, 44.dp, if (mine) Palette.OnAccent else Color.White)
            }
        }
        if (!mine) icon()
        val shape = if (mine) RoundedCornerShape(32.dp, 32.dp, 8.dp, 32.dp) else RoundedCornerShape(32.dp, 32.dp, 32.dp, 8.dp)
        Box(Modifier.background(if (mine) Palette.Accent else Palette.Sub, shape).padding(horizontal = 32.dp, vertical = 24.dp)) {
            Label(text, if (mine) 32 else 30, if (mine) FontWeight.Bold else FontWeight.SemiBold, if (mine) Palette.OnAccent else Palette.Text, maxLines = 2)
        }
        if (mine) icon()
    }
}

@Composable
private fun StyleStep(modifier: Modifier, settings: Settings, onFinish: (Set<CampingStyle>, CampingStyle) -> Unit) {
    var styles by remember { mutableStateOf(settings.styles) }
    var primary by remember { mutableStateOf(settings.primaryStyle) }
    if (primary !in styles) primary = styles.minByOrNull { it.ordinal } ?: CampingStyle.STEALTH
    Column(modifier) {
        Column(Modifier.padding(start = 56.dp, end = 56.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("어떻게 차박하세요?", 64, FontWeight.ExtraBold)
            Label("방식에 따라 되는 곳이 달라요. 방식에 맞는 신호를 먼저 보여 드릴게요. 설정에서 언제든 바꿀 수 있어요.", 30, FontWeight.Medium, Palette.SoftText, maxLines = 2)
        }
        Column(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 36.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf(
                    Triple(CampingStyle.STEALTH, "텐트 없이 차 안에서 자요", "따르는 규칙: 차량 내 숙박"),
                    Triple(CampingStyle.DOCKING, "트렁크와 텐트를 연결해요", "따르는 규칙: 도킹 텐트 · 주차면 크기"),
                    Triple(CampingStyle.ROOFTOP, "차 지붕 위에 텐트를 펴요", "따르는 규칙: 지붕 텐트 · 높이 제한 확인"),
                ).forEach { (style, body, rule) ->
                    val on = style in styles
                    val shape = RoundedCornerShape(36.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(if (on) Palette.SelectedFill else Palette.Card)
                            .border(4.dp, if (on) Palette.Selected else Palette.Gauge, shape)
                            .clickable(role = Role.Checkbox) {
                                if (!(on && styles.size == 1)) styles = if (on) styles - style else styles + style
                            }
                            .padding(36.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (on && style == primary) {
                            Box(Modifier.height(52.dp).background(Palette.Selected, RoundedCornerShape(26.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                                Label("대표", 24, FontWeight.ExtraBold, Palette.LampBody)
                            }
                        }
                        Label(if (style == CampingStyle.ROOFTOP) "루프탑형" else style.label, 44, FontWeight.ExtraBold)
                        Label(body, 28, FontWeight.Medium, Palette.SoftText, maxLines = 2)
                        Label(rule, 24, FontWeight.SemiBold, Palette.SubText, maxLines = 2)
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CheckBox(on, 44)
                            Label(if (on) "선택함" else "선택 안 함", 28, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) Palette.Text else Palette.SubText)
                        }
                    }
                }
            }
            if (styles.size > 1) {
                Row(Modifier.fillMaxWidth().card(28.dp).padding(horizontal = 32.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Label("대표 방식", 30, FontWeight.ExtraBold)
                        Label("여러 개를 고르면 대표 방식의 신호가 목록과 지도에 먼저 보여요", 24, FontWeight.Medium, Palette.SubText)
                    }
                    Segment(styles.sortedBy { it.ordinal }.map { it to it.label }, primary) { primary = it }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(184.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Label("여러 개를 골라도 돼요. 이 선택은 이 기기에만 저장돼요.", 24, FontWeight.Medium, Palette.MutedText, modifier = Modifier.weight(1f), maxLines = 2)
            SecondaryButton("건너뛰기 (스텔스로 시작)", { onFinish(setOf(CampingStyle.STEALTH), CampingStyle.STEALTH) }, Modifier.width(440.dp), height = 128.dp, textSize = 32)
            PrimaryButton("이 방식으로 시작", { onFinish(styles, primary) }, Modifier.width(440.dp), height = 128.dp, textSize = 40, icon = null, trailingChevron = false)
        }
    }
}

/** 화면설계서 25 첫 실행인데 주행 중: 입력 없이 정차 안내만 하고 5초 뒤 01로 간다 */
/** 화면설계서 20-1 첫 실행 · 주행 중이면 정차 안내. 5초 뒤 01 찾기로 넘어간다 */
@Composable
fun OnboardingDrivingScreen(app: AppState, modifier: Modifier = Modifier) {
    var seconds by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1_000)
            seconds--
        }
        app.replace(Route.Find())
    }
    Column(modifier.fillMaxSize()) {
        OnboardHeader(null) { StatusPill(parked = false) }
        Row(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Box(Modifier.size(128.dp).background(Palette.BannerBackground, CircleShape), contentAlignment = Alignment.Center) {
                    LineIconView(LineIcon.Alert, 64.dp, Palette.BannerText, strokeWidth = 2.4f)
                }
                Label("안전한 곳에 잠깐 세운 뒤\n설정해 주세요", 76, FontWeight.ExtraBold, maxLines = 2)
                Label("내 차박 방식은 차를 세웠을 때만 고를 수 있어요.\n지금은 스텔스 기준으로 주변 차박지를 보여 드릴게요.", 34, FontWeight.Medium, Palette.SoftText, maxLines = 2, lineHeight = 1.5.em)
            }
            Column(Modifier.width(560.dp).card().padding(40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Label("차를 세우면", 28, FontWeight.ExtraBold, Palette.SubText)
                listOf("속도 0 · P단을 확인해요", "첫 실행 설정(16 → 20)을 다시 안내해요", "고른 방식으로 신호가 바뀌어요").forEachIndexed { i, text ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.size(64.dp).background(Palette.CardInner, CircleShape), contentAlignment = Alignment.Center) { Label("${i + 1}", 28, FontWeight.ExtraBold) }
                        Label(text, 30, FontWeight.Bold, maxLines = 2)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Label("${seconds}초 뒤 주변 차박지로 넘어가요 · 입력창 없음", 26, FontWeight.Medium, Palette.MutedText, modifier = Modifier.weight(1f))
            PrimaryButton("주변 차박지 보기", { app.replace(Route.Find()) }, Modifier.width(640.dp), height = 136.dp, textSize = 40, icon = LineIcon.Search, trailingChevron = false)
        }
    }
}
