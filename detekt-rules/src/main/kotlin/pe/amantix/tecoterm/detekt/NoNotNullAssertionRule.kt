package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression

public class NoNotNullAssertionRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Defect,
            "Prohibido el operador !!: el nulo se resuelve con el tipo, ?., ?: o un smart cast.",
            Debt.FIVE_MINS,
        )

    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        super.visitPostfixExpression(expression)
        if (expression.operationToken == KtTokens.EXCLEXCL) {
            report(CodeSmell(issue, Entity.from(expression), issue.description))
        }
    }
}
