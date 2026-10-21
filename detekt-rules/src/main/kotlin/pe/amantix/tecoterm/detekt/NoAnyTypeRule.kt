package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class NoAnyTypeRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Defect,
            "Prohibido usar Any como tipo: se modela con una sealed interface o un tipo propio.",
            Debt.TEN_MINS,
        )

    override fun visitUserType(type: KtUserType) {
        super.visitUserType(type)
        if (nombraAny(type) && !esCotaGenerica(type) && !esParametroDeEquals(type)) {
            report(CodeSmell(issue, Entity.from(type), issue.description))
        }
    }

    private fun nombraAny(tipo: KtUserType): Boolean {
        val calificador = tipo.qualifier
        return tipo.referencedName == "Any" && (calificador == null || calificador.text == "kotlin")
    }

    private fun esCotaGenerica(tipo: KtUserType): Boolean =
        tipo.getStrictParentOfType<KtTypeParameter>() != null ||
            tipo.getStrictParentOfType<KtTypeConstraint>() != null

    private fun esParametroDeEquals(tipo: KtUserType): Boolean {
        val funcion = tipo.getStrictParentOfType<KtParameter>()?.getStrictParentOfType<KtNamedFunction>()
        return funcion != null &&
            funcion.name == "equals" &&
            funcion.hasModifier(KtTokens.OVERRIDE_KEYWORD) &&
            funcion.valueParameters.size == 1
    }
}
