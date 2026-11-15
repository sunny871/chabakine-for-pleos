package kr.sanho.chabakine_pleos.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sanho.chabakine_pleos.R
import kr.sanho.chabakine_pleos.ui.theme.Palette

/** 11-08: 예전 '찾기'를 지도(15)와 목록(01 · 02)으로 나눴다 */
enum class RailItem(val title: String) {
    HOME("홈"),
    MAP("지도"),
    FIND("목록"),
    FAVORITES("즐겨찾기"),
    CAMP("차박 모드"),
    SETTINGS("설정"),
}

/**
 * 화면설계서 11 왼쪽 메뉴 줄(11-08 숲 색감 · 떠 있는 바). 132 자리 안에 120 폭 바가 위아래 12씩 떠 있다.
 * 야간은 짙은 숲 그라데이션, 주간은 흰색 → 세이지. 선택은 포인트색(야간 크림 · 주간 짙은 숲). 분할 840에서는 숨김.
 */
@Composable
fun Rail(
    active: RailItem?,
    onSelect: (RailItem) -> Unit,
    modifier: Modifier = Modifier,
    /** 주행 중이면 차박 모드를 흐리게 "정차 후"로 두고 누를 수 없게 한다(N1). */
    campLocked: Boolean = false,
    /** 로고를 짧은 시간에 [SECRET_TAPS]번 누르면(숨은 "만든 사람" 화면) */
    onLogoSecret: () -> Unit = {},
    /** 전기차가 아니면 "차박 모드"를 메뉴 줄에서 뺀다 */
    showCamp: Boolean = true,
) {
    val dark = Palette.isDark
    val shape = RoundedCornerShape(36.dp)
    // 로고 연타: 마지막 [SECRET_WINDOW_MS] 안의 누름만 센다
    val taps = remember { ArrayDeque<Long>() }
    Box(modifier.width(132.dp).fillMaxHeight()) {
        Column(
            Modifier
                .padding(start = 8.dp, top = 12.dp, end = 4.dp, bottom = 12.dp)
                .width(120.dp)
                .fillMaxHeight()
                .shadow(if (dark) 18.dp else 12.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(if (dark) DarkBar else DayBar)
                .border(1.5.dp, if (dark) Color(0x0FFFFFFF) else Color(0x1416241D), shape)
                .padding(top = 22.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 차박이네 로고(iOS 앱 아이콘과 같은 그림)
            Image(
                painterResource(R.drawable.chabakine_logo), "차박이네",
                Modifier.size(80.dp).clip(RoundedCornerShape(22.dp)).clickable(interactionSource = null, indication = null) {
                    val now = SystemClock.uptimeMillis()
                    taps.addLast(now)
                    while (taps.isNotEmpty() && now - taps.first() > SECRET_WINDOW_MS) taps.removeFirst()
                    if (taps.size >= SECRET_TAPS) {
                        taps.clear()
                        onLogoSecret()
                    }
                },
            )
            Label("차박이네", 24, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
            RailItem.entries.filter { it != RailItem.SETTINGS && (showCamp || it != RailItem.CAMP) }.forEach {
                RailButton(it, it == active, it == RailItem.CAMP && campLocked, onSelect)
            }
            Spacer(Modifier.weight(1f))
            ForestScene(dark, Modifier.fillMaxWidth().height(110.dp))
            RailButton(RailItem.SETTINGS, active == RailItem.SETTINGS, false, onSelect, height = 100)
        }
    }
}

private val RailItem.filledIcon: ImageVector
    get() = when (this) {
        RailItem.HOME -> Icons.Rounded.Home
        RailItem.MAP -> MapFilled
        RailItem.FIND -> ListFilled
        RailItem.FAVORITES -> Icons.Rounded.Star
        RailItem.CAMP -> TentFilled
        RailItem.SETTINGS -> Icons.Rounded.Settings
    }

@Composable
private fun RailButton(item: RailItem, selected: Boolean, locked: Boolean, onSelect: (RailItem) -> Unit, height: Int = 104) {
    val dark = Palette.isDark
    val idle = if (dark) Color(0xFFC4CFC4) else Color(0xFF46564B)
    val fg = if (selected) Palette.OnAccent else idle
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .pressShrink(!locked)
            .width(108.dp)
            .height(if (locked) 130.dp else height.dp)
            .then(if (selected) Modifier.shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
            .clip(shape)
            .background(if (selected) Palette.Accent else Color.Transparent)
            .clickable(enabled = !locked, role = Role.Tab, onClick = withHaptic(HapticFeedbackType.SegmentTick) { onSelect(item) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        val alpha = if (locked) 0.45f else 1f
        Icon(item.filledIcon, null, Modifier.size(40.dp), tint = fg.copy(alpha = alpha))
        Label(item.title, 24, if (selected) FontWeight.ExtraBold else FontWeight.Bold, fg.copy(alpha = alpha))
        if (locked) Label("정차 후", 24, FontWeight.Medium, fg.copy(alpha = alpha))
    }
}

/** 메뉴 바 아래 숲 · 텐트 그림: 야간은 텐트 불빛, 주간은 해(설계서 11 RailForest) */
@Composable
private fun ForestScene(dark: Boolean, modifier: Modifier) {
    val mtnFar = if (dark) Color(0xFF33493E) else Color(0xFFC9DBC4)
    val mtnNear = if (dark) Color(0xFF26392F) else Color(0xFFA9C6A2)
    val tree = if (dark) Color(0xFF14201A) else Color(0xFF4F6E57)
    Canvas(modifier) {
        // 설계서 그림 좌표(120 × 150, 위 80 잘림)를 이 칸 크기에 맞춘다
        val sx = size.width / 120f
        val sy = size.height / 150f
        fun p(x: Float, y: Float) = Offset(x * sx, (y - 80f) * sy)
        fun poly(color: Color, vararg pts: Pair<Float, Float>) {
            val path = Path().apply {
                pts.forEachIndexed { i, (x, y) -> val o = p(x, y); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                close()
            }
            drawPath(path, color)
        }
        drawCircle(if (dark) Color(0xFFF4EFE6) else Color(0xFFFFD98A), radius = (if (dark) 2f else 10f) * sx, center = p(94f, 104f))
        poly(mtnFar, 0f to 150f, 38f to 96f, 64f to 128f, 92f to 88f, 120f to 124f, 120f to 230f, 0f to 230f)
        poly(mtnNear, 0f to 176f, 30f to 140f, 58f to 166f, 90f to 132f, 120f to 160f, 120f to 230f, 0f to 230f)
        poly(tree, 18f to 196f, 30f to 150f, 42f to 196f)
        poly(tree, 94f to 200f, 106f to 146f, 118f to 200f)
        poly(tree, 6f to 206f, 14f to 176f, 22f to 206f)
        // 텐트 불빛
        drawCircle(
            Brush.radialGradient(listOf(Color(0x8CFFBE6E), Color(0x00FFBE6E)), center = p(60f, 190f), radius = 38f * sx),
            radius = 38f * sx, center = p(60f, 190f), alpha = if (dark) 1f else 0.35f,
        )
        poly(Color(0xFFF0A64E), 42f to 208f, 60f to 176f, 78f to 208f)
        poly(Color(0xFF7A3F12), 56f to 208f, 60f to 192f, 64f to 208f)
        drawRect(tree, topLeft = p(0f, 208f), size = androidx.compose.ui.geometry.Size(size.width, 22f * sy))
    }
}

private val DarkBar = Brush.verticalGradient(0f to Color(0xFF2C3F35), 0.45f to Color(0xFF22332B), 1f to Color(0xFF1A2822))
private val DayBar = Brush.verticalGradient(0f to Color(0xFFFFFFFF), 0.5f to Color(0xFFF1F5EF), 1f to Color(0xFFE2EBDF))

private val MapFilled: ImageVector by lazy {
    filledIcon("MapFilled", "M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z")
}
private val ListFilled: ImageVector by lazy {
    filledIcon(
        "ListFilled",
        "M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.68-1.5 1.5s.68 1.5 1.5 1.5 1.5-.68 1.5-1.5-.67-1.5-1.5-1.5zM8 19h12c.55 0 1-.45 1-1s-.45-1-1-1H8c-.55 0-1 .45-1 1s.45 1 1 1zm0-6h12c.55 0 1-.45 1-1s-.45-1-1-1H8c-.55 0-1 .45-1 1s.45 1 1 1zM7 6c0 .55.45 1 1 1h12c.55 0 1-.45 1-1s-.45-1-1-1H8c-.55 0-1 .45-1 1z",
    )
}

/** 숨은 화면: 로고를 이 시간(ms) 안에 이만큼 누르면 연다 */
private const val SECRET_TAPS = 5
private const val SECRET_WINDOW_MS = 2_000L
