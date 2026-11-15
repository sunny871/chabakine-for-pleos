package kr.sanho.chabakine_pleos.util

/**
 * 이름 뒤 조사를 받침에 맞춘다(QA-07: "전류리포구을" → "전류리포구를").
 * 마지막 한글 · 숫자 글자를 본다. 괄호나 기호로 끝나면 그 앞 글자를 본다. 영문 등 알 수 없으면 받침 없음으로 본다.
 */
object Josa {

    private enum class Final { NONE, RIEUL, OTHER }

    private fun finalOf(word: String): Final {
        val c = word.lastOrNull { it in '가'..'힣' || it.isDigit() } ?: return Final.NONE
        if (c.isDigit()) {
            // 영 일 이 삼 사 오 육 칠 팔 구
            return when (c) {
                '1', '7', '8' -> Final.RIEUL
                '0', '3', '6' -> Final.OTHER
                else -> Final.NONE
            }
        }
        return when ((c - '가') % 28) {
            0 -> Final.NONE
            8 -> Final.RIEUL
            else -> Final.OTHER
        }
    }

    /** 을/를: "전류리포구를", "남항진해변을" */
    fun eulReul(word: String): String = word + if (finalOf(word) == Final.NONE) "를" else "을"

    /** 은/는: "풍량·내외기는", "조명은" */
    fun eunNeun(word: String): String = word + if (finalOf(word) == Final.NONE) "는" else "은"

    /** 으로/로: "전류리포구로", "대창골로"(ㄹ 받침), "남항진해변으로" */
    fun euroRo(word: String): String = word + if (finalOf(word) == Final.OTHER) "으로" else "로"
}
