package kr.sanho.chabakine_pleos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kr.sanho.chabakine_pleos.data.ThemePref
import kr.sanho.chabakine_pleos.shared.place.Signal

/**
 * 화면설계서 08-1 테마 색 토큰 + 11-08 숲 색감(색감 개선_HS 숲 색감 토큰). 한 토큰에 야간 · 주간 두 값.
 * 포인트는 따로 색을 쓰지 않는다: 야간 크림 #DAD1BF + 짙은 글자, 주간 짙은 숲 #22332B + 크림 글자.
 * 초록은 허용 신호와 성공 표시에만 쓴다.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val text: Color,
    val card: Color,
    val cardInner: Color,
    val sub: Color,
    val subText: Color,
    val mutedText: Color,
    val disabledText: Color,
    val softText: Color,
    val railBackground: Color,
    val chipBorder: Color,
    val gauge: Color,
    val gaugeFill: Color,
    val bannerBackground: Color,
    val bannerText: Color,
    /** 선택 테두리 · 강조 글자 */
    val selected: Color,
    /** 선택한 칸 바탕 */
    val selectedFill: Color,
    /** "방문 확인" 같은 파란 알약 */
    val infoFill: Color,
    val infoText: Color,
    /** 03-1 안내 중 띠 · 선택한 편의시설 칸 */
    val guideFill: Color,
    val guideIcon: Color,
    val success: Color,
    val successFill: Color,
    val danger: Color,
    val dangerFill: Color,
    val caution: Color,
    val star: Color,
    val nextStep: Color,
    val nextStepFill: Color,
    /** 시트 뒤 덮개 */
    val scrim: Color,
    val mapBackground: Color,
    val signalGreenText: Color,
    val signalYellowText: Color,
    val signalRedText: Color,
    val signalUnknownText: Color,
    /** 포인트(안내 · 음성 · 선택 · 메뉴 바 선택): 야간 크림, 주간 짙은 숲 */
    val accent: Color,
    /** 포인트색 위 글자 */
    val onAccent: Color,
)

val DarkColors = AppColors(
    isDark = true,
    background = Color(0xFF101A16),
    text = Color(0xFFF4EFE6),
    card = Color(0xFF1A2620),
    cardInner = Color(0xFF22312A),
    sub = Color(0xFF2A3A32),
    subText = Color(0xFFB9C4B8),
    mutedText = Color(0xFF98A69A),
    disabledText = Color(0xFF7C8A7F),
    softText = Color(0xFFDCD8CC),
    railBackground = Color(0xFF22332B),
    chipBorder = Color(0xFF4F6356),
    gauge = Color(0xFF34463B),
    gaugeFill = Color(0xFF4CC38A),
    bannerBackground = Color(0xFF3A2F12),
    bannerText = Color(0xFFFFD98A),
    selected = Color(0xFFDAD1BF),
    selectedFill = Color(0xFF26382F),
    infoFill = Color(0xFF26382F),
    infoText = Color(0xFFDAD1BF),
    guideFill = Color(0xFF26382F),
    guideIcon = Color(0xFFDAD1BF),
    success = Color(0xFF8EE0B4),
    successFill = Color(0xFF173726),
    danger = Color(0xFFFF7A7E),
    dangerFill = Color(0xFF2A1517),
    caution = Color(0xFFF2B630),
    star = Color(0xFFF2B630),
    nextStep = Color(0xFFC9B8FF),
    nextStepFill = Color(0xFF2D2A4A),
    scrim = Color(0x99000000),
    mapBackground = Color(0xFF18241F),
    signalGreenText = Color(0xFF6FE0A0),
    signalYellowText = Color(0xFFFFC857),
    signalRedText = Color(0xFFFF8A8E),
    signalUnknownText = Color(0xFFC4CFC4),
    accent = Color(0xFFDAD1BF),
    onAccent = Color(0xFF16241D),
)

/** 주간: 옅은 세이지 바탕 + 짙은 숲색 글자 */
val LightColors = AppColors(
    isDark = false,
    background = Color(0xFFF1F4EF),
    text = Color(0xFF16241D),
    card = Color(0xFFFFFFFF),
    cardInner = Color(0xFFE6ECE4),
    sub = Color(0xFFDBE4D8),
    subText = Color(0xFF46564B),
    mutedText = Color(0xFF5A6A5E),
    disabledText = Color(0xFF718075),
    softText = Color(0xFF2C3A31),
    railBackground = Color(0xFFF1F5EF),
    chipBorder = Color(0xFFB3C1B4),
    gauge = Color(0xFFCDD8CC),
    gaugeFill = Color(0xFF1F9D60),
    bannerBackground = Color(0xFFFFF3D1),
    bannerText = Color(0xFF7A5300),
    selected = Color(0xFF22332B),
    selectedFill = Color(0xFFDFE8DC),
    infoFill = Color(0xFFDFE8DC),
    infoText = Color(0xFF22332B),
    guideFill = Color(0xFFDFE8DC),
    guideIcon = Color(0xFF22332B),
    success = Color(0xFF157347),
    successFill = Color(0xFFE3F5EA),
    danger = Color(0xFFC42B2B),
    dangerFill = Color(0xFFFDECEC),
    caution = Color(0xFF9C5300),
    star = Color(0xFFD99400),
    nextStep = Color(0xFF5B3FC4),
    nextStepFill = Color(0xFFEEE9FF),
    scrim = Color(0x59121417),
    mapBackground = Color(0xFFE6ECE4),
    signalGreenText = Color(0xFF157347),
    signalYellowText = Color(0xFF9C5300),
    signalRedText = Color(0xFFC42B2B),
    signalUnknownText = Color(0xFF5A6A5E),
    accent = Color(0xFF22332B),
    onAccent = Color(0xFFF4EFE6),
)

/** 지금 그리는 곳의 테마. AppRoot가 [resolveTheme]으로 정하고, 사진 위처럼 늘 어두운 곳은 [DarkScope]로 덮는다. */
val LocalAppColors = staticCompositionLocalOf { DarkColors }

/** 안쪽을 지정한 테마로 그린다 */
@Composable
fun ThemeScope(colors: AppColors, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalAppColors provides colors, content = content)

/** 사진 위 · 차박 모드처럼 테마와 상관없이 어둡게 그리는 곳 */
@Composable
fun DarkScope(enabled: Boolean = true, content: @Composable () -> Unit) =
    if (enabled) ThemeScope(DarkColors, content) else content()

/** 지금 테마의 색. 이름은 예전 그대로이고 값은 [LocalAppColors]에서 읽는다. */
object Palette {
    val colors: AppColors @Composable @ReadOnlyComposable get() = LocalAppColors.current

    val isDark: Boolean @Composable @ReadOnlyComposable get() = colors.isDark
    val Background: Color @Composable @ReadOnlyComposable get() = colors.background
    val Text: Color @Composable @ReadOnlyComposable get() = colors.text
    val Card: Color @Composable @ReadOnlyComposable get() = colors.card
    val CardInner: Color @Composable @ReadOnlyComposable get() = colors.cardInner
    val Sub: Color @Composable @ReadOnlyComposable get() = colors.sub
    val SubText: Color @Composable @ReadOnlyComposable get() = colors.subText
    val MutedText: Color @Composable @ReadOnlyComposable get() = colors.mutedText
    val DisabledText: Color @Composable @ReadOnlyComposable get() = colors.disabledText
    val SoftText: Color @Composable @ReadOnlyComposable get() = colors.softText
    val RailBackground: Color @Composable @ReadOnlyComposable get() = colors.railBackground
    val ChipBorder: Color @Composable @ReadOnlyComposable get() = colors.chipBorder
    val Gauge: Color @Composable @ReadOnlyComposable get() = colors.gauge
    val GaugeFill: Color @Composable @ReadOnlyComposable get() = colors.gaugeFill
    val BannerBackground: Color @Composable @ReadOnlyComposable get() = colors.bannerBackground
    val BannerText: Color @Composable @ReadOnlyComposable get() = colors.bannerText
    val Selected: Color @Composable @ReadOnlyComposable get() = colors.selected
    val SelectedFill: Color @Composable @ReadOnlyComposable get() = colors.selectedFill
    val InfoFill: Color @Composable @ReadOnlyComposable get() = colors.infoFill
    val InfoText: Color @Composable @ReadOnlyComposable get() = colors.infoText
    val GuideFill: Color @Composable @ReadOnlyComposable get() = colors.guideFill
    val GuideIcon: Color @Composable @ReadOnlyComposable get() = colors.guideIcon
    val Success: Color @Composable @ReadOnlyComposable get() = colors.success
    val SuccessFill: Color @Composable @ReadOnlyComposable get() = colors.successFill
    val Danger: Color @Composable @ReadOnlyComposable get() = colors.danger
    val DangerFill: Color @Composable @ReadOnlyComposable get() = colors.dangerFill
    val Caution: Color @Composable @ReadOnlyComposable get() = colors.caution
    val Star: Color @Composable @ReadOnlyComposable get() = colors.star
    val NextStep: Color @Composable @ReadOnlyComposable get() = colors.nextStep
    val NextStepFill: Color @Composable @ReadOnlyComposable get() = colors.nextStepFill
    val Scrim: Color @Composable @ReadOnlyComposable get() = colors.scrim
    val MapBackground: Color @Composable @ReadOnlyComposable get() = colors.mapBackground
    /** 반전 칸(선택한 칸: 바탕이 글자색) 위 초록 */
    val SuccessOnInverse: Color @Composable @ReadOnlyComposable get() = if (colors.isDark) LightColors.success else DarkColors.success

    val Accent: Color @Composable @ReadOnlyComposable get() = colors.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = colors.onAccent

    // 두 테마에서 같은 색: 신호등 몸체 · 상태 알약
    val LampBody = Color(0xFF08090B)
    val LampOff = Color(0xFF3A3F47)
    val ParkedPill = Color(0xFF157347)
    val DrivingPill = Color(0xFF2A3A32)

    /** 사진 위 글자: 사진 위에는 어둡게 막을 깔아서 두 테마 모두 밝은 글자 */
    val OnPhoto = Color(0xFFF4EFE6)
    val OnPhotoSub = Color(0xFFDCD8CC)
}

/** 설정 12 "화면 테마" */
enum class ThemeMode { DAY, NIGHT }

/**
 * 어떤 테마로 그릴지 정한다.
 * - 차박 모드 쪽 화면(05 · 잠든 화면 · 기상 알람 · 일산화탄소 경보 · 만든 사람 · 사진 크게 보기)은 항상 야간
 * - 주간 · 야간 고정이면 그대로
 * - 자동(예전 "차량 따라"): 차 화면(시스템)의 주간 · 야간 → 모르면 차의 야간 모드(NIGHT_MODE) → 그것도 모르면 야간
 *   (11-11) Pleos는 화면 밝기 설정 · 조도 센서로 시스템 야간 모드를 바꾸고, 계기판 · 홈도 그걸 따른다.
 *   NIGHT_MODE 값은 시스템 화면과 어긋날 수 있어서(에뮬레이터에서 시스템은 야간인데 값은 주간) 시스템을 먼저 본다.
 */
fun resolveTheme(pref: ThemePref, carNight: Boolean?, systemDark: Boolean?, alwaysDarkScreen: Boolean): ThemeMode = when {
    alwaysDarkScreen -> ThemeMode.NIGHT
    pref == ThemePref.DAY -> ThemeMode.DAY
    pref == ThemePref.NIGHT -> ThemeMode.NIGHT
    systemDark != null -> if (systemDark) ThemeMode.NIGHT else ThemeMode.DAY
    carNight != null -> if (carNight) ThemeMode.NIGHT else ThemeMode.DAY
    else -> ThemeMode.NIGHT
}

/** 신호 색 + 글자. 색만으로 구분하지 않도록 항상 라벨과 함께 쓴다. */
val Signal.label: String
    get() = when (this) {
        Signal.GREEN -> "허용"
        Signal.YELLOW -> "조건부"
        Signal.RED -> "금지"
        Signal.UNKNOWN -> "미확인"
    }

/** 켜진 램프 색 (두 테마 같음, 램프 몸체가 늘 어두움) */
val Signal.lampColor: Color
    get() = when (this) {
        Signal.GREEN -> Color(0xFF2FB36B)
        Signal.YELLOW -> Color(0xFFF2B630)
        Signal.RED -> Color(0xFFE5484D)
        Signal.UNKNOWN -> Color(0xFF5F6673)
    }

/** 지금 테마 바탕 위 신호 글자 색 */
val Signal.textColor: Color
    @Composable @ReadOnlyComposable get() = when (this) {
        Signal.GREEN -> Palette.colors.signalGreenText
        Signal.YELLOW -> Palette.colors.signalYellowText
        Signal.RED -> Palette.colors.signalRedText
        Signal.UNKNOWN -> Palette.colors.signalUnknownText
    }

/** 사진 위(늘 어두운 막) 신호 글자 색 */
val Signal.photoTextColor: Color
    get() = when (this) {
        Signal.GREEN -> DarkColors.signalGreenText
        Signal.YELLOW -> DarkColors.signalYellowText
        Signal.RED -> DarkColors.signalRedText
        Signal.UNKNOWN -> DarkColors.signalUnknownText
    }
