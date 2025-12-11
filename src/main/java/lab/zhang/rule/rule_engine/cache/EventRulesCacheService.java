package lab.zhang.rule.rule_engine.cache;

import lab.zhang.rule.rule_engine.model.ExecutionArrangement;

import java.util.List;

/**
 * Event rules cache service interface
 * Manages cache for event rules: eventId -> List<ExecutionArrangement>
 * Used to cache execution arrangements to improve performance and reduce database queries.
 *
 * @author Rongjin Zhang
 */
public interface EventRulesCacheService {

    /**
     * Get cached execution arrangements for an event
     *
     * @param eventId event ID
     * @return cached list of execution arrangements, or null if not found in cache
     */
    List<ExecutionArrangement> get(Integer eventId);

    /**
     * Put execution arrangements into cache for an event
     *
     * @param eventId event ID
     * @param arrangements list of execution arrangements to cache
     */
    void put(Integer eventId, List<ExecutionArrangement> arrangements);

    /**
     * Invalidate cache for an event
     *
     * @param eventId event ID
     */
    void invalidate(Integer eventId);

    /**
     * Invalidate all cached execution arrangements
     */
    void invalidateAll();
}

