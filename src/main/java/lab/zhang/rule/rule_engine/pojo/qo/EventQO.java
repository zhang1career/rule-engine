package lab.zhang.rule.rule_engine.pojo.qo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * Event QO for REST API
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventQO {

    /**
     * Event ID (required for creation, unsigned long integer)
     * Note: For PUT /api/events/{eventId} (update), this field is optional and will be ignored
     */
    @NotNull(message = "Event ID cannot be null", groups = {Create.class})
    @Positive(message = "Event ID must be positive", groups = {Create.class})
    private Long id;

    /**
     * Event name (required for creation, max 100 characters)
     * Note: For PUT /api/events/{eventId} (update), this field is optional. If provided, it will be updated.
     */
    @NotBlank(message = "Event name cannot be blank", groups = {Create.class})
    @Size(max = 100, message = "Event name must not exceed 100 characters", groups = {Create.class, Update.class})
    private String name;

    /**
     * Event description (optional, max 250 characters)
     */
    @Size(max = 250, message = "Event description must not exceed 250 characters", groups = {Create.class, Update.class})
    private String description;


    /**
     * Validation group for create operation
     */
    public interface Create {
    }

    /**
     * Validation group for update operation
     */
    public interface Update {
    }
}

