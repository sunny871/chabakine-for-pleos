package kr.sanho.chabakine_pleos.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.Rating
import kr.sanho.chabakine_pleos.shared.place.distanceKm
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 우리 서버의 로그인 없는 GET. 실패하면 null(앱은 기능만 숨기고 계속 동작한다). */
internal object ServerGet {
    suspend fun text(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(Server.baseUrl + path).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 10_000
                c.readTimeout = 20_000
                if (c.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                c.inputStream.bufferedReader().use { it.readText() }
            } finally {
                c.disconnect()
            }
        }.onFailure { Log.i("ServerGet", "$path 실패: ${it.javaClass.simpleName}") }.getOrNull()
    }
}

data class Review(
    val rating: Int,
    val text: String,
    val nickname: String,
    val createdAt: String,
    val visited: Boolean,
)

data class ReviewPage(val count: Int, val average: Double, val visitedCount: Int, val items: List<Review>)

/**
 * 장소 리뷰(server-api.md 1-2장, 로그인 불필요). 리뷰 쓰기는 폰 앱에서 한다.
 * 서버에서 별점을 받지 못하면 [fallbackRatings](시연용)를 쓴다.
 */
class ReviewsRepository(private val fallbackRatings: Map<String, Rating>) {

    private val _ratings = MutableStateFlow<Map<String, Rating>>(emptyMap())
    val ratings: StateFlow<Map<String, Rating>> = _ratings.asStateFlow()

    /** 서버 별점을 받았는지(받았으면 시연용 값이 아니다) */
    private val _fromServer = MutableStateFlow(false)
    val fromServer: StateFlow<Boolean> = _fromServer.asStateFlow()

    /** 서버를 바꿀 때 지난 서버의 별점을 지운다 */
    fun clear() {
        _ratings.value = emptyMap()
        _fromServer.value = false
    }

    suspend fun refreshRatings(useFallbackOnFailure: Boolean) {
        val json = ServerGet.text("/v1/reviews/ratings")
        val parsed = json?.let { runCatching { parseRatings(JSONObject(it)) }.getOrNull() }
        if (parsed != null) {
            _ratings.value = parsed
            _fromServer.value = true
        } else if (useFallbackOnFailure) {
            _ratings.value = fallbackRatings
            _fromServer.value = false
        }
    }

    suspend fun reviews(placeId: String): ReviewPage? {
        val json = ServerGet.text("/v1/places/$placeId/reviews") ?: return null
        return runCatching {
            val o = JSONObject(json)
            val summary = o.optJSONObject("summary")
            val items = o.optJSONArray("items") ?: JSONArray()
            ReviewPage(
                count = summary?.optInt("count") ?: 0,
                average = summary?.optDouble("average") ?: 0.0,
                visitedCount = summary?.optInt("visitedCount") ?: 0,
                items = (0 until items.length()).map { i ->
                    val r = items.getJSONObject(i)
                    Review(
                        rating = r.optInt("rating"),
                        text = r.optString("text"),
                        nickname = r.optString("nickname").ifBlank { "이용자" },
                        createdAt = r.optString("createdAt").take(10),
                        visited = r.optBoolean("visited"),
                    )
                },
            )
        }.getOrNull()
    }

    private fun parseRatings(o: JSONObject): Map<String, Rating> {
        val places = o.optJSONObject("places") ?: return emptyMap()
        return places.keys().asSequence().associateWith { id ->
            val r = places.getJSONObject(id)
            Rating(r.optInt("count"), r.optDouble("average"))
        }
    }
}

data class Festival(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val location: LatLng,
    val imageUrl: String? = null,
    val imageCredit: String? = null,
    // 10-1 축제 상세 시트에 보여 줄 내용(없으면 null)
    val region: String? = null,
    val address: String? = null,
    val overview: String? = null,
    val phone: String? = null,
    val homepage: String? = null,
    val source: String? = null,
)

/**
 * 오늘 열리고 있는 축제인지(시작일 ≤ 오늘 ≤ 종료일). 10-1 "오늘, 주변"에는 이런 축제만 보여 준다
 * (QA-11: 한 달 뒤 시작하는 축제도 섞여 나왔다). 날짜를 읽을 수 없으면 보여 주지 않는다.
 */
fun Festival.isOngoing(today: java.time.LocalDate): Boolean = runCatching {
    !java.time.LocalDate.parse(startDate).isAfter(today) && !java.time.LocalDate.parse(endDate).isBefore(today)
}.getOrDefault(false)

/** 관리자가 검수한 축제(server-api.md 4장). 받은 파일을 기기에 두고 다음에는 그것부터 쓴다. */
class FeedRepository(context: Context) {
    private val dir = File(context.filesDir, "feeds").apply { mkdirs() }

    private val _festivals = MutableStateFlow(readCached("festivals.json")?.let(::parseFestivals).orEmpty())
    val festivals: StateFlow<List<Festival>> = _festivals.asStateFlow()

    suspend fun refresh() {
        ServerGet.text("/static/festivals/festivals.json")?.let { json ->
            runCatching { parseFestivals(json) }.getOrNull()?.let {
                _festivals.value = it
                cache("festivals.json", json)
            }
        }
    }

    /** 서버를 바꿀 때 지난 서버의 축제를 지운다(예전 버전이 받아 둔 명소 파일도 함께) */
    suspend fun clear() = withContext(Dispatchers.IO) {
        listOf("festivals.json", "attractions.json").forEach { File(dir, it).delete() }
        _festivals.value = emptyList()
    }

    private fun readCached(name: String) = runCatching { File(dir, name).readText() }.getOrNull()

    private suspend fun cache(name: String, json: String) = withContext(Dispatchers.IO) {
        runCatching { File(dir, name).writeText(json) }
    }

    private fun parseFestivals(json: String): List<Festival> {
        val arr = JSONObject(json).optJSONArray("festivals") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            if (!o.has("latitude")) return@mapNotNull null
            Festival(
                o.optString("id"), o.optString("title"), o.optString("startDate"), o.optString("endDate"),
                LatLng(o.getDouble("latitude"), o.getDouble("longitude")),
                o.optString("imageURL").takeIf { it.startsWith("https://") },
                o.optString("imageCredit").ifBlank { null },
                region = o.optString("region").ifBlank { null },
                address = o.optString("address").ifBlank { null },
                overview = (o.optString("overview").ifBlank { o.optString("summary") }).ifBlank { null },
                phone = o.optString("phone").ifBlank { null },
                homepage = o.optString("homepage").ifBlank { null },
                source = o.optString("source").ifBlank { null },
            )
        }
    }
}

/** 기준점에서 가까운 순으로, 반경 안의 항목만 */
fun <T> List<T>.nearby(origin: LatLng, radiusKm: Double, limit: Int, location: (T) -> LatLng): List<Pair<T, Double>> =
    map { it to distanceKm(origin, location(it)) }
        .filter { it.second <= radiusKm }
        .sortedBy { it.second }
        .take(limit)
