package kr.sanho.chabakine_pleos.ui.find

import kr.sanho.chabakine_pleos.ui.components.BackChevron
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PresetChip
import kr.sanho.chabakine_pleos.ui.components.RoundIcon
import kr.sanho.chabakine_pleos.ui.components.VoiceButton
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.theme.Palette

/**
 * 화면설계서 02-1 검색(정차 중에만). 차의 키보드로 장소 이름·지역을 찾는다.
 * 출발하면 이 화면은 닫히고(정차 전용) 음성 검색만 남는다.
 */
@Composable
fun SearchScreen(app: AppState, items: List<PlaceItem>, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var recents by remember { mutableStateOf(app.prefs.recentSearches) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = query.trim()
    val results = if (q.isEmpty()) emptyList() else items.filter { it.matchesQuery(q) }.sortedBy { it.distanceKm }

    fun open(item: PlaceItem) {
        if (q.isNotEmpty()) app.prefs.addRecentSearch(q)
        app.selectedPlaceId = item.place.id
        app.replace(Route.Find())
    }

    /** "검색": 02 목록을 이 검색어로 거르고 가장 가까운 곳을 펼친다(목록 위 칩으로 풀 수 있다) */
    fun submit() {
        val first = results.firstOrNull() ?: return app.notice("\"$q\"에 맞는 장소가 없어요")
        app.prefs.addRecentSearch(q)
        app.filter = app.filter.copy(query = q)
        app.selectedPlaceId = first.place.id
        app.replace(Route.Find())
    }

    Column(modifier.fillMaxSize().padding(start = 36.dp, end = 36.dp, top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BackChevron("검색 닫기") { app.back() }
            Row(
                Modifier
                    .weight(1f)
                    .height(88.dp)
                    .card(30.dp)
                    .border(3.dp, Palette.Accent, RoundedCornerShape(30.dp))
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LineIconView(LineIcon.Search, 40.dp, Palette.SubText)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Label("장소 이름이나 지역", 36, FontWeight.Bold, Palette.MutedText)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(Palette.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) RoundIcon(LineIcon.Cross, "지우기", 72.dp, 32.dp, Palette.Sub, { query = "" })
            }
            VoiceButton(app.listening) { app.startVoiceSearch() }
        }
        if (recents.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Label("최근 검색", 24, FontWeight.Bold, Palette.SubText)
                recents.forEach { r -> PresetChip(r, false, { query = r }) }
                PresetChip("지우기", false, {
                    app.prefs.recentSearches = emptyList()
                    recents = emptyList()
                })
            }
        }
        Label("키보드 입력은 정차 중에만 열려요. 출발하면 입력창이 닫히고 음성 검색만 남아요.", 22, FontWeight.Medium, Palette.MutedText)
        when {
            q.isEmpty() -> Unit
            results.isEmpty() -> Label("\"$q\"에 맞는 장소가 없어요", 30, FontWeight.Bold, Palette.SubText)
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(results, key = { it.place.id }) { item -> PlaceRow(item, selected = false, parked = true) { open(item) } }
            }
        }
    }
}

