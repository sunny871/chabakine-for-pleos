package kr.sanho.chabakine_pleos.ui.camp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.vehicle.CampPreset
import kr.sanho.chabakine_pleos.app.CampOverlay
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 05-7 배터리 부족 알림: 잠든 화면에서 배터리가 새로 20% 아래로 내려가면 화면을 켜고 덮는다.
 * 보이는 동안 알림음 2번 + 음성 한 번(반복 없음). 누를 때까지 남고, 출발하거나 일산화탄소 경보가 오면 닫힌다.
 * 공조 버튼은 차량 연동이 승인된 차면 늘(23에서 맞추지 않았어도), 아니면 직접 줄여 달라는 안내.
 * 알릴 때 공조가 이미 꺼져 있었으면 공조 버튼 대신 "이미 꺼져 있어요"와 다시 잠들기 · 화면 켜 두기.
 */
@Composable
fun LowBatteryOverlay(app: AppState, snapshot: VehicleSnapshot) {
    val alert = app.lowBattery ?: return
    DisposableEffect(alert) {
        app.startLowBatterySound()
        onDispose { app.stopWakeSound() }
    }
    val climateOff = alert.climateOn == false
    val canEase = app.canEaseClimate && !climateOff
    Box(Modifier.fillMaxSize().clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(1180.dp).background(GlassFill, RoundedCornerShape(44.dp)).border(3.dp, Amber, RoundedCornerShape(44.dp))
                .padding(horizontal = 56.dp, vertical = 52.dp)
                .semantics { contentDescription = "배터리 부족 알림" },
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                LineIconView(LineIcon.Battery, 72.dp, AmberText)
                Label("배터리가 ${alert.percent}%예요", 60, FontWeight.ExtraBold, Text, Modifier.weight(1f))
                Label(Instant.ofEpochMilli(alert.atMs).atZone(KOREA).format(HHMM), 34, FontWeight.Bold, Muted)
            }
            val tip = if (climateOff) "공조는 이미 꺼져 있어요 · 남은 배터리는 대기 전력으로 천천히 줄어요" else "공조를 줄이면 아침까지 더 버틸 수 있어요"
            val lines = listOfNotNull(tip, alert.drainText).joinToString(" · ")
            Label(lines, 30, FontWeight.SemiBold, Soft, maxLines = 3, lineHeight = 1.45.em)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val preset = app.appliedPreset
                if (climateOff) {
                    Chip(LineIcon.Fan, "공조 꺼짐")
                } else if (canEase && preset != null) {
                    Chip(LineIcon.Fan, "지금 공조 ${preset.temperatureC.roundToInt()}°C · 바람 ${CampPreset.fanName(preset.fanSpeed)} (23에서 맞춘 값)")
                } else if (canEase) {
                    Chip(LineIcon.Fan, "아래 버튼을 누르면 공조를 바로 줄여요")
                } else {
                    Chip(LineIcon.Fan, "차에서 공조를 직접 줄여 주세요")
                }
                snapshot.energy?.rangeKm?.takeIf { it >= 1f }?.let { Chip(null, "약 ${it.roundToInt()} km 주행 가능") }
            }
            Row(
                Modifier.background(SoundBg, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LineIconView(LineIcon.Speaker, 36.dp, SoundFg)
                Label("알림음 2번 · \"${alert.speech}\" 한 번 읽어 줘요 · 반복 없음", 26, FontWeight.Bold, SoundFg, maxLines = 2)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (canEase) {
                    PrimaryButton("공조 약하게", { app.easeClimateForBattery(off = false) }, Modifier.weight(1.3f), height = 128.dp, textSize = 34, icon = null, trailingChevron = false)
                    Secondary("공조 끄기", Modifier.weight(1f)) { app.easeClimateForBattery(off = true) }
                    Secondary("다시 잠들기", Modifier.weight(1f)) { app.campOverlay = CampOverlay.SLEEP }
                } else {
                    PrimaryButton("다시 잠들기", { app.campOverlay = CampOverlay.SLEEP }, Modifier.weight(1.3f), height = 128.dp, textSize = 34, icon = null, trailingChevron = false)
                    Secondary("화면 켜 두기", Modifier.weight(1f)) { app.campOverlay = CampOverlay.NONE }
                }
            }
        }
    }
}

@Composable
private fun Chip(icon: LineIcon?, text: String) {
    Row(
        Modifier.height(64.dp).background(GlassInner, RoundedCornerShape(32.dp)).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let { LineIconView(it, 30.dp, Sub) }
        Label(text, 26, FontWeight.Bold, Sub)
    }
}

@Composable
private fun Secondary(text: String, modifier: Modifier, onClick: () -> Unit) {
    SecondaryButton(
        text, onClick, modifier, height = 128.dp, textSize = 34,
        background = GlassSecondary, foreground = Text,
    )
}

// 차박 모드 쪽은 테마와 상관없이 항상 어둡게(설계서 05-7)
private val Text = Color(0xFFF4EFE6)
private val Soft = Color(0xFFDCD8CC)
private val Sub = Color(0xFFB9C4B8)
private val Muted = Color(0xFF98A69A)
private val Amber = Color(0xFFF2B630)
private val AmberText = Color(0xFFFFC24A)
private val SoundBg = Color(0xFF3A2F12)
private val SoundFg = Color(0xFFFFD98A)
private val KOREA = ZoneId.of("Asia/Seoul")
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
