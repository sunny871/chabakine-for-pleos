package kr.sanho.chabakine_pleos.vehicle

import kr.sanho.chabakine_pleos.util.Josa
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.control.MoveControl
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** 차박 시작 때 맞출 값(화면설계서 23) */
data class CampPreset(
    /** 앰비언트 조명 색(0xRRGGBB). null이면 끈다. */
    val ambientColor: Int? = WARM_ORANGE,
    val ambientBrightness: Int = 20,
    val temperatureC: Float = 22f,
    /** true면 [temperatureC]를 바깥 온도로 정한 값([comfortTemperature])으로 쓴다. −/+를 누르면 꺼진다. */
    val autoClimate: Boolean = true,
    /** 풍량 단계. 23에서 약 · 중 · 강([FAN_STEPS])으로 고른다. 차가 받는 범위로 맞춰 보낸다 */
    val fanSpeed: Int = 2,
    /** true면 외기 유입(내기 순환 끔) */
    val outsideAir: Boolean = true,
    /** 앞좌석을 끝까지 앞으로 밀어 뒤 공간을 넓힌다 */
    val seatForward: Boolean = true,
    /**
     * 환기할 때 대각선 두 창문(운전석 · 조수석 뒤)을 여는 폭(cm). 평소에는 닫아 두고 환기 때만 이만큼 연다.
     * null이면 창문은 건드리지 않고 알림만
     */
    val windowOpenCm: Int? = 2,
    /** 이 간격마다 [windowOpenCm]만큼 [VENT_OPEN_MINUTES]분 열었다가 닫는다(창문을 못 움직이면 알림만) */
    val ventIntervalMin: Int = 60,
) {
    /** 창문 위치 범위(닫힘~열림)에서 [windowOpenCm]이 차지하는 비율(%) */
    val windowOpenPercent: Int? get() = windowOpenCm?.let { (it * 100 / WINDOW_TRAVEL_CM).coerceIn(1, 100) }

    companion object {
        const val WARM_ORANGE = 0xE58A3A
        const val SOFT_WHITE = 0xE9E4D8

        /** 23 조명 더보기 팝업의 색(이름 · 0xRRGGBB). 앞의 둘은 카드에 바로 보인다(11-11) */
        val AMBIENT_COLORS: List<Pair<String, Int>> = listOf(
            "따뜻한 주황" to WARM_ORANGE,
            "은은한 흰색" to SOFT_WHITE,
            "촛불 노랑" to 0xF2C14E,
            "달빛 파랑" to 0x6F9BE8,
            "숲 초록" to 0x5FBF8A,
            "라벤더" to 0xA88BE0,
            "노을 분홍" to 0xE87FA0,
            "붉은 등" to 0xD9483B,
        )

        fun ambientName(color: Int?): String = if (color == null) "꺼짐" else AMBIENT_COLORS.firstOrNull { it.second == color }?.first ?: "직접 고른 색"

        /** 바람 약 · 중 · 강 → 풍량 단계(11-11) */
        val FAN_STEPS: List<Pair<String, Int>> = listOf("약" to 2, "중" to 4, "강" to 6)

        fun fanName(speed: Int): String = FAN_STEPS.lastOrNull { speed >= it.second }?.first ?: "약"

        const val TEMP_MIN = 16f
        const val TEMP_MAX = 30f
        const val TEMP_STEP = 0.5f

        /** 정해진 환기에서 창문을 열어 두는 시간 */
        const val VENT_OPEN_MINUTES = 2

        /** 일산화탄소 경보 "환기 시키기"에서 크게 여는 정도(%) */
        const val VENT_OPEN_PERCENT = 30

        /** 창문이 끝까지 내려가는 길이. 차종마다 달라 확인 전까지 어림값이다. */
        private const val WINDOW_TRAVEL_CM = 50

        /** 공조 자동: 바깥이 추우면 따뜻하게, 더우면 시원하게 시작한다. 바깥 온도를 모르면 22°C */
        fun comfortTemperature(outsideC: Float?): Float = when {
            outsideC == null -> 22f
            outsideC < 5f -> 24f
            outsideC < 15f -> 23f
            outsideC <= 25f -> 22f
            else -> 20f
        }
    }
}

/** 항목별 결과. [Unsupported]는 차종이 지원하지 않거나 권한이 승인되지 않은 것 */
sealed interface ControlResult {
    data object Idle : ControlResult
    data object Working : ControlResult
    data object Done : ControlResult

    /** 일부 항목만 적용됨 */
    data class Partial(val note: String) : ControlResult
    data class Unsupported(val reason: String) : ControlResult
}

data class CampControlState(
    val ready: Boolean = false,
    /**
     * 차량 제어 권한(CONTROL_CAR_*)이 하나라도 허용됨.
     * 권한을 어떻게 받는지는 Pleos 문서(Vehicle SDK "Declaring permission", 2026-11-14 확인)에 승인 절차가 적혀 있지 않다.
     * 받지 못한 항목은 "지원하지 않음"으로 두고 나머지는 계속 적용한다.
     */
    val approved: Boolean = false,
    val lights: ControlResult = ControlResult.Idle,
    val climate: ControlResult = ControlResult.Idle,
    val windows: ControlResult = ControlResult.Idle,
    val seat: ControlResult = ControlResult.Idle,
) {
    /** 화면설계서의 "차량 연동이 승인된 차": SDK가 연결됐고 제어 권한이 있다 */
    val linked: Boolean get() = ready && approved

    /** 23에서 아직 아무것도 맞추지 않았다(05 "차량 맞추기" 버튼을 보일 때) */
    val notApplied: Boolean get() = listOf(lights, climate, windows).all { it == ControlResult.Idle }

    /** 23에서 맞춘 항목이 모두 적용됐다(좌석을 고르지 않았으면 좌석은 바로 Done) */
    val allDone: Boolean get() = listOf(lights, climate, windows, seat).all { it == ControlResult.Done }

    /** 23에서 보내는 중인 항목이 있다 */
    val working: Boolean get() = listOf(lights, climate, windows, seat).any { it == ControlResult.Working }
}

/**
 * Pleos Vehicle SDK로 차박 시작 때 앰비언트 조명·공조·앞좌석·창문을 맞춘다.
 * 정차 중(P단)에만 호출해야 한다. 시동 끄기와 유틸리티 모드 전환은 API가 없어 운전자가 직접 한다.
 * 각 호출은 비동기이고 실패해도 다른 항목은 계속한다. 실패는 "지원하지 않음"으로 보여 준다.
 */
class CampControl(context: Context) {

    private val appContext = context.applicationContext
    private val vehicle = Vehicle(appContext)
    private val _state = MutableStateFlow(CampControlState())
    val state: StateFlow<CampControlState> = _state.asStateFlow()

    private val frontSeats = setOf(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
    private val frontWindowValues = setOf(WindowValue.ROW_1_LEFT, WindowValue.ROW_1_RIGHT)

    private val handler = Handler(Looper.getMainLooper())

    /** 좌석 움직임에 쓸 영역(지원 확인 결과). 멈출 때 같은 영역에 STOP을 보낸다. */
    private var seatAreas: List<SeatArea> = emptyList()

    fun start() {
        runCatching { vehicle.initialize() }
            .onSuccess { _state.update { it.copy(ready = true, approved = hasControlPermission()) } }
            .onFailure { Log.w(TAG, "Vehicle SDK 초기화 실패", it) }
    }

    private fun hasControlPermission(): Boolean = CONTROL_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun stop() {
        stopSeat()
        runCatching { vehicle.release() }
        _state.update { it.copy(ready = false) }
    }

    /** "차박 시작": 조명·공조·창문·앞좌석을 한 번에 맞춘다 */
    fun applyPreset(preset: CampPreset) {
        applyLights(preset)
        applyClimate(preset)
        applyWindows(preset)
        applySeat(preset)
    }

    /** 23 "실패한 항목 다시 시도": 적용되지 않은 항목만 다시 보낸다 */
    fun retryFailed(preset: CampPreset) {
        val s = _state.value
        if (s.lights !is ControlResult.Done) applyLights(preset)
        if (s.climate !is ControlResult.Done) applyClimate(preset)
        if (s.windows !is ControlResult.Done) applyWindows(preset)
        if (s.seat !is ControlResult.Done) applySeat(preset)
    }

    /** 새 차박을 시작할 때 지난 결과를 지운다(05 "차량 맞추기"가 다시 보이게) */
    fun clearResults() {
        _state.update { it.copy(lights = ControlResult.Idle, climate = ControlResult.Idle, windows = ControlResult.Idle, seat = ControlResult.Idle) }
    }

    /**
     * 차량이 알려 준 지원 영역 중 앞좌석에 해당하는 것만 고른다.
     * 차종에 따라 운전석·조수석이 따로이거나 하나로 묶여 있다. 앞좌석 영역이 없으면 전체(첫 번째)를 쓴다.
     */
    private fun frontOf(areas: Collection<SeatArea>): List<SeatArea> =
        areas.filter { a -> a.values.any { it in frontSeats } }.ifEmpty { areas.take(1) }

    fun applyLights(preset: CampPreset) {
        _state.update { it.copy(lights = ControlResult.Working) }
        val light = vehicle.getLight()
        val onFail = fail("조명") { copy(lights = it) }
        light.checkAmbientLightSwitchCapabilities({ _, switches ->
            val areas = frontOf(switches.keys)
            if (areas.isEmpty()) return@checkAmbientLightSwitchCapabilities unsupported("조명") { copy(lights = it) }
            val color = preset.ambientColor
            all(areas, onDone = { done { copy(lights = it) } }, onFail = onFail) { area, ok, ko ->
                if (color == null) {
                    light.setAmbientLightSwitch(area, LightSwitch.OFF, { _, _ -> ok() }, ko)
                } else {
                    light.setAmbientLightSwitch(area, LightSwitch.ON, { _, _ ->
                        light.setAmbientLightBrightnessColor(area, color, { _, _ ->
                            light.setAmbientLightBrightness(area, preset.ambientBrightness, { _, _ -> ok() }, ko)
                        }, ko)
                    }, ko)
                }
            }
        }, onFail)
    }

    fun applyClimate(preset: CampPreset) {
        _state.update { it.copy(climate = ControlResult.Working) }
        val hvac = vehicle.getHVAC()
        val onFail = fail("공조") { copy(climate = it) }
        // 풍량·내외기는 차종·공조 상태에 따라 거절될 수 있어 실패해도 나머지를 계속하고 일부 적용으로 알린다
        val skipped = mutableListOf<String>()
        val finish = {
            val r = if (skipped.isEmpty()) ControlResult.Done else ControlResult.Partial("${Josa.eunNeun(skipped.joinToString("·"))} 이 차에서 바꾸지 못했어요")
            _state.update { it.copy(climate = r) }
        }
        val skip = { what: String, next: () -> Unit -> { e: Exception ->
            Log.w(TAG, "$what 건너뜀: ${e.message}")
            skipped += what
            next()
        } }
        hvac.checkHvacPowerCapabilities({ powerAreas: List<SeatArea> ->
            all(frontOf(powerAreas), onDone = {
                hvac.checkHvacTemperatureCapabilities({ temps: Map<SeatArea, AreaBounds<Float>> ->
                    all(frontOf(temps.keys), onDone = {
                        val recirculation = {
                            hvac.checkHvacRecirculationCapabilities({ recirc: List<SeatArea> ->
                                all(frontOf(recirc), onDone = finish, onFail = skip("내외기", finish)) { a, ok, ko ->
                                    hvac.setHvacRecirculation(a, !preset.outsideAir, { _, _ -> ok() }, ko)
                                }
                            }, skip("내외기", finish))
                        }
                        hvac.checkHvacFanSpeedCapabilities({ fans: Map<SeatArea, AreaBounds<Int>> ->
                            all(frontOf(fans.keys), onDone = recirculation, onFail = skip("풍량", recirculation)) { a, ok, ko ->
                                val b = fans[a]
                                val speed = if (b == null) preset.fanSpeed else preset.fanSpeed.coerceIn(b.minValue, b.maxValue)
                                hvac.setHvacFanSpeed(a, speed, { _, _ -> ok() }, ko)
                            }
                        }, skip("풍량", recirculation))
                    }, onFail = onFail) { a, ok, ko ->
                        val b = temps[a]
                        val c = if (b == null) preset.temperatureC else preset.temperatureC.coerceIn(b.minValue, b.maxValue)
                        hvac.setHvacTemperature(a, c, TemperatureUnit.CELSIUS, { _, _ -> ok() }, ko)
                    }
                }, onFail)
            }, onFail = onFail) { a, ok, ko -> hvac.setHvacPower(a, true, { _, _ -> ok() }, ko) }
        }, onFail)
    }

    /**
     * 05-7 배터리 부족 "공조 약하게": 앞좌석 풍량을 그 차의 가장 낮은 값으로. 결과는 [onResult](true면 적용됨)로만 알린다
     * (23의 공조 결과 칸은 건드리지 않는다).
     */
    fun lowerFan(onResult: (Boolean) -> Unit) {
        val hvac = vehicle.getHVAC()
        val ko: (Exception) -> Unit = { e -> Log.w(TAG, "풍량 낮추기 실패: ${e.message}"); onResult(false) }
        runCatching {
            hvac.checkHvacFanSpeedCapabilities({ fans: Map<SeatArea, AreaBounds<Int>> ->
                val areas = frontOf(fans.keys)
                if (areas.isEmpty()) onResult(false)
                else all(areas, onDone = { onResult(true) }, onFail = ko) { a, ok, fail ->
                    // 0이 "꺼짐"인 차가 있어 1 이상 중 가장 낮은 값
                    val lowest = fans[a]?.minValue?.coerceAtLeast(1) ?: 1
                    hvac.setHvacFanSpeed(a, lowest, { _, _ -> ok() }, fail)
                }
            }, ko)
        }.onFailure { ko(it as? Exception ?: Exception(it)) }
    }

    /**
     * 05-7 배터리 부족 전에 지금 공조가 켜져 있는지 읽는다(앞좌석 중 하나라도 켜져 있으면 true).
     * 읽지 못하면 null. 결과는 한 번만 [onResult]로 온다
     */
    fun readClimateOn(onResult: (Boolean?) -> Unit) {
        var done = false
        val finish: (Boolean?) -> Unit = { v -> if (!done) { done = true; onResult(v) } }
        val ko: (Exception) -> Unit = { e -> Log.i(TAG, "공조 전원 읽기 실패: ${e.message}"); finish(null) }
        runCatching {
            val hvac = vehicle.getHVAC()
            hvac.checkHvacPowerCapabilities({ powerAreas: List<SeatArea> ->
                val areas = frontOf(powerAreas)
                if (areas.isEmpty()) return@checkHvacPowerCapabilities finish(null)
                var left = areas.size
                var anyOn = false
                areas.forEach { a ->
                    hvac.getHvacPowerState(a, { v: Boolean? ->
                        if (v == true) anyOn = true
                        left -= 1
                        if (left == 0) finish(anyOn)
                    }, ko)
                }
            }, ko)
        }.onFailure { ko(it as? Exception ?: Exception(it)) }
    }

    /** 05-7 배터리 부족 "공조 끄기": 앞좌석 공조 전원을 끈다 */
    fun climateOff(onResult: (Boolean) -> Unit) {
        val hvac = vehicle.getHVAC()
        val ko: (Exception) -> Unit = { e -> Log.w(TAG, "공조 끄기 실패: ${e.message}"); onResult(false) }
        runCatching {
            hvac.checkHvacPowerCapabilities({ powerAreas: List<SeatArea> ->
                val areas = frontOf(powerAreas)
                if (areas.isEmpty()) onResult(false)
                else all(areas, onDone = { onResult(true) }, onFail = ko) { a, ok, fail -> hvac.setHvacPower(a, false, { _, _ -> ok() }, fail) }
            }, ko)
        }.onFailure { ko(it as? Exception ?: Exception(it)) }
    }

    /** 23 차박 시작: 환기 폭을 골랐으면 창문을 닫아 둔다(환기 때만 연다). "알림만"이면 건드리지 않는다 */
    fun applyWindows(preset: CampPreset) {
        if (preset.windowOpenPercent == null) {
            _state.update { it.copy(windows = ControlResult.Done) }
            return
        }
        _state.update { it.copy(windows = ControlResult.Working) }
        setWindows(0, onDone = { done { copy(windows = it) } }, onFail = fail("창문") { copy(windows = it) }) {
            unsupported("창문") { copy(windows = it) }
        }
    }

    /** 차박 중 환기: 대각선 두 창문을 [percent]만큼 연다. 0이면 닫는다(결과 표시는 바꾸지 않는다) */
    fun ventWindows(percent: Int) {
        setWindows(percent, onDone = {}, onFail = { e -> Log.w(TAG, "환기 창문 실패: ${e.message}") }) {}
    }

    /** 운전석과 그 대각선(조수석 뒤) 창문을 닫힘~열림 사이 [percent]만큼 연다. 둘 다 없으면 앞 두 창문 */
    private fun setWindows(percent: Int, onDone: () -> Unit, onFail: (Exception) -> Unit, onUnsupported: () -> Unit) {
        val window = vehicle.getWindow()
        window.checkWindowPositionCapabilities({ bounds: Map<WindowArea, AreaBounds<Int>> ->
            val driver = bounds.keys.filter { a -> WindowValue.ROW_1_LEFT in a.values }
            val rearRight = bounds.keys.filter { a -> WindowValue.ROW_2_RIGHT in a.values }
            val areas = if (driver.isNotEmpty() && rearRight.isNotEmpty()) (driver + rearRight).distinct()
            else bounds.keys.filter { a -> a.values.any { it in frontWindowValues } }
            if (areas.isEmpty()) return@checkWindowPositionCapabilities onUnsupported()
            all(areas, onDone = onDone, onFail = onFail) { a, ok, ko ->
                val b = bounds.getValue(a)
                // 닫힘(min)~열림(max) 사이에서 percent만큼 연다(0이면 닫힘)
                // 범위가 좁으면(예: 0~10) 내림하면 0이 되므로 열기를 고르면 최소 한 칸은 연다
                val step = ((b.maxValue - b.minValue) * percent + 99) / 100
                val target = (b.minValue + step).coerceAtMost(b.maxValue)
                window.setWindowPosition(a, target, { _, _ -> ok() }, ko)
            }
        }, onFail)
    }

    /** 디버그 빌드: 적용이 끝날 즈음 실제 값을 다시 읽어 로그로 남긴다 */
    fun logCurrentValuesLater() {
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        h.postDelayed(::logCurrentValues, 3_000)
        h.postDelayed(::logCurrentValues, 12_000)
    }

    /** 실제 차량 값을 다시 읽어 로그로 남긴다(적용 확인용) */
    fun logCurrentValues() {
        val err: (Exception) -> Unit = { e -> Log.i(TAG, "읽기 실패: ${e.message}") }
        val light = vehicle.getLight()
        light.checkAmbientLightSwitchCapabilities({ _, m ->
            frontOf(m.keys).forEach { a ->
                light.getAmbientLightSwitch(a, { v -> Log.i(TAG, "확인 조명 스위치 ${a.values.toList()} = $v") }, err)
                light.getAmbientLightBrightnessColor(a, { v -> Log.i(TAG, "확인 조명 색 ${a.values.toList()} = #%06X".format(v)) }, err)
                light.getAmbientLightBrightness(a, { v -> Log.i(TAG, "확인 조명 밝기 ${a.values.toList()} = $v") }, err)
            }
        }, err)
        val hvac = vehicle.getHVAC()
        hvac.checkHvacPowerCapabilities({ l: List<SeatArea> -> frontOf(l).forEach { a -> hvac.getHvacPowerState(a, { v: Boolean? -> Log.i(TAG, "확인 공조 전원 ${a.values.toList()} = $v") }, err) } }, err)
        hvac.checkHvacTemperatureCapabilities({ m: Map<SeatArea, AreaBounds<Float>> ->
            frontOf(m.keys).forEach { a -> hvac.getHvacTemperature(a, TemperatureUnit.CELSIUS, { v -> Log.i(TAG, "확인 설정 온도 ${a.values.toList()} = $v") }, err) }
        }, err)
        hvac.checkHvacFanSpeedCapabilities({ m: Map<SeatArea, AreaBounds<Int>> ->
            frontOf(m.keys).forEach { a -> hvac.getHvacFanSpeed(a, { v: Int? -> Log.i(TAG, "확인 풍량 ${a.values.toList()} = $v (범위 ${m[a]})") }, err) }
        }, err)
        hvac.checkHvacRecirculationCapabilities({ l: List<SeatArea> -> frontOf(l).forEach { a -> hvac.getHvacRecirculationState(a, { v: Boolean? -> Log.i(TAG, "확인 내기 순환 ${a.values.toList()} = $v") }, err) } }, err)
        val seat = vehicle.getSeat()
        seat.checkSeatForeAftPositionCapabilities({ _, m: Map<SeatArea, AreaBounds<Int>> ->
            frontOf(m.keys).forEach { a -> seat.getCurrentSeatForeAftPosition(a, { v -> Log.i(TAG, "확인 좌석 앞뒤 ${a.values.toList()} = $v (범위 ${m[a]})") }, err) }
        }, err)
        val window = vehicle.getWindow()
        window.checkWindowPositionCapabilities({ m: Map<WindowArea, AreaBounds<Int>> ->
            m.keys.filter { a -> a.values.any { it in frontWindowValues } }.forEach { a ->
                window.getCurrentWindowPosition(a, { v -> Log.i(TAG, "확인 창문 ${a.values.toList()} = $v (범위 ${m[a]})") }, err)
            }
        }, err)
    }

    /**
     * 앞좌석을 끝까지 앞으로 민다. 위치를 정하는 API가 없어 앞으로 움직이게 한 뒤
     * 위치가 더 바뀌지 않거나 [SEAT_MOVE_LIMIT_MS]가 지나면 멈춘다(끼임 방지는 차량이 한다).
     */
    fun applySeat(preset: CampPreset) {
        if (!preset.seatForward) {
            _state.update { it.copy(seat = ControlResult.Done) }
            return
        }
        _state.update { it.copy(seat = ControlResult.Working) }
        val seat = vehicle.getSeat()
        val onFail = fail("좌석") { copy(seat = it) }
        seat.checkMoveSeatForeAftCapabilities({ _, areas ->
            seatAreas = frontOf(areas)
            if (seatAreas.isEmpty()) return@checkMoveSeatForeAftCapabilities unsupported("좌석") { copy(seat = it) }
            all(seatAreas, onDone = { watchSeat(SystemClock.uptimeMillis(), null, 0) }, onFail = onFail) { a, ok, ko ->
                seat.moveSeatForeAft(a, MoveControl.FORWARD, { _, _ -> ok() }, ko)
            }
        }, onFail)
    }

    /** 첫 좌석 위치를 [SEAT_POLL_MS]마다 읽어 멈췄는지 본다 */
    private fun watchSeat(startedAt: Long, last: Int?, still: Int) {
        if (_state.value.seat != ControlResult.Working) return
        handler.postDelayed({
            val elapsed = SystemClock.uptimeMillis() - startedAt
            val finish = {
                sendSeatStop()
                done { copy(seat = it) }
            }
            if (elapsed > SEAT_MOVE_LIMIT_MS) return@postDelayed finish()
            val area = seatAreas.firstOrNull() ?: return@postDelayed finish()
            vehicle.getSeat().getCurrentSeatForeAftPosition(area, { pos ->
                // 움직이기 시작하는 데 시간이 걸릴 수 있어 처음 몇 초는 멈춘 것으로 보지 않는다
                val n = if (elapsed >= SEAT_START_GRACE_MS && pos != null && pos == last) still + 1 else 0
                if (n >= 2) finish() else watchSeat(startedAt, pos, n)
            }, { watchSeat(startedAt, last, still) })
        }, SEAT_POLL_MS)
    }

    /** 출발하면 움직이던 좌석을 바로 멈춘다(시트 제어는 주행 중 금지) */
    fun stopSeat() {
        if (_state.value.seat != ControlResult.Working) return
        sendSeatStop()
        _state.update { it.copy(seat = ControlResult.Partial("출발해서 멈췄어요")) }
    }

    private fun sendSeatStop() {
        val seat = vehicle.getSeat()
        seatAreas.forEach { area ->
            runCatching { seat.moveSeatForeAft(area, MoveControl.STOP, { _, _ -> }, { e -> Log.w(TAG, "좌석 멈춤 실패", e) }) }
        }
    }

    /** 영역마다 [action]을 보내고 모두 끝나면 [onDone], 하나라도 실패하면 [onFail] 한 번 */
    private fun <A> all(
        areas: List<A>,
        onDone: () -> Unit,
        onFail: (Exception) -> Unit,
        action: (A, ok: () -> Unit, ko: (Exception) -> Unit) -> Unit,
    ) {
        if (areas.isEmpty()) return onDone()
        val left = AtomicInteger(areas.size)
        val failed = AtomicBoolean(false)
        areas.forEach { a ->
            val ko: (Exception) -> Unit = { e -> if (failed.compareAndSet(false, true)) onFail(e) }
            runCatching { action(a, { if (left.decrementAndGet() == 0 && !failed.get()) onDone() }, ko) }
                .onFailure { e -> ko(e as? Exception ?: Exception(e)) }
        }
    }

    private fun unsupported(what: String, apply: CampControlState.(ControlResult) -> CampControlState) {
        Log.i(TAG, "$what: 지원 영역 없음")
        _state.update { it.apply(ControlResult.Unsupported("$what 지원 영역 없음")) }
    }

    private fun done(apply: CampControlState.(ControlResult) -> CampControlState) {
        _state.update { it.apply(ControlResult.Done) }
    }

    private fun fail(what: String, apply: CampControlState.(ControlResult) -> CampControlState): (Exception) -> Unit = { e ->
        Log.w(TAG, "$what 제어 실패: ${e.javaClass.simpleName} ${e.message}")
        _state.update { it.apply(ControlResult.Unsupported("$what: ${e.message ?: e.javaClass.simpleName}")) }
    }

    private companion object {
        const val TAG = "CampControl"
        const val SEAT_POLL_MS = 500L
        const val SEAT_MOVE_LIMIT_MS = 20_000L
        const val SEAT_START_GRACE_MS = 3_000L
        val CONTROL_PERMISSIONS = listOf(
            "pleos.car.permission.CONTROL_CAR_LIGHTS",
            "pleos.car.permission.CONTROL_CAR_CLIMATE",
            "pleos.car.permission.CONTROL_CAR_SEATS",
            "pleos.car.permission.CONTROL_CAR_WINDOWS",
        )
    }
}
