plugins {
    alias(libs.plugins.tecoterm.jvm.library)
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
}

tasks.jar {
    archiveFileName.set("detekt-rules.jar")
}
