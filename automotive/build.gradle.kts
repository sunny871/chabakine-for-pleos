import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 카카오 네이티브 앱 키는 저장소에 올리지 않고 local.properties에서 읽는다
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "kr.sanho.chabakine_pleos"
    compileSdk {
        version = release(37)
    }
    // 차량 속성(CarPropertyManager)을 읽는 AAOS 표준 라이브러리
    useLibrary("android.car")

    defaultConfig {
        applicationId = "kr.sanho.chabakine_pleos"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 키가 없으면 빈 값 → 앱은 지도 대신 안내 문구를 보여 준다
        val kakaoKey = localProperties.getProperty("KAKAO_NATIVE_APP_KEY").orEmpty()
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
    }

    buildTypes {
        release {
            // 공모전 제출용 release APK가 바로 설치되도록 디버그 키로 서명한다(스토어 배포용 키는 따로 정한다)
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
                // kotlin.**은 빼 둔다: Pleos Vehicle SDK가 Kotlin 리플렉션으로 값을 읽어, 최적화하면 SDK 구독이 실패한다
                packageScope = setOf("androidx.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.tts)
    implementation(libs.pleos.stt)
    implementation(libs.pleos.vehicle)
    implementation(libs.kakao.map)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
