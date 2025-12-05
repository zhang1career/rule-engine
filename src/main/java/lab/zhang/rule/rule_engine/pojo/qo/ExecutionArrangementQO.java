package lab.zhang.rule.rule_engine.pojo.qo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * Request Object for batch rules for an event
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionArrangementQO {

    /**
     * List of rule IDs
     * The order in the list represents the execution order (starting from 0)
     */
    @NotNull(message = "Rule IDs cannot be null")
    private List<@Positive(message = "Rule ID must be positive") Long> rules;
}

