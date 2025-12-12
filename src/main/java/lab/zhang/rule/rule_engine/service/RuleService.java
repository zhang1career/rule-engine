package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.engine.ExecutionItem;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;

import java.util.List;

/**
 * Rule service interface
 * 
 * @author Rongjin Zhang
 */
public interface RuleService {

    /**
     * Get all rules
     *
     * @return list of all rules
     */
    List<Rule> getAllRules();

    /**
     * Get rule by ruleId
     */
    Rule getRuleById(Long ruleId);

    /**
     * Create rule
     *
     * @param rule the rule to save
     * @throws IllegalArgumentException if rule with same name already exists
     * todo: first parameter change from Rule to RuleDTO
     */
    void createRule(Rule rule);

    /**
     * Create rule
     *
     * @param rule the rule to save
     * @param currentTimeSeconds current time in seconds
     * @throws IllegalArgumentException if rule with same name already exists
     */
    void createRuleAt(Rule rule, long currentTimeSeconds);

    /**
     * Update rule
     *
     * @param ruleId id of the rule
     * @param rule the rule to save
     * @throws IllegalArgumentException if status transition is not allowed
     */
    void updateRule(Long ruleId, Rule rule);


    /**
     * Delete rule
     * Only rules with OFFLINE status can be deleted.
     *
     * @param ruleId the rule ID to delete
     * @throws IllegalArgumentException if rule is not in OFFLINE status
     */
    void deleteRule(Long ruleId);


    /**
     * Get execution items (rules) in execution sequence by eventId
     * Returns a list of ExecutionItem, which are rules to be executed in order
     *
     * @param eventId event ID
     * @param context execution context (contains userId and userHashInt for rule selection)
     * @return list of execution items (rules)
     */
    List<ExecutionItem> getExecutionItemsByEventId(Integer eventId, RuleExecutionContext context);
}

