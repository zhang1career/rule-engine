package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO;

import java.util.List;
import java.util.Map;

/**
 * Rule group service interface
 * Manages rule groups for A/B testing
 *
 * @author Rongjin Zhang
 */
public interface RuleGroupService {

    /**
     * Get all rule groups
     *
     * @return list of all rule groups
     */
    List<RuleGroup> getAllRuleGroups();

    /**
     * Get rule group by groupId
     */
    RuleGroup getRuleGroup(Long groupId);

    /**
     * Create a new rule group and add the rule to it.
     * Called when a rule changes from other status to ONLINE status.
     * 
     * Business Logic:
     * 1. Check if this rule+event combination already exists in another rule group, if so, exit
     * 2. Create new rule group
     * 3. Create record in gre_rel table (rule group-rule-event relation)
     * 4. Create record in execution_event_rel table (event-rule group relation)
     * 5. Delete record in execution_event_rel table (event-rule relation)
     *
     * @param rule the rule to add to the new group (must be in ONLINE status)
     * @param eventId the event ID to associate with the rule group
     * @return the created rule group
     * @throws IllegalArgumentException if rule is not in ONLINE status, or rule+event combination already exists in another group
     */
    RuleGroup createRuleGroup(Rule rule, Integer eventId);

    /**
     * Delete a rule group
     *
     * @param groupId rule group ID
     * @throws IllegalArgumentException if group not found
     */
    void deleteRuleGroup(Long groupId);

    /**
     * Update rule group traffic control ratios.
     * Only receives ratios parameter for editing.
     * 
     * Validation:
     * - Key must be Long positive integer (rule ID)
     * - Value must be non-negative integer (abTestRatio, 0-100)
     * - Sum of all values must not exceed 100
     *
     * @param groupId rule group ID
     * @param ratios map of rule ID to A/B test ratio (0-100)
     * @throws IllegalArgumentException if group not found, validation fails, or rule not in group
     */
    void updateRuleGroupRatios(Long groupId, Map<Long, Integer> ratios);

    /**
     * Copy a rule within a rule group.
     * Creates a new rule with the same content, event association, and rule group association as the original.
     * The copied rule's abTestRatio is set to 0.
     *
     * @param ruleId the rule ID to copy
     * @param ruleDTO the new rule object to create (name, description, contentType, content)
     * @return the copied rule
     * @throws IllegalArgumentException if rule not found or rule not in any group
     */
    Rule copyRuleInGroup(Long ruleId, RuleDTO ruleDTO);

    /**
     * Select one rule from rule group based on probability distribution
     * Uses cached selection if available, otherwise uses pseudo-random selection based on userHash
     *
     * @param group   the rule group
     * @param context the rule execution context (contains userHash in arguments)
     * @return the selected rule, or null if no rule should be executed
     */
    Rule selectRuleFromGroup(RuleGroup group, RuleExecutionContext context);

}

