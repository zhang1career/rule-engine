package lab.zhang.rule.rule_engine.executor;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;

import javax.validation.constraints.NotBlank;

/**
 * Rule executor interface
 * All rule types need to implement this interface
 *
 * @author Rongjin Zhang
 */
public interface RuleExecutor {

    /**
     * Execute rule
     *
     * @param rule    rule object
     * @param context execution context
     * @return execution result
     */
    TypedValue execute(Rule rule, RuleExecutionContext context);

    /**
     * Get supported rule type
     *
     * @return rule type
     */
    ContentTypeEnum getSupportedRuleType();

    /**
     * Validate rule content
     *
     * @param content rule content to validate
     * @throws IllegalArgumentException if content is invalid
     */
    void validate(@NotBlank String content);
}
