package lab.zhang.rule.rule_engine.cache;

import java.util.Map;
import java.util.Set;

/**
 * Rule content cache service interface
 * Manages cache for rule content: (ruleId) -> content
 * Used to cache rule content to improve performance and reduce database queries.
 * <p>
 * Cache key format: rule:content:{ruleId}
 * Cache value: rule content (String)
 *
 * @author Rongjin Zhang
 */
public interface RuleContentCacheService {
    /**
     * Get cached rule contents for multiple ruleIds in batch
     *
     * @param ruleIds set of rule IDs
     * @return map of ruleId to cached content, only includes found entries
     */
    Map<Long, String> getBatch(Set<Long> ruleIds);

    /**
     * Put rule content into cache for given ruleId
     *
     * @param ruleId  rule ID
     * @param content rule content to cache
     */
    void put(Long ruleId, String content);

    /**
     * Put multiple rule contents into cache in batch
     *
     * @param ruleContentMap map of ruleId to content
     */
    void putBatch(Map<Long, String> ruleContentMap);
}
