package lab.zhang.rule.rule_engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Event model class representing an event in the rule engine.
 *
 * <p>An event represents a business scenario that triggers rule execution.
 * It consists of:
 * <ul>
 *   <li>Unique identifier (eventId)</li>
 *   <li>Event name</li>
 *   <li>Event description</li>
 * </ul>
 *
 * <p>This class follows the builder pattern for object creation.
 * Use {@link EventBuilder} to create instances.
 * 
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Event ID (unique identifier).
     * Must be specified when creating, not auto-increment.
     */
    private Integer id;
    
    /**
     * Event name for identification and display purposes.
     */
    private String name;
    
    /**
     * Human-readable description of the event's purpose and behavior.
     */
    private String description;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

