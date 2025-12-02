package lab.zhang.rule.rule_engine.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Execution item QO for REST API
 * Represents a rule or rule group in execution sequence
 * According to project conventions, enumeration fields use enumeration IDs (integers) instead of enumeration values.
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionItemDTO {

    /**
     * Item type ID (enumeration ID: 0=RULE, 1=RULE_GROUP)
     */
    private Integer itemType;

    /**
     * Item ID (rule ID or rule group ID)
     */
    private Long itemId;

    /**
     * Execution order (starting from 1)
     */
    private Integer executionOrder;
}

