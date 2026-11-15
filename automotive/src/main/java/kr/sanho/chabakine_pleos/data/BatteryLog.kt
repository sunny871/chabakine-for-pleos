package kr.sanho.chabakine_pleos.data

import android.content.Context
import androidx.core.content.edit
import kr.sanho.chabakine_pleos.vehicle.BatteryForecast
import kr.sanho.chabakine_pleos.vehicle.HistoryRate

/** 차박 중 10분마다 남기는 표본 */
data class BatterySample(val atMs: Long, val percent: Float, val outsideTempC: Float?, val climateOn: Boolean)

/** 한 밤 요약: 시간당 평균 % · 바깥 온도 구간 · 공조를 맞췄는지 */
data class NightSummary(val endMs: Long, val tempBand: Int?, val climateOn: Boolean, val hours: Float, val ratePerHour: Float)

/**
 * 밤사이 배터리 기록 · 요약 · 학습(순수 계산). 저장은 [BatteryLog]가 한다.
 * - 배터리가 늘어난 구간(충전)이나 30분 넘게 끊긴 곳에서 나눠, 가장 긴 구간만 쓴다.
 * - 2시간보다 짧으면 버린다. 최근 [MAX_NIGHTS]밤만 둔다.
 */
object BatteryNights {
    const val MIN_HOURS = 2f
    const val MAX_NIGHTS = 30
    const val MIN_NIGHTS = 2
    const val GAP_MS = 30 * 60_000L
    /** 이만큼은 오르내려도 잡음으로 본다 */
    const val NOISE = 0.3f

    fun summarize(samples: List<BatterySample>): NightSummary? {
        if (samples.size < 2) return null
        val sorted = samples.sortedBy { it.atMs }
        val runs = mutableListOf(mutableListOf(sorted.first()))
        for (s in sorted.drop(1)) {
            val prev = runs.last().last()
            val split = s.atMs - prev.atMs > GAP_MS || s.percent > prev.percent + NOISE
            if (split) runs += mutableListOf(s) else runs.last() += s
        }
        val run = runs.maxBy { it.last().atMs - it.first().atMs }
        val hours = (run.last().atMs - run.first().atMs) / 3_600_000f
        val drop = run.first().percent - run.last().percent
        if (hours < MIN_HOURS || drop <= 0f) return null
        val temps = run.mapNotNull { it.outsideTempC }.sorted()
        val median = temps.getOrNull(temps.size / 2)
        return NightSummary(run.last().atMs, BatteryForecast.tempBand(median), run.any { it.climateOn }, hours, drop / hours)
    }

    /** 새 밤을 앞에 넣고 최근 [MAX_NIGHTS]밤만 남긴다 */
    fun add(nights: List<NightSummary>, night: NightSummary): List<NightSummary> = (listOf(night) + nights).take(MAX_NIGHTS)

    /** 같은 온도 구간 · 같은 공조 켬/끔 기록이 [MIN_NIGHTS]밤 이상이면 그 중앙값 */
    fun historyRate(nights: List<NightSummary>, tempBand: Int?, climateOn: Boolean): HistoryRate? {
        val same = nights.filter { it.tempBand == tempBand && it.climateOn == climateOn }
        if (same.size < MIN_NIGHTS) return null
        val rates = same.map { it.ratePerHour }.sorted()
        val mid = rates.size / 2
        val median = if (rates.size % 2 == 1) rates[mid] else (rates[mid - 1] + rates[mid]) / 2f
        return HistoryRate(median, same.size)
    }

    // 저장 형식: 한 줄에 하나, 쉼표로 나눈 숫자(온도 모름은 빈칸)
    fun encodeSample(s: BatterySample) = "${s.atMs},${s.percent},${s.outsideTempC ?: ""},${if (s.climateOn) 1 else 0}"
    fun decodeSample(line: String): BatterySample? = line.split(',').takeIf { it.size == 4 }?.let { p ->
        val at = p[0].toLongOrNull() ?: return null
        val pct = p[1].toFloatOrNull() ?: return null
        BatterySample(at, pct, p[2].toFloatOrNull(), p[3] == "1")
    }

    fun encodeNight(n: NightSummary) = "${n.endMs},${n.tempBand ?: ""},${if (n.climateOn) 1 else 0},${n.hours},${n.ratePerHour}"
    fun decodeNight(line: String): NightSummary? = line.split(',').takeIf { it.size == 5 }?.let { p ->
        val end = p[0].toLongOrNull() ?: return null
        val hours = p[3].toFloatOrNull() ?: return null
        val rate = p[4].toFloatOrNull() ?: return null
        NightSummary(end, p[1].toIntOrNull(), p[2] == "1", hours, rate)
    }
}

/**
 * 밤사이 배터리 기록 저장소. 이 기기 안(SharedPreferences "battery_log")에만 두고 서버로 보내지 않는다.
 * 표본은 그 밤을 요약하면 지운다. 설정 12 "배터리 기록 지우기"로 모두 지운다.
 */
class BatteryLog(context: Context) {
    private val prefs = context.getSharedPreferences("battery_log", Context.MODE_PRIVATE)

    val samples: List<BatterySample>
        get() = lines(KEY_SAMPLES).mapNotNull(BatteryNights::decodeSample)

    val nights: List<NightSummary>
        get() = lines(KEY_NIGHTS).mapNotNull(BatteryNights::decodeNight)

    fun addSample(sample: BatterySample) {
        val next = (lines(KEY_SAMPLES) + BatteryNights.encodeSample(sample)).takeLast(MAX_SAMPLES)
        prefs.edit { putString(KEY_SAMPLES, next.joinToString("\n")) }
    }

    /** 그 밤을 요약해 남기고 표본을 지운다. 요약할 수 없으면(짧음 · 충전) 버린다 */
    fun closeNight(): NightSummary? {
        val night = BatteryNights.summarize(samples)
        prefs.edit {
            remove(KEY_SAMPLES)
            if (night != null) putString(KEY_NIGHTS, BatteryNights.add(nights, night).joinToString("\n") { BatteryNights.encodeNight(it) })
        }
        return night
    }

    fun historyRate(tempBand: Int?, climateOn: Boolean): HistoryRate? = BatteryNights.historyRate(nights, tempBand, climateOn)

    fun clear() = prefs.edit { clear() }

    private fun lines(key: String): List<String> = prefs.getString(key, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val KEY_SAMPLES = "samples"
        const val KEY_NIGHTS = "nights"
        /** 10분마다 하루 넘게 */
        const val MAX_SAMPLES = 200
    }
}
