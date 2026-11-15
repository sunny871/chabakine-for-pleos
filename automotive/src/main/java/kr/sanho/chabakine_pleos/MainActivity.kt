package kr.sanho.chabakine_pleos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kr.sanho.chabakine_pleos.app.AppState
import kr.sanho.chabakine_pleos.data.DemoRepository
import kr.sanho.chabakine_pleos.data.BatteryLog
import kr.sanho.chabakine_pleos.data.FavoritesStore
import kr.sanho.chabakine_pleos.data.FeedRepository
import kr.sanho.chabakine_pleos.data.Server
import kr.sanho.chabakine_pleos.data.PlaceCatalog
import kr.sanho.chabakine_pleos.data.PlacesRepository
import kr.sanho.chabakine_pleos.data.ReviewsRepository
import kr.sanho.chabakine_pleos.data.UserPrefs
import kr.sanho.chabakine_pleos.platform.NaviLauncher
import kr.sanho.chabakine_pleos.platform.AlarmSound
import kr.sanho.chabakine_pleos.platform.Speaker
import kr.sanho.chabakine_pleos.platform.VoiceSearch
import kr.sanho.chabakine_pleos.ui.AppRoot
import kr.sanho.chabakine_pleos.vehicle.CampControl
import kr.sanho.chabakine_pleos.vehicle.VehicleMonitor

class MainActivity : ComponentActivity() {

    private lateinit var vehicle: VehicleMonitor
    private lateinit var speaker: Speaker
    private lateinit var alarmSound: AlarmSound
    private lateinit var voice: VoiceSearch
    private lateinit var navi: NaviLauncher
    private lateinit var campControl: CampControl
    private lateinit var app: AppState

    // 속도·에너지는 위험 권한이라 실행 중에 묻는다. 거절하면 해당 값만 비고 앱은 계속 동작한다.
    private val carPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vehicle.stop()
        vehicle.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 개발자 모드에서 고른 서버로 시작한다(저장소들이 처음 요청하기 전에)
        val prefs = UserPrefs(applicationContext)
        Server.env = prefs.settings.value.server
        vehicle = VehicleMonitor(applicationContext)
        speaker = Speaker(applicationContext)
        alarmSound = AlarmSound(applicationContext) { speaker.speak(it) }
        voice = VoiceSearch(applicationContext)
        navi = NaviLauncher(applicationContext).apply { start() }
        campControl = CampControl(applicationContext).apply { start() }
        app = AppState(
            scope = lifecycleScope,
            vehicle = vehicle,
            places = PlacesRepository(PlaceCatalog(applicationContext)),
            reviews = ReviewsRepository(DemoRepository(applicationContext).ratings),
            feeds = FeedRepository(applicationContext),
            prefs = prefs,
            favorites = FavoritesStore(applicationContext),
            batteryLog = BatteryLog(applicationContext),
            navi = navi,
            campControl = campControl,
            speaker = speaker,
            alarmSound = alarmSound,
            voice = voice,
        )
        navi.onArrived = app::onNaviArrived
        setContent { AppRoot(app) }
    }

    /**
     * 개발 빌드 전용: 마이크 없이 음성 명령을 시험한다(에뮬레이터 마이크는 소리가 들어오지 않는 경우가 많다).
     * adb shell am start -n kr.sanho.chabakine_pleos/.MainActivity --es voice_text "첫 번째로 안내해 줘"
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (BuildConfig.DEBUG) intent.getStringExtra(EXTRA_VOICE_TEXT)?.let(app::runVoiceCommand)
    }

    override fun onStart() {
        super.onStart()
        vehicle.start()
        // 마지막 확인 후 6시간이 지났으면 서버에 새 장소 데이터가 있는지 본다
        app.refreshData()
        carPermissions.launch(
            arrayOf("android.car.permission.CAR_SPEED", "android.car.permission.CAR_ENERGY"),
        )
    }

    override fun onStop() {
        vehicle.stop()
        app.stopSpeaking()
        super.onStop()
    }

    override fun onDestroy() {
        alarmSound.stop()
        speaker.shutdown()
        voice.release()
        navi.stop()
        campControl.stop()
        super.onDestroy()
    }

    private companion object {
        const val EXTRA_VOICE_TEXT = "voice_text"
    }
}
