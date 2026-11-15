package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.data.isOngoing
import kr.sanho.chabakine_pleos.util.DayPart
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.ui.find.BackHeader
import kr.sanho.chabakine_pleos.app.Features
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.Festival
import kr.sanho.chabakine_pleos.data.nearby
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.ui.components.DemoBanner
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.PhotoTextShadow
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
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
import java.time.LocalDate

private val ARRIVE_STEPS = listOf(
    "유틸리티 모드 켜기" to "공조 · 조명을 쓰려면 필요해요",
)

internal val LEAVE_CHECKS = listOf("쓰레기 되가져가기", "흔적 남기지 않기", "유틸리티 모드 끄기 (직접)")

/**
 * 화면설계서 05-0 도착 체크. 유틸리티 모드는 앱이 바꿀 수 없어 운전자가 직접 켜고 체크한다.
 * 체크해야 "다음"이 켜지고, 차량 연동이 승인된 차면 23, 아니면 05로 간다.
 */
@Composable
fun CampArriveScreen(app: AppState, place: Place, vehicleReady: Boolean, bannerText: String, modifier: Modifier = Modifier) {
    val left = ARRIVE_STEPS.count { it.first !in app.checkedItems }
    val linked = Features.VEHICLE_CONTROL && vehicleReady
    Column(modifier.fillMaxSize()) {
        ScreenHeader("도착했어요 · 차박 준비", "${place.name} · 한 가지만 직접 해 주세요") {
            StatusPill(parked = true, text = "정차 중 · P단")
        }
        DemoBanner(bannerText, Modifier.padding(horizontal = 36.dp))
        Row(
            Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ARRIVE_STEPS.forEach { (title, note) -> ArriveStep(app, title, note, Modifier.heightIn(min = 168.dp)) }
            }
            Column(Modifier.width(520.dp).fillMaxHeight().card(32.dp).padding(36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Label("다음 순서", 24, FontWeight.Bold, Palette.SubText)
                Label(if (linked) "23 차량 맞추기 · 조명 · 공조 · 창문" else "차박 모드로 · 오늘 밤 화면", 32, FontWeight.ExtraBold, maxLines = 2)
                Label(
                    if (linked) "유틸리티 모드를 켜면 한 번에 잠자리를 맞춰요" else "이 차는 차량 연동이 승인되지 않아 바로 차박 모드로 가요",
                    24, FontWeight.Medium, Palette.SoftText, maxLines = 3,
                )
                Spacer(Modifier.weight(1f))
                Label(
                    "시동 상태는 읽을 수 있지만 끄고 켜는 건 앱이 못 해요. 유틸리티 모드는 상태를 읽는 방법도 아직 확인 중이라 직접 체크해 주세요.",
                    24, FontWeight.Medium, Palette.MutedText, maxLines = 4,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().height(176.dp).padding(horizontal = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Label("\"다음\"은 차량 연동이 승인된 차면 23, 아니면 바로 05로 가요", 24, FontWeight.SemiBold, Palette.MutedText, Modifier.width(420.dp), maxLines = 2)
            PrimaryButton(
                text = "다음",
                onClick = app::finishArrival,
                modifier = Modifier.weight(1f),
                height = 128.dp,
                textSize = 40,
                icon = null,
                trailingChevron = false,
                enabled = left == 0,
            )
        }
    }
}

@Composable
private fun ArriveStep(app: AppState, title: String, note: String, modifier: Modifier) {
    val checked = title in app.checkedItems
    Row(
        modifier
            .fillMaxWidth()
            .card(28.dp)
            .clickable(role = Role.Checkbox) { app.toggleChecked(title) }
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (checked) Palette.GaugeFill else Color.Transparent)
                .border(4.dp, if (checked) Palette.GaugeFill else Palette.ChipBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { if (checked) LineIconView(LineIcon.Check, 44.dp, Color.White, strokeWidth = 3f) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label(title, 40, FontWeight.ExtraBold)
            Label(note, 26, FontWeight.Medium, Palette.SubText, maxLines = 2)
        }
    }
}

/** 화면설계서 10 철수 안내(아침). 5개를 모두 체크해야 "철수 완료"가 켜진다. */
@Composable
fun CampLeaveScreen(
    app: AppState,
    place: Place,
    nextCandidates: List<PlaceItem>,
    bannerText: String,
    modifier: Modifier = Modifier,
) {
    val done = LEAVE_CHECKS.count { it in app.checkedItems }
    val left = LEAVE_CHECKS.size - done
    Column(modifier.fillMaxSize()) {
        // 11-11 Pleos Page Navigation(Depth): 오른쪽 "차박 모드로" 글자 버튼 대신 왼쪽 꺾쇠로 05에 돌아간다
        BackHeader(app, DayPart.leaveTitle(), "철수 안내 · ${place.name}", onBack = { app.replace(Route.CampMode) }, backLabel = "차박 모드로 돌아가기") {
            StatusPill(parked = true)
        }
        DemoBanner(bannerText, Modifier.padding(horizontal = 36.dp))
        Row(Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 24.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().card().padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label("철수 체크리스트", 32, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Label("$done / ${LEAVE_CHECKS.size}", 26, FontWeight.Bold, Palette.SubText)
                }
                LEAVE_CHECKS.forEach { CheckRow(app, it, null, titleWidth = false) }
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    text = if (left == 0) "철수 완료" else "철수 완료 · ${left}개 남음",
                    onClick = app::finishCamp,
                    modifier = Modifier.fillMaxWidth(),
                    height = 128.dp,
                    textSize = 40,
                    icon = if (left == 0) LineIcon.Check else null,
                    trailingChevron = false,
                    enabled = left == 0,
                )
                Label("모두 체크하면 버튼이 켜져요 · 누르면 오늘${if (DayPart.isMorning()) " 아침" else ""} 주변을 보여 드려요", 22, FontWeight.Medium, Palette.MutedText)
            }
            Column(Modifier.weight(1f).fillMaxHeight().card().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // "이곳 어땠나요?"(차 안 제보)는 다음 단계([Features.PLACE_REPORT])라 시연에서 숨긴다
                Label("다음 후보", 32, FontWeight.ExtraBold)
                if (nextCandidates.isEmpty()) Label("근처에 허용·조건부 장소가 없어요", 26, FontWeight.Medium, Palette.SubText)
                nextCandidates.forEach { NextCandidate(app, it) }
            }
        }
    }
}

@Composable
private fun NextCandidate(app: AppState, item: PlaceItem) {
    val photo = item.place.photoUrls.firstOrNull()
    DarkScope(photo != null) {
    Box(Modifier.fillMaxWidth().card(24.dp, Palette.CardInner)) {
        if (photo != null) {
            AsyncImage(photo, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(0f to Color(0xD908090B), 0.55f to Color(0x9908090B), 1f to Color(0x5908090B))))
        }
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SignalLamp(item.signal, 28.dp, showSymbol = true)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label(item.place.name, 30, FontWeight.ExtraBold)
                    Label("${item.signal.label} · ${formatKm(item.distanceKm)}", 24, FontWeight.Medium, Palette.SubText)
                }
            }
            PrimaryButton("여기로 안내", { app.navigateTo(item.place) }, Modifier.fillMaxWidth(), height = 104.dp, textSize = 32, icon = null, trailingChevron = false)
        }
    }
    }
}

/**
 * 화면설계서 10-1 철수 완료 · 오늘 아침, 주변. 50 km 안에 열리는 축제를 가까운 순으로 오른쪽 목록(위아래로 밀기)에 보여 주고,
 * 고른 축제를 왼쪽에 크게(여기로 안내) 보여 준다. 처음에는 사진이 있는 가장 가까운 축제를 고른다.
 * 05 "근처 축제 보기"로 열면 차박 모드가 켜진 채라, 제목은 "근처 축제", 꺾쇠는 05로 돌아가고 "다음 차박지 찾기"는 숨긴다.
 */
@Composable
fun CampLeaveDoneScreen(
    app: AppState,
    place: Place,
    festivals: List<Festival>,
    bannerText: String,
    modifier: Modifier = Modifier,
) {
    val origin = LatLng(place.latitude, place.longitude)
    val today = LocalDate.now()
    val nearFestivals = festivals
        .filter { it.isOngoing(today) }
        .nearby(origin, 50.0, MAX_FESTIVALS) { it.location }
    var selectedId by remember(place.id) { mutableStateOf<String?>(null) }
    val featured = nearFestivals.firstOrNull { it.first.id == selectedId }
        ?: nearFestivals.firstOrNull { it.first.imageUrl != null }
        ?: nearFestivals.firstOrNull()
    val camping = app.campPlaceId == place.id
    Column(modifier.fillMaxSize()) {
        // 11-11: 05에서 열면 다른 하위 화면처럼 왼쪽 ← 로 05에 돌아간다(오른쪽 "차박 모드로" 글자 버튼은 뺌)
        if (camping) {
            BackHeader(app, "근처 축제", "${place.name} 근처 · 차박 모드는 켜 둔 채예요", onBack = app::back, backLabel = "차박 모드로 돌아가기") {
                StatusPill(parked = true)
            }
        } else {
            // 철수를 마쳤으니 꺾쇠는 홈으로(체크리스트로 되돌아가지 않는다)
            BackHeader(app, DayPart.leaveDoneTitle(), "${place.name} 근처 · 좋은 하루 보내세요", onBack = { app.replace(Route.Home) }, backLabel = "홈으로") {
                StatusPill(parked = true)
            }
        }
        DemoBanner(bannerText, Modifier.padding(horizontal = 36.dp))
        Row(Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 24.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            FeaturedFestival(app, featured, camping, Modifier.weight(1f).fillMaxHeight())
            Column(Modifier.width(560.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (nearFestivals.size > 1) {
                    Label("근처 축제 ${nearFestivals.size}곳 · 누르면 왼쪽에 크게", 24, FontWeight.Bold, Palette.SubText)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(nearFestivals, key = { it.first.id }) { (f, km) ->
                        FestivalCard(f, km, f.id == featured?.first?.id, Modifier.fillMaxWidth().height(FESTIVAL_CARD_HEIGHT)) { selectedId = f.id }
                    }
                }
                if (!camping) SecondaryButton("다음 차박지 찾기", { app.replace(Route.Find()) }, Modifier.fillMaxWidth(), height = 112.dp, textSize = 32)
            }
        }
    }
}

/** 10-1 왼쪽 큰 카드: 고른 축제(사진이 있으면 배경)와 상세 보기 · 여기로 안내. 축제가 없으면 안내 글만 */
@Composable
private fun FeaturedFestival(app: AppState, featured: Pair<Festival, Double>?, camping: Boolean, modifier: Modifier) {
    DarkScope(featured?.first?.imageUrl != null) {
    Box(modifier.card(40.dp, Palette.Sub)) {
        featured?.first?.imageUrl?.let {
            AsyncImage(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x2608090B), 0.5f to Color(0x4008090B), 1f to Color(0xE608090B))))
        }
        Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)) {
            if (featured == null) {
                Label("50 km 안에 열리는 축제가 없어요", 40, FontWeight.ExtraBold, maxLines = 2)
                return@Column
            }
            val (f, km) = featured
            Label("근처 축제 · ${f.startDate.drop(5)} ~ ${f.endDate.drop(5)}" + (f.imageCredit?.let { " · 사진 $it" } ?: ""), 26, FontWeight.Bold, Palette.SubText, shadow = PhotoTextShadow)
            Label("${f.title} · ${formatKm(km)}", 56, FontWeight.ExtraBold, maxLines = 2, shadow = PhotoTextShadow)
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 상세는 02 이유 시트처럼 오른쪽에서 밀려 나온다(FestivalPanel)
                // 11-11: 차박 모드 중(05에서 열 때)에는 안내를 시작하지 않고 상세 보기만
                if (camping) {
                    PrimaryButton("상세 보기", { app.openFestival(f, km) }, Modifier.weight(1f), height = 128.dp, textSize = 40, icon = null, trailingChevron = false)
                } else {
                    SecondaryButton("상세 보기", { app.openFestival(f, km) }, Modifier.weight(2f), height = 128.dp, textSize = 36, background = Color(0xEB2A3A32))
                    PrimaryButton("여기로 안내", { app.navigateToPoint(f.title, f.location) }, Modifier.weight(3f), height = 128.dp, textSize = 40, trailingChevron = false)
                }
            }
        }
    }
    }
}

/** 10-1 오른쪽 축제 카드: 사진이 있으면 배경으로 깔고 아래쪽을 어둡게 해 글자를 읽히게 한다. 고른 카드는 파란 테두리 */
@Composable
private fun FestivalCard(festival: Festival, km: Double, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(36.dp)
    DarkScope(festival.imageUrl != null) {
    Box(
        modifier.clip(shape).background(Palette.Sub)
            .then(if (selected) Modifier.border(4.dp, Palette.Accent, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        festival.imageUrl?.let {
            AsyncImage(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x2608090B), 0.5f to Color(0x6608090B), 1f to Color(0xE608090B))))
        }
        val shadow = if (festival.imageUrl != null) PhotoTextShadow else null
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
            Label(festival.title, 32, FontWeight.ExtraBold, maxLines = 2, shadow = shadow)
            Label("${festival.startDate.drop(5)} ~ ${festival.endDate.drop(5)} · ${formatKm(km)}", 26, FontWeight.SemiBold, Palette.SoftText, shadow = shadow)
        }
    }
    }
}

/** 50 km 안에서 보여 줄 축제 수(가까운 순) */
private const val MAX_FESTIVALS = 10

private val FESTIVAL_CARD_HEIGHT = 200.dp
