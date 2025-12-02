package lab.zhang.rule.rule_engine.pojo.qo;

import lab.zhang.rule.rule_engine.validation.ValidExecutionItemTypeId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Execution item QO for REST API
 * Represents a rule or rule group in execution sequence
 * According to project conventions, enumeration fields use enumeration IDs (integers) instead of enumeration values.
 * 
 * Note: Existence validation (checking if rule/rule group exists in database) is performed in Service layer,
 * not in Controller layer, as per project conventions (business logic validation belongs to Service layer).
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionItemQO {

    /**
     * Item type ID (enumeration ID: 0=RULE, 1=RULE_GROUP)
     */
    @NotNull(message = "Item type cannot be null")
    @ValidExecutionItemTypeId(message = "Invalid item type ID. Valid values are: 0=RULE, 1=RULE_GROUP")
    private Integer itemType;

    /**
     * Item ID (rule ID or rule group ID)
     */
    @NotNull(message = "Item ID cannot be null")
    @Positive(message = "Item ID must be positive")
    private Long itemId;
}

