package kr.sanho.chabakine_pleos.ui.home

import kr.sanho.chabakine_pleos.util.Josa
import kr.sanho.chabakine_pleos.ui.components.pressShrink
import kr.sanho.chabakine_pleos.ui.components.ButtonRadius
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.ui.components.DrivingPhotoDim
import kr.sanho.chabakine_pleos.ui.components.DataStatus
import kr.sanho.chabakine_pleos.ui.find.DRIVING_LIST_LIMIT
import kr.sanho.chabakine_pleos.ui.components.DataStatusStrip
import kr.sanho.chabakine_pleos.ui.components.DemoBanner
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.textColor
import kotlin.math.roundToInt

/** 홈 화면의 버튼 동작. 아직 만들지 않은 화면은 호출하는 쪽에서 안내한다. */
class HomeActions(
    val onVoice: () -> Unit,
    val onSearch: () -> Unit,
    val onNavigate: (PlaceItem) -> Unit,
    val onToggleFavorite: (PlaceItem) -> Unit,
    val onShowAll: () -> Unit,
    val onShowFavorites: () -> Unit,
    val onOpenPlace: (PlaceItem) -> Unit,
    val onRetryData: () -> Unit,
)

/** 화면설계서 14 홈 · 정차 중 / 14-1 홈 · 주행 중([parked]가 false: 밀기·검색 없이 첫 추천만) */
@Composable
fun HomeScreen(
    data: HomeData,
    parked: Boolean,
    style: CampingStyle,
    originText: String,
    favorites: Set<String>,
    bannerText: String,
    dataStatus: DataStatus,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    /** 주행 중 가까운 후보 최대 개수(시스템 주행 제한 값, 없으면 01과 같은 기본값) */
    drivingListLimit: Int = DRIVING_LIST_LIMIT,
    /** 내비로 안내 중인 차박지 이름. 있으면 위에 "안내 중" 띠(누르면 03-1) */
    guidingName: String? = null,
    onOpenGuiding: () -> Unit = {},
) {
    var index by remember(data.recommendations) { mutableIntStateOf(0) }
    if (!parked) index = 0
    val current = data.recommendations.getOrNull(index)
    Column(modifier.fillMaxSize()) {
        Header(
            title = "오늘, 어디서 쉬어갈까요?",
            subtitle = "$originText · 내 방식 ${style.label} 기준",
            parked = parked,
            onSearch = actions.onSearch,
            onVoice = actions.onVoice,
        )
        guidingName?.let { GuidingBand(it, onOpenGuiding, Modifier.padding(start = 36.dp, end = 36.dp, bottom = 12.dp)) }
        DemoBanner(bannerText, Modifier.padding(horizontal = 36.dp))
        DataStatusStrip(
            status = dataStatus,
            onRetry = actions.onRetryData,
            modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 12.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RecommendationCard(
                items = data.recommendations,
                index = index,
                onIndexChange = { index = it },
                parked = parked,
                style = style,
                favorites = favorites,
                actions = actions,
                modifier = Modifier.weight(880f).fillMaxHeight(),
            )
            NearbyCard(
                items = data.nearby.filter { it.place.id != current?.place?.id && it.distanceKm <= NEARBY_RADIUS_KM },
                popular = data.popular,
                favoritesCount = favorites.size,
                parked = parked,
                drivingListLimit = drivingListLimit,
                actions = actions,
                modifier = Modifier.weight(592f).fillMaxHeight(),
            )
        }
    }
}

/** 14 · 14-1 · 07 "○○으로 안내 중 ›" 띠. 내비로 안내 중일 때만, 누르면 03-1 */
@Composable
fun GuidingBand(name: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.GuideFill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LineIconView(LineIcon.Navigate, 36.dp, Palette.GuideIcon)
        Label("${Josa.euroRo(name)} 안내 중", 30, FontWeight.ExtraBold, Palette.Text, Modifier.weight(1f))
        LineIconView(LineIcon.ChevronRight, 40.dp, Palette.GuideIcon)
    }
}

@Composable
private fun Header(title: String, subtitle: String, parked: Boolean, onSearch: () -> Unit, onVoice: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(title, 48, FontWeight.ExtraBold)
            Label(subtitle, 26, FontWeight.Medium, Palette.SubText)
        }
        Box(
            Modifier
                .height(60.dp)
                .background(if (parked) Palette.ParkedPill else Palette.Sub, RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Label(if (parked) "정차 중" else "주행 중", 24, FontWeight.Bold, if (parked) Color.White else Palette.Text)
        }
        // 키보드 검색(02-1)은 정차 중에만. 주행 중엔 음성만 남는다
        if (parked) RoundIconButton(LineIcon.Search, "검색", 88.dp, 42.dp, Palette.Sub, onSearch)
        RoundIconButton(LineIcon.Mic, "음성으로 찾기", 88.dp, 42.dp, Palette.Accent, onVoice)
    }
}

@Composable
private fun RecommendationCard(
    items: List<PlaceItem>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    parked: Boolean,
    style: CampingStyle,
    favorites: Set<String>,
    actions: HomeActions,
    modifier: Modifier,
) {
    // 주행 중(14-1)은 넘기기 없이 첫 추천만(F5)
    if (!parked || items.size < 2) {
        RecommendationPage(items.getOrNull(index), items, index, parked, style, favorites, actions, modifier)
        return
    }
    // 정차 중: 카드가 손가락을 따라 움직이고 놓으면 다음 카드로 붙는다. 끝에서 처음으로 이어지게 페이지를 넉넉히 둔다
    // 추천 목록이 바뀌면(새 데이터·방식 변경) 처음부터 다시
    key(items) {
        val size = items.size
        val pager = rememberPagerState(initialPage = LOOP_PAGES / 2 * size + index) { LOOP_PAGES * size }
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage % size }.collect(onIndexChange)
        }
        HorizontalPager(
            state = pager,
            modifier = modifier,
            pageSpacing = 24.dp,
            key = { it },
        ) { page ->
            val i = page % size
            RecommendationPage(items[i], items, i, parked, style, favorites, actions, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RecommendationPage(
    item: PlaceItem?,
    items: List<PlaceItem>,
    index: Int,
    parked: Boolean,
    style: CampingStyle,
    favorites: Set<String>,
    actions: HomeActions,
    modifier: Modifier,
) {
    // 사진이 있을 때만 깔고, 없거나 못 받으면 어두운 바탕(#2B3039)
    DarkScope(item?.place?.photoUrls?.isNotEmpty() == true) {
    Box(modifier.clip(RoundedCornerShape(40.dp)).background(Palette.Sub)) {
        item?.place?.photoUrls?.firstOrNull()?.let { PlacePhoto(it, driving = !parked) }
        RecommendationContent(item, items, index, parked, style, favorites, actions)
    }
    }
}

/** 장소 사진 + 글자가 읽히도록 위·아래를 어둡게 덮는 그라데이션 */
@Composable
private fun BoxScope.PlacePhoto(url: String, driving: Boolean) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
    Box(
        Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0x99101A16),
                    0.35f to Color(0x33101A16),
                    0.55f to Color(0x99101A16),
                    1f to Color(0xF2101A16),
                ),
            ),
    )
    DrivingPhotoDim(driving)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationContent(
    item: PlaceItem?,
    items: List<PlaceItem>,
    index: Int,
    parked: Boolean,
    style: CampingStyle,
    favorites: Set<String>,
    actions: HomeActions,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (item == null) {
            Spacer(Modifier.weight(1f))
            Label("근처에 추천할 곳이 없어요", 40, FontWeight.ExtraBold)
            Label("내 방식 기준으로 허용·조건부인 장소가 없어요. 모두 보기에서 다른 장소를 확인하세요.", 28, FontWeight.Medium, Palette.SoftText, maxLines = 2)
            Spacer(Modifier.weight(1f))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            SignalLamp(item.signal, 48.dp, showSymbol = true)
            Label("${style.label} ${item.signal.label}", 34, FontWeight.ExtraBold, item.signal.textColor)
            Spacer(Modifier.weight(1f))
            val saved = item.place.id in favorites
            RoundIconButton(
                icon = LineIcon.Star,
                description = if (saved) "즐겨찾기에서 빼기" else "즐겨찾기에 추가",
                size = 96.dp,
                iconSize = 44.dp,
                background = Palette.Background,
                onClick = { actions.onToggleFavorite(item) },
                tint = if (saved) Palette.Star else Palette.Text,
            )
        }
        Spacer(Modifier.weight(1f))
        // 정차 중에만 눌러서 02 자세히 보기로 간다
        Column(
            modifier = if (parked) Modifier.clickable(role = Role.Button) { actions.onOpenPlace(item) } else Modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Label(if (parked) "오늘의 추천 · ${index + 1} / ${items.size}" else "오늘의 추천", 26, FontWeight.Bold, Palette.SoftText)
            Label(item.place.name, 60, FontWeight.ExtraBold, maxLines = 2)
            Label(item.place.reasonFor(style), 30, FontWeight.Medium, Palette.SoftText, maxLines = 2)
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineChip(formatKm(item.distanceKm))
                item.rating?.let { OutlineChip("★ ${it.average} · 리뷰 ${it.count}개") }
            }
        }
        if (parked && items.size > 1) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.indices.forEach { i ->
                    val width by animateDpAsState(if (i == index) 48.dp else 16.dp, label = "dot")
                    Box(
                        Modifier
                            .width(width)
                            .height(16.dp)
                            .background(if (i == index) Palette.Text else Palette.ChipBorder, CircleShape),
                    )
                }
                Spacer(Modifier.weight(1f))
                Label("← 좌우로 밀어서 다른 추천 보기 →", 24, FontWeight.SemiBold, Palette.SubText)
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pressShrink()
                    .height(136.dp)
                    .clip(RoundedCornerShape(ButtonRadius))
                    .background(Palette.Accent)
                    .clickable(role = Role.Button) { actions.onNavigate(item) }
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LineIconView(LineIcon.Navigate, 50.dp, Palette.OnAccent, strokeWidth = 2.2f)
                Label("여기로 안내", 42, FontWeight.ExtraBold, Palette.OnAccent, Modifier.weight(1f))
                LineIconView(LineIcon.ChevronRight, 44.dp, Palette.OnAccent, strokeWidth = 2.4f)
            }
        }
    }
}

@Composable
private fun NearbyCard(
    items: List<PlaceItem>,
    popular: List<PlaceItem>,
    favoritesCount: Int,
    parked: Boolean,
    drivingListLimit: Int,
    actions: HomeActions,
    modifier: Modifier,
) {
    // 가까운 후보 / 인기 장소 탭. 앱이 켜져 있는 동안 고른 탭을 기억한다
    var showPopular by rememberSaveable { mutableStateOf(false) }
    val list = if (showPopular) popular else items
    Column(
        modifier = modifier
            .background(Palette.Card, RoundedCornerShape(36.dp))
            .padding(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(18.dp)).background(Palette.CardInner).padding(6.dp)) {
                listOf(false to "가까운 후보", true to "인기 장소").forEach { (popularTab, title) ->
                    val on = popularTab == showPopular
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                            .background(if (on) Palette.Text else Color.Transparent)
                            .clickable(role = Role.Tab) { showPopular = popularTab },
                        contentAlignment = Alignment.Center,
                    ) { Label(title, 26, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) Palette.Background else Palette.Text) }
                }
            }
            Box(
                Modifier
                    .height(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Palette.Sub)
                    .clickable(role = Role.Button, onClick = actions.onShowAll)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) { Label("모두 보기", 26) }
        }
        Label(
            if (showPopular) "리뷰 많은 순 · 거리와 상관없이 · ${popular.size}곳" else "${NEARBY_RADIUS_KM} km 안 · 가까운 순 · ${items.size}곳",
            24, FontWeight.Medium, Palette.SubText,
        )
        // 위아래로 넘겨 본다. 주행 중엔 시스템 주행 제한 개수까지만
        val shown = if (parked) list else list.take(drivingListLimit)
        // 비어 있으면 안내 문구를 카드 가운데에
        if (shown.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CenterLabel(if (showPopular) "아직 리뷰가 쌓인 곳이 없어요" else "${NEARBY_RADIUS_KM} km 안에 다른 후보가 없어요")
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(shown, key = { it.place.id }) { NearbyRow(it, parked, actions, popular = showPopular) }
            if (shown.size < list.size) {
                item { Label("더 많은 곳은 정차하면 보여요", 24, FontWeight.Medium, Palette.MutedText, Modifier.padding(top = 4.dp)) }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.Sub)
                .clickable(role = Role.Button, onClick = actions.onShowFavorites),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            LineIconView(LineIcon.Star, 32.dp, Palette.Text)
            Label("즐겨찾기 ${favoritesCount}곳", 28)
        }
    }
}

/** 가운데 정렬 안내 문구(두 줄까지) */
@Composable
private fun CenterLabel(text: String) {
    androidx.compose.material3.Text(
        text,
        color = Palette.SubText,
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 2,
    )
}

@Composable
private fun NearbyRow(item: PlaceItem, parked: Boolean, actions: HomeActions, popular: Boolean = false) {
    val detail = buildList {
        // 인기 장소는 별점 · 리뷰 수를 먼저
        if (popular) item.rating?.let { add("★ ${it.average} · 리뷰 ${it.count}개") }
        add(item.signal.label)
        add(formatKm(item.distanceKm))
        if (item.signal == Signal.RED) add("가지 마세요") else if (!popular) item.rating?.let { add("★ ${it.average}") }
    }.joinToString(" · ")
    // 화면설계서 14: 사진이 있으면 줄 배경으로 꽉 채우고(center / cover), 없으면 #262A31
    val photo = item.place.photoUrls.firstOrNull()
    DarkScope(photo != null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.CardInner)
            .clickable(role = Role.Button) { actions.onOpenPlace(item) },
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // 글자가 사진 위에서도 읽히도록 왼쪽(글자 쪽)을 더 어둡게 덮는다
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xD9101A16),
                            0.65f to Color(0x99101A16),
                            1f to Color(0x40101A16),
                        ),
                    ),
            )
            DrivingPhotoDim(driving = !parked)
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SignalLamp(item.signal, 24.dp, vertical = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(item.place.name, 30, FontWeight.ExtraBold)
                Label(detail, 24, if (parked) FontWeight.Medium else FontWeight.SemiBold, Palette.SubText)
            }
        }
    }
    }
}

@Composable
private fun OutlineChip(text: String) {
    Box(
        Modifier
            .height(64.dp)
            .border(2.dp, Palette.ChipBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 26) }
}

@Composable
private fun RoundIconButton(
    icon: LineIcon,
    description: String,
    size: Dp,
    iconSize: Dp,
    background: Color,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { LineIconView(icon, iconSize, tint) }
}

/** 홈 가까운 후보로 보여 줄 거리 */
private const val NEARBY_RADIUS_KM = 30

/** 끝에서 처음으로 이어 넘기도록 실제 추천 수의 몇 배로 페이지를 둔다 */
private const val LOOP_PAGES = 1000
