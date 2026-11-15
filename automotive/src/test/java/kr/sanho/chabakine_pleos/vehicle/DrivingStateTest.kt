package kr.sanho.chabakine_pleos.vehicle

import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot.Companion.GEAR_PARK
import org.junit.Assert.assertEquals
import org.junit.Test

class DrivingStateTest {

    @Test
    fun parkedWithSpeedZeroAndParkEvenIfBrakeReleased() {
        val s = VehicleSnapshot(speedMps = 0f, gear = GEAR_PARK, parkingBrakeOn = false)
        assertEquals(DrivingState.PARKED, s.drivingState)
    }

    @Test
    fun parkedWhenBrakeUnreadable() {
        val s = VehicleSnapshot(speedMps = 0f, gear = GEAR_PARK, parkingBrakeOn = null)
        assertEquals(DrivingState.PARKED, s.drivingState)
    }

    @Test
    fun drivingWhenNotInPark() {
        val s = VehicleSnapshot(speedMps = 0f, gear = 0x0008, parkingBrakeOn = true)
        assertEquals(DrivingState.DRIVING, s.drivingState)
    }

    @Test
    fun drivingWhenMoving() {
        val s = VehicleSnapshot(speedMps = 5f, gear = GEAR_PARK)
        assertEquals(DrivingState.DRIVING, s.drivingState)
    }

    @Test
    fun systemRestrictionWinsOverParked() {
        val s = VehicleSnapshot(speedMps = 0f, gear = GEAR_PARK, uxRequiresDistractionOptimization = true)
        assertEquals(DrivingState.DRIVING, s.effectiveState(manualParked = false))
    }

    @Test
    fun manualParkedOnlyWhenUnknown() {
        assertEquals(DrivingState.PARKED, VehicleSnapshot().effectiveState(manualParked = true))
        assertEquals(DrivingState.UNKNOWN, VehicleSnapshot().effectiveState(manualParked = false))
        val driving = VehicleSnapshot(speedMps = 8f, gear = 0x0008)
        assertEquals(DrivingState.DRIVING, driving.effectiveState(manualParked = true))
        val restricted = VehicleSnapshot(uxRequiresDistractionOptimization = true)
        assertEquals(DrivingState.DRIVING, restricted.effectiveState(manualParked = true))
    }

    @Test
    fun unknownWhenSpeedOrGearMissing() {
        assertEquals(DrivingState.UNKNOWN, VehicleSnapshot(gear = GEAR_PARK).drivingState)
        assertEquals(DrivingState.UNKNOWN, VehicleSnapshot(speedMps = 0f).drivingState)
    }
}
