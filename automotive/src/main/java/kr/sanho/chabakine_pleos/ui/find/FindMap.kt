package kr.sanho.chabakine_pleos.ui.find

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kr.sanho.chabakine_pleos.BuildConfig
import kr.sanho.chabakine_pleos.vehicle.DrivingState
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.app.Route
import kr.sanho.chabakine_pleos.shared.place.Signal
import kr.sanho.chabakine_pleos.ui.components.Label
import kr.sanho.chabakine_pleos.ui.components.LineIcon
import kr.sanho.chabakine_pleos.ui.components.LineIconView
import kr.sanho.chabakine_pleos.ui.components.PrimaryButton
import kr.sanho.chabakine_pleos.ui.components.SignalLamp
import kr.sanho.chabakine_pleos.ui.home.PlaceItem
import kr.sanho.chabakine_pleos.ui.home.formatKm
import kr.sanho.chabakine_pleos.ui.theme.Palette
import kr.sanho.chabakine_pleos.ui.theme.DarkScope
import kr.sanho.chabakine_pleos.ui.theme.label
import kr.sanho.chabakine_pleos.ui.theme.lampColor
import kr.sanho.chabakine_pleos.shared.place.LatLng as PlaceLatLng

private const val TAG = "FindMap"
private const val DEFAULT_ZOOM = 11
private const val CAMERA_MS = 300

// 처음 열 때 한 화면에 넣을 가까운 장소 수와 가장자리 여백(마커 · 오른쪽 위 버튼이 가리지 않게)
private const val FIT_NEAREST = 5
private const val FIT_PADDING_DP = 140
private const val LAYOUT_WAIT_MS = 600L

/** 화면에서 이 거리(dp, 가로·세로)보다 가까운 장소는 하나로 묶는다. 마커가 가로로 긴 신호등이라 가로를 넓게 */
private const val CLUSTER_RADIUS_X_DP = 170
private const val CLUSTER_RADIUS_Y_DP = 72

/** 묶음을 누르면 이만큼 확대한다 */
private const val CLUSTER_ZOOM_STEP = 2

/**
 * 화면설계서 15 찾기 · 지도 보기(카카오맵 SDK).
 * 지도는 우리 앱에서 보여 주기만 하고, 길안내는 "여기로 안내"로 Pleos 내비에 맡긴다(단일 경로 정책).
 * 키(local.properties의 KAKAO_NATIVE_APP_KEY)가 없거나 지도를 못 열면 안내 문구를 보여 준다.
 */
@Composable
fun FindMap(
    app: AppState,
    items: List<PlaceItem>,
    modifier: Modifier,
    onSelect: (PlaceItem) -> Unit,
    /** 15(11-08): 메뉴 줄 오른쪽을 꽉 채운다(모서리 없음). 확대 버튼은 위에 뜬 줄 아래로 내린다 */
    fullBleed: Boolean = false,
    /** 지도가 메뉴 줄 뒤까지 깔릴 때 그 폭: 고른 곳 카드와 처음 맞추는 범위를 그만큼 오른쪽으로 */
    insetStart: Dp = 0.dp,
) {
    val origin = app.origin
    // 지도는 처음에 아무것도 고르지 않은 채로 연다. 마커를 누르면 고르고,
    // 같은 마커를 다시 누르거나 빈 지도 · 카드의 닫기를 누르면 고른 것을 푼다.
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = items.firstOrNull { it.place.id == pickedId }
    var map by remember { mutableStateOf<KakaoMap?>(null) }
    var zoom by remember { mutableIntStateOf(DEFAULT_ZOOM) }
    var error by remember { mutableStateOf(if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) "지도 키가 없어요 · local.properties에 KAKAO_NATIVE_APP_KEY를 넣어 주세요" else null) }

    Box(modifier.clip(RoundedCornerShape(if (fullBleed) 0.dp else 36.dp)).background(Palette.MapBackground)) {
        if (error == null) {
            KakaoMapView(
                origin = origin,
                onReady = { map = it },
                onError = { error = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(Modifier.align(Alignment.Center).padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("지도를 열지 못했어요", 36, FontWeight.ExtraBold)
                Label(error.orEmpty(), 24, FontWeight.Medium, Palette.SubText, maxLines = 3)
            }
        }

        val context = LocalContext.current
        val markers = remember { MarkerBitmaps(context) }
        // 지도가 막 열렸을 땐 화면 크기가 정해지기 전이라 px 배율을 잘못 잴 수 있어 잠시 뒤 한 번 더 계산한다
        var laidOut by remember { mutableStateOf(false) }
        LaunchedEffect(map) {
            val m = map ?: return@LaunchedEffect
            m.setOnCameraMoveEndListener { _, position, _ -> zoom = position.zoomLevel }
            // 빈 지도를 누르면 고른 것을 푼다(마커를 누를 때는 위 마커 리스너만 불린다)
            m.setOnMapClickListener { _, _, _, _ -> pickedId = null }
            delay(LAYOUT_WAIT_MS)
            zoom = m.zoomLevel
            laidOut = true
        }
        // 확대 단계·장소·선택이 바뀌면 묶음을 다시 계산해 마커를 올린다(수십 곳이라 전부 다시 그려도 가볍다)
        LaunchedEffect(map, items, selected?.place?.id, zoom, laidOut) {
            val m = map ?: return@LaunchedEffect
            val layer = m.labelManager?.layer ?: return@LaunchedEffect
            // 이 확대 단계에서 1도가 몇 px인지 재서, 화면에서 가까운 장소끼리 묶는다.
            // 고른 곳을 맨 앞에 두어 그 묶음의 기준(마커 위치)이 되게 한다
            val scale = m.pixelsPerDegree(origin)
            val density = context.resources.displayMetrics.density
            val clusters = clusterPlaces(
                items.sortedWith(compareBy({ it.place.id != selected?.place?.id }, { it.signal.ordinal })),
                { it.place.latitude }, { it.place.longitude },
                scale.first, scale.second, (CLUSTER_RADIUS_X_DP * density).toDouble(), (CLUSTER_RADIUS_Y_DP * density).toDouble(),
            )
            m.setOnLabelClickListener { _, _, label ->
                when (val tag = label.tag) {
                    is String -> items.firstOrNull { it.place.id == tag }?.let { item ->
                        if (pickedId == item.place.id) {
                            pickedId = null
                        } else {
                            pickedId = item.place.id
                            onSelect(item)
                        }
                    }
                    // 묶음을 누르면 그쪽으로 조금씩 확대한다
                    is MapCluster<*> -> m.moveCamera(
                        CameraUpdateFactory.newCenterPosition(LatLng.from(tag.latitude, tag.longitude), (m.zoomLevel + CLUSTER_ZOOM_STEP).coerceAtMost(m.maxZoomLevel)),
                        CameraAnimation.from(CAMERA_MS),
                    )
                }
                true
            }
            layer.removeAll()
            layer.addLabel(
                LabelOptions.from(LatLng.from(origin.latitude, origin.longitude))
                    .setStyles(LabelStyles.from(LabelStyle.from(markers.origin()).setApplyDpScale(false).setAnchorPoint(0.5f, 0.5f)))
                    .setClickable(false)
                    .setRank(0),
            )
            clusters.forEach { c ->
                val hasSelected = c.items.any { it.place.id == selected?.place?.id }
                if (c.isSingle) {
                    val item = c.items.single()
                    layer.addLabel(placeLabel(markers, item, item.place.name, selected = hasSelected))
                } else {
                    // 고른 곳이 든 묶음은 파란 테두리(iOS 앱의 강조 묶음과 같은 역할)
                    val signals = c.items.map { it.signal }.toSet()
                    layer.addLabel(
                        LabelOptions.from(LatLng.from(c.latitude, c.longitude))
                            .setStyles(LabelStyles.from(LabelStyle.from(markers.cluster(signals, c.items.size, hasSelected)).setApplyDpScale(false).setAnchorPoint(0.5f, 0.5f)))
                            .setTag(c)
                            .setClickable(true)
                            .setRank(if (hasSelected) 900L else 200L + c.items.size),
                    )
                }
            }
        }
        // 고른 곳 없이 열면 내 위치와 가까운 몇 곳이 한 화면에 들어오게 맞춘다(처음 한 번만)
        var fitted by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(map, laidOut, items.isNotEmpty()) {
            val m = map ?: return@LaunchedEffect
            if (fitted || !laidOut || items.isEmpty() || pickedId != null) return@LaunchedEffect
            val points = (listOf(LatLng.from(origin.latitude, origin.longitude)) +
                items.sortedBy { it.distanceKm }.take(FIT_NEAREST).map { LatLng.from(it.place.latitude, it.place.longitude) }).toTypedArray()
            val padding = (FIT_PADDING_DP * context.resources.displayMetrics.density).toInt()
            m.moveCamera(CameraUpdateFactory.fitMapPoints(points, padding), CameraAnimation.from(CAMERA_MS))
            fitted = true
        }
        LaunchedEffect(map, selected?.place?.id) {
            val m = map ?: return@LaunchedEffect
            selected?.let { m.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(it.place.latitude, it.place.longitude)), CameraAnimation.from(CAMERA_MS)) }
        }

        Column(Modifier.align(Alignment.TopEnd).padding(start = 28.dp, end = 28.dp, bottom = 28.dp, top = if (fullBleed) 300.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MapButton(LineIcon.Plus, "확대") { map?.moveCamera(CameraUpdateFactory.zoomIn(), CameraAnimation.from(CAMERA_MS)) }
            MapButton(LineIcon.Minus, "축소") { map?.moveCamera(CameraUpdateFactory.zoomOut(), CameraAnimation.from(CAMERA_MS)) }
            MapButton(LineIcon.Locate, "내 위치로") {
                map?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(origin.latitude, origin.longitude), DEFAULT_ZOOM), CameraAnimation.from(CAMERA_MS))
            }
        }
        selected?.let { SelectedCard(app, it, Modifier.align(Alignment.BottomStart).padding(start = insetStart + 28.dp, end = 28.dp, top = 28.dp, bottom = 28.dp)) { pickedId = null } }
    }
}

/** 한 장소 마커. 고른 곳은 크게·이름과 함께 맨 위에 */
private fun placeLabel(markers: MarkerBitmaps, item: PlaceItem, name: String?, selected: Boolean): LabelOptions =
    LabelOptions.from(item.place.id, LatLng.from(item.place.latitude, item.place.longitude))
        .setStyles(LabelStyles.from(LabelStyle.from(markers.place(item.signal, name, selected)).setApplyDpScale(false).setAnchorPoint(0.5f, 0.3f)))
        .setTag(item.place.id)
        .setClickable(true)
        .setRank(if (selected) 1000 else 100L - item.signal.ordinal)

/**
 * 지금 확대 단계에서 위도·경도 1도가 화면에서 몇 px인지. 국내 범위에선 거의 일정해서 차 위치 근처에서 잰다.
 * 지도가 아직 그려지기 전이면 넉넉한 값을 돌려준다.
 */
private fun KakaoMap.pixelsPerDegree(at: PlaceLatLng): Pair<Double, Double> {
    val a = toScreenPoint(LatLng.from(at.latitude, at.longitude))
    val b = toScreenPoint(LatLng.from(at.latitude + 0.1, at.longitude + 0.1))
    if (a == null || b == null) return 1_000.0 to 1_000.0
    val lat = kotlin.math.abs(b.y - a.y) / 0.1
    val lng = kotlin.math.abs(b.x - a.x) / 0.1
    return if (lat < 1 || lng < 1) 1_000.0 to 1_000.0 else lat to lng
}

/** 카카오 MapView를 화면 수명에 맞춰 resume/pause/finish 한다 */
@Composable
private fun KakaoMapView(origin: PlaceLatLng, onReady: (KakaoMap) -> Unit, onError: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        if (!KakaoMapSdk.isInitialized()) KakaoMapSdk.init(context.applicationContext, BuildConfig.KAKAO_NATIVE_APP_KEY)
        MapView(context).apply {
            start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit
                    override fun onMapError(e: Exception) {
                        // 키 해시 미등록·인증 실패·네트워크 오류 등. 원인은 로그로 남긴다
                        Log.w(TAG, "지도 오류: ${e.message}", e)
                        onError(e.message ?: e.javaClass.simpleName)
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) = onReady(kakaoMap)
                    override fun getPosition(): LatLng = LatLng.from(origin.latitude, origin.longitude)
                    override fun getZoomLevel(): Int = DEFAULT_ZOOM
                },
            )
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

/**
 * 아래 왼쪽: 고른 장소 카드(사진 배경) + 여기로 안내. 신호등은 세로로 세워 카드가 그만큼 높다(설계서 15).
 * 카드를 누르면 목록으로 돌아가 그 장소의 상세를 연다(정차 중 02, 주행 중 01 요약).
 */
@Composable
private fun SelectedCard(app: AppState, item: PlaceItem, modifier: Modifier, onClose: () -> Unit) {
    DarkScope(item.place.photoUrls.isNotEmpty()) {
    Box(
        modifier.width(820.dp).clip(RoundedCornerShape(32.dp)).background(Palette.Card)
            .clickable(role = Role.Button, onClickLabel = "자세히 보기") {
                app.selectedPlaceId = item.place.id
                app.replace(Route.Find(map = false))
            },
    ) {
        RowPhoto(item.place.photoUrls.firstOrNull(), driving = app.drivingState != DrivingState.PARKED)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SignalLamp(item.signal, 40.dp, showSymbol = true, vertical = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(item.place.name, 36, FontWeight.ExtraBold)
                val sub = listOfNotNull(
                    item.signal.label,
                    formatKm(item.distanceKm),
                    item.rating?.let { "★ ${it.average}" },
                    item.place.site?.type,
                ).joinToString(" · ")
                Label(sub, 24, FontWeight.Medium, Palette.SubText)
                Label("눌러서 자세히 보기 ›", 24, FontWeight.Bold, Palette.SoftText)
            }
            PrimaryButton("여기로 안내", { app.navigateTo(item.place) }, height = 112.dp, textSize = 34, trailingChevron = false)
            // 고른 것 풀기(빈 지도를 눌러도 된다). 누르는 칸 88
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(44.dp)).background(Palette.Sub)
                    .clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "선택 풀기" },
                contentAlignment = Alignment.Center,
            ) { LineIconView(LineIcon.Cross, 40.dp, Palette.Text, strokeWidth = 2.4f) }
        }
    }
    }
}

@Composable
private fun MapButton(icon: LineIcon, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(104.dp).clip(RoundedCornerShape(26.dp)).background(Palette.Background.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { LineIconView(icon, 40.dp, Palette.Text, strokeWidth = 2.4f) }
}


/**
 * 지도 마커 그림(설계서 15): 가로 신호등 + 아래 이름표. 고른 곳은 크게, 파란 테두리.
 * 지도 SDK는 비트맵만 받으므로 Canvas로 직접 그리고 같은 모양은 다시 쓴다.
 */
private class MarkerBitmaps(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val cache = HashMap<String, Bitmap>()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.OnPhoto.toArgb() } // 이름표 바탕이 늘 어두운 신호등 몸체라 두 테마 모두 밝은 글자

    private fun px(dp: Float) = dp * density

    fun origin(): Bitmap = cache.getOrPut("origin") {
        val size = px(72f)
        Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888).also { bmp ->
            val c = Canvas(bmp)
            val mid = size / 2
            fill.color = MarkerAccent.copy(alpha = 0.25f).toArgb()
            c.drawCircle(mid, mid, mid, fill)
            fill.color = android.graphics.Color.WHITE
            c.drawCircle(mid, mid, px(20f), fill)
            fill.color = MarkerAccent.toArgb()
            c.drawCircle(mid, mid, px(14f), fill)
        }
    }

    /** [name]이 null이면 이름표 없이 신호등만 */
    fun place(signal: Signal, name: String?, selected: Boolean): Bitmap = cache.getOrPut("$signal|$name|$selected") {
        val lamp = px(if (selected) 36f else 24f)
        val gap = px(if (selected) 10f else 6f)
        val padH = px(if (selected) 16f else 10f)
        val padV = px(if (selected) 12f else 8f)
        val border = if (selected) px(4f) else 0f
        val bodyW = lamp * 3 + gap * 2 + padH * 2 + border * 2
        val bodyH = lamp + padV * 2 + border * 2

        text.textSize = px(24f)
        text.typeface = Typeface.create(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        text.isFakeBoldText = !selected
        val tagPadH = px(if (selected) 14f else 12f)
        val tagPadV = px(if (selected) 6f else 4f)
        val textW = name?.let { text.measureText(it) } ?: 0f
        val metrics = text.fontMetrics
        val tagW = if (name == null) 0f else textW + tagPadH * 2
        val tagH = if (name == null) 0f else (metrics.descent - metrics.ascent) + tagPadV * 2
        val spacing = if (name == null) 0f else px(if (selected) 8f else 6f)

        val w = maxOf(bodyW, tagW)
        val h = bodyH + spacing + tagH
        Bitmap.createBitmap(w.toInt() + 2, h.toInt() + 2, Bitmap.Config.ARGB_8888).also { bmp ->
            val c = Canvas(bmp)
            val left = (w - bodyW) / 2
            if (selected) {
                fill.color = SELECTED_BORDER
                c.drawRoundRect(RectF(left, 0f, left + bodyW, bodyH), bodyH / 2, bodyH / 2, fill)
            }
            fill.color = Palette.LampBody.toArgb()
            c.drawRoundRect(RectF(left + border, border, left + bodyW - border, bodyH - border), bodyH / 2, bodyH / 2, fill)
            val lit = if (signal == Signal.UNKNOWN) null else signal
            listOf(Signal.GREEN, Signal.YELLOW, Signal.RED).forEachIndexed { i, s ->
                fill.color = (if (s == lit) s.lampColor else Palette.LampOff).toArgb()
                val cx = left + border + padH + lamp / 2 + i * (lamp + gap)
                c.drawCircle(cx, border + padV + lamp / 2, lamp / 2, fill)
            }
            if (name == null) return@also
            val tagLeft = (w - tagW) / 2
            val tagTop = bodyH + spacing
            fill.color = Palette.LampBody.toArgb()
            c.drawRoundRect(RectF(tagLeft, tagTop, tagLeft + tagW, tagTop + tagH), px(10f), px(10f), fill)
            c.drawText(name, tagLeft + tagPadH, tagTop + tagPadV - metrics.ascent, text)
        }
    }

    /** 묶음 마커: 안에 있는 신호 색만 켠 신호등 + 오른쪽 개수(iOS 앱과 같은 모양) */
    fun cluster(signals: Set<Signal>, count: Int, selected: Boolean): Bitmap = cache.getOrPut("cluster|${signals.sorted()}|$count|$selected") {
        val lamp = px(28f)
        val gap = px(8f)
        val padH = px(14f)
        val padV = px(10f)
        text.textSize = px(28f)
        text.typeface = Typeface.DEFAULT_BOLD
        text.isFakeBoldText = false
        val label = count.toString()
        val textW = text.measureText(label)
        val metrics = text.fontMetrics
        val border = if (selected) px(4f) else 0f
        val h = lamp + padV * 2 + border * 2
        val w = padH + lamp * 3 + gap * 2 + px(14f) + textW + padH + border * 2
        Bitmap.createBitmap(w.toInt() + 2, h.toInt() + 2, Bitmap.Config.ARGB_8888).also { bmp ->
            val c = Canvas(bmp)
            if (selected) {
                fill.color = SELECTED_BORDER
                c.drawRoundRect(RectF(0f, 0f, w, h), h / 2, h / 2, fill)
            }
            fill.color = Palette.LampBody.copy(alpha = if (selected) 1f else 0.92f).toArgb()
            c.drawRoundRect(RectF(border, border, w - border, h - border), h / 2, h / 2, fill)
            listOf(Signal.GREEN, Signal.YELLOW, Signal.RED).forEachIndexed { i, s ->
                fill.color = (if (s in signals) s.lampColor else Palette.LampOff).toArgb()
                c.drawCircle(border + padH + lamp / 2 + i * (lamp + gap), h / 2, lamp / 2, fill)
            }
            val textX = border + padH + lamp * 3 + gap * 2 + px(14f)
            c.drawText(label, textX, h / 2 - (metrics.ascent + metrics.descent) / 2, text)
        }
    }

    private companion object {
        val SELECTED_BORDER = android.graphics.Color.rgb(0xDA, 0xD1, 0xBF) // 11-08 숲 색감: 선택은 크림
    }
}

/** 지도 위 내 위치 · 선택 테두리: 11-08 숲 색감 크림(지도 위라 두 테마 같음) */
private val MarkerAccent = androidx.compose.ui.graphics.Color(0xFFDAD1BF)
