package kr.sanho.chabakine_pleos.ui.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.data.PlacesState

/** 상단 데이터 띠 문구: 지금 쓰는 장소 데이터 버전과 마지막 확인 날짜. 번들이면 버전 대신 "앱 내장본" */
fun dataBannerText(state: PlacesState): String = listOfNotNull(
    state.version?.let { "장소 데이터 v$it" } ?: "장소 데이터 앱 내장본",
    state.checkedAtMillis?.let { "마지막 확인 ${DateFormat.format("yyyy-MM-dd", it)}" },
    "실제 규정과 다를 수 있어요",
).joinToString(" · ")

/** 장소 데이터 상태를 직접 읽어 띠를 그린다(문구를 넘겨받지 않는 화면용) */
@Composable
fun DataBanner(app: AppState, modifier: Modifier = Modifier) {
    val state by app.places.state.collectAsState()
    DemoBanner(dataBannerText(state), modifier)
}
