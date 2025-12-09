package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Rule entity class representing a business rule in the rule engine.
 *
 * <p>A rule consists of:
 * <ul>
 *   <li>Unique identifier (ruleId)</li>
 *   <li>Rule type (expression, script, API query, SQL query)</li>
 *   <li>Rule content (the actual rule logic)</li>
 *   <li>Status (offline, test, A/B test, full, gray)</li>
 * </ul>
 *
 * <p>This class follows the builder pattern for object creation.
 * Use {@link RuleBuilder} to create instances.
 *
 * @author Rongjin Zhang
 * @see ContentTypeEnum
 * @see RuleStatusEnum
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule extends BaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Rule ID (unique identifier).
     * Default value is 0, not required for rule calculation.
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
     * Rule content type determines how the rule content is executed.
     *
     * @see ContentTypeEnum
     */
    private ContentTypeEnum contentType;

    /**
     * List of argument names that the rule content depends on.
     * sorted in alphabetical order.
     */
    private List<String> contentArgList;

    /**
     * Rule content in a format specific to the rule content type.
     * <ul>
     *   <li>EXPRESSION: MVEL expression</li>
     *   <li>SCRIPT: Groovy script</li>
     *   <li>API_QUERY: API endpoint URL</li>
     *   <li>SQL_QUERY: SQL query string</li>
     * </ul>
     */
    private String content;

    /**
     * Rule status determines when and where the rule can be executed.
     *
     * @see RuleStatusEnum
     */
    private RuleStatusEnum ruleStatus;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(id, rule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "ruleId=" + id +
                ", ruleName='" + name + '\'' +
                ", contentType=" + contentType +
                ", ruleStatus=" + ruleStatus +
                '}';
    }
}

