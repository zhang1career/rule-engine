package lab.zhang.rule.rule_engine.pojo.qo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Rule ratio QO for rule group
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRatioQO {

    /**
     * Rule ID
     */
    @NotNull(message = "Rule ID cannot be null")
    @Min(value = 1, message = "Rule ID must be a positive integer")
    private Long ruleId;

    /**
     * A/B test ratio (0-100)
     */
    @NotNull(message = "Ratio cannot be null")
    @Min(value = 0, message = "Ratio must be between 0 and 100")
    private Integer ratio;
}

