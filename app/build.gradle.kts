import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

val keystoreFilePath: String? = System.getenv("KEYSTORE_FILE")
val keystorePasswordEnv: String? = System.getenv("KEYSTORE_PASSWORD")
val keyAliasEnv: String? = System.getenv("KEY_ALIAS")
val keyPasswordEnv: String? = System.getenv("KEY_PASSWORD")

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

fun feedbackProperty(propertyName: String, envName: String): String =
    (localProperties.getProperty(propertyName)
        ?: providers.gradleProperty(propertyName).orNull
        ?: System.getenv(envName)
        ?: "").trim()

fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val githubApiToken = feedbackProperty("github.api.token", "GH_API_TOKEN")
val githubRepoOwner = feedbackProperty("github.repo.owner", "GH_REPO_OWNER")
val githubRepoName = feedbackProperty("github.repo.name", "GH_REPO_NAME")
val googleWebClientId = feedbackProperty("google.web.client.id", "GOOGLE_WEB_CLIENT_ID")

val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerId = "ca-app-pub-3940256099942544/6300978111"
val testAdmobInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testAdmobNativeId = "ca-app-pub-3940256099942544/2247696110"

fun requireEnv(name: String): String =
    System.getenv(name) ?: error("$name must be set for release builds")

val releaseBuildRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
}
val bundleBuildRequested = gradle.startParameter.taskNames.any {
    it.contains("bundle", ignoreCase = true)
}

val releaseAdmobAppId = if (releaseBuildRequested) requireEnv("ADMOB_APP_ID") else testAdmobAppId
val releaseAdmobBannerId = if (releaseBuildRequested) requireEnv("ADMOB_BANNER_ID") else testAdmobBannerId
val releaseAdmobInterstitialId = if (releaseBuildRequested) requireEnv("ADMOB_INTERSTITIAL_ID") else testAdmobInterstitialId
val releaseAdmobNativeId = if (releaseBuildRequested) requireEnv("ADMOB_NATIVE_ID") else testAdmobNativeId

val envVersionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull()
val envVersionName = System.getenv("ANDROID_VERSION_NAME")

android {
    namespace = "com.charles.scamradar.app"
    compileSdk = 37

    signingConfigs {
        create("release") {
            if (keystoreFilePath != null) {
                storeFile = file(keystoreFilePath)
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    defaultConfig {
        applicationId = "com.charles.scamradar.app"
        minSdk = 26
        targetSdk = 36
        versionCode = envVersionCode ?: 1
        versionName = envVersionName ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "MODEL_SHA256",
            "\"\""
        )
        buildConfigField(
            "String",
            "MODEL_DOWNLOAD_URL",
            "\"https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm\""
        )
        buildConfigField(
            "String",
            "MODEL_CONFIG_URL",
            "\"https://scamradar.github.io/model-config.json\""
        )
        buildConfigField(
            "long",
            "MODEL_SIZE_BYTES",
            "2588147712L"
        )
        buildConfigField("String", "GITHUB_API_TOKEN", buildConfigString(githubApiToken))
        buildConfigField("String", "GITHUB_REPO_OWNER", buildConfigString(githubRepoOwner))
        buildConfigField("String", "GITHUB_REPO_NAME", buildConfigString(githubRepoName))
        buildConfigField("String", "FEEDBACK_ASSETS_DIR", buildConfigString("feedback-assets"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", buildConfigString(googleWebClientId))
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("Boolean", "USE_TEST_ADS", "true")
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdmobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdmobBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testAdmobInterstitialId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$testAdmobNativeId\"")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "USE_TEST_ADS", "false")
            buildConfigField("String", "ADMOB_APP_ID", "\"$releaseAdmobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$releaseAdmobBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$releaseAdmobInterstitialId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$releaseAdmobNativeId\"")
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // 16 KB page-size compatible APK packaging (see
            // https://developer.android.com/guide/practices/page-sizes).
            // Required by Google Play for new uploads from Nov 1, 2025.
            useLegacyPackaging = false
        }
    }

    splits {
        abi {
            isEnable = !bundleBuildRequested
            reset()
            // Modern Android (Pixel 6+, all Android 15 devices) is 64-bit only.
            // Dropping 32-bit ABIs removes the legacy 4-KB-aligned native libs
            // and halves the APK size.
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")

    implementation("com.google.mlkit:text-recognition:16.0.1")

    implementation("com.google.android.gms:play-services-ads:25.3.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.android.billingclient:billing-ktx:9.0.0")

    // On-device ASR via Sherpa-ONNX (16 KB-aligned native libs, fully offline)
    implementation(files("libs/sherpa-onnx-1.13.2.aar"))

    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.13.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    implementation("androidx.webkit:webkit:1.16.0")

    implementation("com.google.zxing:core:3.5.4")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.animation:animation-tooling-internal")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
