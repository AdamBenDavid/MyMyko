plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.androidx.navigation.safeargs)
  id("kotlin-kapt")
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example.mymyko"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.mymyko"
    minSdk = 29
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
  implementation("com.google.android.gms:play-services-maps:18.1.0")


  implementation ("com.google.firebase:firebase-auth:22.0.0")
  implementation ("com.google.android.gms:play-services-auth:20.7.0")

  implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
  implementation("com.google.firebase:firebase-auth-ktx")
  implementation("com.google.firebase:firebase-firestore-ktx")

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)

  implementation(libs.picasso)
  implementation(libs.cloudinary.android)

  implementation(libs.room.runtime)
  implementation(libs.firebase.common.ktx)
  implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.play.services.location)
    kapt(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)

  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  implementation(libs.retrofit)
  implementation(libs.retrofit.gson)
  implementation(libs.okhttp.logging.interceptor)
  implementation("com.squareup.okhttp3:okhttp:4.9.0") // Added OkHttp core dependency
  implementation(libs.gson)

  implementation(libs.glide)

  implementation(libs.circleimageview)
}
