package kr.sanho.chabakine_pleos.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale
import ai.pleos.playground.tts.TextToSpeech as PleosTts

/**
 * 요약·리뷰 읽어 주기. Pleos Playground 문서의 TextToSpeech SDK(v3.3.1, 기기 안 ON_DEVICE 모드)로만 읽는다.
 * 공모전 운영사무국 안내(2026-11-11)에 따라 문서에 없는 방식(Android 표준 TTS 대체)은 쓰지 않는다.
 * SDK를 쓸 수 없거나 읽다가 오류가 나면 [speak]가 [SpeakResult.UNAVAILABLE]을 돌려주고, 앱은 화면 글로만 안내한다.
 * 읽는 동안만 오디오 포커스를 잠깐 받아(다른 앱 소리는 줄어듦) 끝나면 돌려준다(Pleos 오디오 포커스 정책).
 */
class Speaker(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    // 리스너가 없으면 같은 앱의 다른 포커스(알람 소리)와 한 요청으로 취급돼, 읽기가 끝나며 알람 포커스까지 놓아 버린다
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { }
        .build()

    // Pleos TTS: 기기 안 엔진(ON_DEVICE)이라 서버 앱 등록 없이 쓴다
    @Volatile private var pleosReady = false
    private val pleos: PleosTts? = runCatching { PleosTts(context.applicationContext, Mode.ON_DEVICE) }
        .onFailure { Log.w(TAG, "Pleos TTS 만들기 실패", it) }
        .getOrNull()
        ?.also { tts ->
            runCatching {
                tts.initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        runCatching {
                            // 국가까지 넣어야 한다: Locale.KOREAN("ko")은 엔진에 "ko-"로 넘어가 지원 안 되는 언어로 실패한다
                            tts.setLocale(Locale.KOREA)
                            tts.setAudioAttributes(attributes)
                            tts.addEventListener(pleosEvents)
                        }.onFailure { Log.w(TAG, "Pleos TTS 설정 실패", it) }
                        pleosReady = true
                        Log.i(TAG, "Pleos TTS 준비됨 (SDK ${PleosTts.version})")
                    }

                    override fun onFailure(error: ErrorType) {
                        pleosReady = false
                        Log.w(TAG, "Pleos TTS 초기화 실패: $error · 읽어 주기를 쓸 수 없어요")
                    }
                })
            }.onFailure { Log.w(TAG, "Pleos TTS 초기화 호출 실패", it) }
        }

    private val pleosEvents = object : EventListener {
        override fun onStart() = Unit
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = finish()
        override fun onStop() = finish()
        override fun onError(error: ErrorType) {
            Log.w(TAG, "Pleos TTS 읽기 오류: $error")
            finish()
            // 언어·엔진 문제는 이번 실행 동안 다시 시도하지 않는다
            if (error == ErrorType.UNAVAILABLE_LANGUAGE || error == ErrorType.ON_DEVICE_ENGINE_ERROR || error == ErrorType.NOT_SUPPORTED_API) pleosReady = false
        }
    }

    fun speak(text: String): SpeakResult {
        val pleos = pleos?.takeIf { pleosReady } ?: return SpeakResult.UNAVAILABLE
        // 내비 안내 같은 더 중요한 소리가 포커스를 쥐고 있으면 끼어들지 않는다
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return SpeakResult.BUSY
        val ok = runCatching { pleos.speak(text, true) }
            .onFailure { Log.w(TAG, "Pleos TTS speak 실패", it) }
            .isSuccess
        if (!ok) release()
        return if (ok) SpeakResult.SPOKEN else SpeakResult.UNAVAILABLE
    }

    fun stop() {
        runCatching { pleos?.stop() }
        release()
    }

    fun shutdown() {
        release()
        runCatching { pleos?.removeEventListener(pleosEvents) }
        runCatching { pleos?.release() }
    }

    private fun finish() = release()

    private fun release() {
        audio.abandonAudioFocusRequest(focus)
    }

    private companion object {
        const val TAG = "Speaker"
    }
}

enum class SpeakResult { SPOKEN, UNAVAILABLE, BUSY }
