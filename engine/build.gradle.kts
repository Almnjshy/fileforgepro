plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.fileforge.pro.engine"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:common"))
    api(project(":domain"))
    api(project(":core:storage"))
    api(project(":core:filesystem"))
    api(project(":core:security"))

    // Compose — needed by SyntaxHighlighter (AnnotatedString, SpanStyle, Color)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-graphics")

    api(libs.bundles.coil)
    api(libs.commons.compress)
    api(libs.zip4j)
    api(libs.commons.net)
    api(libs.jcifs)
    api(libs.sardine)
    api(libs.pdfbox.android)
    api(libs.bundles.media3)
    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.documentfile)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
