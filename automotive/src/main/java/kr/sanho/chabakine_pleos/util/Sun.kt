package kr.sanho.chabakine_pleos.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * 일출 시각을 기기에서 계산한다(NOAA 근사식, 오차 1~2분). 서버나 날씨 API를 쓰지 않는다.
 * 극지방처럼 해가 뜨지 않는 날은 null.
 */
fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId = ZoneId.systemDefault()): LocalTime? {
    fun rad(d: Double) = Math.toRadians(d)
    fun deg(r: Double) = Math.toDegrees(r)
    fun norm(v: Double, max: Double) = ((v % max) + max) % max

    val lngHour = longitude / 15.0
    val t = date.dayOfYear + (6.0 - lngHour) / 24.0
    val m = 0.9856 * t - 3.289
    val l = norm(m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634, 360.0)
    var ra = norm(deg(atan(0.91764 * tan(rad(l)))), 360.0)
    ra += floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0
    ra /= 15.0
    val sinDec = 0.39782 * sin(rad(l))
    val cosDec = cos(asin(sinDec))
    val cosH = (cos(rad(90.833)) - sinDec * sin(rad(latitude))) / (cosDec * cos(rad(latitude)))
    if (cosH > 1 || cosH < -1) return null
    val h = (360.0 - deg(acos(cosH))) / 15.0
    val ut = norm(h + ra - 0.06571 * t - 6.622 - lngHour, 24.0)
    val offsetHours = zone.rules.getOffset(date.atStartOfDay(zone).toInstant()).totalSeconds / 3600.0
    val local = norm(ut + offsetHours, 24.0)
    val minutes = (local * 60).toInt()
    return LocalTime.of(minutes / 60, minutes % 60)
}
