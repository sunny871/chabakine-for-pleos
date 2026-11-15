package kr.sanho.chabakine_pleos.ui.theme

import kr.sanho.chabakine_pleos.data.ThemePref
import kr.sanho.chabakine_pleos.ui.theme.ThemeMode.DAY
import kr.sanho.chabakine_pleos.ui.theme.ThemeMode.NIGHT
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun 차박_화면은_늘_야간() {
        for (pref in ThemePref.entries) {
            assertEquals(NIGHT, resolveTheme(pref, carNight = false, systemDark = false, alwaysDarkScreen = true))
        }
    }

    @Test
    fun 주간_야간_고정() {
        assertEquals(DAY, resolveTheme(ThemePref.DAY, carNight = true, systemDark = true, alwaysDarkScreen = false))
        assertEquals(NIGHT, resolveTheme(ThemePref.NIGHT, carNight = false, systemDark = false, alwaysDarkScreen = false))
    }

    @Test
    fun 차량_따라() {
        // 11-11: 차 화면(시스템)이 먼저. NIGHT_MODE 값이 어긋나도 시스템을 따른다
        assertEquals(NIGHT, resolveTheme(ThemePref.FOLLOW_CAR, carNight = false, systemDark = true, alwaysDarkScreen = false))
        assertEquals(DAY, resolveTheme(ThemePref.FOLLOW_CAR, carNight = true, systemDark = false, alwaysDarkScreen = false))
        assertEquals(NIGHT, resolveTheme(ThemePref.FOLLOW_CAR, carNight = true, systemDark = null, alwaysDarkScreen = false))
        assertEquals(DAY, resolveTheme(ThemePref.FOLLOW_CAR, carNight = false, systemDark = null, alwaysDarkScreen = false))
        // 차 값이 없어도 시스템을 따르고, 둘 다 모르면 야간
        assertEquals(DAY, resolveTheme(ThemePref.FOLLOW_CAR, carNight = null, systemDark = false, alwaysDarkScreen = false))
        assertEquals(NIGHT, resolveTheme(ThemePref.FOLLOW_CAR, carNight = null, systemDark = true, alwaysDarkScreen = false))
        assertEquals(NIGHT, resolveTheme(ThemePref.FOLLOW_CAR, carNight = null, systemDark = null, alwaysDarkScreen = false))
    }

    @Test
    fun 주간_글자_대비() {
        // 본문 글자는 바탕 · 카드와 4.5:1 이상
        for (bg in listOf(LightColors.background, LightColors.card, LightColors.cardInner)) {
            for (fg in listOf(LightColors.text, LightColors.subText, LightColors.mutedText, LightColors.signalGreenText, LightColors.signalYellowText, LightColors.signalRedText)) {
                assert(contrast(fg, bg) >= 4.5) { "대비 부족 $fg on $bg = ${contrast(fg, bg)}" }
            }
        }
    }

    private fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
        fun lum(c: androidx.compose.ui.graphics.Color): Double {
            fun ch(v: Float) = if (v <= 0.03928f) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
            return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
        }
        val (x, y) = lum(a) to lum(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
