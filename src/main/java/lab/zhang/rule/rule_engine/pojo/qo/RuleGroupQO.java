package lab.zhang.rule.rule_engine.pojo.qo;

import lab.zhang.rule.rule_engine.validation.ValidRuleRatios;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request Object for creating a rule group
 * <p>
 * Note: Existence validation (checking if rules exist in database) is performed in Service layer,
 * not in Controller layer, as per project conventions (business logic validation belongs to Service layer).
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleGroupQO {

    /**
     * List of rule ratios for A/B test (0-100)
     */
    @NotEmpty(message = "Rules cannot be empty")
    @ValidRuleRatios(message = "Invalid rule ratios")
    private List<RuleRatioQO> ruleRatios;
}

