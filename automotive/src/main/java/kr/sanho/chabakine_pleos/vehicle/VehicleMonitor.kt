package kr.sanho.chabakine_pleos.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.EvBatteryLevelListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 차량 표준 속성 값. 읽지 못한 값은 null이다. */
data class VehicleSnapshot(
    val speedMps: Float? = null,
    val gear: Int? = null,
    val parkingBrakeOn: Boolean? = null,
    val fuelTypes: List<Int>? = null,
    val evCapacityWh: Float? = null,
    val evLevelWh: Float? = null,
    /** Pleos Vehicle SDK 충전율(%). 계기판 · Virtual Console과 같은 값이라 [evLevelWh] 비율보다 먼저 쓴다 */
    val evSocPercent: Int? = null,
    val rangeMeters: Float? = null,
    val outsideTempC: Float? = null,
    /** 시스템(CarUxRestrictions)이 주행 방해 최적화를 요구하는지. 앱 판단보다 우선한다. */
    val uxRequiresDistractionOptimization: Boolean? = null,
    /** 주행 중 목록에 보여도 되는 최대 개수(UX_RESTRICTIONS_LIMIT_CONTENT일 때). 없으면 null */
    val uxMaxListItems: Int? = null,
    /** 시스템이 내려 준 제한 항목(CarUxRestrictions.getActiveRestrictions 비트). 모르면 0 */
    val uxActiveRestrictions: Int = 0,
    /** 차량 내 조명 모드(NIGHT_MODE). 테마 "자동"에서 시스템 값이 없을 때 쓴다. */
    val nightMode: Boolean? = null,
) {
    /**
     * 속도 0 + P단이면 정차. 둘 중 하나라도 모르면 주행으로 간주한다.
     * 주차 브레이크는 판단에 쓰지 않는다(Pleos 에뮬레이터에서 조작할 수 없고 차종마다 다를 수 있다).
     * 읽히면 [parkingBrakeOn]으로 화면에 참고로만 보여 준다.
     */
    val drivingState: DrivingState
        get() {
            if (speedMps == null || gear == null) return DrivingState.UNKNOWN
            val parked = kotlin.math.abs(speedMps) < 0.3f && gear == GEAR_PARK
            return if (parked) DrivingState.PARKED else DrivingState.DRIVING
        }

    /**
     * 화면이 보는 최종 상태(화면설계서 F4).
     * 시스템이 주행 제한을 요구하면 무조건 주행. 차량 값을 못 읽을 때만 사용자의 "주차했어요"(06-A)를 받아들인다.
     */
    fun effectiveState(manualParked: Boolean): DrivingState = when {
        uxRequiresDistractionOptimization == true -> DrivingState.DRIVING
        drivingState != DrivingState.UNKNOWN -> drivingState
        manualParked -> DrivingState.PARKED
        else -> DrivingState.UNKNOWN
    }

    /**
     * 순수 전기차인지(연료 종류가 전기 하나뿐). 하이브리드 · 내연기관은 false, 아직 못 읽었으면 null.
     * 차박 모드는 전기차에서만 연다(밤새 배터리로 공조·조명을 쓰는 기능이라).
     */
    val isElectric: Boolean?
        get() = fuelTypes?.let { FUEL_ELECTRIC in it && it.size == 1 }

    /** 전기차 배터리(05 배터리 카드). 차박 모드는 전기차 전용이라 전기차가 아니거나 값을 모르면 null → 카드를 숨긴다. */
    val energy: Energy?
        get() {
            if (isElectric != true) return null
            // 충전율(Pleos)이 있으면 그 값, 없으면 표준 잔량(Wh) ÷ 용량. 에뮬레이터에서 둘이 따로 움직여(잔량 72% · 계기판 16%) 계기판과 같은 쪽을 먼저 쓴다
            val percent = evSocPercent?.toFloat()
                ?: if (evLevelWh != null && evCapacityWh != null && evCapacityWh > 0f) evLevelWh / evCapacityWh * 100f else return null
            return Energy(percent.coerceIn(0f, 100f), rangeMeters?.let { it / 1000f })
        }

    companion object {
        /** VehicleGear.GEAR_PARK */
        const val GEAR_PARK = 0x0004

        /** FuelType.ELECTRIC */
        const val FUEL_ELECTRIC = 10
    }
}

enum class DrivingState { PARKED, DRIVING, UNKNOWN }

/** 전기차 배터리 잔량과 주행 가능 거리 */
data class Energy(val batteryPercent: Float, val rangeKm: Float?)

/**
 * 차량 값을 읽는다. 제3자 앱은 읽기만 한다.
 * - 기어 · 배터리 용량 · 잔량 · 충전율 · 주행 가능 거리: Pleos Vehicle SDK를 먼저 쓰고,
 *   SDK가 값을 못 주면(지원 안 함 · 권한 없음 · 실패) AAOS 표준 속성([CarPropertyManager]) 값으로 돌아간다([SdkFirst]).
 * - 속도 · 주차 브레이크 · 연료 종류 · 바깥 기온 · 야간 모드 · 주행 제한: SDK에 읽는 API가 없어 표준 속성만 쓴다.
 * 권한이 없거나 차량이 지원하지 않는 값은 null로 남겨 두고 그 기능만 숨긴다.
 */
@Suppress("DEPRECATION") // SDK 기어 · 잔량 · 용량 · 주행 거리 API는 2.3.2에서 deprecated(동작은 함). 실패하면 표준 속성으로 돌아간다
class VehicleMonitor(private val context: Context) {

    private val _snapshot = MutableStateFlow(VehicleSnapshot())
    val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var uxManager: CarUxRestrictionsManager? = null

    // 같은 값을 SDK와 표준 속성 양쪽에서 받아 SDK 쪽을 먼저 쓴다
    private val gear = SdkFirst<Int>()
    private val evCapacity = SdkFirst<Float>()
    private val evLevel = SdkFirst<Float>()
    private val range = SdkFirst<Float>()

    /** [field]에 새 값을 넣고 고른 값을 스냅샷에 반영한다. 값의 출처가 바뀌면 로그를 남긴다 */
    private fun <T : Any> merge(
        field: SdkFirst<T>,
        name: String,
        change: SdkFirst<T>.() -> Unit,
        put: VehicleSnapshot.(T?) -> VehicleSnapshot,
    ) = synchronized(field) {
        val before = field.source
        val old = field.value
        field.change()
        if (field.source != before) Log.i(TAG, "$name 출처: $before → ${field.source}")
        val v = field.value
        // 기어는 정차 판단에 바로 쓰여 바뀔 때마다 남긴다(시연 · 점검용)
        if (field === gear && v != old) Log.i(TAG, "기어 $old → $v (${field.source})")
        _snapshot.update { it.put(v) }
    }

    private fun sdkGear(g: VehicleGear?) = merge(gear, "기어", { fromSdk(g.toStandardGear()) }) { copy(gear = it) }
    private fun sdkCapacity(v: Float?) = merge(evCapacity, "배터리 용량", { fromSdk(v?.takeIf { it > 0f }) }) { copy(evCapacityWh = it) }
    private fun sdkLevel(v: Float?) = merge(evLevel, "배터리 잔량", { fromSdk(v?.takeIf { it >= 0f }) }) { copy(evLevelWh = it) }
    private fun sdkRange(v: Float?) = merge(range, "주행 가능 거리", { fromSdk(v?.takeIf { it >= 0f }) }) { copy(rangeMeters = it) }

    // Pleos Vehicle SDK(읽기만): 기어(pleos CAR_DRIVE), 충전율 · 잔량(pleos CAR_ENERGY), 용량(pleos CAR_INFO), 주행 가능 거리(pleos CAR_DRIVE)
    private var pleos: Vehicle? = null
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = sdkGear(gear)
        override fun onFailed(exception: Exception) {
            Log.i(TAG, "SDK 기어 구독 오류: ${exception.javaClass.simpleName}")
            sdkGear(null)
        }
    }
    private val levelListener = object : EvBatteryLevelListener {
        override fun onEvBatteryLevelUpdated(level: Float?) = sdkLevel(level)
        override fun onFailed(exception: Exception) {
            Log.i(TAG, "SDK 배터리 잔량 구독 오류: ${exception.javaClass.simpleName}")
            sdkLevel(null)
        }
    }
    private val rangeListener = object : RangeRemainingListener {
        override fun onRangeRemainingChanged(rangeRemaining: Float?) = sdkRange(rangeRemaining)
        override fun onFailed(exception: Exception) {
            Log.i(TAG, "SDK 주행 가능 거리 구독 오류: ${exception.javaClass.simpleName}")
            sdkRange(null)
        }
    }
    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
            _snapshot.update { it.copy(evSocPercent = value?.takeIf { v -> v in 0..100 }) }
        }

        override fun onFailed(exception: Exception) {
            Log.i(TAG, "충전율 구독 오류: ${exception.javaClass.simpleName}")
        }
    }

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.status == CarPropertyValue.STATUS_AVAILABLE) apply(value.propertyId, value.value)
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "속성 오류 ${VehiclePropertyIds.toString(propertyId)}")
        }
    }

    fun start() {
        if (car != null) return
        startPleos()
        val connected = runCatching { Car.createCar(context) }.getOrNull() ?: return
        car = connected
        val manager = connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return
        properties = manager

        // 한 번만 읽는 차량 정보
        read(manager, VehiclePropertyIds.INFO_FUEL_TYPE)
        read(manager, VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)

        listOf(
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            VehiclePropertyIds.GEAR_SELECTION,
            VehiclePropertyIds.PARKING_BRAKE_ON,
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            VehiclePropertyIds.RANGE_REMAINING,
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE,
            VehiclePropertyIds.NIGHT_MODE,
        ).forEach { subscribe(manager, it) }

        // 주행 제한은 시스템이 알려 주는 값을 따른다(권한 필요 없음)
        val ux = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        uxManager = ux
        ux?.let { m ->
            runCatching {
                applyUx(m.currentCarUxRestrictions)
                m.registerListener { applyUx(it) }
            }.onFailure { Log.i(TAG, "주행 제한 구독 실패: ${it.javaClass.simpleName}") }
        }
    }

    private fun applyUx(restrictions: CarUxRestrictions?) {
        val limit = restrictions
            ?.takeIf { it.activeRestrictions and CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT != 0 }
            ?.maxCumulativeContentItems
            ?.takeIf { it > 0 }
        _snapshot.update {
            it.copy(
                uxRequiresDistractionOptimization = restrictions?.isRequiresDistractionOptimization,
                uxMaxListItems = limit,
                uxActiveRestrictions = restrictions?.takeIf { r -> r.isRequiresDistractionOptimization }?.activeRestrictions ?: 0,
            )
        }
    }

    private fun startPleos() {
        val v = runCatching { Vehicle(context.applicationContext).also { it.initialize() } }
            .onFailure { Log.i(TAG, "Pleos Vehicle SDK 초기화 실패: ${it.javaClass.simpleName}") }
            .getOrNull() ?: return
        pleos = v
        runCatching {
            val battery = v.getEvBattery()
            battery.getEvBatteryStateOfCharge({ pct -> socListener.onEvBatteryStateOfChargeUpdated(pct) }, { e -> Log.i(TAG, "충전율 읽기 실패: ${e.javaClass.simpleName}") })
            battery.registerEvBatteryStateOfCharge(socListener)
        }.onFailure { Log.i(TAG, "충전율 구독 실패: ${it.javaClass.simpleName}") }

        // 아래 값들은 SDK에 check…Capabilities가 없어 첫 읽기로 지원 여부를 본다. 실패하면 표준 속성 값이 남는다
        sdkTry("기어") {
            val g = v.getGear()
            g.getCurrentGearSelectionState(::sdkGear) { e -> sdkFailed("기어", e); sdkGear(null) }
            g.registerGearSelection(gearListener)
        }
        sdkTry("배터리 용량") {
            v.getEvBattery().getEvBatteryCapacity(::sdkCapacity) { e -> sdkFailed("배터리 용량", e); sdkCapacity(null) }
        }
        sdkTry("배터리 잔량") {
            val battery = v.getEvBattery()
            battery.getEvBatteryLevel(::sdkLevel) { e -> sdkFailed("배터리 잔량", e); sdkLevel(null) }
            battery.registerEvBatteryLevel(levelListener)
        }
        sdkTry("주행 가능 거리") {
            val odometer = v.getOdometer()
            odometer.getRangeRemainingDistance(::sdkRange) { e -> sdkFailed("주행 가능 거리", e); sdkRange(null) }
            odometer.registerRangeRemainingListener(rangeListener)
        }
    }

    private inline fun sdkTry(name: String, block: () -> Unit) {
        runCatching(block).onFailure { Log.i(TAG, "SDK $name 구독 실패(표준 속성 사용): ${it.javaClass.simpleName}") }
    }

    private fun sdkFailed(name: String, e: Exception) = Log.i(TAG, "SDK $name 읽기 실패(표준 속성 사용): ${e.javaClass.simpleName}")

    fun stop() {
        pleos?.let { v ->
            runCatching { v.getEvBattery().unregisterEvBatteryStateOfCharge(socListener) }
            runCatching { v.getGear().unregisterGearSelection(gearListener) }
            runCatching { v.getEvBattery().unregisterEvBatteryLevel(levelListener) }
            runCatching { v.getOdometer().unregisterRangeRemainingListener(rangeListener) }
            runCatching { v.release() }
        }
        pleos = null
        uxManager?.let { runCatching { it.unregisterListener() } }
        uxManager = null
        properties?.let { runCatching { it.unregisterCallback(callback) } }
        properties = null
        car?.disconnect()
        car = null
    }

    private fun read(manager: CarPropertyManager, propertyId: Int) {
        if (manager.getCarPropertyConfig(propertyId) == null) {
            Log.i(TAG, "읽을 수 없음(설정 없음·권한 없음) ${VehiclePropertyIds.toString(propertyId)}")
            return
        }
        runCatching { manager.getProperty<Any>(propertyId, 0) }
            .onSuccess { if (it.status == CarPropertyValue.STATUS_AVAILABLE) apply(propertyId, it.value) }
            .onFailure { Log.i(TAG, "읽기 실패 ${VehiclePropertyIds.toString(propertyId)}: ${it.javaClass.simpleName}") }
    }

    private fun subscribe(manager: CarPropertyManager, propertyId: Int) {
        val config = manager.getCarPropertyConfig(propertyId) ?: return
        val rate = if (config.maxSampleRate > 0f) {
            CarPropertyManager.SENSOR_RATE_NORMAL.coerceIn(config.minSampleRate, config.maxSampleRate)
        } else {
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        }
        runCatching { manager.registerCallback(callback, propertyId, rate) }
            .onFailure { Log.i(TAG, "구독 실패 ${VehiclePropertyIds.toString(propertyId)}: ${it.javaClass.simpleName}") }
    }

    private fun apply(propertyId: Int, value: Any?) {
        // SDK와 겹치는 값은 표준 쪽 값으로 저장해 두고, SDK 값이 없을 때만 쓴다
        when (propertyId) {
            VehiclePropertyIds.GEAR_SELECTION -> return merge(gear, "기어", { fromStandard(value as? Int) }) { copy(gear = it) }
            VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> return merge(evCapacity, "배터리 용량", { fromStandard(value as? Float) }) { copy(evCapacityWh = it) }
            VehiclePropertyIds.EV_BATTERY_LEVEL -> return merge(evLevel, "배터리 잔량", { fromStandard(value as? Float) }) { copy(evLevelWh = it) }
            VehiclePropertyIds.RANGE_REMAINING -> return merge(range, "주행 가능 거리", { fromStandard(value as? Float) }) { copy(rangeMeters = it) }
        }
        _snapshot.update { s ->
            when (propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> s.copy(speedMps = value as? Float)
                VehiclePropertyIds.PARKING_BRAKE_ON -> s.copy(parkingBrakeOn = value as? Boolean)
                VehiclePropertyIds.INFO_FUEL_TYPE -> s.copy(fuelTypes = (value as? Array<*>)?.filterIsInstance<Int>()).also {
                    Log.i(TAG, "연료 종류 ${it.fuelTypes} · 전기차 ${it.isElectric}")
                }
                VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> s.copy(outsideTempC = value as? Float)
                VehiclePropertyIds.NIGHT_MODE -> s.copy(nightMode = value as? Boolean)
                else -> s
            }
        }
    }

    private companion object {
        const val TAG = "VehicleMonitor"
    }
}
