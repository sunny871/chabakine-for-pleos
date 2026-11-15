package kr.sanho.chabakine_pleos.ui.settings

import kr.sanho.chabakine_pleos.ui.components.PleosSwitch
import kr.sanho.chabakine_pleos.ui.components.pressShrink
import androidx.compose.runtime.ReadOnlyComposable
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Features
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.data.ServerEnv
import kr.sanho.chabakine_pleos.data.PlaceCatalog
import kr.sanho.chabakine_pleos.data.PlacesState
import kr.sanho.chabakine_pleos.data.Settings
import kr.sanho.chabakine_pleos.data.ThemePref
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.ChipRadius
import kr.sanho.chabakine_pleos.ui.components.icon
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.selection.toggleable
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.ScreenHeader
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.vehicle.DrivingState
import kr.sanho.chabakine_pleos.vehicle.VehicleSnapshot


/** 화면설계서 12 설정(정차 중에만, 주행 중 잠금) */
@Composable
fun SettingsScreen(
    app: AppState,
    settings: Settings,
    places: PlacesState,
    snapshot: VehicleSnapshot,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    Column(modifier.fillMaxSize()) {
        ScreenHeader("설정", null) { StatusPill(parked = true) }
        Row(Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 8.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Section("내 차박 방식") {
                    Label("여러 개 고를 수 있어요. 대표 방식이 목록의 신호 기준이에요.", 24, FontWeight.Medium, Palette.SubText)
                    StyleChecks(settings) { styles -> app.prefs.update { it.copy(styles = styles) } }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Label("대표 방식", 28, FontWeight.Bold, modifier = Modifier.weight(1f))
                        Segment(settings.styles.sortedBy { it.ordinal }.map { it to it.label }, settings.primaryStyle) { s ->
                            app.prefs.update { it.copy(primaryStyle = s) }
                        }
                    }
                }
                Section("화면과 음성") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Label("화면 테마", 28, FontWeight.Bold, modifier = Modifier.weight(1f))
                        Segment(
                            listOf(ThemePref.FOLLOW_CAR to "자동", ThemePref.DAY to "주간", ThemePref.NIGHT to "야간"),
                            settings.theme,
                        ) { t -> app.prefs.update { it.copy(theme = t) } }
                    }
                    ToggleRow("장소를 고르면 요약 읽어 주기", settings.readSummaryOnSelect) { v -> app.prefs.update { it.copy(readSummaryOnSelect = v) } }
                    if (app.campModeAvailable) {
                        ToggleRow("정차하면 차박 모드를 켤지 묻기", settings.askCampModeOnPark) { v -> app.prefs.update { it.copy(askCampModeOnPark = v) } }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().card(32.dp).padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Label("계정", 30, FontWeight.ExtraBold)
                        val linked = app.linkedAccount
                        Label(
                            when {
                                linked != null -> "연결됨 · $linked (시연)"
                                Features.ACCOUNT_LINK -> "연결 안 됨 · 이 기기에만 저장 중"
                                else -> "연결 안 됨 · 이 기기에만 저장 중 · 계정 연결은 다음 단계"
                            },
                            24, FontWeight.Medium, Palette.SubText, maxLines = 2,
                        )
                    }
                    if (Features.ACCOUNT_LINK) {
                        SecondaryButton(
                            if (app.linkedAccount != null) "연결 관리" else "폰으로 연결", { app.navigate(Route.AccountLink) },
                            height = 96.dp, textSize = 28, background = Palette.Accent, foreground = Palette.OnAccent,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Section("위치와 차량 정보") {
                    val readable = snapshot.drivingState != DrivingState.UNKNOWN
                    ValueRow("주행·정차 감지", if (readable) "사용 중" else "읽을 수 없음", if (readable) Palette.Success else Palette.BannerText)
                    ValueRow(
                        "차종",
                        when (snapshot.isElectric) {
                            true -> "전기차 · 차박 모드 사용"
                            false -> "전기차 아님 · 차박 모드 없음"
                            null -> "읽지 못함 · 차박 모드 숨김"
                        },
                        Palette.SubText,
                    )
                    // 11-10 밤사이 배터리 기록: 차박할수록 아침 예상이 정확해진다. 이 기기에만 둔다
                    if (snapshot.isElectric == true) {
                        var logVersion by remember { mutableIntStateOf(0) }
                        val nights = remember(logVersion) { app.batteryLog.nights }
                        ValueRow("밤사이 배터리 기록", if (nights.isEmpty()) "아직 없음" else "${nights.size}밤 · 이 기기에만", Palette.SubText)
                        if (nights.isNotEmpty()) {
                            SecondaryButton("배터리 기록 지우기", { app.clearBatteryLog(); logVersion++ }, Modifier.fillMaxWidth(), height = 96.dp, textSize = 28)
                        }
                    }
                }
                Section("장소 데이터") {
                    val (sourceText, sourceColor) = when (places.source) {
                        PlaceCatalog.Source.CACHE -> "서버 · ${places.places.size}곳" to Palette.SoftText
                        PlaceCatalog.Source.BUNDLE -> "시연용 기본 데이터 · ${places.places.size}곳" to Palette.BannerText
                    }
                    ValueRow("현재 데이터", sourceText, sourceColor)
                    ValueRow("마지막 확인", places.checkedAtMillis?.let { DateFormat.format("yyyy-MM-dd HH:mm", it).toString() } ?: "받은 적 없음")
                    SecondaryButton(
                        "지금 새로 받기",
                        { app.refreshData(force = true) },
                        Modifier.fillMaxWidth(), height = 96.dp, textSize = 28, icon = LineIcon.Refresh,
                        loading = places.refreshing,
                    )
                }
                Section("정보", gap = 8) {
                    LinkRow("사용법 다시 보기") { app.navigate(Route.Onboarding(1, replay = true)) }
                    LinkRow("개인정보처리방침") { app.notice("차량용 개인정보처리방침은 준비 중이에요") }
                    LinkRow("이용약관 · 오픈소스 라이선스") { app.notice("이용약관과 오픈소스 라이선스는 준비 중이에요") }
                    // 버전을 7번 누르면 개발자 모드(안드로이드 관례). 켜져 있으면 아래 개발자 칸이 보인다
                    var taps by remember { mutableIntStateOf(0) }
                    Label(
                        "버전 $version" + if (settings.developerMode) " · 개발자 모드" else "", 22, FontWeight.Medium, Palette.MutedText,
                        Modifier.clickable(interactionSource = null, indication = null) {
                            if (settings.developerMode) return@clickable
                            taps++
                            if (taps >= DEVELOPER_TAPS) {
                                taps = 0
                                app.setDeveloperMode(true)
                            }
                        },
                    )
                }
                if (settings.developerMode) {
                    Section("개발자") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Label("서버", 28, FontWeight.Bold, modifier = Modifier.weight(1f))
                            Segment(ServerEnv.entries.map { it to it.label }, settings.server) { app.selectServer(it) }
                        }
                        Label(settings.server.baseUrl, 22, FontWeight.Medium, Palette.SubText)
                        Label("서버를 바꾸면 받아 둔 장소 데이터를 지우고 새 서버에서 처음부터 받아요", 22, FontWeight.Medium, Palette.MutedText, maxLines = 2)
                        BatteryLogDebug(app)
                        SecondaryButton("개발자 모드 끄기", { app.setDeveloperMode(false) }, Modifier.fillMaxWidth(), height = 96.dp, textSize = 28)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, gap: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().card(32.dp).padding(32.dp), verticalArrangement = Arrangement.spacedBy(gap.dp)) {
        Label(title, 30, FontWeight.ExtraBold)
        content()
    }
}

@Composable
private fun StyleChecks(settings: Settings, onChange: (Set<CampingStyle>) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CampingStyle.entries.forEach { s ->
            val on = s in settings.styles
            // Pleos Toggle button: 앞 아이콘 + 이름, 켜짐은 포인트 색 + 그림자 + 뒤 ✓(색만으로 구분하지 않게)
            val shape = RoundedCornerShape(ChipRadius)
            val fg = if (on) Palette.OnAccent else Palette.Text
            Row(
                Modifier
                    .weight(1f)
                    .pressShrink()
                    .height(104.dp)
                    .shadow(if (on) 8.dp else 0.dp, shape)
                    .clip(shape)
                    .background(if (on) Palette.Accent else Palette.CardInner)
                    .toggleable(value = on, role = Role.Switch) {
                        // 마지막 하나는 끌 수 없다
                        if (!(on && settings.styles.size == 1)) onChange(if (on) settings.styles - s else settings.styles + s)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Icon(s.icon, contentDescription = null, tint = fg, modifier = Modifier.size(40.dp))
                Label(s.label, 30, if (on) FontWeight.ExtraBold else FontWeight.Bold, fg)
                if (on) LineIconView(LineIcon.Check, 32.dp, fg, strokeWidth = 3f)
            }
        }
    }
}

@Composable
fun CheckBox(checked: Boolean, size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(if (checked) Palette.Accent else Color.Transparent)
            .border(3.dp, if (checked) Palette.Accent else Palette.ChipBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { if (checked) LineIconView(LineIcon.Check, (size * 0.75f).dp, Palette.OnAccent, strokeWidth = 3f) }
}

@Composable
fun <T> Segment(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.background(Palette.Background, RoundedCornerShape(18.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, text) ->
            val on = value == selected
            Box(
                Modifier.pressShrink().height(96.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Palette.Text else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(value) }.padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center,
            ) { Label(text, 26, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) Palette.Background else Palette.Text) }
        }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(96.dp).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Label(text, 28, FontWeight.Bold, modifier = Modifier.weight(1f))
        // 11-11 Pleos 가이드: 켜기 · 끄기는 스위치(여러 개 고르기만 체크박스)
        PleosSwitch(checked)
    }
}

@Composable
private fun ValueRow(title: String, value: String, valueColor: Color = Palette.SoftText, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(if (onClick != null) 96.dp else 64.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Label(title, 28, FontWeight.Bold, modifier = Modifier.weight(1f))
        Label(value, 26, FontWeight.SemiBold, valueColor)
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(96.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(text, 28, FontWeight.Bold, modifier = Modifier.weight(1f))
        LineIconView(LineIcon.ChevronRight, 36.dp, Palette.SubText)
    }
}

/** 설정 "버전"을 이만큼 누르면 개발자 모드 */
private const val DEVELOPER_TAPS = 7

/**
 * 개발자 칸 · 밤사이 배터리 기록 표(11-10). 실차를 쓸 수 있을 때 [BatteryForecast]의 어림값(W)을 맞추는 용도.
 * "logcat으로 내보내기"는 태그 BatteryLog로 CSV를 찍는다(서버로 보내지 않음).
 */
@Composable
private fun BatteryLogDebug(app: AppState) {
    val nights = remember { app.batteryLog.nights }
    val samples = remember { app.batteryLog.samples }
    Label("밤사이 배터리 기록 · 지금 밤 표본 ${samples.size}개 · 요약 ${nights.size}밤", 24, FontWeight.Bold)
    nights.take(8).forEach { n ->
        Label(
            "${DateFormat.format("MM-dd HH:mm", n.endMs)} · 바깥 ${n.tempBand?.let { "$it~${it + 5}°C" } ?: "모름"} · 공조 ${if (n.climateOn) "맞춤" else "모름"} · " +
                "${"%.1f".format(n.hours)}시간 · 시간당 ${"%.2f".format(n.ratePerHour)}%",
            22, FontWeight.Medium, Palette.SubText,
        )
    }
    SecondaryButton(
        "logcat으로 내보내기",
        {
            android.util.Log.i("BatteryLog", "kind,time,percent_or_rate,temp_or_band,climate,hours")
            samples.forEach { android.util.Log.i("BatteryLog", "sample,${it.atMs},${it.percent},${it.outsideTempC ?: ""},${it.climateOn},") }
            nights.forEach { android.util.Log.i("BatteryLog", "night,${it.endMs},${it.ratePerHour},${it.tempBand ?: ""},${it.climateOn},${it.hours}") }
            app.notice("logcat에 BatteryLog로 ${samples.size + nights.size}줄을 남겼어요")
        },
        Modifier.fillMaxWidth(), height = 96.dp, textSize = 28,
    )
}
