package kr.sanho.chabakine_pleos.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kr.sanho.chabakine_pleos.shared.place.Place

/** 화면에 보여 줄 장소 목록과 데이터 상태(화면설계서 24 오프라인·데이터 상태). */
data class PlacesState(
    val places: List<Place>,
    val source: PlaceCatalog.Source,
    /** 받아 둔 장소 데이터 버전. 앱에 넣어 둔 번들이면 null */
    val version: Int?,
    val checkedAtMillis: Long?,
    val refreshing: Boolean = false,
    /** 마지막 갱신이 서버에 닿지 못해 실패했는지 */
    val offline: Boolean = false,
)

class PlacesRepository(private val catalog: PlaceCatalog) {

    private val _state = MutableStateFlow(catalog.loadCachedOrBundled().toState())
    val state: StateFlow<PlacesState> = _state.asStateFlow()

    suspend fun refresh(force: Boolean = false) {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        val result = catalog.refresh(force)
        val offline = result is PlaceCatalog.RefreshResult.Failed && result.offline
        // 새로 받았든 아니든 저장본을 다시 읽는다(확인 시각이 바뀐다)
        _state.value = catalog.loadCachedOrBundled().toState().copy(offline = offline)
    }

    /** 서버를 바꿨다: 지난 서버의 저장본을 지우고 새 서버에서 처음부터 받는다 */
    suspend fun switchServer() {
        catalog.clearCache()
        _state.value = catalog.loadCachedOrBundled().toState()
        refresh(force = true)
    }

    private fun PlaceCatalog.Loaded.toState() = PlacesState(places, source, version, checkedAtMillis)
}
