package kr.sanho.chabakine_pleos.app

/**
 * 05-7 잠든 동안 배터리 부족 알림 기준.
 * 잠들 때 배터리가 20% 이상이면 20% 아래로 내려갈 때 알린다. 이미 20% 아래로 잠들면 다음 단계(10% → 5%)로 낮춰,
 * 낮은 배터리로 잠든 사람도 한 번은 깨운다. 5% 아래로 잠들면 더 알릴 단계가 없어 잠들기 창에서 미리 경고만 한다.
 * 알림 뒤 "다시 잠들기"를 누르면 그때 배터리로 다시 정하므로 20% → 10% → 5% 순서로 한 번씩 울린다.
 */
object LowBatteryRule {

    /** 알림 단계(%). 첫 값이 기본 기준이다 */
    val STEPS = listOf(20f, 10f, 5f)

    /** 잠들 때 배터리([startPercent])로 정한 이번 잠의 알림 기준. 배터리를 모르면 기본 20%, 더 알릴 단계가 없으면 null */
    fun threshold(startPercent: Float?): Float? =
        if (startPercent == null) STEPS.first() else STEPS.firstOrNull { startPercent >= it }

    /**
     * 더 알릴 단계가 없는 채로 잠들 때 잠든 화면에 남기는 한 줄(QA-05). 칩이 말없이 빠지면 알림이 꺼진 것처럼 보여서다.
     * 배터리를 모르거나 아직 단계가 남았으면 null
     */
    fun noStepNote(startPercent: Float?): String? =
        if (startPercent == null || threshold(startPercent) != null) null
        else "배터리 ${startPercent.toInt()}% · ${STEPS.last().toInt()}% 아래라 더 알리지 않아요"

    /** 잠든 뒤 [nowPercent]가 기준 아래로 새로 내려갔으면 true */
    fun shouldAlert(startPercent: Float?, nowPercent: Float?): Boolean {
        val limit = threshold(startPercent) ?: return false
        return nowPercent != null && nowPercent < limit
    }
}
