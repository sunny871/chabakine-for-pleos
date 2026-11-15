package kr.sanho.chabakine_pleos.ui.credits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.launch
import kr.sanho.chabakine_pleos.BuildConfig
import kr.sanho.chabakine_pleos.ui.components.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val SkyTop = Color(0xFF081714)
private val SkyMid = Color(0xFF123829)
private val SkyBottom = Color(0xFF050D0D)
private val Mint = Color(0xFF7FE0A8)
private val MintLight = Color(0xFFC8F7DA)
private val LampGreen = Color(0xFF2FB36B)
private val LampOrange = Color(0xFFF2A630)
private val LampRed = Color(0xFFE5484D)
private val LampOff = Color(0xFF1C2A27)

/**
 * 숨은 "만든 사람" 화면(이스터에그). 메뉴 줄 로고를 연타하면 앱 전체를 덮어 열린다.
 * 정차(P단) 중에만 열리고 출발하면 [AppState]가 닫는다. 화면 아무 곳이나 누르면 닫힌다.
 * 소리·진동은 쓰지 않는다. 동작 줄이기(애니메이션 배율 0)면 한 번에 모두 보이고 신호등은 초록에서 멈춘다.
 */
@Composable
fun CreditsScreen(onClose: () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    // 나타나는 순서: 0 왼쪽(신호등·차박이네) → 1 "만든 사람" → 2 첫째 이름 → 3 둘째 이름 → 4 응원 문구
    val steps = remember { List(5) { Animatable(if (reduceMotion) 1f else 0f) } }
    // 다 나타난 뒤에야 "화면을 누르면 닫혀요"가 보이고 눌러서 닫을 수 있다(연타하던 손가락에 바로 닫히지 않게)
    var ready by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(350)
        val jobs = steps.mapIndexed { i, a ->
            if (i > 0) delay(700)
            launch {
                a.animateTo(1f, if (i == 2 || i == 3) spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow) else tween(600))
            }
        }
        jobs.joinAll()
        ready = true
    }
    val hintAlpha by animateFloatAsState(if (ready) 0.28f else 0f, tween(if (reduceMotion) 0 else 600), label = "hint")
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)))
            .clickable(enabled = ready, interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "닫기", onClick = onClose)
            .semantics { contentDescription = "만든 사람 · Ji Jun Ho, Chun Ho San · 화면을 누르면 닫혀요" },
    ) {
        StarField(reduceMotion)
        ShootingStars(reduceMotion)
        Row(Modifier.fillMaxSize()) {
            // 왼쪽: 신호등 · 차박이네 · 버전
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.weight(1f))
                Column(Modifier.rise(steps[0].value, 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CyclingLamp(reduceMotion)
                    Spacer(Modifier.height(36.dp))
                    Text("차박이네", color = Color.White.copy(alpha = 0.78f), fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.em)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "버전 ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.38f), fontSize = 24.sp,
                    modifier = Modifier.rise(steps[0].value, 0.dp),
                )
            }
            // 오른쪽: 만든 사람 · 이름 두 줄 · 응원 문구 · 닫기 안내
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    "만든 사람", color = Mint, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.em,
                    modifier = Modifier.rise(steps[1].value, 20.dp),
                )
                Spacer(Modifier.height(28.dp))
                CreatorName("Ji Jun Ho", steps[2].value)
                Spacer(Modifier.height(8.dp))
                CreatorName("Chun Ho San", steps[3].value)
                Spacer(Modifier.height(40.dp))
                Text(
                    "함께 떠나는 모든 차박을 응원해요", color = Color.White.copy(alpha = 0.92f), fontSize = 32.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.rise(steps[4].value, 20.dp),
                )
                Spacer(Modifier.weight(1f))
                Text("화면을 누르면 닫혀요", color = Color.White.copy(alpha = hintAlpha), fontSize = 24.sp)
            }
        }
    }
}

/** 아래에서 [from]만큼 떠오르며 나타난다 */
private fun Modifier.rise(progress: Float, from: Dp): Modifier = graphicsLayer {
    alpha = progress.coerceIn(0f, 1f)
    translationY = (1f - progress) * from.toPx()
}

/** 이름: 흰색 → 연초록 그라데이션 글자 + 은은한 초록 글로우. 흐릿하게 아래에서 올라오며 선명해진다 */
@Composable
private fun CreatorName(name: String, progress: Float) {
    val blur = ((1f - progress) * 14f).coerceAtLeast(0f)
    Text(
        name,
        modifier = Modifier.rise(progress, 40.dp).blur(blur.dp),
        style = TextStyle(
            brush = Brush.verticalGradient(listOf(Color.White, MintLight)),
            fontSize = 84.sp,
            fontWeight = FontWeight.ExtraBold,
            shadow = Shadow(Mint.copy(alpha = 0.45f), Offset.Zero, blurRadius = 36f),
        ),
    )
}

/** 세로 신호등. 초록 → 주황 → 빨강을 1.1초마다 부드럽게 바꾼다(동작 줄이기면 초록에서 멈춤) */
@Composable
private fun CyclingLamp(reduceMotion: Boolean) {
    var lit by remember { mutableIntStateOf(0) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(1_100)
            lit = (lit + 1) % 3
        }
    }
    Column(
        Modifier
            .shadow(24.dp, RoundedCornerShape(72.dp), ambientColor = Mint, spotColor = Mint)
            .background(Color(0xFF040908), RoundedCornerShape(72.dp))
            .padding(horizontal = 26.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        listOf(LampGreen, LampOrange, LampRed).forEachIndexed { i, on ->
            val color by animateColorAsState(if (i == lit) on else LampOff, tween(if (reduceMotion) 0 else 450), label = "lamp$i")
            Box(
                Modifier
                    .size(92.dp)
                    .then(if (i == lit) Modifier.shadow(28.dp, CircleShape, ambientColor = on, spotColor = on) else Modifier)
                    .background(color, CircleShape),
            )
        }
    }
}

/** 밤하늘 별. 자리는 고정이고 밝기만 천천히 깜빡인다 */
@Composable
private fun StarField(reduceMotion: Boolean) {
    val stars = remember {
        val r = Random(20261031)
        List(90) { Star(r.nextFloat(), r.nextFloat() * 0.85f, 0.8f + r.nextFloat() * 1.8f, r.nextFloat() * 2f * PI.toFloat(), 0.5f + r.nextFloat()) }
    }
    val t by if (reduceMotion) remember { mutableFloatStateOfZero() } else rememberInfiniteTransition(label = "stars").animateFloat(
        0f, 2f * PI.toFloat(), infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart), label = "twinkle",
    )
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        stars.forEach { s ->
            val a = 0.35f + 0.45f * (0.5f + 0.5f * sin(t * s.speed + s.phase))
            drawCircle(Color.White.copy(alpha = a), radius = with(density) { s.radius.dp.toPx() }, center = Offset(s.x * size.width, s.y * size.height))
        }
    }
}

/**
 * 별똥별: 위쪽 하늘에서 왼쪽 아래로 가끔 스쳐 지나간다. 세 개가 서로 다른 주기로 돌아 겹치지 않게 보인다.
 * 동작 줄이기면 그리지 않는다.
 */
@Composable
private fun ShootingStars(reduceMotion: Boolean) {
    if (reduceMotion) return
    val transition = rememberInfiniteTransition(label = "meteors")
    val meteors = remember {
        listOf(
            Meteor(startX = 0.78f, startY = 0.06f, periodMs = 7_000, delayMs = 1_800),
            Meteor(startX = 0.46f, startY = 0.14f, periodMs = 9_500, delayMs = 5_200),
            Meteor(startX = 0.92f, startY = 0.22f, periodMs = 12_000, delayMs = 8_400),
        )
    }
    val progress = meteors.map { m ->
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(m.periodMs, easing = LinearEasing), RepeatMode.Restart, initialStartOffset = StartOffset(m.periodMs - m.delayMs % m.periodMs)),
            label = "meteor",
        )
    }
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        meteors.forEachIndexed { i, m ->
            // 한 주기 중 앞 [METEOR_ACTIVE]만 날아가고 나머지는 쉰다
            val p = progress[i].value
            if (p > METEOR_ACTIVE) return@forEachIndexed
            val q = p / METEOR_ACTIVE
            val travel = Offset(-size.width * 0.28f, size.height * 0.30f)
            val head = Offset(m.startX * size.width, m.startY * size.height) + travel * q
            val tail = head - travel * 0.22f
            val alpha = sin(q * PI.toFloat())
            drawLine(
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f * alpha), Color.Transparent), start = head, end = tail),
                start = head, end = tail, strokeWidth = with(density) { 2.5.dp.toPx() }, cap = StrokeCap.Round,
            )
            drawCircle(Color.White.copy(alpha = alpha), radius = with(density) { 2.2.dp.toPx() }, center = head)
        }
    }
}

private class Meteor(val startX: Float, val startY: Float, val periodMs: Int, val delayMs: Int)

/** 별똥별이 한 주기 중 날아가는 비율(나머지는 쉼) */
private const val METEOR_ACTIVE = 0.12f

private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

private fun mutableFloatStateOfZero() = androidx.compose.runtime.mutableFloatStateOf(0f)
