package kr.sanho.chabakine_pleos.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sanho.chabakine_pleos.shared.place.Place
import kr.sanho.chabakine_pleos.shared.place.PlaceDataRules
import kr.sanho.chabakine_pleos.shared.place.PlaceParser
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 장소 데이터의 원격 갱신(server-api.md 4장, iOS `PlaceCatalog`와 같은 규칙).
 * 서버에서 받은 사본을 기기에 저장하고, 없거나 깨졌으면 앱에 들어 있는 번들을 쓴다. 실패해도 앱은 멈추지 않는다.
 */
class PlaceCatalog(
    private val context: Context,
    /** 요청할 때마다 읽는다(개발자 모드에서 서버를 바꿀 수 있다) */
    private val baseUrl: () -> String = { Server.baseUrl },
    private val now: () -> Long = System::currentTimeMillis,
) {
    enum class Source { BUNDLE, CACHE }

    data class Loaded(val places: List<Place>, val source: Source, val version: Int?, val checkedAtMillis: Long?)

    sealed interface RefreshResult {
        data class Updated(val version: Int) : RefreshResult
        data object UpToDate : RefreshResult
        data object Skipped : RefreshResult

        /** [offline]이면 서버에 닿지 못한 것, 아니면 받은 데이터를 쓸 수 없거나 서버 오류 */
        data class Failed(val message: String, val offline: Boolean) : RefreshResult
    }

    private data class Meta(val version: Int, val sha256: String, val checkedAtMillis: Long)

    private val dir = File(context.filesDir, "places")
    private val placesFile = File(dir, "places.json")
    private val metaFile = File(dir, "meta.json")

    // 읽기

    /** 저장된 사본이 온전하면 그것을, 아니면 번들을 돌려준다. */
    fun loadCachedOrBundled(): Loaded {
        loadCache()?.let { (places, meta) -> return Loaded(places, Source.CACHE, meta.version, meta.checkedAtMillis) }
        val bundled = context.assets.open(BUNDLE_ASSET).bufferedReader().use { it.readText() }
        return Loaded(PlaceParser.parse(bundled), Source.BUNDLE, null, null)
    }

    /** 서버를 바꾸면 버전 번호가 서로 달라 저장본을 비우고 처음부터 받는다 */
    fun clearCache() {
        placesFile.delete()
        metaFile.delete()
    }

    private fun loadCache(): Pair<List<Place>, Meta>? {
        val meta = readMeta() ?: return null
        val bytes = runCatching { placesFile.readBytes() }.getOrNull() ?: return null
        if (PlaceDataRules.sha256Hex(bytes) != meta.sha256) return null
        val places = runCatching { PlaceParser.parse(bytes.decodeToString()) }.getOrNull()
        return places?.takeIf { it.isNotEmpty() }?.let { it to meta }
    }

    private fun readMeta(): Meta? = runCatching {
        val o = JSONObject(metaFile.readText())
        Meta(o.getInt("version"), o.getString("sha256"), o.getLong("checkedAt"))
    }.getOrNull()

    private fun writeMeta(meta: Meta) {
        val json = JSONObject()
            .put("version", meta.version)
            .put("sha256", meta.sha256)
            .put("checkedAt", meta.checkedAtMillis)
        writeAtomically(metaFile, json.toString().toByteArray())
    }

    // 갱신

    /** 새 버전이 있으면 내려받아 검증한 뒤 저장한다. 6시간 안에 확인했으면 [force]가 아닌 한 건너뛴다. */
    suspend fun refresh(force: Boolean = false): RefreshResult = withContext(Dispatchers.IO) {
        val existing = loadCache()?.second
        if (!force && existing != null && now() - existing.checkedAtMillis < RECHECK_INTERVAL_MS) {
            return@withContext RefreshResult.Skipped
        }
        try {
            dir.mkdirs()
            val manifest = PlaceDataRules.parseManifest(get(MANIFEST_PATH).decodeToString())
                ?: return@withContext invalid("manifest 형식이 올바르지 않다")

            if (manifest.version <= (existing?.version ?: 0)) {
                existing?.let { writeMeta(it.copy(checkedAtMillis = now())) }
                return@withContext RefreshResult.UpToDate
            }

            val path = PlaceDataRules.serverPath(manifest.url, baseUrl())
                ?: return@withContext invalid("서버 밖을 가리키는 장소 데이터 주소")
            val bytes = get(path)

            if (PlaceDataRules.sha256Hex(bytes) != manifest.sha256) return@withContext invalid("해시가 맞지 않음")
            val places = PlaceParser.parse(bytes.decodeToString())
            if (!PlaceDataRules.isAcceptable(places, manifest.count)) return@withContext invalid("개수·번호 검증 실패")

            writeAtomically(placesFile, bytes)
            writeMeta(Meta(manifest.version, manifest.sha256, now()))
            Log.i(TAG, "장소 데이터를 버전 ${manifest.version}으로 갱신 (${places.size}곳)")
            RefreshResult.Updated(manifest.version)
        } catch (e: ServerError) {
            Log.w(TAG, "장소 데이터 갱신 실패: HTTP ${e.status}")
            RefreshResult.Failed("서버 오류가 났어요 (${e.status}). 잠시 뒤에 다시 시도해 주세요.", offline = false)
        } catch (e: IOException) {
            Log.w(TAG, "장소 데이터 갱신 실패: ${e.javaClass.simpleName}")
            RefreshResult.Failed("서버에 연결하지 못했어요.", offline = true)
        } catch (e: RuntimeException) {
            Log.w(TAG, "장소 데이터 갱신 실패", e)
            invalid(e.javaClass.simpleName)
        }
    }

    private fun invalid(reason: String): RefreshResult {
        Log.w(TAG, "받은 장소 데이터를 버림: $reason")
        return RefreshResult.Failed("받은 데이터가 올바르지 않아 적용하지 않았어요.", offline = false)
    }

    private class ServerError(val status: Int) : Exception()

    private fun get(path: String): ByteArray {
        val connection = URL(baseUrl() + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw ServerError(status)
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) throw IOException("${target.name} 저장 실패")
    }

    companion object {
        private const val MANIFEST_PATH = "/static/places/manifest.json"

        /** 앱에 들어 있는 기본 데이터(시연용). 서버에서 받은 적이 없거나 저장본이 깨졌을 때 쓴다. */
        private const val BUNDLE_ASSET = "demo-places.json"
        private const val RECHECK_INTERVAL_MS = 6 * 3600 * 1000L
        private const val TAG = "PlaceCatalog"
    }
}
