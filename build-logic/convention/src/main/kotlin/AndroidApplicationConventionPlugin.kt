import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<ApplicationExtension> {
                compileSdk = VERSION_SDK_COMPILACION
                defaultConfig {
                    minSdk = VERSION_SDK_MINIMA
                    targetSdk = VERSION_SDK_COMPILACION
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(VERSION_JAVA)
                    targetCompatibility = JavaVersion.toVersion(VERSION_JAVA)
                }
                buildFeatures {
                    compose = true
                }
                lint {
                    abortOnError = true
                    warningsAsErrors = true
                    checkDependencies = true
                    lintConfig = rootProject.file("config/lint/lint.xml")
                }
            }
            configurarCompiladorKotlin()
        }
    }

    private companion object {
        const val VERSION_SDK_COMPILACION: Int = 37
        const val VERSION_SDK_MINIMA: Int = 33
    }
}
