package kr.sanho.chabakine_pleos.platform

import kr.sanho.chabakine_pleos.util.DayPart
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kr.sanho.chabakine_pleos.app.CoLevel

/**
 * 일산화탄소 경보 소리(화면설계서 개발 기준 메모, 음량 · 간격은 초안 값).
 * - 1단계 주의: 부드러운 알림음 2번 · 보통 음량 · 반복 없음
 * - 2단계 경고: 경고음 5초마다 반복 · 알람 음량이 작아도 70%까지 올린다
 * - 3단계 위험: 사이렌 + 음성 "즉시 차 밖으로" · 최대 음량 · 끌 때까지 계속
 * 기상 알람(05-6) 소리도 여기서 낸다([playWake]).
 * 울리는 동안 알람 용도로 오디오 포커스를 받아 다른 소리를 멈추게 하고, 올린 알람 음량과 포커스는 [stop]에서 되돌린다.
 */
class AlarmSound(context: Context, private val speak: (String) -> Unit) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null
    private var savedVolume: Int? = null
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        // 읽어 주기(Speaker)와 다른 요청으로 구분되게 리스너를 둔다(없으면 읽기가 끝날 때 알람 포커스도 풀린다)
        .setOnAudioFocusChangeListener { }
        .build()

    /** 지금 울리는 소리가 기상 알람인지. 일산화탄소 경보가 끼어들면 false가 되어 [stopWake]가 경보를 끄지 않는다 */
    private var wakePlaying = false

    fun play(level: CoLevel) {
        stop()
        if (level != CoLevel.NONE) audio.requestAudioFocus(focus)
        when (level) {
            CoLevel.NONE -> Unit
            CoLevel.CAUTION -> generator(60)?.startTone(ToneGenerator.TONE_PROP_BEEP2)
            CoLevel.WARNING -> {
                raiseVolume(0.7f)
                val gen = generator(100)
                repeat(WARNING_REPEAT_MS) { gen?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 1_500) }
            }
            CoLevel.DANGER -> {
                raiseVolume(1f)
                val gen = generator(100)
                // 음성이 사이렌에 묻히지 않게 번갈아: 음성 → 사이렌 7초
                repeat(DANGER_REPEAT_MS) {
                    gen?.stopTone()
                    speak("즉시 차 밖으로 나가세요")
                    handler.postDelayed({ gen?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 7_000) }, 3_000)
                }
            }
        }
    }

    /**
     * 기상 알람(05-6). 알림음 + 음성: 부드러운 알림음 뒤 [DayPart.wakeSpeech](아침이면 "좋은 아침이에요")를 20초마다, 알람 음량은 50%까지만 올린다.
     * 음성만: 소리 없이 음성만 30초마다. 끌 때까지([stopWake]) 계속.
     */
    fun playWake(voiceOnly: Boolean) {
        stop()
        wakePlaying = true
        audio.requestAudioFocus(focus)
        if (voiceOnly) {
            repeat(WAKE_VOICE_REPEAT_MS) { speak(DayPart.wakeSpeech()) }
            return
        }
        raiseVolume(0.5f)
        val gen = generator(70)
        repeat(WAKE_REPEAT_MS) {
            gen?.startTone(ToneGenerator.TONE_PROP_BEEP2)
            handler.postDelayed({ speak(DayPart.wakeSpeech()) }, 1_500)
        }
    }

    /**
     * 배터리 부족(05-7): 알림음 2번 뒤 [message]를 한 번 읽는다. 반복 없음, 알람 음량은 50%까지만.
     * 기상 알람과 같이 [stopWake]로 멈추고, 일산화탄소 경보가 끼어들면 경보가 이긴다.
     */
    fun playLowBattery(message: String) {
        stop()
        wakePlaying = true
        audio.requestAudioFocus(focus)
        raiseVolume(0.5f)
        val gen = generator(70)
        gen?.startTone(ToneGenerator.TONE_PROP_BEEP)
        handler.postDelayed({ gen?.startTone(ToneGenerator.TONE_PROP_BEEP) }, LOW_BATTERY_BEEP_GAP_MS)
        handler.postDelayed({ speak(message) }, LOW_BATTERY_BEEP_GAP_MS + 900)
    }

    /** 기상 알람만 멈춘다. 그사이 일산화탄소 경보로 바뀌었으면 그대로 둔다 */
    fun stopWake() {
        if (wakePlaying) stop()
    }

    fun stop() {
        wakePlaying = false
        handler.removeCallbacksAndMessages(null)
        tone?.release()
        tone = null
        savedVolume?.let { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
        savedVolume = null
        audio.abandonAudioFocusRequest(focus)
    }

    /** 소리 장치를 못 열면(일부 기기에서 예외) null: 화면 경보만 남는다 */
    private fun generator(volume: Int): ToneGenerator? =
        runCatching { ToneGenerator(AudioManager.STREAM_ALARM, volume) }.getOrNull()?.also { tone = it }

    private fun raiseVolume(atLeast: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val now = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val target = (max * atLeast).toInt().coerceAtLeast(1)
        if (now >= target) return
        savedVolume = now
        audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
    }

    private fun repeat(everyMs: Long, action: () -> Unit) {
        val tick = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, everyMs)
            }
        }
        handler.post(tick)
    }

    private companion object {
        const val WARNING_REPEAT_MS = 5_000L
        const val DANGER_REPEAT_MS = 10_000L
        const val WAKE_REPEAT_MS = 20_000L
        const val LOW_BATTERY_BEEP_GAP_MS = 700L
        const val WAKE_VOICE_REPEAT_MS = 30_000L
    }
}
