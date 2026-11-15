package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.sanho.chabakine_pleos.ui.theme.Palette

/** 설계서 글자 한 줄. 크기는 설계서 px(160dpi에서 dp와 같다)를 그대로 쓴다. */
@Composable
fun Label(
    text: String,
    size: Int,
    weight: FontWeight = FontWeight.Bold,
    color: Color = Palette.Text,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    lineHeight: TextUnit = 1.2.em,
    /** 사진 위 글자처럼 바탕이 밝을 수 있을 때 [PhotoTextShadow] */
    shadow: Shadow? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, lineHeight = lineHeight, shadow = shadow),
    )
}

/** 사진 위 글자 그림자: 밝은 하늘·모래 위에서도 글자 테두리가 보이게 짙고 넓게 */
val PhotoTextShadow = Shadow(Color(0xCC000000), Offset(0f, 2f), blurRadius = 12f)

/** 화면설계서 24 오프라인·데이터 상태 띠. 최신일 때는 [Hidden]으로 숨긴다. */
sealed interface DataStatus {
    data object Hidden : DataStatus

    /** A. 연결 끊김 · 저장된 데이터로 보여 줌 */
    data class Offline(val receivedDate: String) : DataStatus

    /** B. 받은 적 없음 · 앱에 들어 있는 기본 데이터 */
    data object Bundled : DataStatus

    /** C. 받는 중 */
    data object Loading : DataStatus
}

/** 임시검증 띠 바로 아래 한 줄. 색은 회색 계열만 쓴다(빨강·초록은 신호 전용). [onRetry]가 있으면 버튼을 둔다(정차 중에만). */
@Composable
fun DataStatusStrip(status: DataStatus, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val (icon, text) = when (status) {
        DataStatus.Hidden -> return
        is DataStatus.Offline -> LineIcon.WifiOff to "오프라인 · ${status.receivedDate}에 받은 데이터로 보여 줘요"
        DataStatus.Bundled -> LineIcon.Package to "기본 데이터 · 연결되면 최신 정보로 바꿔요"
        DataStatus.Loading -> LineIcon.Refresh to "새 정보를 받는 중이에요 · 지금 보이는 목록은 그대로 쓸 수 있어요"
    }
    val showRetry = status is DataStatus.Offline && onRetry != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 버튼이 있으면 터치 영역 최소 96dp를 지키도록 띠를 키운다
            .height(if (showRetry) 112.dp else 64.dp)
            .background(Palette.Sub, RoundedCornerShape(16.dp))
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LineIconView(icon, 34.dp, Palette.Text)
        Label(text, 26, FontWeight.Bold, modifier = Modifier.weight(1f))
        if (showRetry) {
            Box(
                Modifier
                    .height(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.Background)
                    .clickable(role = Role.Button) { onRetry?.invoke() }
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) { Label("다시 연결", 24) }
        }
    }
}

/** 모든 화면 상단의 데이터 안내 띠. 주의(노랑)와 헷갈리지 않게 회색 */
@Composable
fun DemoBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Palette.CardInner, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Label(text, 24, FontWeight.SemiBold, Palette.MutedText)
    }
}

/**
 * 주행 중 사진 위에 한 겹 더 까는 어두운 막. Pleos 주행 안전 가이드(이미지는 정보에 필요한 만큼만, 장식은 지양)에 맞춰
 * 주행 중에는 사진이 눈에 덜 띄게 한다. 정차 중이면 아무것도 그리지 않는다. 사진과 기존 그라데이션 다음에 둔다.
 */
@Composable
fun BoxScope.DrivingPhotoDim(driving: Boolean) {
    if (driving) Box(Modifier.matchParentSize().background(DrivingDim))
}

private val DrivingDim = Color(0x8C08090B)
