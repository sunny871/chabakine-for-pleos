package kr.sanho.chabakine_pleos.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 이 기기에만 저장하는 즐겨찾기(장소 번호). 서버로 보내지 않는다. */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun toggle(placeId: String) {
        if (placeId in _ids.value) remove(placeId) else add(placeId)
    }

    /** 저장한 때(09-1 "저장한 순"). 이 기능 전에 저장한 곳은 null */
    fun savedAt(placeId: String): Long? = prefs.getLong(savedKey(placeId), 0L).takeIf { it > 0 }

    /** [savedAt]을 주면 그 시각으로 되살린다(빼기 되돌리기) */
    fun add(placeId: String, savedAt: Long = System.currentTimeMillis()) {
        val next = _ids.value + placeId
        _ids.value = next
        prefs.edit {
            putStringSet(KEY, next)
            putLong(savedKey(placeId), savedAt)
        }
    }

    fun remove(placeId: String) {
        val next = _ids.value - placeId
        _ids.value = next
        prefs.edit {
            putStringSet(KEY, next)
            remove(savedKey(placeId))
        }
    }

    private fun savedKey(placeId: String) = "savedAt:$placeId"

    private companion object {
        const val KEY = "ids"
    }
}
