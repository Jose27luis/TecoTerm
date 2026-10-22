plugins {
    alias(libs.plugins.tecoterm.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "pe.amantix.tecoterm.data"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core-ssh"))
    implementation(project(":core-security"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)
}
