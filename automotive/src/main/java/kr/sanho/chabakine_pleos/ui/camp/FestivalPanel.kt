package kr.sanho.chabakine_pleos.ui.camp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.FestivalSheet
import kr.sanho.chabakine_pleos.data.Festival
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.find.SidePanel
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette

/**
 * 10-1 축제 상세(정차 중): "상세 보기"를 누르면 02 이유 시트처럼 오른쪽에서 밀려 나온다.
 * 사진 · 기간 · 거리 · 장소 · 전화 · 홈페이지 · 소개 글 · 출처, 맨 아래 여기로 안내. 바깥이나 닫기를 누르면 닫힌다.
 */
@Composable
fun FestivalPanel(app: AppState, sheet: FestivalSheet?) {
    SidePanel(sheet, app::closeFestival) { FestivalContent(app, it.festival, it.km) }
}

@Composable
private fun FestivalContent(app: AppState, f: Festival, km: Double) {
    Column(
        Modifier.fillMaxSize().background(Palette.Card)
            .semantics { paneTitle = "${f.title} 상세" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // 패널 안 누름은 바깥으로 새지 않게
            .padding(start = 40.dp, end = 32.dp, top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(listOfNotNull("근처 축제", f.region).joinToString(" · "), 24, FontWeight.Bold, Palette.SoftText)
                Label(f.title, 40, FontWeight.ExtraBold, maxLines = 2)
            }
            SecondaryButton("닫기", app::closeFestival, Modifier.width(140.dp), height = 96.dp, textSize = 28)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            f.imageUrl?.let {
                AsyncImage(it, null, Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
            }
            InfoRow(LineIcon.Info, "기간 · 거리", "${f.startDate} ~ ${f.endDate} · ${formatKm(km)}")
            f.address?.let { InfoRow(LineIcon.Pin, "장소", it) }
            f.phone?.let { InfoRow(LineIcon.Phone, "전화", it) }
            f.homepage?.let { InfoRow(LineIcon.Navigate, "홈페이지", it) }
            f.overview?.let {
                Column(
                    Modifier.fillMaxWidth().background(Palette.CardInner, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Label("소개", 24, FontWeight.ExtraBold, Palette.SubText)
                    Label(it, 28, FontWeight.Medium, Palette.Text, maxLines = Int.MAX_VALUE, lineHeight = 1.5.em)
                }
            }
            val credit = listOfNotNull(f.source?.let { "출처 $it" }, f.imageCredit?.let { "사진 $it" })
            if (credit.isNotEmpty()) Label(credit.joinToString(" · "), 22, FontWeight.Medium, Palette.MutedText, maxLines = 2)
        }
        // 11-11: 차박 모드 중(05 → 근처 축제)에는 안내 버튼 없이 정보만
        if (app.campPlaceId == null) {
            PrimaryButton(
                "여기로 안내", {
                    app.closeFestival()
                    app.navigateToPoint(f.title, f.location)
                },
                Modifier.fillMaxWidth(), height = 112.dp, textSize = 36, trailingChevron = false,
            )
        }
    }
}

@Composable
private fun InfoRow(icon: LineIcon, title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().background(Palette.CardInner, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LineIconView(icon, 34.dp, Palette.SoftText)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(title, 22, FontWeight.ExtraBold, Palette.SubText)
            Label(value, 28, FontWeight.SemiBold, Palette.Text, maxLines = 3, lineHeight = 1.4.em)
        }
    }
}
