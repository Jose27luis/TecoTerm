import com.android.build.api.dsl.ApplicationExtension
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
                    configurarJava()
                }
                buildFeatures {
                    compose = true
                }
                lint {
                    configurarLintEstricto(target)
                }
            }
            configurarCompiladorKotlin()
        }
    }
}
