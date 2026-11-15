package kr.sanho.chabakine_pleos.vehicle

import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** 예상에 쓴 시간당 %가 어디서 왔는지(화면 아랫줄 문구) */
enum class RateSource { MEASURED, HISTORY, ESTIMATE }

/** 항목별 시간당 % (대기 · 공조 · 조명 · 환기) */
data class Rates(val idle: Float, val climate: Float, val light: Float, val vent: Float) {
    val total: Float get() = idle + climate + light + vent
}

/** 아침까지 예상: 쓸 % · 남을 % */
data class Forecast(
    val usePercent: Int,
    val leftPercent: Int,
    val ratePerHour: Float,
    val hours: Float,
    val source: RateSource,
    /** [RateSource.HISTORY]일 때 쓴 지난 밤 수 */
    val nights: Int = 0,
)

/**
 * 밤사이 배터리 예상(화면설계서 05 · 23 · 07-3, 11-10).
 * 시간당 %는 이 순서로 고른다: 그날 밤 실측 → 같은 조건의 지난 기록(2밤 이상) → 차 정보로 만든 어림값.
 * 기록 · 계산은 모두 이 기기 안에서만 한다.
 */
object BatteryForecast {
    // ── 어림값(W). 실차 확인 전 값이다. 실차에서 25 만든 사람(디버그) 기록 표로 맞춘다 ──
    /** 유틸리티 모드 대기(화면 · 통신 · 12V 등) */
    const val IDLE_W = 250f
    /** 앰비언트 조명 밝기 100%일 때 */
    const val LIGHT_FULL_W = 40f
    /** 공조: |설정 온도 − 바깥 온도| 구간별 */
    val CLIMATE_W_BY_DELTA = listOf(3f to 300f, 8f to 600f, 15f to 1000f, Float.MAX_VALUE to 1500f)
    /** 바깥 온도를 모르거나 공조 설정을 모를 때 */
    const val CLIMATE_UNKNOWN_W = 700f
    /** 환기로 창문을 2분 열 때 공조가 더 쓰는 몫(1회, 2 cm 기준 Wh) */
    const val VENT_WH_PER_OPENING = 25f

    /** 실측으로 인정하는 최소 시간 · 최소 감소 */
    const val MEASURE_MIN_MS = 30 * 60_000L
    const val MEASURE_MIN_DROP = 0.5f

    /** 기준 시각(23 화면은 내일 오전 7시로 고정) */
    const val MORNING_MINUTES = 7 * 60

    /** W를 시간당 %로: 전력 ÷ 용량 × 100 */
    fun percentPerHour(watts: Float, capacityWh: Float): Float = if (capacityWh <= 0f) 0f else watts / capacityWh * 100f

    fun climateWatts(preset: CampPreset?, outsideTempC: Float?): Float {
        if (preset == null || outsideTempC == null) return CLIMATE_UNKNOWN_W
        val target = if (preset.autoClimate) CampPreset.comfortTemperature(outsideTempC) else preset.temperatureC
        val delta = abs(target - outsideTempC)
        val base = CLIMATE_W_BY_DELTA.first { delta < it.first }.second
        // 풍량 2(약풍)를 1.0으로: 한 단계마다 7.5%
        return base * (0.85f + 0.075f * preset.fanSpeed)
    }

    /** 차 정보(용량 · 바깥 온도)와 고른 설정으로 만든 어림값 */
    fun model(capacityWh: Float, preset: CampPreset?, outsideTempC: Float?): Rates {
        val light = preset?.ambientColor?.let { LIGHT_FULL_W * preset.ambientBrightness / 100f } ?: 0f
        val vent = preset?.windowOpenCm?.let { cm -> VENT_WH_PER_OPENING * (cm / 2f) * (60f / max(1, preset.ventIntervalMin)) } ?: 0f
        return Rates(
            idle = percentPerHour(IDLE_W, capacityWh),
            climate = percentPerHour(climateWatts(preset, outsideTempC), capacityWh),
            light = percentPerHour(light, capacityWh),
            vent = percentPerHour(vent, capacityWh),
        )
    }

    /** [now] 다음에 오는 [minutesOfDay] 시각까지 남은 시간. 0시~7시 사이면 그날 7시 */
    fun hoursUntil(now: ZonedDateTime, minutesOfDay: Int = MORNING_MINUTES): Float {
        var target = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(minutesOfDay.toLong())
        if (!target.isAfter(now)) target = target.plusDays(1)
        return (target.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()) / 3_600_000f
    }

    /** 그날 밤 실측: 30분이 지나고 0.5% 이상 줄었을 때만 */
    fun measuredRate(startPercent: Float?, startMs: Long?, nowPercent: Float, nowMs: Long): Float? {
        if (startPercent == null || startMs == null) return null
        val ms = nowMs - startMs
        val drop = startPercent - nowPercent
        if (ms < MEASURE_MIN_MS || drop < MEASURE_MIN_DROP) return null
        return drop / (ms / 3_600_000f)
    }

    /** 실측 → 지난 기록 → 어림값 순서로 시간당 %를 골라 [hours] 뒤를 예상한다. 0% 아래로는 내려가지 않는다 */
    fun forecast(nowPercent: Float, hours: Float, measured: Float?, history: HistoryRate?, model: Float): Forecast {
        val (rate, source, nights) = when {
            measured != null -> Triple(measured, RateSource.MEASURED, 0)
            history != null -> Triple(history.ratePerHour, RateSource.HISTORY, history.nights)
            else -> Triple(model, RateSource.ESTIMATE, 0)
        }
        val use = (rate * hours).coerceIn(0f, nowPercent)
        val left = (nowPercent - use).coerceAtLeast(0f)
        return Forecast(use.roundToInt(), left.roundToInt(), rate, hours, source, nights)
    }

    /** 바깥 온도 5°C 구간(−3 → −5, 7 → 5). 모르면 null */
    fun tempBand(tempC: Float?): Int? = tempC?.let { (floor(it / 5f) * 5).toInt() }
}

/** 지난 기록에서 고른 시간당 % */
data class HistoryRate(val ratePerHour: Float, val nights: Int)
