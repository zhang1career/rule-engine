package lab.zhang.rule.rule_engine.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event DTO for REST API
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {

    /**
     * Event ID (required for creation, unsigned integer)
     * Note: For PUT /api/events/{eventId} (update), this field is optional and will be ignored
     */
    private Integer id;

    /**
     * Event name (required for creation, max 100 characters)
     * Note: For PUT /api/events/{eventId} (update), this field is optional. If provided, it will be updated.
     */
    private String name;

    /**
     * Event description (optional, max 250 characters)
     */
    private String description;
}

