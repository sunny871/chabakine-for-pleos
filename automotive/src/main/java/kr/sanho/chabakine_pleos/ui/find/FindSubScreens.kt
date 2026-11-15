package kr.sanho.chabakine_pleos.ui.find

import kr.sanho.chabakine_pleos.util.Josa
import kr.sanho.chabakine_pleos.ui.components.BackChevron
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.ReviewPage
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.ui.components.DemoBanner
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.PresetChip
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.RoundIcon
import kr.sanho.chabakine_pleos.ui.components.ScreenHeader
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.ui.theme.label

private enum class FavoriteSort(val label: String) { NEAR("가까운 순"), SAVED("저장한 순"), SIGNAL("신호 순") }

private val SIGNAL_ORDER = listOf(Signal.GREEN, Signal.YELLOW, Signal.UNKNOWN, Signal.RED)

/** 화면설계서 09 즐겨찾기·주행 중(목록과 안내만) / 09-1 정차 중(정렬·상세·빼기와 되돌리기) */
@Composable
fun FavoritesScreen(
    app: AppState,
    items: List<PlaceItem>,
    favorites: Set<String>,
    parked: Boolean,
    settings: Settings,
    bannerText: String,
    modifier: Modifier = Modifier,
) {
    var sort by remember { mutableStateOf(FavoriteSort.NEAR) }
    // 방금 뺀 장소와 원래 저장 시각(되돌리기용)
    var removed by remember { mutableStateOf<Pair<PlaceItem, Long?>?>(null) }
    LaunchedEffect(removed) {
        if (removed != null) {
            delay(UNDO_MS)
            removed = null
        }
    }
    val activeSort = if (parked) sort else FavoriteSort.NEAR
    val saved = items.filter { it.place.id in favorites }.let { list ->
        when (activeSort) {
            FavoriteSort.NEAR -> list.sortedBy { it.distanceKm }
            FavoriteSort.SAVED -> list.sortedByDescending { app.favorites.savedAt(it.place.id) ?: 0L }
            FavoriteSort.SIGNAL -> list.sortedWith(compareBy({ SIGNAL_ORDER.indexOf(it.signal) }, { it.distanceKm }))
        }
    }
    Column(modifier.fillMaxSize()) {
        val subtitle = if (parked) "이 기기에 저장한 후보 · ${settings.primaryStyle.label} 기준"
        else "이 기기에 저장한 후보 · 가까운 순 · ${settings.primaryStyle.label} 기준"
        ScreenHeader("즐겨찾기", subtitle) {
            StatusPill(parked)
            if (parked) RoundIcon(LineIcon.Search, "검색", 88.dp, 42.dp, Palette.Sub, { app.navigate(Route.Search) })
        }
        DemoBanner(bannerText, Modifier.padding(horizontal = 36.dp))
        if (parked) {
            Row(
                Modifier.padding(start = 36.dp, end = 36.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Label("정렬", 26, FontWeight.Bold, Palette.SubText)
                // 01 · 02 칩 줄과 같은 높이(76)
                FavoriteSort.entries.forEach { s -> PresetChip(s.label, s == sort, { sort = s }, height = 76.dp) }
            }
        }
        Column(
            Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = if (parked) 20.dp else 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (saved.isEmpty()) {
                Column(Modifier.fillMaxWidth().weight(1f).card().padding(48.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    Label("저장한 후보가 없어요", 44, FontWeight.ExtraBold)
                    Label("찾기나 홈에서 별을 눌러 저장하세요", 30, FontWeight.Medium, Palette.SubText)
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(saved, key = { it.place.id }) { item -> FavoriteRow(app, item, parked) { at -> removed = item to at } }
                }
            }
            removed?.let { (item, at) ->
                Row(
                    Modifier.fillMaxWidth().height(112.dp).card(28.dp, Palette.Sub).padding(start = 32.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Label("${Josa.eulReul(item.place.name)} 즐겨찾기에서 뺐어요", 28, FontWeight.Bold, modifier = Modifier.weight(1f))
                    SecondaryButton("되돌리기", {
                        app.favorites.add(item.place.id, at ?: System.currentTimeMillis())
                        removed = null
                    }, height = 96.dp, textSize = 28, background = Palette.Accent, foreground = Palette.OnAccent)
                }
            }
            Label(
                if (parked) "정차 중에는 정렬, 상세 보기, 빼기를 할 수 있어요. 빼기는 되돌리기 안내를 잠깐 띄워요. 폰 앱 즐겨찾기 연동은 계정 연결 이후 단계예요."
                else "주행 중에는 목록과 안내만 보여요. 삭제와 정렬은 정차 중에 할 수 있어요. 폰 앱 즐겨찾기 연동은 계정 연결 이후 단계예요.",
                24, FontWeight.Medium, Palette.MutedText, maxLines = 2,
            )
        }
    }
}

@Composable
private fun FavoriteRow(app: AppState, item: PlaceItem, parked: Boolean, onRemoved: (Long?) -> Unit) {
    DarkScope(item.place.photoUrls.isNotEmpty()) {
    Box(Modifier.fillMaxWidth().height(if (parked) 176.dp else 196.dp).card(28.dp)) {
        RowPhoto(item.place.photoUrls.firstOrNull(), driving = !parked)
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (parked) 28.dp else 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (parked) 24.dp else 28.dp),
        ) {
            SignalLamp(item.signal, 40.dp, showSymbol = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(item.place.name, if (parked) 36 else 38, FontWeight.ExtraBold)
                val savedAt = if (parked) app.favorites.savedAt(item.place.id) else null
                val sub = listOfNotNull(
                    item.signal.label,
                    formatKm(item.distanceKm),
                    item.rating?.let { "★ ${it.average}" },
                    savedAt?.let { "저장 ${DateFormat.format("MM-dd", it)}" },
                ).joinToString(" · ")
                Label(sub, 26, FontWeight.Medium, if (parked) Palette.SoftText else Palette.SubText)
            }
            if (parked) {
                SecondaryButton("상세", {
                    app.selectedPlaceId = item.place.id
                    app.replace(Route.Find())
                }, Modifier.width(200.dp), height = 104.dp, textSize = 30)
                PrimaryButton("안내", { app.navigateTo(item.place) }, Modifier.width(200.dp), height = 104.dp, textSize = 32, icon = null, trailingChevron = false)
                RoundIcon(LineIcon.Trash, "즐겨찾기에서 빼기", 104.dp, 44.dp, Palette.Sub, {
                    val at = app.favorites.savedAt(item.place.id)
                    app.favorites.remove(item.place.id)
                    onRemoved(at)
                })
            } else {
                PrimaryButton("안내", { app.navigateTo(item.place) }, Modifier.width(280.dp), height = 116.dp, textSize = 34, icon = null, trailingChevron = false)
            }
        }
    }
    }
}

private const val UNDO_MS = 5_000L

/** 화면설계서 21 리뷰 전체(정차 중). 리뷰 쓰기는 폰 앱에서 한다. */
@Composable
fun ReviewListScreen(app: AppState, placeId: String, modifier: Modifier = Modifier) {
    val place = app.place(placeId) ?: return
    val page by produceState<ReviewPage?>(null, placeId) { value = app.reviews.reviews(placeId) }
    var visitedOnly by remember { mutableStateOf(false) }
    val reviews = page?.items.orEmpty().let { list -> if (visitedOnly) list.filter { it.visited } else list }
    Column(modifier.fillMaxSize()) {
        BackHeader(app, "리뷰 · ${place.name}", "리뷰 쓰기는 폰 앱에서 해요") {
            StatusPill(parked = true)
            SecondaryButton("읽어 주기", { app.speak(reviewSpeech(place, page)) }, height = 108.dp, textSize = 30, icon = LineIcon.Volume, background = Palette.Accent, foreground = Palette.OnAccent)
        }
        DemoBanner("리뷰는 이용자가 쓴 글이에요 · 실제 규정과 다를 수 있어요", Modifier.padding(horizontal = 36.dp))
        Row(Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.width(460.dp).fillMaxHeight().card().padding(36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Label("별점 요약", 26, FontWeight.Bold, Palette.SubText)
                val p = page
                Label(if (p != null && p.count > 0) "★ ${"%.1f".format(p.average)}" else "★ -", 96, FontWeight.ExtraBold, lineHeight = 1.1.em)
                Label(if (p != null) "리뷰 ${p.count}개 · 방문 확인 ${p.visitedCount}개" else "불러오는 중", 26, FontWeight.Medium, Palette.SubText)
                val all = p?.items.orEmpty()
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    (5 downTo 1).forEach { star ->
                        val n = all.count { it.rating == star }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Label("$star", 24, modifier = Modifier.width(36.dp))
                            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Palette.Gauge)) {
                                Box(Modifier.fillMaxWidth(if (all.isEmpty()) 0f else n / all.size.toFloat()).fillMaxHeight().background(Palette.Text))
                            }
                            Label("$n", 24, color = Palette.SubText, modifier = Modifier.width(24.dp))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PresetChip("최신순", !visitedOnly, { visitedOnly = false })
                    PresetChip("방문 확인만", visitedOnly, { visitedOnly = true })
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (reviews.isEmpty()) {
                    Column(Modifier.fillMaxWidth().weight(1f).card().padding(48.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(if (page == null) "리뷰를 불러오는 중이에요" else "아직 리뷰가 없어요", 40, FontWeight.ExtraBold)
                        Label("다녀온 뒤 폰 앱에서 리뷰를 남겨 주세요", 28, FontWeight.Medium, Palette.SubText)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(reviews) { r ->
                            Column(Modifier.fillMaxWidth().card(32.dp).padding(horizontal = 32.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Label("★ ${r.rating}", 24)
                                    Label(r.nickname, 24, color = Palette.SubText)
                                    if (r.visited) {
                                        Box(Modifier.height(40.dp).background(Palette.InfoFill, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                            Label("방문 확인", 24, FontWeight.Bold, Palette.InfoText)
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Label(r.createdAt, 24, color = Palette.SubText)
                                }
                                Label(r.text, 30, FontWeight.Medium, maxLines = 6, lineHeight = 1.5.em)
                            }
                        }
                    }
                }
                Label("출발하면 이 화면은 닫히고 01 찾기로 돌아가요. 주행 중 리뷰는 음성으로만 들을 수 있어요.", 24, FontWeight.Medium, Palette.MutedText)
            }
        }
    }
}

/** 화면설계서 22 필터 더 보기(정차 중). 고친 값은 "n곳 보기"를 눌러야 적용된다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterMoreScreen(app: AppState, items: List<PlaceItem>, favorites: Set<String>, settings: Settings, modifier: Modifier = Modifier) {
    var draft by remember { mutableStateOf(app.filter) }
    val count = items.applyFilter(draft, favorites).size
    Column(modifier.fillMaxSize()) {
        BackHeader(app, "필터 더 보기", "주행 중에는 프리셋 칩만 보이고, 이 화면은 정차 중에만 열려요") { StatusPill(parked = true) }
        Column(Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // 10-30: 신호는 셋 중 하나만(찾기 화면의 신호 칩과 같은 값)
                FilterSection("신호", Modifier.weight(1f), note = "한 가지만 골라요 · 찾기 화면 위에서 고른 방식 기준이에요 (지금: ${settings.primaryStyle.label})") {
                    SignalScope.entries.forEach { s ->
                        PresetChip(if (s == SignalScope.ALL) "전부 보기 (미확인·금지 포함)" else s.label, draft.signalScope == s, { draft = draft.withSignal(s) })
                    }
                }
                FilterSection("거리", Modifier.weight(1f)) {
                    listOf(10, 30, 50).forEach { km ->
                        PresetChip("$km km", draft.maxDistanceKm == km, { draft = draft.copy(maxDistanceKm = km) })
                    }
                    PresetChip("제한 없음", draft.maxDistanceKm == null, { draft = draft.copy(maxDistanceKm = null) })
                }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                FilterSection("편의시설", Modifier.weight(1f)) {
                    PresetChip("화장실", draft.toilet, { draft = draft.copy(toilet = !draft.toilet) })
                    PresetChip("수도", draft.water, { draft = draft.copy(water = !draft.water) })
                    PresetChip("전기", draft.electricity, { draft = draft.copy(electricity = !draft.electricity) })
                    PresetChip("샤워", draft.shower, { draft = draft.copy(shower = !draft.shower) })
                    PresetChip("취사 가능", draft.cookAllowed, { draft = draft.copy(cookAllowed = !draft.cookAllowed) })
                }
                FilterSection("장소 종류 · 현장 조건", Modifier.weight(1f)) {
                    listOf("오토캠핑장", "해변 주차장").forEach { type ->
                        val on = type in draft.siteTypes
                        PresetChip(type, on, { draft = draft.copy(siteTypes = if (on) draft.siteTypes - type else draft.siteTypes + type) })
                    }
                    PresetChip("높이 제한 없음", draft.noHeightLimit, { draft = draft.copy(noHeightLimit = !draft.noHeightLimit) })
                    PresetChip("공식 근거만", draft.officialOnly, { draft = draft.copy(officialOnly = !draft.officialOnly) })
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(168.dp).padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SecondaryButton("초기화", { draft = draft.reset() }, height = 120.dp, textSize = 32)
            PrimaryButton(
                "${count}곳 보기",
                {
                    app.filter = draft
                    app.back()
                },
                Modifier.weight(1f), height = 120.dp, textSize = 38, icon = null, trailingChevron = false,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, modifier: Modifier, note: String? = null, content: @Composable () -> Unit) {
    Column(modifier.fillMaxHeight().card(32.dp).padding(32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Label(title, 32, FontWeight.ExtraBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        note?.let { Label(it, 22, FontWeight.Medium, Palette.MutedText) }
    }
}

/** 뒤로 가기 버튼이 있는 머리글(21·22·13) */
@Composable
fun BackHeader(
    app: AppState,
    title: String,
    subtitle: String?,
    onBack: () -> Unit = { app.back() },
    backLabel: String = "돌아가기",
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 11-11 Pleos Page Navigation(Depth): 바탕 없는 꺾쇠 + 제목
        BackChevron(backLabel, onBack)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, 40, FontWeight.ExtraBold)
            subtitle?.let { Label(it, 26, FontWeight.Medium, Palette.SubText) }
        }
        trailing()
    }
}
