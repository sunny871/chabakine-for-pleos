package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/*
 * Pleos 디자인 가이드 Foundations › Interaction › Button interaction(11-11):
 * 누를 수 있는 것은 누르는 동안 95%로 줄고 바탕색이 바뀐다. 손가락이 영역 밖으로 나가면 취소.
 * - 바탕색: [PressOverlayIndication]을 AppRoot에서 LocalIndication으로 깔아 모든 clickable에 들어간다(기본 물결 대신)
 * - 줄어들기: 바탕까지 같이 줄어야 해서 [pressShrink]를 각 버튼 modifier 맨 앞에 둔다(공용 버튼 · 칩 · 메뉴 줄 · 목록 줄)
 */

/** 누르는 동안 덮는 색. 야간은 흰색 8%, 주간은 짙은 숲 6% */
class PressOverlayIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressOverlayNode(interactionSource, color)
    override fun equals(other: Any?) = other is PressOverlayIndication && other.color == color
    override fun hashCode() = color.hashCode()
}

private class PressOverlayNode(private val source: InteractionSource, private val color: Color) : Modifier.Node(), DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            source.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> presses += i
                    is PressInteraction.Release -> presses -= i.press
                    is PressInteraction.Cancel -> presses -= i.press
                }
                val now = presses.isNotEmpty()
                if (now != pressed) {
                    pressed = now
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) drawRect(color)
    }
}

/**
 * 누르는 동안 95%로 줄인다. 바탕까지 줄도록 modifier 맨 앞에 둔다.
 * 누름만 지켜보고 터치는 가로채지 않는다. 손가락이 움직이면(스크롤) · 영역 밖으로 나가면 바로 되돌린다.
 */
fun Modifier.pressShrink(enabled: Boolean = true): Modifier = if (!enabled) this else composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) PRESS_SCALE else 1f, tween(PRESS_MS), label = "pressShrink")
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                val slop = viewConfiguration.touchSlop
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    val moved = (change.position - down.position).getDistance() > slop
                    val outside = change.position.x < 0 || change.position.y < 0 || change.position.x > size.width || change.position.y > size.height
                    if (moved || outside) pressed = false
                    if (!change.pressed) break
                }
                pressed = false
            }
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
}

private const val PRESS_SCALE = 0.95f
private const val PRESS_MS = 100
