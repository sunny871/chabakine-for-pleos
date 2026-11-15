package kr.sanho.chabakine_pleos.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale
import ai.pleos.playground.stt.SpeechToText as PleosStt

/**
 * 음성 인식(말로 앱 조작). Pleos Playground 문서의 SpeechToText SDK(v3.3.1, 기기 안 ON_DEVICE 모드)로만 듣는다.
 * 마이크는 Pleos 음성 서비스가 다루고, 앱은 문서대로 STT_SERVICE 권한만 선언한다. 음성 버튼을 눌렀을 때만 듣는다.
 * 공모전 운영사무국 안내(2026-11-11)에 따라 문서에 없는 방식(Android 표준 음성 인식 대체)은 쓰지 않는다.
 */
class VoiceSearch(private val context: Context) {

    /** Pleos 음성 인식을 쓸 수 있을 때만 true. 아니면 음성 버튼이 "쓸 수 없어요"를 알려 준다 */
    val isAvailable: Boolean get() = pleosReady

    private val main = Handler(Looper.getMainLooper())

    // Pleos STT
    @Volatile private var pleosReady = false
    private var pending: ((String?) -> Unit)? = null
    private var lastPartial: String? = null

    private val pleosListener = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onUpdatedEpdData(epdOn: Long, epdOff: Long) = Unit

        override fun onUpdated(text: String, isFinal: Boolean) {
            if (text.isNotBlank()) lastPartial = text
            if (isFinal) main.post { deliver(text.ifBlank { null }) }
        }

        override fun onEndedRecognition() {
            // 최종 결과 없이 끝나면 마지막 중간 결과를 쓴다
            main.post { deliver(lastPartial) }
        }

        override fun onError(error: ErrorType) {
            Log.w(TAG, "Pleos STT 오류: $error")
            main.post {
                // 엔진·언어·권한 문제면 이번 실행 동안 다시 시도하지 않는다. 어느 쪽이든 이번 결과는 없음
                if (error == ErrorType.ON_DEVICE_ENGINE_ERROR || error == ErrorType.UNAVAILABLE_LANGUAGE ||
                    error == ErrorType.NOT_SUPPORTED_API || error == ErrorType.PERMISSION_DENIED
                ) pleosReady = false
                deliver(null)
            }
        }
    }

    private val pleos: PleosStt? = runCatching { PleosStt(context.applicationContext, Mode.ON_DEVICE) }
        .onFailure { Log.w(TAG, "Pleos STT 만들기 실패", it) }
        .getOrNull()
        ?.also { stt ->
            runCatching {
                stt.initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        runCatching {
                            stt.setLocale(Locale.KOREA)
                            stt.addListener(pleosListener)
                        }.onFailure { Log.w(TAG, "Pleos STT 설정 실패", it) }
                        pleosReady = true
                        Log.i(TAG, "Pleos STT 준비됨")
                    }

                    override fun onFailure(error: ErrorType) {
                        pleosReady = false
                        Log.w(TAG, "Pleos STT 초기화 실패: $error · 음성 명령을 쓸 수 없어요")
                    }
                })
            }.onFailure { Log.w(TAG, "Pleos STT 초기화 호출 실패", it) }
        }

    fun listen(onResult: (String?) -> Unit) {
        cancel()
        val stt = pleos?.takeIf { pleosReady }
        if (stt == null) {
            onResult(null)
            return
        }
        pending = onResult
        lastPartial = null
        val started = runCatching { stt.request() }.onFailure { Log.w(TAG, "Pleos STT request 실패", it) }.isSuccess
        if (!started) {
            deliver(null)
            return
        }
        // 끝 신호가 오지 않을 때를 대비해 일정 시간 뒤 마지막 중간 결과로 끝낸다
        main.postDelayed({ if (pending === onResult) { runCatching { stt.stop() }; deliver(lastPartial) } }, PLEOS_TIMEOUT_MS)
    }

    /** 결과를 한 번만 넘긴다(최종 결과 · 끝 신호 · 시간 초과 중 먼저 온 것) */
    private fun deliver(text: String?) {
        val callback = pending ?: return
        pending = null
        lastPartial = null
        callback(text)
    }

    fun cancel() {
        main.removeCallbacksAndMessages(null)
        if (pending != null) {
            pending = null
            runCatching { pleos?.stop() }
        }
    }

    fun release() {
        cancel()
        runCatching { pleos?.removeListener(pleosListener) }
        runCatching { pleos?.release() }
    }

    private companion object {
        const val TAG = "VoiceSearch"
        const val PLEOS_TIMEOUT_MS = 12_000L
    }
}
