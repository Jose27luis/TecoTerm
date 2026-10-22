import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal const val VERSION_SDK_COMPILACION: Int = 37
internal const val VERSION_SDK_MINIMA: Int = 33

internal fun CompileOptions.configurarJava() {
    sourceCompatibility = JavaVersion.toVersion(VERSION_JAVA)
    targetCompatibility = JavaVersion.toVersion(VERSION_JAVA)
}

internal fun Lint.configurarLintEstricto(proyecto: Project) {
    abortOnError = true
    warningsAsErrors = true
    checkDependencies = true
    lintConfig = proyecto.rootProject.file("config/lint/lint.xml")
}
