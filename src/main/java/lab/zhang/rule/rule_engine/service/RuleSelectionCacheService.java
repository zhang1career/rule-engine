package lab.zhang.rule.rule_engine.service;

/**
 * Rule selection cache service interface
 * Manages cache for rule selection: (userId, eventId, groupId) -> ruleId
 * Used to ensure consistent rule selection for the same user, event, and rule group combination.
 * Since there can be multiple rule groups, this cache records which specific rule was selected
 * from each rule group for a user-event combination.
 *
 * Cache key format: rule:gw:abt:{userId}:{eventId}:{groupId}
 * Cache value: selected rule ID (Long)
 *
 * @author Rongjin Zhang
 */
public interface RuleSelectionCacheService {

    /**
     * Get cached rule ID for given userId, eventId, and groupId
     *
     * @param userId  user ID
     * @param eventId event ID
     * @param groupId rule group ID
     * @return cached rule ID, or null if not found
     */
    Long get(Long userId, Integer eventId, Long groupId);

    /**
     * Put rule ID into cache for given userId, eventId, and groupId
     *
     * @param userId  user ID
     * @param eventId event ID
     * @param groupId rule group ID
     * @param ruleId  rule ID to cache
     */
    void put(Long userId, Integer eventId, Long groupId, Long ruleId);

    /**
     * Remove cached rule ID for given userId, eventId, and groupId
     *
     * @param userId  user ID
     * @param eventId event ID
     * @param groupId rule group ID
     */
    void remove(Long userId, Integer eventId, Long groupId);

    /**
     * Get all cache keys matching the prefix
     * Used by scheduled tasks to clean invalid cache entries
     *
     * @return set of cache keys
     */
    java.util.Set<String> getAllCacheKeys();
}

