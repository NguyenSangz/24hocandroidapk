import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.time.Duration
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(37) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.applet.ofedqc"
    minSdk = 23
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    val envProps = Properties().apply {
      val envFile = rootProject.file(".env")
      if (envFile.exists()) envFile.inputStream().use { load(it) }
      val localPropsFile = rootProject.file("local.properties")
      if (localPropsFile.exists()) localPropsFile.inputStream().use { load(it) }
    }
    fun cfgValue(key: String, defaultValue: String): String {
      val raw = (envProps.getProperty(key) ?: System.getenv(key) ?: defaultValue).trim()
      return raw.replace("\\", "\\\\").replace("\"", "\\\"")
    }
    fun cfgBool(key: String, defaultValue: Boolean): Boolean =
      (envProps.getProperty(key) ?: System.getenv(key) ?: defaultValue.toString()).trim().equals("true", ignoreCase = true)

    buildConfigField("String", "POCKETBASE_BASE_URL", "\"${cfgValue("POCKETBASE_BASE_URL", "https://api.tenmiencuatoi.vn").trimEnd('/')}\"")
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${cfgValue("GOOGLE_WEB_CLIENT_ID", "")}\"")
    buildConfigField("String", "POCKETBASE_CERT_PIN_PRIMARY", "\"${cfgValue("POCKETBASE_CERT_PIN_PRIMARY", "")}\"")
    buildConfigField("String", "POCKETBASE_CERT_PIN_BACKUP", "\"${cfgValue("POCKETBASE_CERT_PIN_BACKUP", "")}\"")
    // F20a: cờ tính năng thông báo đẩy. Mặc định TẮT để app hoạt động y như trước khi người dùng bật.
    buildConfigField("boolean", "ENABLE_PUSH", cfgBool("ENABLE_PUSH", false).toString())
    // F20c: cờ tính năng gia sư AI (triển khai ở phần F20c).
    buildConfigField("boolean", "ENABLE_AI_TUTOR", cfgBool("ENABLE_AI_TUTOR", false).toString())
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
      all {
        it.maxParallelForks = 1
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  lint {
    abortOnError = true
    checkDependencies = false
    lintConfig = file("lint.xml")
    htmlReport = true
    xmlReport = true
    textReport = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  coreLibraryDesugaring(libs.desugar.jdk.libs)
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // F20a: chỉ thêm FCM tối thiểu; KHÔNG thêm Analytics/Crashlytics/Ads
  implementation(libs.firebase.messaging)
  // Self-hosted PocketBase REST dùng Retrofit + OkHttp + Moshi; tắt Firebase Auth/Firestore để giảm dung lượng APK
  // implementation(libs.firebase.firestore)
  // implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.withType<Test> {
  failFast = true
  timeout.set(Duration.ofMinutes(2))
  testLogging {
    events("passed", "failed", "skipped")
  }
}
