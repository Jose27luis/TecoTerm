package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtFile

public class NoCommentsRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Style,
            "El código no lleva comentarios: ni KDoc, ni de línea, ni de bloque.",
            Debt.FIVE_MINS,
        )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        PsiTreeUtil.collectElementsOfType(file, PsiComment::class.java).forEach { comentario ->
            report(CodeSmell(issue, Entity.from(comentario), issue.description))
        }
    }
}
