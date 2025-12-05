package lab.zhang.rule.rule_engine.service.impl;

import lab.zhang.rule.rule_engine.service.RuleSelectionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Rule selection cache service implementation using Redis
 * Cache key prefix: rule:gw:abt
 * 
 * This cache service records which specific rule was selected from each rule group
 * for a user-event combination. Since there can be multiple rule groups, the cache key
 * includes the groupId to distinguish selections from different rule groups.
 * 
 * Cache key format: rule:gw:abt:{userId}:{eventId}:{groupId}
 * Cache value: selected rule ID (Long)
 * Cache expiration: 24 hours
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class RuleSelectionCacheServiceImpl implements RuleSelectionCacheService {

    private static final String CACHE_KEY_PREFIX = "rule:gw:abt:";
    private static final long CACHE_EXPIRE_HOURS = 24; // Cache expires after 24 hours

    @Autowired
    private RedisTemplate<String, Long> redisTemplate;

    @Override
    public Long get(Long userId, Integer eventId, Long groupId) {
        if (userId == null || eventId == null || groupId == null) {
            return null;
        }
        String key = buildCacheKey(userId, eventId, groupId);
        try {
            Long ruleId = redisTemplate.opsForValue().get(key);
            if (ruleId != null) {
                log.debug("Cache hit: key={}, ruleId={}", key, ruleId);
            }
            return ruleId;
        } catch (Exception e) {
            log.error("Error getting cache value for key: {}", key, e);
            return null;
        }
    }

    @Override
    public void put(Long userId, Integer eventId, Long groupId, Long ruleId) {
        if (userId == null || eventId == null || groupId == null || ruleId == null) {
            return;
        }
        String key = buildCacheKey(userId, eventId, groupId);
        try {
            redisTemplate.opsForValue().set(key, ruleId, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("Cache put: key={}, ruleId={}, expireHours={}", key, ruleId, CACHE_EXPIRE_HOURS);
        } catch (Exception e) {
            log.error("Error putting cache value for key: {}, ruleId: {}", key, ruleId, e);
        }
    }

    @Override
    public void remove(Long userId, Integer eventId, Long groupId) {
        if (userId == null || eventId == null || groupId == null) {
            return;
        }
        String key = buildCacheKey(userId, eventId, groupId);
        try {
            redisTemplate.delete(key);
            log.debug("Cache remove: key={}", key);
        } catch (Exception e) {
            log.error("Error removing cache value for key: {}", key, e);
        }
    }

    @Override
    public java.util.Set<String> getAllCacheKeys() {
        try {
            return redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        } catch (Exception e) {
            log.error("Error getting all cache keys", e);
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Build cache key from userId, eventId, and groupId
     * Format: rule:gw:abt:{userId}:{eventId}:{groupId}
     *
     * @param userId  user ID
     * @param eventId event ID
     * @param groupId rule group ID
     * @return cache key
     */
    private String buildCacheKey(Long userId, Integer eventId, Long groupId) {
        return CACHE_KEY_PREFIX + userId + ":" + eventId + ":" + groupId;
    }
}

