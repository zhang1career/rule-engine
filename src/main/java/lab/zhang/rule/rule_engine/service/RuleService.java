package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.engine.ExecutionItem;
import lab.zhang.rule.rule_engine.model.Rule;

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
     */
    void createRule(Rule rule);

    /**
     * Update rule
     *
     * @param ruleId id of the rule
     * @param rule the rule to save
     * @throws IllegalArgumentException if status transition is not allowed
     */
    void updateRule(Long ruleId, Rule rule);

    /**
     * Update rule in database without handling status change logic.
     * This is a pure database update method that does not trigger status change handlers.
     * Use this method when you need to update a rule without triggering status change callbacks.
     *
     * @param existingRule the existing rule
     * @param newRule the new rule data
     * @throws IllegalArgumentException if rule not found
     */
    void doUpdateRule(Rule existingRule, Rule newRule);

    /**
     * Delete rule
     * Only rules with OFFLINE status can be deleted.
     *
     * @param ruleId the rule ID to delete
     * @throws IllegalArgumentException if rule is not in OFFLINE status
     */
    void deleteRule(Long ruleId);

    /**
     * Save execution sequence
     */
    void saveExecutionSequence(Long eventId, List<Long> ruleIds);

    /**
     * Get execution items (rules or rule groups) in execution sequence by eventId
     * Returns a list of ExecutionItem, which can be either a single rule or a rule group
     *
     * @param eventId event ID
     * @return list of execution items
     */
    List<ExecutionItem> getExecutionItemsByEventId(Long eventId);
}

