import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.fileforge.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fileforge.pro"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    // Force a single BouncyCastle version to avoid duplicate classes.
    // jcifs pulls in bcprov-jdk15to18:1.72; sardine pulls bcprov-jdk18on:1.78.1.
    // We standardize on bcprov-jdk18on:1.78.1 (newer, JDK 17 compatible).
    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk18on:1.78.1")
            force("org.bouncycastle:bcpkix-jdk18on:1.78.1")
            force("org.bouncycastle:bcutil-jdk18on:1.78.1")
        }
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcutil-jdk15to18")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // ---- Modules ----
    implementation(project(":core:common"))
    implementation(project(":core:filesystem"))
    implementation(project(":core:storage"))
    implementation(project(":core:permissions"))
    implementation(project(":core:security"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":engine"))
    implementation(project(":feature:home"))
    implementation(project(":feature:browser"))
    implementation(project(":feature:search"))
    implementation(project(":feature:recent"))
    implementation(project(":feature:favorites"))
    implementation(project(":feature:properties"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:analyzer"))
    implementation(project(":feature:archive"))
    implementation(project(":feature:media"))
    implementation(project(":feature:texteditor"))
    implementation(project(":feature:apk"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:network"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:windows"))

    // ---- AndroidX Core ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // ---- Lifecycle ----
    implementation(libs.bundles.lifecycle)

    // ---- Compose ----
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ---- Navigation ----
    implementation(libs.androidx.navigation.compose)

    // ---- Coroutines ----
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ---- Hilt ----
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // ---- WorkManager ----
    implementation(libs.androidx.work.runtime.ktx)

    // ---- Material (XML fallback) ----
    implementation(libs.material)

    // ---- Testing ----
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = "17" }
}
