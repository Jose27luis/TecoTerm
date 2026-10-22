import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                compileSdk = VERSION_SDK_COMPILACION
                defaultConfig {
                    minSdk = VERSION_SDK_MINIMA
                }
                compileOptions {
                    configurarJava()
                }
                lint {
                    configurarLintEstricto(target)
                }
            }
            configurarCompiladorKotlin()
            configurarPruebas()
        }
    }
}
