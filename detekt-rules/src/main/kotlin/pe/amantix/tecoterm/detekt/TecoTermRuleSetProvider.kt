package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

public class TecoTermRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "tecoterm"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                NoCommentsRule(config),
                NoNotNullAssertionRule(config),
                NoUnsafeCastRule(config),
                NoAnyTypeRule(config),
                NoSuppressRule(config),
                NoLateinitRule(config),
                NoGlobalScopeRule(config),
            ),
        )
}
