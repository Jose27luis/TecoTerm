import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.toVersion(VERSION_JAVA)
                targetCompatibility = JavaVersion.toVersion(VERSION_JAVA)
            }
            configurarCompiladorKotlin()
            dependencies {
                add("testImplementation", platform(catalogo.findLibrary("junit-bom").get()))
                add("testImplementation", catalogo.findLibrary("junit-jupiter").get())
                add("testRuntimeOnly", catalogo.findLibrary("junit-platform-launcher").get())
            }
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
