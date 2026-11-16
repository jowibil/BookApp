plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.bookappp.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bookappp.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.2.0"

        // Required by Readium for java.time and other java.* backports below API 26.
        // AGENTS.md pins minSdk 26, but desugaring keeps this safe if we lower it later.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets {
        // MigrationTestHelper reads the exported Room schemas from test assets.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Readium hard-requires core library desugaring.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Readium's preference types initialise defaults through android.graphics.Color,
            // which plain JVM tests otherwise reject as "not mocked" (ReaderPreferencesMapperTest).
            isReturnDefaultValues = true
        }
    }
}

ksp {
    // Export Room schemas for migration testing (AGENTS.md testing expectations).
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    // AppCompatActivity host for Readium navigator fragments.
    implementation(libs.androidx.appcompat)
    // Fragment host used by the reader navigator.
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose BOM keeps all Compose artifact versions in sync
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // MenuBook/add icons used by the library screen live in the extended set.
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation (type-safe routes via kotlinx.serialization)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Dependency injection (Hilt, KSP-based since kapt is gone with built-in Kotlin)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Local database (Room) and preferences (DataStore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // Images (Coil 3, Compose integration)
    implementation(libs.coil.compose)

    // EPUB/PDF engine (Readium Kotlin Toolkit 3.4.0)
    // Keep Readium usage isolated to ui/reader and data/files per AGENTS.md.
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.readium.navigator)
    // PDF parsing + rendering: PdfiumDocumentFactory and the Pdfium engine adapter.
    implementation(libs.readium.adapter.pdfium)

    // Unit tests: ViewModels and repositories with fakes (AGENTS.md testing expectations).
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)

    // Instrumented Compose UI tests for the library and reader-settings flows.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
