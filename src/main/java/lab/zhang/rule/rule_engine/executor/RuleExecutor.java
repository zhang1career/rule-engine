package lab.zhang.rule.rule_engine.executor;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;

/**
 * Rule executor interface
 * All rule types need to implement this interface
 * 
 * @author rule-engine
 */
public interface RuleExecutor {
    
    /**
     * Execute rule
     * 
     * @param rule rule object
     * @param context execution context
     * @return execution result
     */
    TypedValue execute(Rule rule, RuleExecutionContext context);
    
    /**
     * Get supported rule type
     * 
     * @return rule type
     */
    lab.zhang.rule.rule_engine.enums.RuleType getSupportedRuleType();
}

