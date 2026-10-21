package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty

public class NoLateinitRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Defect,
            "Prohibido lateinit: la propiedad se inicializa en el constructor o se modela como nullable.",
            Debt.TEN_MINS,
        )

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.hasModifier(KtTokens.LATEINIT_KEYWORD)) {
            report(CodeSmell(issue, Entity.from(property), issue.description))
        }
    }
}
