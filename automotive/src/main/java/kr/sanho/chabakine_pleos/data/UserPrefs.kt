package kr.sanho.chabakine_pleos.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.sanho.chabakine_pleos.shared.place.LatLng
import kr.sanho.chabakine_pleos.shared.place.CampingStyle

enum class ThemePref { FOLLOW_CAR, DAY, NIGHT }

/** 이 기기에만 저장하는 사용자 설정(화면설계서 12·20). 서버로 보내지 않는다. */
data class Settings(
    /** 고른 방식들. 비어 있지 않다. */
    val styles: Set<CampingStyle> = setOf(CampingStyle.STEALTH),
    /** 대표 방식: 목록·지도의 신호 기준 */
    val primaryStyle: CampingStyle = CampingStyle.STEALTH,
    val onboardingDone: Boolean = false,
    val theme: ThemePref = ThemePref.FOLLOW_CAR,
    val readSummaryOnSelect: Boolean = false,
    val askCampModeOnPark: Boolean = true,
    /** 설정 "버전"을 7번 눌러 켠다. 켜져 있을 때만 서버를 고를 수 있다 */
    val developerMode: Boolean = false,
    val server: ServerEnv = Server.DEFAULT,
)

class UserPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    /** 켜 둔 차박 모드의 장소. 앱을 다시 열면 05로 돌아온다(버튼 표 0). */
    var activeCampPlaceId: String?
        get() = prefs.getString("activeCampPlaceId", null)
        set(value) = prefs.edit { if (value == null) remove("activeCampPlaceId") else putString("activeCampPlaceId", value) }

    /** 차박 모드를 켤 때의 신호(대표 방식). 새 데이터로 신호가 바뀌면 05에서 알린다. */
    var campStartSignal: String?
        get() = prefs.getString("campStartSignal", null)
        set(value) = prefs.edit { if (value == null) remove("campStartSignal") else putString("campStartSignal", value) }

    /** 차박 모드를 켤 때(또는 처음 값을 읽었을 때) 배터리 %와 시각. 05 "밤사이 n% 썼어요" · 실측 예상에 쓴다 */
    var campStartBattery: Float?
        get() = prefs.getFloat("campStartBattery", -1f).takeIf { it >= 0f }
        set(value) = prefs.edit { if (value == null) remove("campStartBattery") else putFloat("campStartBattery", value) }

    var campStartMs: Long?
        get() = prefs.getLong("campStartMs", -1L).takeIf { it >= 0L }
        set(value) = prefs.edit { if (value == null) remove("campStartMs") else putLong("campStartMs", value) }

    /** 정차 감지로 04를 이미 물어본 장소(장소당 한 번). 앱을 다시 열어도 다시 묻지 않는다. */
    var askedCampPlaceIds: Set<String>
        get() = prefs.getStringSet("askedCampPlaceIds", null).orEmpty()
        set(value) = prefs.edit { putStringSet("askedCampPlaceIds", value.toSet()) }

    /** 마지막으로 내비가 알려 준 차 위치(거리 기준). 다음에 켤 때 위치를 받기 전까지 쓴다. 이 기기에만 저장한다 */
    var lastLocation: LatLng?
        get() = prefs.getString("lastLocation", null)?.split(',')?.let { p ->
            p.getOrNull(0)?.toDoubleOrNull()?.let { lat -> p.getOrNull(1)?.toDoubleOrNull()?.let { lng -> LatLng(lat, lng) } }
        }
        set(value) = prefs.edit { if (value == null) remove("lastLocation") else putString("lastLocation", "${value.latitude},${value.longitude}") }

    /** 02-1 최근 검색어(최근 것이 앞, 최대 [MAX_RECENT]개). 이 기기에만 저장한다. */
    var recentSearches: List<String>
        get() = prefs.getString("recentSearches", null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        set(value) = prefs.edit { putString("recentSearches", value.take(MAX_RECENT).joinToString("\n")) }

    fun addRecentSearch(query: String) {
        recentSearches = listOf(query) + recentSearches.filter { it != query }
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value).normalized()
        _settings.value = next
        save(next)
    }

    private fun Settings.normalized(): Settings {
        val styles = styles.ifEmpty { setOf(primaryStyle) }
        return copy(styles = styles, primaryStyle = if (primaryStyle in styles) primaryStyle else styles.first())
    }

    private fun load(): Settings {
        fun style(name: String?) = name?.let { runCatching { CampingStyle.valueOf(it) }.getOrNull() }
        val styles = prefs.getStringSet("styles", null)?.mapNotNull(::style)?.toSet().orEmpty()
        return Settings(
            styles = styles.ifEmpty { setOf(CampingStyle.STEALTH) },
            primaryStyle = style(prefs.getString("primaryStyle", null)) ?: CampingStyle.STEALTH,
            onboardingDone = prefs.getBoolean("onboardingDone", false),
            theme = ThemePref.entries.firstOrNull { it.name == prefs.getString("theme", null) } ?: ThemePref.FOLLOW_CAR,
            readSummaryOnSelect = prefs.getBoolean("readSummaryOnSelect", false),
            askCampModeOnPark = prefs.getBoolean("askCampModeOnPark", true),
            developerMode = prefs.getBoolean("developerMode", false),
            server = ServerEnv.entries.firstOrNull { it.name == prefs.getString("server", null) } ?: Server.DEFAULT,
        ).normalized()
    }

    private fun save(s: Settings) = prefs.edit {
        putStringSet("styles", s.styles.map { it.name }.toSet())
        putString("primaryStyle", s.primaryStyle.name)
        putBoolean("onboardingDone", s.onboardingDone)
        putString("theme", s.theme.name)
        putBoolean("readSummaryOnSelect", s.readSummaryOnSelect)
        putBoolean("askCampModeOnPark", s.askCampModeOnPark)
        putBoolean("developerMode", s.developerMode)
        putString("server", s.server.name)
    }
}

private const val MAX_RECENT = 5

val CampingStyle.label: String
    get() = when (this) {
        CampingStyle.STEALTH -> "스텔스"
        CampingStyle.DOCKING -> "도킹형"
        CampingStyle.ROOFTOP -> "루프탑"
    }
