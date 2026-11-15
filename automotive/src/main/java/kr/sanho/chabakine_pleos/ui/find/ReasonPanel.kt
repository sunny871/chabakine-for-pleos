package kr.sanho.chabakine_pleos.ui.find

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.produceState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.ReasonSheet
import kr.sanho.chabakine_pleos.app.SheetTopic
import kr.sanho.chabakine_pleos.data.ReviewPage
import kr.sanho.chabakine_pleos.ui.components.PresetChip
import kr.sanho.chabakine_pleos.shared.place.CampingStyle
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.shared.place.StayRule
import kr.sanho.chabakine_pleos.data.label
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.SecondaryButton
import kr.sanho.chabakine_pleos.ui.components.icon
import kr.sanho.chabakine_pleos.ui.components.rememberReduceMotion
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.textColor
import kr.sanho.chabakine_pleos.ui.theme.label

/**
 * 이유 시트(정차 중): 02 방식 칸 · 취사·불 칸, 계절·시간 · 주의사항 카드, 리뷰 보기를 누르면 오른쪽에서 45% 폭으로 밀려 나온다(iOS는 아래 시트).
 * 바깥(흐린 영역)이나 닫기를 누르면 닫히고, 출발하면 [kr.sanho.chabakine_pleos.app.AppState]가 닫는다. 읽기 전용.
 * [sheet]가 null이 되어도 닫히는 동안은 마지막 내용을 그대로 보여 준다.
 */
@Composable
fun ReasonPanel(app: AppState, sheet: ReasonSheet?) {
    SidePanel(sheet, app::closeReasonSheet) { shown ->
        val target = app.place(shown.placeId) ?: return@SidePanel
        when (val topic = shown.topic) {
            is SheetTopic.Rule -> PanelContent(reasonOf(target, topic.style), app::closeReasonSheet)
            SheetTopic.Season -> NotesContent(target, caution = false, app::closeReasonSheet)
            SheetTopic.Caution -> NotesContent(target, caution = true, app::closeReasonSheet)
            SheetTopic.Reviews -> ReviewsContent(app, target, app::closeReasonSheet)
        }
    }
}

/**
 * 오른쪽에서 45% 폭으로 밀려 나오는 시트 틀(02 이유 시트 · 10-1 축제 상세가 같이 쓴다).
 * 바깥(흐린 영역)을 누르면 [onClose]. [item]이 null이 되어도 닫히는 동안은 마지막 내용을 그대로 보여 준다.
 */
@Composable
fun <T : Any> SidePanel(item: T?, onClose: () -> Unit, content: @Composable (T) -> Unit) {
    val reduceMotion = rememberReduceMotion()
    var last by remember { mutableStateOf<T?>(null) }
    if (item != null) last = item
    val shown = last ?: return
    val visible = item != null

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible,
            enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(OPEN_MS)),
            exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(CLOSE_MS)),
        ) {
            Box(
                Modifier.fillMaxSize().background(Palette.Scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "닫기", onClick = onClose),
            )
        }
        AnimatedVisibility(
            visible,
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(PANEL_WIDTH),
            enter = if (reduceMotion) EnterTransition.None else slideInHorizontally(tween(OPEN_MS, easing = LinearOutSlowInEasing)) { it },
            exit = if (reduceMotion) ExitTransition.None else slideOutHorizontally(tween(CLOSE_MS, easing = FastOutLinearInEasing)) { it },
        ) {
            content(shown)
        }
    }
}

/** 시트에 보여 줄 한 규칙: 무엇일 때 · 신호 · 이유 · 근거 유형 */
private class Reason(val title: String, val icon: @Composable (Color) -> Unit, val signal: Signal, val text: String, val evidence: String?)

private fun reasonOf(place: Place, style: CampingStyle?): Reason {
    val rule = style?.let(place::ruleFor) ?: place.rule(StayRule.COOK)
    // 대표 신호는 차량 내 숙박과 같아서, 스텔스 이유가 비면 장소의 대표 이유를 쓴다
    val text = rule.reason.ifBlank { if (style == CampingStyle.STEALTH) place.reason else "" }
    val evidence = place.evidence.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    return if (style == null) {
        Reason("취사·불일 때", { LineIconView(LineIcon.Fire, 34.dp, it, strokeWidth = 2.2f) }, rule.signal, text, evidence)
    } else {
        val name = if (style == CampingStyle.ROOFTOP) "루프탑형" else style.label
        Reason("${name}일 때", { Icon(style.icon, null, Modifier.size(34.dp), tint = it) }, rule.signal, text, evidence)
    }
}

@Composable
private fun PanelContent(reason: Reason, onClose: () -> Unit) {
    val color = reason.signal.sheetColor
    Column(
        Modifier.fillMaxSize().background(Palette.Card)
            // 열리면 제목을 먼저 읽어 준다
            .semantics { paneTitle = "${reason.title} ${reason.signal.label}" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // 패널 안 누름은 바깥으로 새지 않게
            .padding(start = 40.dp, end = 32.dp, top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 1. 제목 줄: 신호 색 원 안 아이콘 · "○○일 때" · 신호
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(64.dp).background(color.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) { reason.icon(color) }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(reason.title, 24, FontWeight.Bold, Palette.SoftText)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(32.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                        LineIconView(reason.signal.sheetSymbol, 20.dp, Palette.LampBody, strokeWidth = 3f)
                    }
                    Label(reason.signal.label, 40, FontWeight.ExtraBold, color)
                }
            }
            SecondaryButton("닫기", onClose, Modifier.width(140.dp), height = 96.dp, textSize = 28)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 2. 이유 카드: 왼쪽에 신호 색 띠
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Palette.CardInner, RoundedCornerShape(20.dp))
                    .semantics(mergeDescendants = true) {},
            ) {
                Box(Modifier.width(4.dp).fillMaxHeight().padding(vertical = 16.dp).background(color, RoundedCornerShape(2.dp)))
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("이유", 24, FontWeight.ExtraBold, Palette.SubText)
                    if (reason.text.isNotBlank()) {
                        Label(reason.text, 32, FontWeight.Medium, Palette.Text, maxLines = 12, lineHeight = 1.5.em)
                    } else {
                        Label(NO_REASON, 30, FontWeight.Medium, Palette.SubText, maxLines = 4, lineHeight = 1.5.em)
                    }
                }
            }
            // 3. 근거 유형 카드(데이터가 없으면 숨긴다)
            reason.evidence?.let { evidence ->
                Row(
                    Modifier.fillMaxWidth().background(Palette.CardInner, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LineIconView(LineIcon.Shield, 40.dp, Palette.SoftText, strokeWidth = 2f)
                        LineIconView(LineIcon.Check, 18.dp, Palette.SoftText, strokeWidth = 3f)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Label("근거 유형", 24, FontWeight.ExtraBold, Palette.SubText)
                        Label(evidence, 32, FontWeight.SemiBold, Palette.Text, maxLines = 3, lineHeight = 1.4.em)
                    }
                }
            }
        }
        // 4. 맨 아래 안내
        Label("현장 안내판과 관계 기관의 지시가 항상 우선합니다.", 24, FontWeight.Medium, Palette.MutedText, maxLines = 2)
    }
}

/** 계절·시간 · 주의사항 전체: 제목 줄 + 항목마다 카드(글은 자르지 않는다) */
@Composable
private fun NotesContent(place: Place, caution: Boolean, onClose: () -> Unit) {
    val (title, lines, color, icon) =
        if (caution) NoteSheet("주의사항", place.notes, NOTE_CAUTION, LineIcon.Alert)
        else NoteSheet("계절·시간", place.seasonRules, NOTE_SEASON, LineIcon.Info)
    Column(
        Modifier.fillMaxSize().background(Palette.Card)
            .semantics { paneTitle = "${place.name} $title ${lines.size}가지" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(start = 40.dp, end = 32.dp, top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(64.dp).background(color.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                LineIconView(icon, 34.dp, color, strokeWidth = 2.4f)
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(place.name, 24, FontWeight.Bold, Palette.SoftText)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Label(title, 40, FontWeight.ExtraBold, if (caution) color else Palette.Text)
                    Label("${lines.size}가지", 26, FontWeight.Bold, Palette.SubText, Modifier.padding(bottom = 4.dp))
                }
            }
            SecondaryButton("닫기", onClose, Modifier.width(140.dp), height = 96.dp, textSize = 28)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            lines.forEach { line ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Palette.CardInner, RoundedCornerShape(20.dp))
                        .semantics(mergeDescendants = true) {},
                ) {
                    Box(Modifier.width(4.dp).fillMaxHeight().padding(vertical = 16.dp).background(color, RoundedCornerShape(2.dp)))
                    Label(line, 32, FontWeight.Medium, Palette.Text, Modifier.padding(horizontal = 24.dp, vertical = 20.dp), maxLines = Int.MAX_VALUE, lineHeight = 1.5.em)
                }
            }
        }
        Label("현장 안내판과 관계 기관의 지시가 항상 우선합니다.", 24, FontWeight.Medium, Palette.MutedText, maxLines = 2)
    }
}

/** 리뷰 전체(21 대신): 별점 요약 · 최신순/방문 확인만 · 읽어 주기 · 리뷰 카드. 쓰기는 폰 앱에서 */
@Composable
private fun ReviewsContent(app: AppState, place: Place, onClose: () -> Unit) {
    val page by produceState<ReviewPage?>(null, place.id) { value = app.reviews.reviews(place.id) }
    var visitedOnly by remember { mutableStateOf(false) }
    val reviews = page?.items.orEmpty().let { list -> if (visitedOnly) list.filter { it.visited } else list }
    val summary = page?.let { p -> if (p.count > 0) "★ ${"%.1f".format(p.average)} · ${p.count}개" else "아직 없어요" } ?: "불러오는 중"
    Column(
        Modifier.fillMaxSize().background(Palette.Card)
            .semantics { paneTitle = "${place.name} 리뷰 $summary" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(start = 40.dp, end = 32.dp, top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(64.dp).background(REVIEW_STAR.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                LineIconView(LineIcon.Star, 34.dp, REVIEW_STAR, strokeWidth = 2.4f)
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label(place.name, 24, FontWeight.Bold, Palette.SoftText)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Label("리뷰", 40, FontWeight.ExtraBold)
                    Label(summary, 26, FontWeight.Bold, Palette.SubText, Modifier.padding(bottom = 4.dp))
                }
            }
            SecondaryButton("닫기", onClose, Modifier.width(140.dp), height = 96.dp, textSize = 28)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PresetChip("최신순", !visitedOnly, { visitedOnly = false }, height = 96.dp)
            PresetChip("방문 확인만", visitedOnly, { visitedOnly = true }, height = 96.dp)
            Spacer(Modifier.weight(1f))
            SecondaryButton("읽어 주기", { app.speak(reviewSpeech(place, page)) }, height = 96.dp, textSize = 26, icon = LineIcon.Volume)
        }
        if (reviews.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                val filteredOut = visitedOnly && page?.items.orEmpty().isNotEmpty()
                Label(
                    when {
                        page == null -> "리뷰를 불러오는 중이에요"
                        filteredOut -> "방문 확인된 리뷰가 없어요"
                        else -> "아직 리뷰가 없어요"
                    },
                    32, FontWeight.ExtraBold,
                )
                Label(if (filteredOut) "최신순을 누르면 모든 리뷰를 볼 수 있어요" else "다녀온 뒤 폰 앱에서 리뷰를 남겨 주세요", 26, FontWeight.Medium, Palette.SubText)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(reviews) { r ->
                    Column(
                        Modifier.fillMaxWidth().background(Palette.CardInner, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 20.dp)
                            .semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Label("★ ${r.rating}", 24, FontWeight.ExtraBold, REVIEW_STAR)
                            Label(r.nickname, 24, FontWeight.Bold, Palette.SubText)
                            if (r.visited) {
                                Box(Modifier.background(Palette.InfoFill, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 2.dp)) {
                                    Label("방문 확인", 24, FontWeight.Bold, Palette.InfoText)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Label(r.createdAt, 24, FontWeight.Medium, Palette.SubText)
                        }
                        Label(r.text, 32, FontWeight.Medium, Palette.Text, maxLines = Int.MAX_VALUE, lineHeight = 1.5.em)
                    }
                }
            }
        }
        Label("리뷰는 이용자가 쓴 글이에요 · 리뷰 쓰기는 폰 앱에서 해요", 24, FontWeight.Medium, Palette.MutedText, maxLines = 2)
    }
}

private val REVIEW_STAR: Color @Composable @ReadOnlyComposable get() = Palette.Star

private data class NoteSheet(val title: String, val lines: List<String>, val color: Color, val icon: LineIcon)

/** 계절·시간은 차분한 회색, 주의사항은 노랑(조건부 색) */
internal val NOTE_SEASON: Color @Composable @ReadOnlyComposable get() = if (Palette.isDark) Color(0xFF98A69A) else Palette.MutedText
internal val NOTE_CAUTION: Color @Composable @ReadOnlyComposable get() = Palette.Caution

/** 시트의 신호 색(설계: 허용 #2E9E5B · 조건부 #E0A100 · 금지 #D64545 · 미확인 #8A9099) */
private val Signal.sheetColor: Color
    @Composable @ReadOnlyComposable get() = if (!Palette.isDark) textColor else when (this) {
        Signal.GREEN -> Color(0xFF2E9E5B)
        Signal.YELLOW -> Color(0xFFE0A100)
        Signal.RED -> Color(0xFFD64545)
        Signal.UNKNOWN -> Color(0xFF98A69A)
    }

/** 색만으로 가르지 않게 신호마다 기호를 함께 쓴다 */
private val Signal.sheetSymbol: LineIcon
    get() = when (this) {
        Signal.GREEN -> LineIcon.Check
        Signal.YELLOW -> LineIcon.Alert
        Signal.RED -> LineIcon.Cross
        Signal.UNKNOWN -> LineIcon.Question
    }

private const val NO_REASON = "아직 확인하지 못했어요. 허용이 아니니, 가기 전에 관리 기관에 먼저 확인해 주세요."
private const val PANEL_WIDTH = 0.45f
private const val OPEN_MS = 220
private const val CLOSE_MS = 180
