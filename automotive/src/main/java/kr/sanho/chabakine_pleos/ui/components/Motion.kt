package kr.sanho.chabakine_pleos.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** 시스템 "애니메이션 배율"이 0(동작 줄이기)이면 true. 그때는 움직임 없이 한 번에 보여 준다 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * 화면에 들어올 때 차례로 나타나기: [index]번째는 [startMs] + [index] × [stepMs] 뒤에
 * 조금 아래에서 올라오며 보인다. 동작 줄이기면 처음부터 보인다.
 */
@Composable
fun Modifier.riseIn(index: Int, reduceMotion: Boolean, startMs: Int = 0, stepMs: Int = 110): Modifier {
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay((startMs + index * stepMs).toLong())
        progress.animateTo(1f, tween(RISE_MS, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * RISE_DISTANCE.toPx()
    }
}

private const val RISE_MS = 420
private val RISE_DISTANCE = 28.dp
