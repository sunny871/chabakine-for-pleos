package kr.sanho.chabakine_pleos.vehicle

import ai.pleos.playground.vehicle.constant.control.VehicleGear

/**
 * Pleos Vehicle SDK와 Android 표준 속성 양쪽에서 들어오는 같은 값을 하나로 고른다.
 * SDK 값이 있으면 그 값, SDK가 값을 못 주거나(지원 안 함 · 권한 없음 · 실패) 아직 안 왔으면 표준 속성 값.
 */
internal class SdkFirst<T : Any> {
    var sdk: T? = null
        private set
    var standard: T? = null
        private set

    val value: T? get() = sdk ?: standard

    /** 지금 [value]가 어디서 왔는지(로그용) */
    val source: String get() = when {
        sdk != null -> "Pleos SDK"
        standard != null -> "표준 속성"
        else -> "없음"
    }

    fun fromSdk(v: T?) { sdk = v }
    fun fromStandard(v: T?) { standard = v }
}

/**
 * SDK 기어를 표준 GEAR_SELECTION 값(VehicleGear 비트)으로 바꾼다. [VehicleSnapshot.drivingState]가 이 값을 그대로 본다.
 * GEAR_UNKNOWN · null은 "SDK가 모름"으로 보고 null을 돌려 표준 속성 값으로 돌아가게 한다.
 */
internal fun VehicleGear?.toStandardGear(): Int? = when (this) {
    null, VehicleGear.GEAR_UNKNOWN -> null
    VehicleGear.GEAR_NEUTRAL -> 0x0001
    VehicleGear.GEAR_REVERSE -> 0x0002
    VehicleGear.GEAR_PARK -> VehicleSnapshot.GEAR_PARK
    VehicleGear.GEAR_DRIVE -> 0x0008
    VehicleGear.GEAR_1 -> 0x0010
    VehicleGear.GEAR_2 -> 0x0020
    VehicleGear.GEAR_3 -> 0x0040
    VehicleGear.GEAR_4 -> 0x0080
    VehicleGear.GEAR_5 -> 0x0100
    VehicleGear.GEAR_6 -> 0x0200
    VehicleGear.GEAR_7 -> 0x0400
    VehicleGear.GEAR_8 -> 0x0800
    VehicleGear.GEAR_9 -> 0x1000
}
