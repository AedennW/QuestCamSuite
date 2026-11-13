import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.camerasuite.quest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.camerasuite.quest"
        // Horizon OS v76+ (Camera2 passthrough support) is Android 14 based.
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // Optional release signing. Activated ONLY when CI provides the keystore
    // through environment variables (see .github/workflows/android-build.yml).
    if (System.getenv("QUEST_KEYSTORE") != null) {
        signingConfigs {
            create("release") {
                storeFile = file(System.getenv("QUEST_KEYSTORE"))
                storePassword = System.getenv("QUEST_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("QUEST_KEY_ALIAS")
                keyPassword = System.getenv("QUEST_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            versionNameSuffix = "-debug"
            buildConfigField("String", "BUILD_CHANNEL", "\"debug\"")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BUILD_CHANNEL", "\"release\"")
            if (System.getenv("QUEST_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        // Lint findings are reported but never fail CI; correctness is enforced
        // by compilation + unit tests.
        abortOnError = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
