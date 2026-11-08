// Generates the app's Baseline Profile: records the code used at start-up and while scrolling,
// so Android compiles it ahead of time and the app opens and scrolls faster from the first run.
// Run: ./gradlew :app:generateBaselineProfile   (with an emulator or phone connected)
plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.androidx.baselineprofile)
}

android {
  namespace = "com.example.baselineprofile"
  compileSdk { version = release(37) }

  defaultConfig {
    minSdk = 28
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  targetProjectPath = ":app"
}

baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(libs.androidx.junit)
  implementation(libs.androidx.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
}
