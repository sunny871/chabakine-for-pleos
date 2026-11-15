package kr.sanho.chabakine_pleos.ui.find

/** 지도에서 한 마커로 묶인 장소들. 위치는 묶음의 기준(첫) 장소라 마커끼리 반경만큼 떨어진다 */
data class MapCluster<T>(val items: List<T>, val latitude: Double, val longitude: Double) {
    val isSingle: Boolean get() = items.size == 1
}

/**
 * 화면에서 가로 [radiusXPx]·세로 [radiusYPx] 타원 안에 드는 장소를 하나로 묶는다(iOS 앱 지도와 같은 방식).
 * 마커가 가로로 긴 신호등이라 가로를 더 넓게 본다.
 * 확대 단계마다 1도가 몇 px인지([pxPerDegLat]·[pxPerDegLng])만 바꿔 부르면 되고, 지도를 옮겨도 묶음이 바뀌지 않는다.
 * 먼저 온 장소가 묶음의 기준이 되므로 중요한 장소를 앞에 둔다.
 */
fun <T> clusterPlaces(
    items: List<T>,
    latitude: (T) -> Double,
    longitude: (T) -> Double,
    pxPerDegLat: Double,
    pxPerDegLng: Double,
    radiusXPx: Double,
    radiusYPx: Double = radiusXPx,
): List<MapCluster<T>> {
    class Group(val anchor: T, val anchorX: Double, val anchorY: Double) {
        val members = mutableListOf<T>()
    }
    val groups = mutableListOf<Group>()
    items.forEach { item ->
        val x = longitude(item) * pxPerDegLng
        val y = latitude(item) * pxPerDegLat
        val near = groups.firstOrNull {
            val dx = (it.anchorX - x) / radiusXPx
            val dy = (it.anchorY - y) / radiusYPx
            dx * dx + dy * dy < 1
        }
        (near ?: Group(item, x, y).also { groups += it }).members += item
    }
    return groups.map { g -> MapCluster(g.members, latitude(g.anchor), longitude(g.anchor)) }
}
