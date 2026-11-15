package kr.sanho.chabakine_pleos.ui.find

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapClusteringTest {
    private data class P(val name: String, val lat: Double, val lng: Double)

    private val places = listOf(
        P("주문진", 37.89, 128.82),
        P("아들바위", 37.90, 128.83),
        P("사천", 37.84, 128.87),
        P("속초", 38.20, 128.59),
    )

    private fun cluster(pxPerDeg: Double) = clusterPlaces(places, { it.lat }, { it.lng }, pxPerDeg, pxPerDeg, radiusXPx = 60.0)

    @Test
    fun 축소하면_모두_하나로_묶인다() {
        val result = cluster(pxPerDeg = 100.0)
        assertEquals(1, result.size)
        assertEquals(4, result.first().items.size)
    }

    @Test
    fun 중간이면_가까운_곳만_묶인다() {
        val result = cluster(pxPerDeg = 800.0)
        assertEquals(listOf(3, 1), result.map { it.items.size })
    }

    @Test
    fun 확대하면_모두_따로_나온다() {
        val result = cluster(pxPerDeg = 100_000.0)
        assertEquals(4, result.size)
        assertTrue(result.all { it.isSingle })
    }

    @Test
    fun 가로로_더_넓게_묶는다() {
        val side = listOf(P("왼쪽", 37.0, 128.0), P("오른쪽", 37.0, 128.1))
        val above = listOf(P("아래", 37.0, 128.0), P("위", 37.1, 128.0))
        // 1도 = 1000px → 둘 다 100px 떨어짐. 가로 반경 150, 세로 반경 60
        assertEquals(1, clusterPlaces(side, { it.lat }, { it.lng }, 1000.0, 1000.0, 150.0, 60.0).size)
        assertEquals(2, clusterPlaces(above, { it.lat }, { it.lng }, 1000.0, 1000.0, 150.0, 60.0).size)
    }

    @Test
    fun 묶음_위치는_첫_장소() {
        val pair = clusterPlaces(places.take(2), { it.lat }, { it.lng }, 100.0, 100.0, 60.0).single()
        assertEquals(37.89, pair.latitude, 1e-9)
        assertEquals(128.82, pair.longitude, 1e-9)
    }
}
