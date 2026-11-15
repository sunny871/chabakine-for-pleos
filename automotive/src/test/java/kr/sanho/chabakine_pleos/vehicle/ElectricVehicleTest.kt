package kr.sanho.chabakine_pleos.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 차박 모드는 순수 전기차에서만: 연료 종류가 전기 하나일 때만 true */
class ElectricVehicleTest {

    private fun of(vararg types: Int) = VehicleSnapshot(fuelTypes = types.toList())

    @Test
    fun onlyPureElectricCounts() {
        assertEquals(true, of(VehicleSnapshot.FUEL_ELECTRIC).isElectric)
        // 하이브리드(휘발유 1 + 전기)와 내연기관은 아니다
        assertEquals(false, of(1, VehicleSnapshot.FUEL_ELECTRIC).isElectric)
        assertEquals(false, of(1).isElectric)
    }

    @Test
    fun unknownUntilRead() {
        assertNull(VehicleSnapshot().isElectric)
    }
}
