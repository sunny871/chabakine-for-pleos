package kr.sanho.chabakine_pleos.ui.camp

import kr.sanho.chabakine_pleos.util.DayPart
import kr.sanho.chabakine_pleos.ui.components.BackChevron
import kr.sanho.chabakine_pleos.ui.components.ChipRadius
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.CampOverlay
import kr.sanho.chabakine_pleos.app.SleepAlerts
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.VoiceButton
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.components.withHaptic
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.util.sunrise
import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 05-5 기상 알람: 시각 · 빠른 시각(일출 등) · 소리 · 다시 알림. 05 잠들기 창의 "기상 시각 · 바꾸기 ›"에서 연다.
 * 앱 전체를 덮고, 출발하면 닫힌다(AppRoot). 바꾼 값은 바로 [AppState.sleepAlerts]에 들어간다.
 */
@Composable
fun WakeSetOverlay(app: AppState, place: Place?) {
    val alerts = app.sleepAlerts
    fun update(next: SleepAlerts) {
        app.sleepAlerts = next
    }
    fun back() {
        app.campOverlay = CampOverlay.ASK_SLEEP
    }
    val now by produceState(LocalTime.now(KOREA)) {
        while (true) {
            delay(15_000)
            value = LocalTime.now(KOREA)
        }
    }
    val rise = remember(place?.id) {
        place?.let { sunrise(nextMorning(LocalDateTime.now(KOREA)), it.latitude, it.longitude, KOREA) }
    }
    Column(
        Modifier.fillMaxSize().clickable(enabled = false) {}.padding(start = 40.dp, end = 40.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.height(104.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BackChevron("잠들기 창으로 돌아가기", ::back)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label("기상 알람", 40, FontWeight.ExtraBold)
                Label("${place?.name ?: "이곳"} · 내일 아침", 24, FontWeight.Medium, Palette.SubText)
            }
            Row(
                Modifier.height(96.dp).card(24.dp)
                    .clickable(role = Role.Switch, onClick = withHaptic(if (alerts.wake) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn) { update(alerts.copy(wake = !alerts.wake)) })
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Label(if (alerts.wake) "켜짐" else "꺼짐", 28, FontWeight.ExtraBold)
                Switch(alerts.wake)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // 왼쪽: 시각
            Column(
                Modifier.width(860.dp).fillMaxHeight().card(40.dp).padding(horizontal = 44.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Label("일어날 시각", 26, FontWeight.Bold, Palette.SubText, Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    TimeColumn("%02d".format(alerts.wakeMinutes / 60), "시", { update(alerts.shiftWake(60)) }, { update(alerts.shiftWake(-60)) })
                    Label(":", 140, FontWeight.ExtraBold, Palette.ChipBorder)
                    TimeColumn("%02d".format(alerts.wakeMinutes % 60), "분", { update(alerts.shiftWake(5)) }, { update(alerts.shiftWake(-5)) })
                }
                Label(
                    if (alerts.wake) "${alerts.wakeLabel} · ${untilText(alerts.wakeMinutes, now)}" else "알람 꺼짐 · 울리지 않아요",
                    30, FontWeight.ExtraBold, WakeGreen,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    quickTimes(rise).forEach { (text, minutes) ->
                        QuickChip(text, selected = alerts.wake && alerts.wakeMinutes == minutes) { update(alerts.wakeAt(minutes).copy(wake = true)) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Label("일출 시각은 이곳 좌표와 날짜로 기기에서 계산해요", 24, FontWeight.Medium, Palette.MutedText)
            }
            // 오른쪽: 소리 · 다시 알림 · 안내 · 저장
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OptionCard("소리") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OptionTile("알림음 + 음성", "알림음 뒤 \"일어날 시간이에요\"", !alerts.wakeVoiceOnly, Modifier.weight(1f)) { update(alerts.copy(wakeVoiceOnly = false)) }
                        OptionTile("음성만", "옆 사람이 깨지 않게 조용히", alerts.wakeVoiceOnly, Modifier.weight(1f)) { update(alerts.copy(wakeVoiceOnly = true)) }
                    }
                }
                OptionCard("다시 알림") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SNOOZE_CHOICES.forEach { minutes ->
                            OptionTile(if (minutes > 0) "${minutes}분 뒤" else "안 함", null, alerts.snoozeMinutes == minutes, Modifier.weight(1f)) {
                                update(alerts.copy(snoozeMinutes = minutes))
                            }
                        }
                    }
                }
                OptionCard("일어나면") {
                    Label("화면을 밝게 켜고 10 철수 안내로 이어 가요", 26, FontWeight.Medium, Palette.SoftText)
                }
                Row(
                    Modifier.fillMaxWidth().card(22.dp, NextStepBg).padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.height(40.dp).background(NextStepChip, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Label("다음 단계", 24, FontWeight.ExtraBold, NextStepFg)
                    }
                    Label("지금은 차박 모드 화면이 켜져 있는 동안만 울려요. 시동을 끈 뒤에도 울리는지는 확인 전이에요", 24, FontWeight.Medium, NextStepFg, maxLines = 3, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    if (alerts.wake) "${alerts.wakeLabel}에 깨우기" else "알람 없이 돌아가기",
                    ::back, Modifier.fillMaxWidth(), height = 128.dp, textSize = 38, icon = null, trailingChevron = false,
                )
            }
        }
    }
}

/**
 * 05-6 기상 알람 울림: 정한 시각에 잠든 화면을 밝게 켜고 덮는다. 보이는 동안 소리를 내고, 닫히면(버튼 · 출발 · 경보) 멈춘다.
 */
@Composable
fun WakeRingOverlay(app: AppState, place: Place?, snapshot: VehicleSnapshot) {
    DisposableEffect(Unit) {
        app.startWakeSound()
        onDispose { app.stopWakeSound() }
    }
    val now by produceState(LocalDateTime.now(KOREA)) {
        while (true) {
            delay(15_000)
            value = LocalDateTime.now(KOREA)
        }
    }
    val rise = remember(place?.id) { place?.let { sunrise(now.toLocalDate(), it.latitude, it.longitude, KOREA) } }
    val warm = Color(0xFFF7F3EE)
    val soft = Color(0xFFE6DCD2)
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(0f to Color(0xFF0B1424), 0.46f to Color(0xFF1B2440), 0.82f to Color(0xFF5A3A3A), 1f to Color(0xFFB8693F)))
            .clickable(enabled = false) {}
            .padding(start = 56.dp, end = 56.dp, top = 40.dp, bottom = 48.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.height(56.dp).background(Color(0x24FFFFFF), RoundedCornerShape(28.dp)).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LineIconView(LineIcon.Sun, 30.dp, Dawn)
                Label("기상 알람", 26, FontWeight.ExtraBold, warm)
            }
            Label("${place?.name ?: "이곳"} · 차박 모드", 26, FontWeight.SemiBold, Color(0xFFD8D2E6))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Label(DayPart.wakeTitle(now.toLocalTime()), 44, FontWeight.Bold, Dawn)
            Label(now.format(HHMM), 240, FontWeight.ExtraBold, warm, lineHeight = TextUnit.Unspecified)
            Label(now.format(DATE), 32, FontWeight.SemiBold, soft)
            Row(Modifier.padding(top = 36.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rise?.let { RingChip(LineIcon.Sun, Dawn, "일출 ${it.format(HMM)}") }
                snapshot.energy?.let { RingChip(null, null, "배터리 ${it.batteryPercent.roundToInt()}%") }
                snapshot.outsideTempC?.let { RingChip(LineIcon.Thermometer, soft, "바깥 ${it.roundToInt()}°C") }
            }
        }
        val snooze = app.sleepAlerts.snoozeMinutes
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // 말로: "5분 더" · "일어났어". 누르는 동안 알람 소리를 멈추고 듣는다
            VoiceButton(app.listening, size = 136.dp) { app.startVoiceSearch() }
            if (snooze > 0) {
                SecondaryButton(
                    "${snooze}분 뒤 다시", { app.snoozeWake() }, Modifier.weight(1f),
                    height = 136.dp, textSize = 38, icon = LineIcon.Moon, background = Color(0x29FFFFFF), foreground = warm,
                )
            }
            PrimaryButton("일어났어요 · 철수 안내로", app::dismissWake, Modifier.weight(2f), height = 136.dp, textSize = 40, icon = null)
        }
        Label(
            "알람을 끄면 차박 모드는 그대로예요 · 철수는 10에서 체크리스트로", 24, FontWeight.Medium, soft,
            Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp),
        )
    }
}

/** 기상 시각까지 남은 시간 "8시간 50분 뒤"(오늘 지났으면 내일) */
internal fun untilText(wakeMinutes: Int, now: LocalTime): String {
    val left = (millisUntil(wakeMinutes, now) / 60_000).toInt()
    val h = left / 60
    val m = left % 60
    return when {
        h == 0 -> "${m}분 뒤"
        m == 0 -> "${h}시간 뒤"
        else -> "${h}시간 ${m}분 뒤"
    }
}

/** 다음에 맞을 아침의 날짜: 정오 전이면 오늘, 그 뒤면 내일 */
internal fun nextMorning(now: LocalDateTime): LocalDate =
    if (now.hour < 12) now.toLocalDate() else now.toLocalDate().plusDays(1)

/**
 * 빠른 시각 칩: 일출 · 일출 30분 뒤(일출을 모르면 뺀다) · 6:30 · 7:00 · 8:00. 일출은 5분 단위로 내림.
 * 같은 시각이 겹치면 앞의 일출 칩만 남긴다(두 칩이 함께 골라져 보이지 않게)
 */
internal fun quickTimes(sunrise: LocalTime?): List<Pair<String, Int>> {
    val rise = sunrise?.let { (it.hour * 60 + it.minute) / 5 * 5 }
    return listOfNotNull(
        rise?.let { "일출 ${it / 60}:${"%02d".format(it % 60)}" to it },
        rise?.let { "일출 30분 뒤" to it + 30 },
        "6:30" to 6 * 60 + 30,
        "7:00" to 7 * 60,
        "8:00" to 8 * 60,
    ).distinctBy { it.second }
}

@Composable
private fun TimeColumn(value: String, unit: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepBox("+", "$unit 올리기", onUp)
        Label(value, 160, FontWeight.ExtraBold, lineHeight = TextUnit.Unspecified)
        StepBox("−", "$unit 내리기", onDown)
    }
}

@Composable
private fun StepBox(text: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.width(160.dp).height(96.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Sub)
            .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.SegmentTick, onClick))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Label(text, 40, FontWeight.ExtraBold) }
}

@Composable
private fun QuickChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(76.dp).clip(RoundedCornerShape(ChipRadius))
            .background(if (selected) Palette.Text else Color.Transparent)
            .border(2.dp, if (selected) Palette.Text else Palette.Gauge, RoundedCornerShape(ChipRadius))
            .clickable(role = Role.Button, onClick = withHaptic(HapticFeedbackType.SegmentTick, onClick))
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 26, FontWeight.ExtraBold, if (selected) Palette.Background else Palette.Text) }
}

@Composable
private fun OptionCard(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().card(32.dp).padding(horizontal = 32.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Label(title, 28, FontWeight.ExtraBold)
        content()
    }
}

@Composable
private fun OptionTile(title: String, sub: String?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(if (sub != null) 104.dp else 96.dp).clip(RoundedCornerShape(22.dp)).background(Palette.CardInner)
            .border(3.dp, if (selected) SelectedBorder else Color.Transparent, RoundedCornerShape(22.dp))
            .clickable(role = Role.RadioButton, onClick = withHaptic(HapticFeedbackType.SegmentTick, onClick))
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = if (sub != null) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        Label(title, 28, FontWeight.ExtraBold)
        sub?.let { Label(it, 24, FontWeight.Medium, Palette.SubText) }
    }
}

@Composable
private fun Switch(on: Boolean) {
    Box(
        Modifier.width(96.dp).height(52.dp).background(if (on) Palette.Accent else Palette.Gauge, RoundedCornerShape(26.dp)).padding(4.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(44.dp).background(if (on) Palette.OnAccent else Color.White, RoundedCornerShape(22.dp))) }
}

@Composable
private fun RingChip(icon: LineIcon?, tint: Color?, text: String) {
    Row(
        Modifier.height(72.dp).background(Color(0x5208090B), RoundedCornerShape(36.dp)).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null && tint != null) LineIconView(icon, 34.dp, tint)
        Label(text, 28, FontWeight.Bold, Color(0xFFF7F3EE))
    }
}

private val KOREA = ZoneId.of("Asia/Seoul")
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val HMM = DateTimeFormatter.ofPattern("H:mm")
private val DATE = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
private val WakeGreen = Color(0xFF8EE0B4)
private val Dawn = Color(0xFFFFD9A8)
private val SelectedBorder = Color(0xFFDAD1BF)
private val NextStepBg = Color(0xFF241F3A)
private val NextStepChip = Color(0xFF3B2F6B)
private val NextStepFg = Color(0xFFC9B8FF)
private val SNOOZE_CHOICES = listOf(5, 10, 0)
