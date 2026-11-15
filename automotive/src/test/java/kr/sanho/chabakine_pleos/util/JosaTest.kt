package kr.sanho.chabakine_pleos.util

import org.junit.Assert.assertEquals
import org.junit.Test

class JosaTest {

    @Test
    fun eulReul() {
        assertEquals("전류리포구를", Josa.eulReul("전류리포구"))
        assertEquals("남항진해변을", Josa.eulReul("남항진해변"))
        assertEquals("대창골을", Josa.eulReul("대창골"))
    }

    @Test
    fun euroRo() {
        assertEquals("전류리포구로", Josa.euroRo("전류리포구"))
        assertEquals("남항진해변으로", Josa.euroRo("남항진해변"))
        // ㄹ 받침은 "로"
        assertEquals("대창골로", Josa.euroRo("대창골"))
    }

    @Test
    fun eunNeun() {
        assertEquals("풍량·내외기는", Josa.eunNeun("풍량·내외기"))
        assertEquals("조명은", Josa.eunNeun("조명"))
    }

    @Test
    fun trailingSymbolsAndDigits() {
        assertEquals("섬강유원지(무료)로", Josa.euroRo("섬강유원지(무료)"))
        assertEquals("제2주차장을", Josa.eulReul("제2주차장"))
        assertEquals("주차장 3을", Josa.eulReul("주차장 3"))
        assertEquals("주차장 2를", Josa.eulReul("주차장 2"))
        assertEquals("주차장 1로", Josa.euroRo("주차장 1"))
    }
}
