package kr.sanho.chabakine_pleos.shared.place

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

/**
 * `places.json` / `places-v{n}.json` 파서.
 * 새 형식(`stay`, `evidence`, `site`)과 옛 형식(`signal`만)을 모두 읽는다.
 * 옛 형식은 `signal`을 차량 내 숙박으로 보고 도킹형 · 루프탑형 · 취사는 미확인으로 둔다.
 * `stay.docking` · `stay.rooftop`이 없고 `stay.tent`만 있는 데이터(나누기 전)는 [Place.ruleFor]가 두 방식 모두 `tent`로 읽는다.
 */
object PlaceParser {

    private val ID_PATTERN = Regex("^KR-\\d{2}-[A-Z]{2}-\\d{3}$")

    /** `stay`에서 읽는 항목. 모르는 신호 값은 그 항목만 미확인([Signal.from]) */
    private val STAY_KEYS = listOf(
        "car" to StayRule.CAR, "docking" to StayRule.DOCKING, "rooftop" to StayRule.ROOFTOP,
        "tent" to StayRule.TENT, "cook" to StayRule.COOK,
    )

    /** 장소 번호는 KR-42-DH-001 형식(ISO 3166-2:KR 시도 코드)이다. */
    fun isValidId(id: String): Boolean = ID_PATTERN.matches(id)

    /**
     * 형식이 맞지 않는 장소는 건너뛴다. 원격 데이터는 이어서 [PlaceDataRules.isAcceptable]로
     * 개수를 맞춰 보므로, 건너뛴 장소가 있으면 파일 전체를 버리게 된다.
     */
    fun parse(json: String): List<Place> =
        Json.parseToJsonElement(json).jsonArray.mapNotNull { (it as? JsonObject)?.let(::parsePlace) }

    private fun parsePlace(o: JsonObject): Place? {
        val id = o.str("id") ?: return null
        if (!isValidId(id)) return null
        val name = o.str("name") ?: return null
        val lat = o.num("latitude") ?: return null
        val lng = o.num("longitude") ?: return null
        val legacySignal = Signal.from(o.str("signal"))
        val legacyReason = o.str("reason").orEmpty()

        val stayObject = o["stay"] as? JsonObject
        val stay = if (stayObject == null) {
            mapOf(StayRule.CAR to RuleSignal(legacySignal, legacyReason))
        } else {
            buildMap {
                for ((key, rule) in STAY_KEYS) {
                    val r = stayObject[key] as? JsonObject ?: continue
                    put(rule, RuleSignal(Signal.from(r.str("signal")), r.str("reason").orEmpty()))
                }
            }
        }
        val carSignal = stay[StayRule.CAR]?.signal ?: legacySignal

        return Place(
            id = id,
            name = name,
            region = o.str("region").orEmpty(),
            latitude = lat,
            longitude = lng,
            signal = carSignal,
            reason = legacyReason.ifBlank { stay[StayRule.CAR]?.reason.orEmpty() },
            stay = stay,
            evidence = o.strList("evidence"),
            site = (o["site"] as? JsonObject)?.let(::parseSite),
            amenities = (o["amenities"] as? JsonObject)?.let {
                Amenities(it.bool("toilet"), it.bool("water"), it.bool("electricity"), it.bool("shower"))
            } ?: Amenities(),
            photoUrls = o.strList("photoURLs"),
            sourceName = o.str("sourceName"),
            verifiedAt = o.str("verifiedAt"),
            confidence = (o["confidence"] as? JsonPrimitive)?.intOrNull ?: 1,
            verification = o.str("verification"),
            seasonRules = o.strList("seasonRules"),
            isDemo = o.bool("demo") == true,
            notes = o.strList("notes"),
        )
    }

    private fun parseSite(o: JsonObject): Site {
        val height = o["heightLimit"] as? JsonObject
        val state = height?.str("state")
        return Site(
            type = o.str("type"),
            parkingSize = o.str("parkingSize") ?: "unknown",
            heightLimitMeters = if (state == "limited") height.num("meters") else null,
            heightLimitKnown = state == "limited" || state == "none",
            wind = o.str("wind") ?: "unknown",
        )
    }

    private fun JsonObject.prim(key: String): JsonPrimitive? =
        (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

    private fun JsonObject.str(key: String): String? = prim(key)?.takeIf { it.isString }?.content

    private fun JsonObject.num(key: String): Double? = prim(key)?.doubleOrNull

    private fun JsonObject.bool(key: String): Boolean? = prim(key)?.booleanOrNull

    private fun JsonObject.strList(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
            ?: emptyList()
}
