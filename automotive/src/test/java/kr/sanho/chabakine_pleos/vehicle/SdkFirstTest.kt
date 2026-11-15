package kr.sanho.chabakine_pleos.vehicle

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SdkFirstTest {

    @Test
    fun sdkValueWinsOverStandard() {
        val f = SdkFirst<Int>()
        f.fromStandard(0x0008)
        f.fromSdk(VehicleSnapshot.GEAR_PARK)
        assertEquals(VehicleSnapshot.GEAR_PARK, f.value)
        assertEquals("Pleos SDK", f.source)
    }

    @Test
    fun fallsBackToStandardWhenSdkFails() {
        val f = SdkFirst<Int>()
        f.fromSdk(VehicleSnapshot.GEAR_PARK)
        f.fromStandard(0x0008)
        f.fromSdk(null) // SDK 실패
        assertEquals(0x0008, f.value)
        assertEquals("표준 속성", f.source)
    }

    @Test
    fun unknownWhenNeither() {
        assertNull(SdkFirst<Float>().value)
    }

    @Test
    fun sdkGearMapsToStandardBits() {
        assertEquals(VehicleSnapshot.GEAR_PARK, VehicleGear.GEAR_PARK.toStandardGear())
        assertEquals(0x0008, VehicleGear.GEAR_DRIVE.toStandardGear())
        assertEquals(0x0002, VehicleGear.GEAR_REVERSE.toStandardGear())
        assertEquals(0x0001, VehicleGear.GEAR_NEUTRAL.toStandardGear())
        assertNull(VehicleGear.GEAR_UNKNOWN.toStandardGear())
        assertNull((null as VehicleGear?).toStandardGear())
    }

    @Test
    fun drivingStateSameWhetherGearFromSdkOrStandard() {
        for ((sdk, std) in listOf(VehicleGear.GEAR_PARK to VehicleSnapshot.GEAR_PARK, VehicleGear.GEAR_DRIVE to 0x0008)) {
            val viaSdk = VehicleSnapshot(speedMps = 0f, gear = sdk.toStandardGear()).drivingState
            val viaStd = VehicleSnapshot(speedMps = 0f, gear = std).drivingState
            assertEquals(viaStd, viaSdk)
        }
        // SDK가 모르는 기어면 표준 값으로 돌아가므로 판단이 바뀌지 않는다
        val f = SdkFirst<Int>()
        f.fromStandard(VehicleSnapshot.GEAR_PARK)
        f.fromSdk(VehicleGear.GEAR_UNKNOWN.toStandardGear())
        assertEquals(DrivingState.PARKED, VehicleSnapshot(speedMps = 0f, gear = f.value).drivingState)
    }
}
