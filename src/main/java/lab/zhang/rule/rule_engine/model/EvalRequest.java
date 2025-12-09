package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import java.util.Map;

/**
 * Rule evaluation request DTO
 *
 * @author Rongjin Zhang
 */
@Data
public class EvalRequest {

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

