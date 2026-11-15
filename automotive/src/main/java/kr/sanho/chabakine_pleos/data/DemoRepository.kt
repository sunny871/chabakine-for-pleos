package kr.sanho.chabakine_pleos.data

import android.content.Context
import kr.sanho.chabakine_pleos.shared.place.Rating
import kr.sanho.chabakine_pleos.shared.place.RatingsParser

/**
 * 서버에서 별점을 받지 못할 때 쓰는 시연용 별점(assets의 demo-ratings.json).
 * 장소 목록은 [PlacesRepository]가 서버 → 저장본 → 번들 순으로 준다.
 */
class DemoRepository(private val context: Context) {

    val ratings: Map<String, Rating> by lazy {
        RatingsParser.parse(context.assets.open("demo-ratings.json").bufferedReader().use { it.readText() })
    }
}
