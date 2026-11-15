package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sanho.chabakine_pleos.ui.theme.Palette

/**
 * 신뢰도 등급. iOS TrustBadge.level(for:)와 같은 이름 · 설명 · 색이다(테마와 관계없이 같은 색).
 * 색만으로 구분하지 않게 방패 안 표시가 등급마다 다르다: 공식 확인 ✓ · 간접 확인 빈 방패 · 비공식 !
 */
enum class TrustLevel(val title: String, val headline: String, val foreground: Color, val background: Color) {
    OFFICIAL("공식 확인", "관리 기관 공식 자료로 직접 확인했어요", Color(0xFF7AE0B3), Color(0xFF1F5C42)),
    INDIRECT("간접 확인", "간접 자료와 이용자 제보로 확인했어요", Color(0xFFFFD66B), Color(0xFF5C4A12)),
    UNOFFICIAL("비공식", "비공식 정보뿐이라 꼭 확인이 필요해요", Color(0xFFFF9999), Color(0xFF4A2929)),
    ;

    companion object {
        /** 장소 데이터의 confidence(1~3) → 등급. 3 이상 공식, 2 간접, 그 밖은 비공식 */
        fun of(confidence: Int): TrustLevel = when {
            confidence >= 3 -> OFFICIAL
            confidence == 2 -> INDIRECT
            else -> UNOFFICIAL
        }
    }
}

/**
 * 신뢰도 아이콘: 등급 색 둥근 칸 안에 채운 방패(iOS 안내서의 30 pt 칸과 같은 비율).
 * [hero]면 장소 상세 머리의 큰 칸처럼 바탕이 비스듬히 옅어지고, 테두리와 은은한 빛이 붙는다(iOS 58 pt 칸).
 * 읽어 주기 라벨은 "신뢰도 공식 확인"처럼 등급 이름이다.
 */
@Composable
fun TrustIcon(confidence: Int, size: Dp = 30.dp, modifier: Modifier = Modifier, hero: Boolean = false) {
    val level = TrustLevel.of(confidence)
    // iOS: 30 칸 · 모서리 8 · 기호 16 / 58 칸 · 모서리 18 · 기호 28
    val shape = RoundedCornerShape(size * if (hero) 18f / 58f else 8f / 30f)
    val glyph = size * if (hero) 28f / 58f else 16f / 30f
    val tile = if (hero) {
        Modifier
            .shadow(size * 10f / 58f, shape, ambientColor = level.foreground.copy(alpha = 0.28f), spotColor = level.foreground.copy(alpha = 0.28f))
            .background(Brush.linearGradient(listOf(level.background, level.background.copy(alpha = 0.55f))), shape)
            .border(1.dp, level.foreground.copy(alpha = 0.35f), shape)
    } else {
        Modifier.background(level.background, shape)
    }
    Box(
        modifier.size(size).then(tile).semantics { contentDescription = "신뢰도 ${level.title}" },
        contentAlignment = Alignment.Center,
    ) {
        TrustShield(level, glyph, level.foreground)
    }
}

/**
 * 칸 없이 방패만(채운 방패 + 안쪽 표시를 뚫어 바탕이 보이게). iOS SF Symbols 모양을 24 격자로 옮겼다.
 * [shadow]가 있으면 사진 위에서도 보이게 방패 둘레에 그림자를 그린다(자리 크기는 [size] 그대로).
 */
@Composable
fun TrustShield(level: TrustLevel, size: Dp, color: Color = level.foreground, modifier: Modifier = Modifier, shadow: Shadow? = null) {
    // 그림자가 잘리지 않게 그리는 칸만 넓히고 자리는 [size]로 둔다
    val pad = if (shadow != null) SHADOW_ROOM else 0.dp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // 표시를 뚫으려면 방패만 따로 합성해야 한다(BlendMode.Clear가 바탕까지 지우지 않게)
        Canvas(Modifier.requiredSize(size + pad * 2).graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            val p = pad.toPx()
            val s = (this.size.minDimension - p * 2) / 24f
            translate(p, p) {
                scale(s, pivot = Offset.Zero) {
                    shadow?.let { sh ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            this.color = sh.color.toArgb()
                            setShadowLayer(sh.blurRadius / s, sh.offset.x / s, sh.offset.y / s, sh.color.toArgb())
                        }
                        drawIntoCanvas { it.nativeCanvas.drawPath(SHIELD.asAndroidPath(), paint) }
                    }
                    drawShield(level, color)
                }
            }
        }
    }
}

private fun DrawScope.drawShield(level: TrustLevel, color: Color) {
    drawPath(SHIELD, color)
    when (level) {
        TrustLevel.OFFICIAL -> drawPath(CHECK, Color.Black, style = MARK_STROKE, blendMode = BlendMode.Clear)
        TrustLevel.INDIRECT -> Unit
        TrustLevel.UNOFFICIAL -> {
            drawLine(Color.Black, Offset(12f, 7.6f), Offset(12f, 12.6f), strokeWidth = 2.5f, cap = StrokeCap.Round, blendMode = BlendMode.Clear)
            drawCircle(Color.Black, radius = 1.45f, center = Offset(12f, 16.2f), blendMode = BlendMode.Clear)
        }
    }
}

private val SHADOW_ROOM = 8.dp

private val SHIELD = PathParser().parsePathString(
    "M12 2.2L19.4 4.9Q20.4 5.3 20.4 6.4V11.4Q20.4 17.4 12.7 21.5Q12 21.9 11.3 21.5Q3.6 17.4 3.6 11.4V6.4Q3.6 5.3 4.6 4.9Z",
).toPath()
private val CHECK = PathParser().parsePathString("M8.1 11.9L10.9 14.7L15.9 8.9").toPath()
private val MARK_STROKE = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Preview(name = "신뢰도 아이콘", widthDp = 520, heightDp = 300, backgroundColor = 0xFF121417, showBackground = true)
@Composable
private fun TrustIconPreview() {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        listOf(3, 2, 1).forEach { c ->
            val level = TrustLevel.of(c)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TrustIcon(c, 30.dp)
                TrustIcon(c, 58.dp, hero = true)
                TrustShield(level, 40.dp)
                Column {
                    Label(level.title, 24, FontWeight.ExtraBold, level.foreground)
                    Label(level.headline, 20, FontWeight.Medium, Palette.SoftText)
                }
            }
        }
    }
}
