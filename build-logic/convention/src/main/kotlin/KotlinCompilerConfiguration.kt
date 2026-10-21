import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal const val VERSION_JAVA: Int = 17

internal val Project.catalogo: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configurarCompiladorKotlin() {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        val esCompilacionDePruebas = name.contains("Test")
        compilerOptions {
            allWarningsAsErrors.set(true)
            if (this is KotlinJvmCompilerOptions) {
                jvmTarget.set(JvmTarget.fromTarget(VERSION_JAVA.toString()))
            }
            if (!esCompilacionDePruebas) {
                freeCompilerArgs.add("-Xexplicit-api=strict")
            }
        }
    }
}
