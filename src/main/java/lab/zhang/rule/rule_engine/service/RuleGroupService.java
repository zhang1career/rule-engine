package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.model.RuleGroup;

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
     * Create a new rule group and add the rule to it
     * Called when a rule changes from other status to AB_TEST status
     *
     * @param rule the rule to add to the new group
     * @return the created rule group
     */
    RuleGroup createRuleGroup(Rule rule);

    /**
     * Delete a rule group
     * Sets all rules in the group to OFFLINE, removes them from group, then deletes the group
     *
     * @param groupId rule group ID
     * @throws IllegalArgumentException if group not found
     */
    void deleteRuleGroup(Long groupId);

    /**
     * Delete a rule group and all its associations
     *
     * @param groupId rule group ID
     */
    void doDeleteRuleGroup(Long groupId);

    /**
     * Create a new rule group with rules
     * Validates that rules can transition to AB_TEST status, then creates group and updates rules
     *
     * @param rules map of rule ID to A/B test ratio (0-100)
     * @return the created rule group
     * @throws IllegalArgumentException if any rule cannot transition to AB_TEST status
     */
    RuleGroup createRuleGroupWithRules(Map<Long, Integer> rules);

    /**
     * Update a rule group with rules
     * Validates that rules can transition to AB_TEST status (or are already AB_TEST), then updates group
     * Rules not in the input map will be set to OFFLINE and removed from group
     *
     * @param groupId rule group ID
     * @param rules   map of rule ID to A/B test ratio (0-100)
     * @throws IllegalArgumentException if group not found or any rule cannot transition to AB_TEST status
     */
    void updateRuleGroupWithRules(Long groupId, Map<Long, Integer> rules);

    /**
     * Add rule to existing rule group
     * Called when a rule changes from other status to AB_TEST status with existing groupId
     *
     * @param rule    the rule to add
     * @param groupId the existing group ID
     */
    void addRuleToGroup(Rule rule, Long groupId);

    /**
     * Set all other rules in the group to OFFLINE status
     * Called when a rule changes from AB_TEST status to FULL status
     *
     * @param rule the rule that changed to FULL status
     */
    void setOtherRulesOffline(Rule rule);

    /**
     * Delete rule group if it's empty
     *
     * @param groupId the group ID to check and delete
     */
    void deleteRuleGroupIfEmpty(Long groupId);

    /**
     * Select one rule from rule group based on probability distribution
     * Uses cached selection if available, otherwise uses pseudo-random selection based on userHash
     *
     * @param group   the rule group
     * @param context the rule execution context (contains userHash in arguments)
     * @return the selected rule, or null if no rule should be executed
     */
    Rule selectRuleFromGroup(RuleGroup group, RuleExecutionContext context);

    /**
     * Associate a rule group with an eventId
     *
     * @param groupId rule group ID
     * @param eventId event ID
     */
    void associateGroupWithEventId(Long groupId, Long eventId);

    /**
     * Remove association between a rule group and an eventId
     *
     * @param groupId rule group ID
     * @param eventId event ID
     */
    void removeGroupEventIdAssociation(Long groupId, Long eventId);

    /**
     * Validate and clean invalid cache entries
     * This method should be called by scheduled tasks to clean invalid cache entries
     * It validates cached rules and removes invalid entries from cache
     */
    void validateAndCleanInvalidCache();
}

