plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Resolve each Robolectric instrumented SDK jar in its own configuration.
// Gradle resolves conflicting versions of the same module to one version when they
// share a configuration; separate configurations are required to stage API 26/34/35.
val robolectricSdkArtifacts = listOf(
    "8.0.0_r4-robolectric-r1-i7",
    "14-robolectric-10818077-i7",
    "15-robolectric-12650502-i7"
).mapIndexed { index, version ->
    val sdkConfiguration = configurations.create("robolectricSdkApi${listOf(26, 34, 35)[index]}") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(
        sdkConfiguration.name,
        "org.robolectric:android-all-instrumented:$version"
    )
    version to sdkConfiguration
}
val robolectricSdkDirectory = layout.buildDirectory.dir("robolectric-sdk")
val prepareRobolectricSdk = tasks.register(
    "prepareRobolectricSdk",
    org.gradle.api.tasks.Sync::class.java
) {
    robolectricSdkArtifacts.forEach { (_, sdkConfiguration) ->
        from(sdkConfiguration)
    }
    into(robolectricSdkDirectory)
    doLast {
        val missingArtifacts = robolectricSdkArtifacts
            .map { (version, _) -> "android-all-instrumented-$version.jar" }
            .filterNot { fileName -> robolectricSdkDirectory.get().file(fileName).asFile.isFile }
        check(missingArtifacts.isEmpty()) {
            "Robolectric SDK pre-warm did not stage required artifacts: ${missingArtifacts.joinToString()}"
        }
    }
}

android {
    namespace = "com.superdenot.powerguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.superdenot.superdguard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty(
                    "robolectric.dependency.dir",
                    robolectricSdkDirectory.get().asFile.absolutePath
                )
            }
        }
    }
}

dependencies {
    // Core & Coroutines
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Room Database (dengan KSP)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager (Untuk Daily Rollup & Pembersihan Log)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // DataStore (Menyimpan Kalibrasi Sensor & Kapasitas Desain Pabrik)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Unit & Regression Testing (Room + Robolectric + Coroutines)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Robolectric 4.14.1 pre-instrumented framework jars for every SDK selected by tests:
    // API 26 (method-level override), API 34 (class-level configs), and API 35
    // (Robolectric's default SDK for AutoStartDiagnosticLogTest).
    add(robolectricSdkArtifacts.name, "org.robolectric:android-all-instrumented:8.0.0_r4-robolectric-r1-i7")
    add(robolectricSdkArtifacts.name, "org.robolectric:android-all-instrumented:14-robolectric-10818077-i7")
    add(robolectricSdkArtifacts.name, "org.robolectric:android-all-instrumented:15-robolectric-12650502-i7")
}

tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
    dependsOn(prepareRobolectricSdk)
}
