import java.util.Properties

plugins {
    alias(libs.plugins.tecoterm.android.application)
}

val archivoFirma: File = rootProject.file("secrets/keystore.properties")

android {
    namespace = "pe.amantix.tecoterm"

    defaultConfig {
        applicationId = "pe.amantix.tecoterm"
        versionCode = 1
        versionName = "0.1.0"
    }

    if (archivoFirma.exists()) {
        val propiedades = Properties().apply { archivoFirma.inputStream().use(::load) }
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(propiedades.getProperty("storeFile"))
                storePassword = propiedades.getProperty("storePassword")
                keyAlias = propiedades.getProperty("keyAlias")
                keyPassword = propiedades.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (archivoFirma.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":core-ssh"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}
