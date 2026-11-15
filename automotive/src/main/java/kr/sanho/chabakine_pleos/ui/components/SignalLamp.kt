package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.lampColor

/**
 * iOS 앱과 같은 신호등: 램프 3개(초록·노랑·빨강) 중 해당 신호만 켜진다.
 * 미확인은 회색 램프 한 칸에 물음표. [showSymbol]이면 켜진 램프 안에 기호를 넣는다.
 */
@Composable
fun SignalLamp(signal: Signal, lampSize: Dp, showSymbol: Boolean = false, vertical: Boolean = false) {
    val big = lampSize >= 40.dp
    val gap = when {
        big -> if (vertical) 10.dp else 12.dp
        vertical -> 6.dp
        else -> 7.dp
    }
    val padH = when {
        big -> if (vertical) 12.dp else 18.dp
        vertical -> 9.dp
        else -> 11.dp
    }
    val padV = when {
        big -> if (vertical) 16.dp else 14.dp
        vertical -> 8.dp
        else -> 9.dp
    }
    val body = Modifier
        .semantics { contentDescription = signal.label }
        .background(Palette.LampBody, RoundedCornerShape(50))
        .padding(horizontal = padH, vertical = padV)
    val lamps = @Composable {
        if (signal == Signal.UNKNOWN) {
            Lamp(on = true, signal = signal, size = lampSize, showSymbol = true)
        } else {
            listOf(Signal.GREEN, Signal.YELLOW, Signal.RED).forEach {
                Lamp(on = it == signal, signal = it, size = lampSize, showSymbol = showSymbol)
            }
        }
    }
    // 목록 줄은 세로 신호등(화면설계서 14·01·02), 큰 카드는 가로
    if (vertical) {
        Column(body, verticalArrangement = Arrangement.spacedBy(gap)) { lamps() }
    } else {
        Row(body, horizontalArrangement = Arrangement.spacedBy(gap)) { lamps() }
    }
}

@Composable
private fun Lamp(on: Boolean, signal: Signal, size: Dp, showSymbol: Boolean) {
    val color = if (on) signal.lampColor else Palette.LampOff
    Box(
        modifier = Modifier
            .size(size)
            .then(if (on && signal != Signal.UNKNOWN) Modifier.shadow(size / 3, CircleShape, ambientColor = color, spotColor = color) else Modifier)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on && showSymbol) {
            val icon = when (signal) {
                Signal.GREEN -> LineIcon.Check
                Signal.YELLOW -> LineIcon.Alert
                Signal.RED -> LineIcon.Cross
                Signal.UNKNOWN -> LineIcon.Question
            }
            val tint = if (signal == Signal.UNKNOWN) Palette.Text else Palette.LampBody
            LineIconView(icon, size * 0.62f, tint, strokeWidth = 3.2f)
        }
    }
}
