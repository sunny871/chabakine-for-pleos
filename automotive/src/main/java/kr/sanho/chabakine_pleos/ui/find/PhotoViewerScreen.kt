package kr.sanho.chabakine_pleos.ui.find

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.theme.Palette

/**
 * 화면설계서 02-2 사진 크게 보기(정차 중, 출발하면 01로 닫힘).
 * 큰 사진을 좌우로 밀거나 화살표로 넘기고, 아래 썸네일을 누르면 그 사진으로 간다. 닫으면 02.
 * 사진마다 출처·찍은 시기 값은 데이터에 아직 없어서 장소의 근거 출처만 보여 준다.
 */
@Composable
fun PhotoViewerScreen(app: AppState, place: Place, startIndex: Int, modifier: Modifier = Modifier) {
    val photos = place.photoUrls
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) { photos.size }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().background(Palette.LampBody)) {
        BackHeader(
            app, "${place.name} 사진",
            listOfNotNull(
                if (photos.isEmpty()) "사진이 없어요" else "${pager.currentPage + 1} / ${photos.size}",
                place.sourceName?.let { "출처 $it" },
            ).joinToString(" · "),
        ) { StatusPill(parked = true) }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 36.dp)) {
            HorizontalPager(pager, Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Palette.Background), key = { it }) { page ->
                Box(Modifier.fillMaxSize()) {
                    // 비율이 맞지 않아 남는 칸: 같은 사진을 꽉 채워 흐리게 깔고 살짝 어둡게 덮는다
                    AsyncImage(photos[page], null, Modifier.fillMaxSize().blur(PHOTO_FILL_BLUR), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color(0x66000000)))
                    AsyncImage(
                        photos[page], null,
                        Modifier.fillMaxSize().semantics { contentDescription = "${place.name} 사진 ${page + 1}" },
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            if (photos.size > 1) {
                ArrowButton(LineIcon.ChevronLeft, "이전 사진", Modifier.align(Alignment.CenterStart).padding(start = 24.dp)) {
                    scope.launch { pager.animateScrollToPage((pager.currentPage - 1 + photos.size) % photos.size) }
                }
                ArrowButton(LineIcon.ChevronRight, "다음 사진", Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)) {
                    scope.launch { pager.animateScrollToPage((pager.currentPage + 1) % photos.size) }
                }
            }
        }
        LazyRow(
            Modifier.fillMaxWidth().height(190.dp).padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(photos) { i, url ->
                val on = i == pager.currentPage
                AsyncImage(
                    url, "사진 ${i + 1}",
                    Modifier
                        .width(220.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Palette.Card)
                        .border(4.dp, if (on) Color(0xFFDAD1BF) else Color.Transparent, RoundedCornerShape(22.dp))
                        .alpha(if (on) 1f else 0.6f)
                        .clickable(role = Role.Tab) { scope.launch { pager.animateScrollToPage(i) } },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: LineIcon, description: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color(0xD9121417))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { LineIconView(icon, 56.dp, Palette.Text, strokeWidth = 2.4f) }
}

/** 사진 옆 빈 칸을 채우는 같은 사진의 흐림 정도(안드로이드 12 미만은 흐림 없이 덮개만) */
private val PHOTO_FILL_BLUR = 40.dp
