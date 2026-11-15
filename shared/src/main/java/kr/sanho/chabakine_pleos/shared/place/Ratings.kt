package kr.sanho.chabakine_pleos.shared.place

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

data class Rating(val count: Int, val average: Double)

/** `GET /v1/reviews/ratings` 응답(`{ "places": { id: { count, average } } }`) 파서. */
object RatingsParser {
    fun parse(json: String): Map<String, Rating> {
        val places = (Json.parseToJsonElement(json) as? JsonObject)?.get("places") as? JsonObject
            ?: return emptyMap()
        return places.mapNotNull { (id, value) ->
            val o = value as? JsonObject ?: return@mapNotNull null
            val count = (o["count"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val average = (o["average"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            id to Rating(count, average)
        }.toMap()
    }
}
