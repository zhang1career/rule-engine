package lab.zhang.rule.rule_engine.cache;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.model.Rule;

import java.util.List;

/**
 * Rule evaluation result cache service interface
 * Manages cache for rule evaluation results: (ruleId + hashOfArgs) -> result
 * Used to cache rule evaluation results to improve performance and reduce redundant calculations.
 *
 * @author Rongjin Zhang
 */
public interface EvalCacheService {

    /**
     * Get cached evaluation result for a rule with given arguments
     *
     * @param rule the rule to evaluate
     * @param argValues sorted list of argument values for the rule
     * @return cached TypedValue result, or null if not found in cache
     */
    TypedValue get(Rule rule, List<String> argValues);

    /**
     * Put evaluation result into cache for a rule with given arguments
     *
     * @param rule the rule that was evaluated
     * @param argValues sorted list of argument values used for evaluation
     * @param result the evaluation result to cache
     */
    void put(Rule rule, List<String> argValues, TypedValue result);

    /**
     * Build cache key for a rule with given arguments
     *
     * @param keyPrefix the key prefix (rule ID as string)
     * @param argValues sorted list of argument values
     * @return cache key string
     */
    String buildCacheKey(String keyPrefix, List<String> argValues);
}
