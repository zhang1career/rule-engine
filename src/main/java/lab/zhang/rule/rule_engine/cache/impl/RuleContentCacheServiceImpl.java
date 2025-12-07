package lab.zhang.rule.rule_engine.cache.impl;

import lab.zhang.rule.rule_engine.cache.RuleContentCacheService;
import lab.zhang.rule.rule_engine.constant.RedisConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Rule content cache service implementation using Redis
 * Cache key prefix: rule:content
 * <p>
 * This cache service records rule content for faster access.
 * Since rule content can be large and changes infrequently, caching improves performance.
 * <p>
 * Cache key format: rule:content:{ruleId}
 * Cache value: rule content (String)
 * Cache expiration: 24 hours
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class RuleContentCacheServiceImpl implements RuleContentCacheService {

    @Qualifier("redisTemplateStringString")
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public Map<Long, String> getBatch(Set<Long> ruleIdSet) {
        if (ruleIdSet == null || ruleIdSet.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build cache keys
        Map<String, Long> cacheKeyRuleIdMap = new HashMap<>();
        for (Long ruleId : ruleIdSet) {
            if (ruleId == null) {
                continue;
            }
            String cacheKey = buildCacheKey(ruleId);
            cacheKeyRuleIdMap.put(cacheKey, ruleId);
        }
        if (cacheKeyRuleIdMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> cacheKeyList = new ArrayList<>(cacheKeyRuleIdMap.keySet());

        try {
            ValueOperations<String, String> strOps = redisTemplate.opsForValue();
            List<String> cacheValueList = strOps.multiGet(cacheKeyList);
            if (cacheValueList == null) {
                return Collections.emptyMap();
            }

            Map<Long, String> result = new HashMap<>();
            for (int i = 0; i < cacheKeyList.size(); i++) {
                String cacheValue = cacheValueList.get(i);
                if (cacheValue == null) {
                    continue;
                }
                String cacheKey = cacheKeyList.get(i);
                Long ruleId = cacheKeyRuleIdMap.get(cacheKey);
                result.put(ruleId, cacheValue);
                if (log.isDebugEnabled()) {
                    log.debug("[cache_exp] batch cache hit: key={}, content length={}", cacheKey, cacheValue.length());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("[cache_exp] batch cache operation: requested {} keys, found {} in cache", cacheKeyList.size(), result.size());
            }
            return result;
        } catch (Exception e) {
            log.error("[cache_exp] error getting batch cache values for keys: {}", cacheKeyList, e);
            return Collections.emptyMap();
        }
    }


    @Override
    public void put(@NotNull Long ruleId, @NotEmpty String content) {
        String cacheKey = buildCacheKey(ruleId);
        try {
            redisTemplate.opsForValue().set(cacheKey, content, RedisConst.RULE_CONTENT_TTL, TimeUnit.SECONDS);
            if (log.isDebugEnabled()) {
                log.debug("[cache_exp] cache put: key={}, content length={}, expireSeconds={}", cacheKey, content.length(), RedisConst.RULE_CONTENT_TTL);
            }
        } catch (Exception e) {
            log.error("[cache_exp] error putting cache value for key: {}, content length: {}", cacheKey, content.length(), e);
        }
    }

    @Override
    public void putBatch(@NotEmpty Map<Long, String> ruleContentMap) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<Long, String> entry : ruleContentMap.entrySet()) {
                    Long ruleId = entry.getKey();
                    String content = entry.getValue();
                    if (ruleId == null || content == null || content.isEmpty()) {
                        if (log.isDebugEnabled()) {
                            log.debug("[cache_exp] batch cache put skipped: invalid entry for ruleId={}, content length={}", ruleId, content != null ? content.length() : 0);
                        }
                        continue;
                    }
                    String cacheKey = buildCacheKey(ruleId);
                    connection.set(cacheKey.getBytes(), content.getBytes());
                    connection.expire(cacheKey.getBytes(), RedisConst.RULE_CONTENT_TTL);
                }
                return null;
            });

            if (log.isDebugEnabled()) {
                log.debug("[cache_exp] batch cache put: {} entries, expireSeconds={}", ruleContentMap.size(), RedisConst.RULE_CONTENT_TTL);
            }
        } catch (Exception e) {
            log.error("[cache_exp] error putting batch cache values for {} entries", ruleContentMap.size(), e);
        }
    }


    /**
     * Build cache key from ruleId
     * Format: rule:content:{ruleId}
     *
     * @param ruleId rule ID
     * @return cache key
     */
    private String buildCacheKey(Long ruleId) {
        return RedisConst.RULE_CONTENT_KEY + ruleId;
    }
}
