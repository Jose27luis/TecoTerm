package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

public class NoGlobalScopeRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Defect,
            "Prohibido GlobalScope: toda corrutina vive en un alcance con dueño.",
            Debt.TEN_MINS,
        )

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        super.visitSimpleNameExpression(expression)
        if (expression.getReferencedName() == "GlobalScope") {
            report(CodeSmell(issue, Entity.from(expression), issue.description))
        }
    }
}
