package kr.sanho.chabakine_pleos.ui.camp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.ui.theme.AppColors
import kr.sanho.chabakine_pleos.ui.theme.Palette

/*
 * 화면설계서 11-08 "4번 구역을 05에 맞춤": 차박 구역 화면은 보드 전체가 장소 사진(위아래로 어둡게)이고
 * 카드는 반투명 유리판이다. 주간 사본은 밝은 막 + 흰 유리.
 * 야간: 카드 rgba(14,22,18,0.62) · 흰 테두리 14% · 칸 흰색 8% · 보조 버튼 rgba(42,58,50,0.7)
 * 주간: 카드 흰색 74% · 진한 테두리 10% · 칸 진한색 6% · 보조 버튼 rgba(219,228,216,0.85)
 */

val GlassFill: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0x9E0E1612) else Color(0xBDFFFFFF)
val GlassLine: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0x24FFFFFF) else Color(0x1A16241D)
val GlassInner: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0x14FFFFFF) else Color(0x0F16241D)
val GlassSecondary: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0xB32A3A32) else Color(0xD9DBE4D8)
val GlassSecondaryLine: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0x29FFFFFF) else Color(0x1F16241D)

/** 유리 카드: 반투명 판 + 얇은 테두리 */
@Composable
fun Modifier.glass(radius: Dp = 28.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return clip(shape).background(GlassFill).border(1.dp, GlassLine, shape)
}

/** 사진 위 보조 버튼(철수 준비 등) */
@Composable
fun Modifier.glassButton(radius: Dp = 20.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return clip(shape).background(GlassSecondary).border(1.dp, GlassSecondaryLine, shape)
}

/**
 * 보드 전체 장소 사진 + 막. 야간은 위아래로 어둡게(0.55 → 0.35 → 0.8), 주간은 밝은 막(0.66 → 0.48 → 0.86).
 * 사진이 없으면 막 색만.
 */
@Composable
fun CampPhotoBackdrop(photoUrl: String?, modifier: Modifier = Modifier.fillMaxSize()) {
    val dark = Palette.isDark
    Box(modifier.background(if (dark) Color(0xFF0B1310) else Color(0xFFF1F4EF))) {
        photoUrl?.let { AsyncImage(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
        val veil = if (dark) {
            Brush.verticalGradient(0f to Color(0x8C080C0A), 0.4f to Color(0x59080C0A), 1f to Color(0xCC080C0A))
        } else {
            Brush.verticalGradient(0f to Color(0xA8F1F4EF), 0.4f to Color(0x7AF1F4EF), 1f to Color(0xDBF1F4EF))
        }
        Box(Modifier.matchParentSize().background(veil))
    }
}

/**
 * 차박 구역 화면(05-0 · 23 · 10 · 10-1 · 06-A)에서 쓰는 색: 카드 · 칸 · 보조 버튼을 유리로 바꾼 같은 테마.
 * 화면 코드는 그대로 Palette.Card · CardInner · Sub를 쓰고, AppRoot가 사진 바탕과 함께 이 색으로 감싼다.
 */
fun glassColors(base: AppColors): AppColors = if (base.isDark) {
    base.copy(card = Color(0x9E0E1612), cardInner = Color(0x14FFFFFF), sub = Color(0xB32A3A32), railBackground = base.railBackground)
} else {
    base.copy(card = Color(0xBDFFFFFF), cardInner = Color(0x0F16241D), sub = Color(0xD9DBE4D8))
}

/** 05-5 · 05-7처럼 화면 전체를 덮는 차박 창: 장소 사진 바탕 + 유리 색으로 [content]를 그린다 */
@Composable
fun CampGlassLayer(place: kr.sanho.chabakine_pleos.shared.place.Place?, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CampPhotoBackdrop(place?.photoUrls?.firstOrNull())
        kr.sanho.chabakine_pleos.ui.theme.ThemeScope(glassColors(Palette.colors), content)
    }
}
