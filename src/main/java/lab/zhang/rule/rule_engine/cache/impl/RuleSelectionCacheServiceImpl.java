package lab.zhang.rule.rule_engine.cache.impl;

import lab.zhang.rule.rule_engine.cache.RuleSelectionCacheService;
import lab.zhang.rule.rule_engine.constant.RedisConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Rule selection cache service implementation using Redis
 * Cache key prefix: rule:gw:abt
 * <p>
 * This cache service records which specific rule was selected from each rule group
 * for a user-event combination. Since there can be multiple rule groups, the cache uses
 * Redis Hash structure where groupId is the hash field key and ruleId is the hash field value.
 * <p>
 * Cache key format: rule:gw:abt:{userId}:{eventId}
 * Cache value: Redis Hash where field key is groupId (Long) and field value is ruleId (Long)
 * Cache expiration: 24 hours
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class RuleSelectionCacheServiceImpl implements RuleSelectionCacheService {

    @Qualifier("redisTemplateStringObject")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public Long get(Long userId, Integer eventId, Long groupId) {
        if (userId == null || eventId == null || groupId == null) {
            return null;
        }
        String cacheKey = buildCacheKey(userId, eventId);
        try {
            HashOperations<String, Long, Long> hashOps = redisTemplate.opsForHash();
            Long ruleId = hashOps.get(cacheKey, groupId);
            if (ruleId == null) {
                if (log.isDebugEnabled()) {
                    log.debug("[cache_sel] cache miss: key={}, groupId={}", cacheKey, groupId);
                }
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("[cache_sel] cache hit: key={}, groupId={}, ruleId={}", cacheKey, groupId, ruleId);
            }
            return ruleId;
        } catch (Exception e) {
            log.error("[cache_sel] error getting cache value for key: {}, groupId: {}", cacheKey, groupId, e);
            return null;
        }
    }


    @Override
    public void put(Long userId, Integer eventId, Long groupId, Long ruleId) {
        if (userId == null || eventId == null || groupId == null || ruleId == null) {
            return;
        }
        String cacheKey = buildCacheKey(userId, eventId);
        try {
            redisTemplate.opsForHash().put(cacheKey, groupId, ruleId);
            redisTemplate.expire(cacheKey, RedisConst.SELECTED_RULE_TTL, TimeUnit.SECONDS);
            if (log.isDebugEnabled()) {
                log.debug("[cache_sel] cache put: key={}, groupId={}, ruleId={}, expireSeconds={}", cacheKey, groupId, ruleId, RedisConst.SELECTED_RULE_TTL);
            }
        } catch (Exception e) {
            log.error("[cache_sel] error putting cache value for key: {}, groupId: {}, ruleId: {}", cacheKey, groupId, ruleId, e);
        }
    }


    /**
     * Build cache key from userId and eventId
     * Format: rule:gw:abt:{userId}:{eventId}
     *
     * @param userId  user ID
     * @param eventId event ID
     * @return cache key
     */
    private String buildCacheKey(Long userId, Integer eventId) {
        return RedisConst.SELECTED_RULE_KEY + userId + ":" + eventId;
    }
}

