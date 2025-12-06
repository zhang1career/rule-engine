package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.Event;
import lab.zhang.rule.rule_engine.model.ExecutionArrangement;

import java.util.List;

/**
 * Event service interface
 *
 * @author Rongjin Zhang
 */
public interface EventService {

    /**
     * Get all events
     *
     * @return list of all events
     */
    List<Event> getAllEvents();

    /**
     * Get event by eventId
     *
     * @param eventId event ID (unsigned long integer)
     * @return event model, or null if not found
     */
    Event getEventById(Long eventId);

    /**
     * Create a new event
     *
     * @param id          event ID (required, unsigned long integer, must be specified)
     * @param name        event name (required, max 100 characters)
     * @param description event description (optional, max 250 characters)
     * @return created event model
     * @throws IllegalArgumentException if id is null or event with same id already exists
     */
    Event createEvent(Long id, String name, String description);

    /**
     * Update event
     *
     * @param eventId     event ID (unsigned long integer)
     * @param name        event name (optional, max 100 characters)
     * @param description event description (optional, max 250 characters)
     * @return updated event model
     * @throws IllegalArgumentException if event not found
     */
    Event updateEvent(Long eventId, String name, String description);

    /**
     * Delete event
     *
     * @param eventId event ID (unsigned long integer)
     * @throws IllegalArgumentException if event not found
     */
    void deleteEvent(Long eventId);

    /**
     * Get execution event relations for an event
     *
     * @param eventId event ID
     * @return list of execution event relation entities
     */
    List<ExecutionArrangement> getExecutionItems(Integer eventId);

    /**
     * Batch set execution items (rules and rule groups) for an event
     * The order of items in the list represents the execution order
     * <p>
     * This method will:
     * - Delete existing relations that are not in the new list
     * - Create new relations that don't exist
     * - Update execution order for existing relations
     *
     * @param eventId        event ID
     * @param ruleIdList     list of rule IDs (order represents execution order)
     * @throws IllegalArgumentException if event not found, or any item not found
     */
    void setExecutionItems(Integer eventId, List<Long> ruleIdList);
}

