package kr.sanho.chabakine_pleos.ui.camp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kr.sanho.chabakine_pleos.R
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Features
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.Festival
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.data.nearby
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.ui.components.DemoBanner
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.ScreenHeader
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.VoiceButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.find.SPLIT_MODE_MAX_WIDTH_DP
import kr.sanho.chabakine_pleos.ui.find.siteWarnings
import kr.sanho.chabakine_pleos.ui.find.styleReason
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.lampColor
import kr.sanho.chabakine_pleos.ui.theme.textColor
import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot
import java.time.LocalDate

/**
 * 화면설계서 04 정차 감지 · 차박 모드를 켤까요? 자동으로 켜지 않고 한 번 묻는다.
 * 주차 브레이크는 정차 판단에 쓰지 않고, 읽히면 참고로만 보여 준다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampPromptOverlay(app: AppState, place: Place, snapshot: VehicleSnapshot) {
    val arrived = app.arrivalPlaceId == place.id
    BoxWithConstraints(
        // 뒤 화면을 가린다(11-08): 장소 사진 + 막, 카드는 유리판
        Modifier.fillMaxSize().clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        CampPhotoBackdrop(place.photoUrls.firstOrNull())
        // 07-2 분할(840): 카드가 화면을 채우고 버튼을 위아래로 쌓는다
        val split = maxWidth.value < SPLIT_MODE_MAX_WIDTH_DP
        Column(
            (if (split) Modifier.fillMaxSize().padding(28.dp) else Modifier.width(1080.dp))
                .glass(44.dp).padding(if (split) 44.dp else 64.dp).semantics { contentDescription = "차박 모드를 켤까요?" },
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Image(painterResource(R.drawable.chabakine_logo), null, Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)))
                Label(
                    if (arrived) "${place.name}에 도착했어요" else "${place.name} 근처에 주차했어요", 30, FontWeight.Bold, Palette.SubText,
                    modifier = Modifier.weight(1f),
                )
                // 말로 답하기: "켜 줘" · "나중에". 카드가 메뉴 줄을 덮으므로 마이크를 여기에 둔다
                VoiceButton(app.listening) { app.startVoiceSearch() }
            }
            Label(if (split) "차박 모드를\n켤까요?" else "차박 모드를 켤까요?", 64, FontWeight.ExtraBold, maxLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val speedKmh = snapshot.speedMps?.let { (it * 3.6f).toInt() }
                ConditionChip(speedKmh?.let { "속도 $it km/h" } ?: "속도 확인 못 함", speedKmh == 0)
                ConditionChip(if (snapshot.gear == VehicleSnapshot.GEAR_PARK) "P단" else "P단 확인 못 함", snapshot.gear == VehicleSnapshot.GEAR_PARK)
                if (snapshot.parkingBrakeOn == true) ConditionChip("주차 브레이크", true)
            }
            Label(
                if (split) "마트나 휴게소에서도 주차가 되므로 자동으로 켜지 않아요."
                else "방식별 가능 여부, 현장 조건, 체크리스트를 보여 드려요. 마트나 휴게소에서도 주차가 되므로 자동으로 켜지 않아요.",
                30, FontWeight.Medium, Palette.SoftText, maxLines = 3, lineHeight = 1.5.em,
            )
            if (split) {
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    "차박 모드 켜기", { app.beginCampMode(place.id) }, Modifier.fillMaxWidth(),
                    height = 132.dp, textSize = 40, icon = LineIcon.Tent, trailingChevron = false,
                )
                SecondaryButton("나중에", app::dismissCampPrompt, Modifier.fillMaxWidth(), height = 120.dp, textSize = 36)
                return@Column
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SecondaryButton("나중에", app::dismissCampPrompt, Modifier.weight(1f), height = 132.dp, textSize = 38)
                PrimaryButton(
                    "차박 모드 켜기", { app.beginCampMode(place.id) }, Modifier.weight(2f),
                    height = 132.dp, textSize = 40, icon = LineIcon.Tent, trailingChevron = false,
                )
            }
        }
    }
}

@Composable
private fun ConditionChip(text: String, ok: Boolean) {
    Row(
        Modifier.height(68.dp).background(if (ok) Palette.SuccessFill else Palette.Sub, RoundedCornerShape(34.dp)).padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val fg = if (ok) Palette.Success else Palette.SubText
        LineIconView(if (ok) LineIcon.Check else LineIcon.Question, 30.dp, fg, strokeWidth = 2.6f)
        Label(text, 28, FontWeight.Bold, fg)
    }
}

@Composable
internal fun CheckRow(app: AppState, title: String, note: String?, titleWidth: Boolean = true) {
    val checked = title in app.checkedItems
    Row(
        Modifier.fillMaxWidth().height(96.dp).card(20.dp, Palette.CardInner).clickable(role = Role.Checkbox) { app.toggleChecked(title) }.padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (checked) Palette.GaugeFill else Color.Transparent).border(3.dp, if (checked) Palette.GaugeFill else Palette.ChipBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { if (checked) LineIconView(LineIcon.Check, 32.dp, Color.White, strokeWidth = 3f) }
        Label(title, 28, FontWeight.ExtraBold, modifier = if (titleWidth && note != null) Modifier.width(260.dp) else Modifier)
        note?.let { Label(it, 24, FontWeight.Medium, Palette.SubText) }
    }
}
