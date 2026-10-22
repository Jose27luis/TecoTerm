import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configurarPruebas() {
    dependencies {
        add("testImplementation", platform(catalogo.findLibrary("junit-bom").get()))
        add("testImplementation", catalogo.findLibrary("junit-jupiter").get())
        add("testRuntimeOnly", catalogo.findLibrary("junit-platform-launcher").get())
    }
    val entornoPruebas = providers.environmentVariablesPrefixedBy("TECOTERM_")
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        environment(entornoPruebas.get())
    }
}
