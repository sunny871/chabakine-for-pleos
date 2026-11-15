package kr.sanho.chabakine_pleos.shared.place

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.net.URI
import java.security.MessageDigest

/** 서버의 `/static/places/manifest.json` (server-api.md 4장). */
data class PlaceManifest(
    val version: Int,
    val updatedAt: String,
    /** 목록 파일 주소. `/static/…`처럼 서버 기준 경로이거나 같은 서버의 전체 주소. */
    val url: String,
    val sha256: String,
    val count: Int,
)

/** 원격 장소 데이터 검증 규칙. iOS `PlaceCatalog`와 같다. */
object PlaceDataRules {

    fun parseManifest(json: String): PlaceManifest? {
        val o = runCatching { Json.parseToJsonElement(json) as? JsonObject }.getOrNull() ?: return null
        fun str(key: String) = (o[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        fun int(key: String) = (o[key] as? JsonPrimitive)?.intOrNull
        return PlaceManifest(
            version = int("version") ?: return null,
            updatedAt = str("updatedAt").orEmpty(),
            url = str("url") ?: return null,
            sha256 = str("sha256")?.lowercase() ?: return null,
            count = int("count") ?: return null,
        )
    }

    /** manifest의 주소를 서버 기준 경로로 바꾼다. 다른 호스트를 가리키면 null(버린다). */
    fun serverPath(url: String, baseUrl: String): String? {
        if (url.startsWith("/")) return url
        val target = runCatching { URI(url) }.getOrNull() ?: return null
        val base = runCatching { URI(baseUrl) }.getOrNull() ?: return null
        if (target.scheme == null || target.host != base.host || target.port != base.port) return null
        val path = target.rawPath.orEmpty().ifEmpty { "/" }
        return path + (target.rawQuery?.let { "?$it" } ?: "")
    }

    /** 목록이 비어 있지 않고, 개수가 맞고, 번호가 형식에 맞고 겹치지 않는지 본다. */
    fun isAcceptable(places: List<Place>, expectedCount: Int): Boolean {
        if (places.isEmpty() || places.size != expectedCount) return false
        val ids = places.map { it.id }
        return ids.toSet().size == ids.size && ids.all(PlaceParser::isValidId)
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
