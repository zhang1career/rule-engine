package lab.zhang.rule.rule_engine.pojo.qo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Request Object for batch setting execution items for an event
 * The order of items in the list represents the execution order (starting from 1)
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSetExecutionItemsQO {

    /**
     * List of execution items (rules and rule groups)
     * The order in the list represents the execution order (starting from 1)
     * Note: The executionOrder field in ExecutionItemDTO will be ignored, only list order matters
     */
    @NotNull(message = "Execution items cannot be null")
    @Valid
    private List<ExecutionItemQO> executionItems;
}

