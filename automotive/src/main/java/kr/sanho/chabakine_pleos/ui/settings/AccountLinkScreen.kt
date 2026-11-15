package kr.sanho.chabakine_pleos.ui.settings

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.StatusPill
import kr.sanho.chabakine_pleos.ui.components.card
import kr.sanho.chabakine_pleos.ui.find.BackHeader
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kotlin.random.Random

private val QrInk = Color(0xFF121417)
// QR 종이: 두 테마 모두 밝게(카메라가 읽도록)
private val QrPaper = Color(0xFFF2F3F5)
private val LinkedGreen: Color @Composable @ReadOnlyComposable get() = Palette.Success
private val LinkedPanel: Color @Composable @ReadOnlyComposable get() = Palette.SuccessFill

/**
 * 화면설계서 13 폰으로 계정 연결(정차 중에만). 서버에 차량 연결 API가 아직 없어 시연용이다:
 * 앱이 6자리 코드와 QR 모양을 만들고(5분 뒤 만료), "시연: 폰에서 승인"을 누르면 연결된 것으로 바뀐다.
 * 실제 계정 · 토큰은 오가지 않고, 앱을 다시 켜면 연결이 풀린다([AppState.linkedAccount]).
 */
@Composable
fun AccountLinkScreen(app: AppState, favoritesCount: Int, modifier: Modifier = Modifier) {
    val linked = app.linkedAccount
    Column(modifier.fillMaxSize()) {
        BackHeader(app, "폰으로 계정 연결", "차에서는 비밀번호를 입력하지 않아요. 폰 앱에서 승인해 주세요.") {
            StatusPill(parked = true, text = "정차 중에만")
        }
        Row(Modifier.weight(1f).padding(start = 36.dp, end = 36.dp, top = 8.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.weight(1f).fillMaxHeight().card().padding(44.dp), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                if (linked == null) CodePanel(app) else LinkedPanel(linked)
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Label("이렇게 연결해요", 36, FontWeight.ExtraBold)
                    listOf(
                        "폰에서 차박이네 앱을 열고 로그인해요",
                        "설정 › 차량 연결에서 QR을 찍거나 코드를 입력해요",
                        "폰에서 \"이 차와 연결\"을 승인하면 끝이에요",
                    ).forEachIndexed { i, text ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 96.dp).card(22.dp, Palette.CardInner).padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            val done = linked != null
                            Box(Modifier.size(56.dp).background(if (done) LinkedPanel else Palette.Accent, CircleShape), contentAlignment = Alignment.Center) {
                                if (done) LineIconView(LineIcon.Check, 30.dp, LinkedGreen, strokeWidth = 3f) else Label("${i + 1}", 28, FontWeight.ExtraBold)
                            }
                            Label(text, 28, FontWeight.Bold, maxLines = 2)
                        }
                    }
                    Column(Modifier.padding(top = 8.dp).fillMaxWidth().card(22.dp, Palette.CardInner).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Label("연결하면 이어지는 것", 26, FontWeight.ExtraBold)
                        Label(
                            "폰에서 저장한 즐겨찾기를 차에서 보고, 차에서 남긴 도착 기록이 폰의 다녀온 곳·메달에 반영돼요. 위치는 보내지 않고 장소 번호와 날짜만 저장해요.",
                            24, FontWeight.Medium, Palette.SoftText, maxLines = 4,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Label("시연용 화면 · 서버에 차량 연결 API가 생기면 실제 계정과 연결돼요", 22, FontWeight.Medium, Palette.MutedText, maxLines = 2)
                }
            }
            AccountCard(app, linked, favoritesCount, Modifier.width(420.dp))
        }
    }
}

/** 연결 전: 시연용 QR · 6자리 코드 · 남은 시간, 새 코드 · 시연 승인 */
@Composable
private fun CodePanel(app: AppState) {
    var code by remember { mutableStateOf(newCode()) }
    var expiresAt by remember { mutableLongStateOf(System.currentTimeMillis() + CODE_VALID_MS) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiresAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val left = ((expiresAt - now) / 1000).coerceAtLeast(0)
    val expired = left == 0L
    Column(Modifier.width(420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(320.dp).background(QrPaper, RoundedCornerShape(28.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
            DemoQr(code, Modifier.fillMaxSize().semantics { contentDescription = "시연용 QR 모양" })
            if (expired) Box(Modifier.matchParentSize().background(Color(0xD9F2F3F5)), contentAlignment = Alignment.Center) {
                Label("코드가 만료됐어요", 30, FontWeight.ExtraBold, QrInk)
            }
        }
        Label("또는 코드 입력", 24, FontWeight.Bold, Palette.SubText)
        Label(code.chunked(3).joinToString(" "), 60, FontWeight.ExtraBold, if (expired) Palette.MutedText else Palette.Text)
        Label(
            if (expired) "새 코드를 받아 주세요" else "%d:%02d 뒤 만료 · 시연용(찍어도 연결되지 않아요)".format(left / 60, left % 60),
            22, FontWeight.SemiBold, Palette.BannerText, maxLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("새 코드", {
                code = newCode()
                expiresAt = System.currentTimeMillis() + CODE_VALID_MS
            }, Modifier.weight(1f), height = 96.dp, textSize = 26)
            PrimaryButton("시연: 폰에서 승인", app::demoLinkAccount, Modifier.weight(1.6f), height = 96.dp, textSize = 26, icon = null, trailingChevron = false, enabled = !expired)
        }
    }
}

/** 연결 뒤: 연결됨 표시 */
@Composable
private fun LinkedPanel(nickname: String) {
    Column(
        Modifier.width(420.dp).fillMaxHeight().background(LinkedPanel, RoundedCornerShape(28.dp)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(120.dp).background(Palette.Success.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            LineIconView(LineIcon.Check, 64.dp, LinkedGreen, strokeWidth = 3f)
        }
        Label("연결됐어요", 44, FontWeight.ExtraBold, LinkedGreen)
        // Label은 가운데 정렬이 없어 두 줄로 넘치면 왼쪽에 붙는다: 한 줄씩 나눠 가운데에 둔다
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label(nickname, 30, FontWeight.ExtraBold)
            Label("계정과 이 차가 이어졌어요", 24, FontWeight.Bold, Palette.SoftText)
        }
        Label("앱을 다시 켜면 연결이 풀려요(시연)", 22, FontWeight.Medium, Palette.MutedText)
    }
}

/** 오른쪽 계정 카드: 연결 전엔 미리보기, 연결 뒤엔 닉네임 · 연결 끊기 */
@Composable
private fun AccountCard(app: AppState, linked: String?, favoritesCount: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label(if (linked == null) "연결된 뒤 (미리보기)" else "연결된 계정", 26, FontWeight.ExtraBold, Palette.SubText)
        Column(Modifier.fillMaxWidth().card(32.dp).padding(32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(88.dp).background(if (linked == null) Palette.Sub else LinkedPanel, CircleShape), contentAlignment = Alignment.Center) {
                    LineIconView(LineIcon.Phone, 44.dp, if (linked == null) Palette.SubText else LinkedGreen)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label(linked ?: "닉네임", 30, FontWeight.ExtraBold)
                    Label(if (linked == null) "연결 안 됨" else "연결됨 · 시연", 22, FontWeight.SemiBold, if (linked == null) Palette.SubText else LinkedGreen)
                }
            }
            Label(
                if (linked == null) "이 기기 즐겨찾기 ${favoritesCount}곳 · 다녀온 곳은 연결 후" else "이 기기 즐겨찾기 ${favoritesCount}곳 · 폰 즐겨찾기와 다녀온 곳은 서버 연결 뒤에 이어져요",
                24, FontWeight.Medium, Palette.SoftText, maxLines = 3,
            )
            SecondaryButton(
                "연결 끊기 (로그아웃)",
                { if (linked == null) app.notice("아직 연결된 계정이 없어요") else app.unlinkAccount() },
                Modifier.fillMaxWidth(), height = 104.dp, textSize = 30,
            )
            Label("운전자가 바뀌면 바로 끊을 수 있어요. 토큰은 기기의 보안 저장소에만 둬요.", 22, FontWeight.Medium, Palette.MutedText, maxLines = 2)
        }
    }
}

/**
 * 시연용 QR 모양: 코드로 칸을 채운 25×25 격자와 세 모서리 위치 표시.
 * 실제 QR 인코딩이 아니라 찍어도 읽히지 않는다(서버 연결 주소가 생기면 진짜 QR로 바꾼다).
 */
@Composable
private fun DemoQr(code: String, modifier: Modifier) {
    val cells = remember(code) {
        val rnd = Random(code.hashCode())
        List(QR_SIZE * QR_SIZE) { rnd.nextBoolean() }
    }
    Canvas(modifier) {
        val cell = size.minDimension / QR_SIZE
        fun finder(cx: Int, cy: Int) = cx in 0..6 && cy in 0..6 || cx in QR_SIZE - 7 until QR_SIZE && cy in 0..6 || cx in 0..6 && cy in QR_SIZE - 7 until QR_SIZE
        for (y in 0 until QR_SIZE) for (x in 0 until QR_SIZE) {
            if (finder(x, y)) continue
            if (cells[y * QR_SIZE + x]) drawRect(QrInk, Offset(x * cell, y * cell), Size(cell, cell))
        }
        // 세 모서리 위치 표시: 7칸 테두리 + 3칸 가운데
        listOf(0 to 0, QR_SIZE - 7 to 0, 0 to QR_SIZE - 7).forEach { (fx, fy) ->
            drawRect(QrInk, Offset(fx * cell, fy * cell), Size(7 * cell, 7 * cell))
            drawRect(QrPaper, Offset((fx + 1) * cell, (fy + 1) * cell), Size(5 * cell, 5 * cell))
            drawRect(QrInk, Offset((fx + 2) * cell, (fy + 2) * cell), Size(3 * cell, 3 * cell))
        }
    }
}

private fun newCode(): String = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')

private const val QR_SIZE = 25
private const val CODE_VALID_MS = 5 * 60_000L
