package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

/** 설계서의 선 아이콘(24×24 viewBox, stroke)을 그대로 그린다. */
enum class LineIcon(vararg val paths: String) {
    Mic("M12 3a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z", "M5 11a7 7 0 0 0 14 0", "M12 18v3"),
    Star("M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"),
    ChevronLeft("M15 5l-7 7 7 7"),
    ChevronRight("M9 5l7 7-7 7"),
    ChevronDown("M5 9l7 7 7-7"),
    ChevronUp("M5 15l7-7 7 7"),
    ArrowRight("M4 12h14M13 6l6 6-6 6"),
    Navigate("M3 11l18-8-8 18-2-8z"),
    Home("M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"),
    Search("M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M20 20l-3.5-3.5"),
    Tent("M3 20L12 5l9 15z", "M12 20l-3-5h6z"),
    Settings(
        "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
        "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1",
    ),
    Check("M5 12l5 5L20 7"),
    Alert("M12 7v6", "M12 17h.01"),
    Cross("M7 7l10 10M17 7L7 17"),
    Question("M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6", "M12 17h.01"),
    Volume("M4 9v6h4l5 4V5L8 9z", "M16.5 8.5a5 5 0 0 1 0 7"),
    ListView("M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"),
    MapView("M9 4L3 6v14l6-2 6 2 6-2V4l-6 2z", "M9 4v14M15 6v14"),
    Plus("M12 5v14M5 12h14"),
    Minus("M5 12h14"),
    Locate("M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M12 2v3M12 19v3M2 12h3M19 12h3"),
    Info("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 8v5", "M12 16v.01"),
    Sliders("M4 6h16M7 12h10M10 18h4"),
    Trash("M4 7h16", "M10 11v6M14 11v6", "M6 7l1 13h10l1-13", "M9 7V4h6v3"),
    Phone("M7 2h10a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z", "M11 18h2"),
    Moon("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"),
    Sun("M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"),
    Bulb("M9 18h6M10 21h4", "M12 3a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 3z"),
    Battery("M4 7h13a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z", "M22 11v2", "M5 10h3v4H5z"),
    Fan("M12 12m-2 0a2 2 0 1 0 4 0a2 2 0 1 0-4 0", "M12 10c0-4 1-7 4-7s2 5-2 7", "M14 12c4 0 7 1 7 4s-5 2-7-2", "M12 14c0 4-1 7-4 7s-2-5 2-7", "M10 12c-4 0-7-1-7-4s5-2 7 2"),
    Seat("M7 3h4l1 9h6l2 9", "M6 12h6", "M9 21H6"),
    Window("M4 4h16v16H4z", "M4 13h16"),
    // 23 환기 · 창문(11-10): 창문 + 바람 · 종(알림만) · 시계(간격)
    VentWindow("M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M3 9h18", "M7 6h3M14 6h3", "M7 13c2-1.5 4 1.5 6 0s4 1.5 4 0", "M7 17c2-1.5 4 1.5 6 0s4 1.5 4 0"),
    Bell("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0"),
    Clock("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 7v5l3 2"),
    Car("M3 17V13l2-5h14l2 5v4z", "M6 8l1-3h10l1 3"),
    // 05 차박 모드: 종료 · 근처 축제 · 실내 공기(BLE)
    Exit("M12 3v8", "M6.3 6.8a8 8 0 1 0 11.4 0"),
    Flag("M5 21V4M5 4h11l-2 4 2 4H5"),
    Bluetooth("M7 7l10 10-5 5V2l5 5L7 17"),
    Thermometer("M14 14.5V5a2 2 0 0 0-4 0v9.5a4 4 0 1 0 4 0z"),
    // 일산화탄소 경보(05-2 ~ 05-4): 끄기 · 저절로 꺼짐 · 소리 단계(스피커와 물결 셋)
    BellOff("M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8", "M10.3 20a2 2 0 0 0 3.4 0", "M3 3l18 18"),
    TrendDown("M4 7l6 6 4-4 6 6", "M20 10v5h-5"),
    Speaker("M3 9.5h3.5L11 5.5v13l-4.5-4H3z"),
    SoundWave1("M15.5 9.5a3.5 3.5 0 0 1 0 5"),
    SoundWave2("M18 7a7 7 0 0 1 0 10"),
    SoundWave3("M20.5 4.5a10.5 10.5 0 0 1 0 15"),
    WifiOff("M2 8.8a15 15 0 0 1 4.2-2.6M22 8.8a15 15 0 0 0-11-3.7M5 12.5a10 10 0 0 1 3.5-2M19 12.5a10 10 0 0 0-4-2.2M8.5 16a5 5 0 0 1 7 0M12 20h.01M3 3l18 18"),
    Package("M21 8l-9-5-9 5 9 5z", "M3 8v8l9 5 9-5V8"),
    Refresh("M21 12a9 9 0 1 1-3-6.7", "M21 4v5h-5"),
    // 편의시설·현장 조건(화면설계서 02·17)
    Toilet("M7 3h6v7H7z", "M5 10h14a5 5 0 0 1-5 5h-4a5 5 0 0 1-5-5z", "M9 15l-1 6h8l-1-6"),
    Water("M12 3c3 4.5 6 7.5 6 11a6 6 0 0 1-12 0c0-3.5 3-6.5 6-11z"),
    Power("M13 2L4 14h7l-1 8 9-12h-7z"),
    Shower("M5 21V8a4 4 0 0 1 8 0", "M9 8h8", "M11 12v.01M14 12v.01M17 12v.01M12.5 15v.01M15.5 15v.01M14 18v.01"),
    Fire("M12 3c3 4 6 6 6 10a6 6 0 0 1-12 0c0-3 2-5 3-7 1 2 3 3 3 3z", "M12 14c1 1.5 2 2 2 3.5a2 2 0 0 1-4 0c0-1 .5-2 2-3.5z"),
    Shield("M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"),
    Pin("M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z", "M12 8a2 2 0 1 0 0 4a2 2 0 1 0 0-4z"),
    HeightLimit(
        "M3 3h18", "M8 5v4M6.5 7.5L8 9l1.5-1.5M16 5v4M14.5 7.5L16 9l1.5-1.5",
        "M5 14h14a2 2 0 0 1 2 2v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1a2 2 0 0 1 2-2z", "M6 14l2-3h8l2 3",
        "M7.5 18a1.5 1.5 0 1 0 0 3a1.5 1.5 0 1 0 0-3z", "M16.5 18a1.5 1.5 0 1 0 0 3a1.5 1.5 0 1 0 0-3z",
    ),
    Narrow("M3.5 3v18M20.5 3v18", "M11 5h2a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3h-2a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M9.5 9h5M9.5 16h5"),
    Wind(
        "M3 6h8a2 2 0 1 0-2-2", "M3 10h12a2 2 0 1 1-2 2", "M6 15h12a2 2 0 0 1 0 4H6a2 2 0 0 1 0-4z", "M7 15l1.5-3h7L17 15",
        "M8 18.1a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8z", "M16 18.1a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8z",
    ),
}

@Composable
fun LineIconView(
    icon: LineIcon,
    size: Dp,
    color: Color,
    strokeWidth: Float = 2f,
    modifier: Modifier = Modifier,
    /** 사진 위 아이콘처럼 바탕이 밝을 수 있을 때 [PhotoTextShadow] */
    shadow: Shadow? = null,
) {
    val paths = remember(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, pivot = Offset.Zero) {
            // 그림자: 같은 선을 그림자 색으로 그리며 번지게 하고, 그 위에 원래 선을 덮는다(크기가 24 격자로 줄었으니 번짐도 나눈다)
            shadow?.let { sh ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    this.color = sh.color.toArgb()
                    setShadowLayer(sh.blurRadius / s, sh.offset.x / s, sh.offset.y / s, sh.color.toArgb())
                }
                drawIntoCanvas { canvas -> paths.forEach { canvas.nativeCanvas.drawPath(it.asAndroidPath(), paint) } }
            }
            paths.forEach {
                drawPath(it, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
