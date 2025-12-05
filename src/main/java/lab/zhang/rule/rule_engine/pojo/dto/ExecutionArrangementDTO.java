package lab.zhang.rule.rule_engine.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Execution item DTO for REST API
 * Represents a rule in execution sequence
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionArrangementDTO {

    /**
     * Event ID
     */
    private Integer eventId;
    /**
     * Rule ID
     */
    private Long ruleId;

    /**
     * Rule group ID (0 means standalone rule, non-zero means rule belongs to a group)
     */
    private Long groupId;

    /**
     * Execution order (starting from 0)
     */
    private Integer exeOrder;

    /**
     * A/B test ratio (0-100), traffic control for rules in group
     */
    private Integer abRatio;
}
