package lab.zhang.rule.rule_engine.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lab.zhang.rule.rule_engine.cache.EvalCacheService;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.constant.CacheConst;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.util.HashUtil;
import lab.zhang.rule.rule_engine.util.ListUtil;
import lab.zhang.rule.rule_engine.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Rule evaluation result cache service implementation using Caffeine
 * Cache key format: rule:eval:{ruleId}:{hashOfArgs}
 * Cache value: TypedValue result
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class EvalCacheServiceLocalImpl implements EvalCacheService {

    @Value("${rule.eval.cache.capacity}")
    private int cacheCapacity;

    @Value("${rule.eval.cache.ttl}")
    private int cacheTtl;

    @Value("${rule.eval.cache.key.length}")
    private int keyLength;

    private Cache<String, TypedValue> cache;


    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheCapacity)
                .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
                .recordStats()
                .build();
        if (log.isDebugEnabled()) {
            log.debug("EvalCacheService initialized with capacity={}, ttl={}, keyLength={}",
                    cacheCapacity, cacheTtl, keyLength);
        }
    }

    @Override
    public TypedValue get(Rule rule, List<String> argValues) {
        if (rule.getId() == null) {
            throw new IllegalArgumentException("Rule ID cannot be null");
        }

        String cacheKey = buildCacheKey(rule.getId().toString(), argValues);
        TypedValue cacheValue = cache.getIfPresent(cacheKey);

        if (log.isDebugEnabled()) {
            log.debug("[eval] get cache for ruleId={}, key={}, hit={}",
                    rule.getId(), cacheKey, cacheValue != null);
        }
        return cacheValue;
    }

    @Override
    public void put(Rule rule, List<String> argValues, TypedValue result) {
        if (rule.getId() == null) {
            throw new IllegalArgumentException("Rule ID cannot be null");
        }
        String cacheKey = buildCacheKey(rule.getId().toString(), argValues);

        if (log.isDebugEnabled()) {
            log.debug("[eval] put cache for ruleId={}, key={}",
                    rule.getId(), cacheKey);
        }
        cache.put(cacheKey, result);
    }

    @Override
    public String buildCacheKey(String keyPrefix, List<String> argValues) {
        if (StrUtil.isBlank(keyPrefix)) {
            throw new IllegalArgumentException("Key prefix cannot be null or blank");
        }
        if (ListUtil.isEmpty(argValues)) {
            throw new IllegalArgumentException("Argument values cannot be null or empty");
        }

        String argStr = StrUtil.implode(argValues, "|");
        String key = HashUtil.blake3Hash(argStr, keyLength);
        return CacheConst.RULE_EVAL_KEY + keyPrefix + ":" + key;
    }

}
