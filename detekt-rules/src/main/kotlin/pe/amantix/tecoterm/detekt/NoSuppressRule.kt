package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry

public class NoSuppressRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Defect,
            "Prohibido silenciar advertencias con @Suppress: se corrige la causa.",
            Debt.TEN_MINS,
        )

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        if (annotationEntry.shortName?.asString() in anotacionesProhibidas) {
            report(CodeSmell(issue, Entity.from(annotationEntry), issue.description))
        }
    }

    private companion object {
        val anotacionesProhibidas: Set<String> = setOf("Suppress", "SuppressWarnings")
    }
}
