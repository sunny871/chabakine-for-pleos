package kr.sanho.chabakine_pleos.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import kr.sanho.chabakine_pleos.shared.place.CampingStyle

/**
 * 채운 아이콘 한 장(24 격자, 구멍은 even-odd로 비운다). 색은 Icon의 tint가 정한다.
 * [scale]은 그림이 납작해 작아 보일 때 가운데([pivotY])를 기준으로 키운다.
 */
internal fun filledIcon(name: String, vararg paths: String, scale: Float = 1f, pivotY: Float = 12f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addGroup(scaleX = scale, scaleY = scale, pivotX = 12f, pivotY = pivotY)
        paths.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) }
        clearGroup()
    }.build()

/** 채운 텐트(가운데 문은 비운다). 메뉴 줄 '차박 모드'와 도킹형에 쓴다 */
val TentFilled: ImageVector by lazy {
    filledIcon("TentFilled", "M10.7 3.8Q12 2.2 13.3 3.8L21.6 18.4Q22.4 20 20.6 20L3.4 20Q1.6 20 2.4 18.4ZM12 13.4L14.3 20L9.7 20Z")
}

/** 앞에서 본 차(스텔스: 차 안에서 잔다) */
private val CarFrontFilled: ImageVector by lazy {
    filledIcon(
        "CarFrontFilled",
        "M7.2 4.5H16.8Q17.9 4.5 18.3 5.5L19.8 9.5H20Q21 9.5 21 10.5V17Q21 18 20 18V19.5Q20 20.5 19 20.5H18Q17 20.5 17 19.5V18H7V19.5Q7 20.5 6 20.5H5Q4 20.5 4 19.5V18Q3 18 3 17V10.5Q3 9.5 4 9.5H4.2L5.7 5.5Q6.1 4.5 7.2 4.5Z" +
            "M7.4 6.3H16.6L17.8 9.5H6.2Z" +
            "M5.2 13.5a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0-2.6 0Z" +
            "M16.2 13.5a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0-2.6 0Z" +
            "M9.5 14.6H14.5V15.8H9.5Z",
    )
}

/** 옆에서 본 차 + 지붕 텐트(루프탑형) */
private val CarRooftopFilled: ImageVector by lazy {
    filledIcon(
        "CarRooftopFilled",
        // 차체(바퀴 자리는 둥글게 파고, 창은 비운다)
        "M2 15.6Q2 13.7 3.8 13.3L6.6 12.7L9 10Q9.6 9.4 10.4 9.4H15.6Q16.5 9.4 17.1 10.1L19.5 12.8L20.6 13Q22 13.3 22 14.8V16.6Q22 17.6 21 17.6H19.9A2.4 2.4 0 0 0 15.1 17.6H8.9A2.4 2.4 0 0 0 4.1 17.6H3Q2 17.6 2 16.6Z" +
            "M10.7 10.8H13.1V12.7H8.9Z" +
            "M14.3 10.8H15.4Q15.9 10.8 16.2 11.2L17.5 12.7H14.3Z",
        // 바퀴
        "M4.8 17.6a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0Z",
        "M15.8 17.6a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0Z",
        // 지붕 텐트
        "M9.6 7.8Q9.6 6.2 11.2 6.2H17.3Q18.9 6.2 18.9 7.8V8.4H9.6Z",
        scale = 1.25f,
        pivotY = 12.8f,
    )
}

/** 방식별 아이콘: 스텔스 차 · 도킹형 텐트 · 루프탑형 지붕 텐트 차 */
val CampingStyle.icon: ImageVector
    get() = when (this) {
        CampingStyle.STEALTH -> CarFrontFilled
        CampingStyle.DOCKING -> TentFilled
        CampingStyle.ROOFTOP -> CarRooftopFilled
    }
