package lab.zhang.rule.rule_engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Rule group entity class.
 *
 * <p>A rule group contains one or more A/B test rules.
 * Only one rule in a group will be executed per execution,
 * selected based on the rules' A/B test ratios.
 *
 * <p>This class follows the builder pattern for object creation.
 * Use {@link RuleGroupBuilder} to create instances.
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleGroup extends BaseModel implements Serializable {

    public static final long DEFAULT_RULE_GROUP_ID = 0L;


    private static final long serialVersionUID = 1L;

    /**
     * Rule group ID (unique identifier).
     */
    private Long id;

    /**
     * Rule name for identification and display purposes.
     */
    private String name;

    /**
     * Human-readable description of the rule's purpose and behavior.
     */
    private String description;

    /**
     * Rules in this group (all rules must be in AB_TEST status).
     * Key: rule ID, Value: Pair of Rule model and A/B test ratio (0-100).
     */
    @Builder.Default
    private Map<Long, Pair<Rule, Integer>> rules = new HashMap<>();


    /**
     * Creates a new rule group with the specified group ID.
     *
     * @param id the group ID, must not be null
     * @throws IllegalArgumentException if groupId is null
     */
    public RuleGroup(@NonNull Long id) {
        this.id = Objects.requireNonNull(id, "Group ID cannot be null");
        this.rules = new HashMap<>();
    }

    /**
     * Gets an unmodifiable view of the rule IDs in this group.
     *
     * @return an unmodifiable list of rule IDs
     */
    public List<Long> getRuleIds() {
        return rules != null ? Collections.unmodifiableList(new ArrayList<>(rules.keySet())) : Collections.emptyList();
    }

    /**
     * Adds a rule to this group.
     *
     * @param ruleId the rule ID to add, must not be null
     * @param rule the rule model, must not be null
     * @param ratio the A/B test ratio (0-100), must not be null
     * @return true if the rule was added, false if it already exists
     * @throws IllegalArgumentException if ruleId, rule, or ratio is null
     */
    public boolean addRule(@NonNull Long ruleId, @NonNull Rule rule, @NonNull Integer ratio) {
        Objects.requireNonNull(ruleId, "Rule ID cannot be null");
        Objects.requireNonNull(rule, "Rule cannot be null");
        Objects.requireNonNull(ratio, "Ratio cannot be null");
        if (rules.containsKey(ruleId)) {
            return false;
        }
        rules.put(ruleId, Pair.of(rule, ratio));
        return true;
    }

    /**
     * Removes a rule from this group.
     *
     * @param ruleId the rule ID to remove
     * @return true if the rule was removed, false if it was not in the group
     */
    public boolean removeRule(Long ruleId) {
        return rules != null && rules.remove(ruleId) != null;
    }

    /**
     * Checks if this group is empty (contains no rules).
     *
     * @return true if the group has no rules, false otherwise
     */
    public boolean isEmpty() {
        return rules == null || rules.isEmpty();
    }

    /**
     * Gets the number of rules in this group.
     *
     * @return the number of rules
     */
    public int size() {
        return rules != null ? rules.size() : 0;
    }

    /**
     * Checks if this group contains the specified rule ID.
     *
     * @param ruleId the rule ID to check
     * @return true if the group contains the rule ID, false otherwise
     */
    public boolean contains(Long ruleId) {
        return rules != null && rules.containsKey(ruleId);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleGroup ruleGroup = (RuleGroup) o;
        return Objects.equals(id, ruleGroup.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RuleGroup{" +
                "groupId=" + id +
                ", ruleIds=" + getRuleIds() +
                ", size=" + size() +
                '}';
    }
}

