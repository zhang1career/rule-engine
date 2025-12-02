package lab.zhang.rule.rule_engine.pojo.dto;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import java.util.Map;

/**
 * Rule evaluation request DTO
 *
 * @author Rongjin Zhang
 */
@Data
public class EvalDTO {

    /**
     * User ID
     */
    private Long userId;

    /**
     * Event ID (unsigned long integer)
     */
    private Long eventId;

    /**
     * Business request trace ID
     */
    private Long traceId;

    /**
     * Parameter dictionary for rule calculation
     */
    private Map<String, TypedValue> arguments;
}

