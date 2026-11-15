package kr.sanho.chabakine_pleos.app

/** 일산화탄소 경보 단계(화면설계서 05-2 ~ 05-4 · 개발 기준 메모). 50 ppm 미만은 경보 없음 */
enum class CoLevel(val minPpm: Int) {
    NONE(0),
    CAUTION(50),
    WARNING(100),
    DANGER(200),
    ;

    companion object {
        fun of(ppm: Int): CoLevel = entries.last { ppm >= it.minPpm }
    }
}

/**
 * 경보 상태. [candidate]는 수치가 새로 들어간 구간, [candidateSince]는 그 구간에 들어간 시각(ms).
 * [dismissed]는 사용자가 알림을 끈 상태(단계가 올라가면 다시 울린다).
 */
data class CoAlarmState(
    val level: CoLevel = CoLevel.NONE,
    val candidate: CoLevel = CoLevel.NONE,
    val candidateSince: Long = 0L,
    val dismissed: Boolean = false,
) {
    val ringing: Boolean get() = level != CoLevel.NONE && !dismissed
}

/** 새 구간이 이만큼 넘게 이어져야 단계를 바꾼다(경계값에서 자주 바뀌지 않게) */
const val CO_LEVEL_HOLD_MS = 5_000L

/** 50 ppm 아래로 이만큼 넘게 이어지면 경보를 저절로 끈다 */
const val CO_CLEAR_HOLD_MS = 10_000L

/**
 * 수치 하나를 받아 다음 상태를 돌려준다. [ppm]이 null(센서 값 없음)이면 그대로 둔다.
 * - 단계는 올라가든 내려가든 새 구간이 5초 넘게 이어질 때만 바뀐다(3→2→1처럼 수치를 따라 내려간다).
 * - 경보 없음으로 돌아가는 것은 50 ppm 아래가 10초 넘게 이어질 때.
 * - 끈 뒤에도 단계가 다시 올라가면 다시 울린다.
 */
fun nextCoAlarm(state: CoAlarmState, ppm: Int?, nowMs: Long): CoAlarmState {
    if (ppm == null) return state
    val band = CoLevel.of(ppm)
    if (band == state.level) return state.copy(candidate = band, candidateSince = nowMs)
    if (band != state.candidate) return state.copy(candidate = band, candidateSince = nowMs)
    val hold = if (band == CoLevel.NONE) CO_CLEAR_HOLD_MS else CO_LEVEL_HOLD_MS
    if (nowMs - state.candidateSince < hold) return state
    val rose = band > state.level
    return state.copy(level = band, dismissed = state.dismissed && !rose && band != CoLevel.NONE)
}
