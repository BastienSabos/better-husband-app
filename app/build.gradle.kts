plugins {
    // AGP 9 compile le Kotlin lui-même : pas de plugin org.jetbrains.kotlin.android.
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    // Captures d'écran des tests Robolectric (app/src/test/screenshots/), comme le budget planner.
    alias(libs.plugins.roborazzi)
}

// Schémas Room exportés puis committés : ils servent aux tests de migration et ne se modifient jamais à la main.
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    // ⚠️ L'applicationId est définitif une fois publié sur le Play Store : à remplacer par un identifiant
    // reflétant la marque d'appli de couple avant le premier upload (le namespace peut suivre plus tard).
    namespace = "com.bastiensabos.betterhusband"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bastiensabos.betterhusband"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric lit les ressources (textes, thème, polices) comme sur un téléphone.
        unitTests.isIncludeAndroidResources = true
        animationsDisabled = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        // Les montées de version sont gérées par Dependabot, pas par Lint.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi")
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    // Verrouillage par empreinte, visage ou code de l'appareil (BiometricPrompt, tire FragmentActivity).
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // Icônes Material (barre de navigation, retour, réglages) ; R8 retire celles qui ne servent pas. Comme le budget planner.
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    constraints {
        // Alignées sur les tests instrumentés (room-testing, androidx.test), sinon AGP refuse les deux classpaths.
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.androidx.concurrent.futures.ktx)
    }
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.accessibility.check)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
