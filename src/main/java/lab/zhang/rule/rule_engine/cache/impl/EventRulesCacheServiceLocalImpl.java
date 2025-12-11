package lab.zhang.rule.rule_engine.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lab.zhang.rule.rule_engine.cache.EventRulesCacheService;
import lab.zhang.rule.rule_engine.model.ExecutionArrangement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Event rules cache service implementation using Caffeine
 * Cache key format: rule:arrangement:{eventId}
 * Cache value: List<ExecutionArrangement>
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rule.event-rules.cache.enabled", havingValue = "true", matchIfMissing = true)
public class EventRulesCacheServiceLocalImpl implements EventRulesCacheService {

    @Value("${rule.event-rules.cache.capacity:1000}")
    private int cacheCapacity;

    @Value("${rule.event-rules.cache.ttl:300}")
    private int cacheTtl;

    private Cache<Integer, List<ExecutionArrangement>> cache;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheCapacity)
                .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
                .recordStats()
                .build();
        log.info("[init] execution arrangement cache initialized: capacity={}, ttl={}s",
                cacheCapacity, cacheTtl);
    }

    @Override
    public List<ExecutionArrangement> get(Integer eventId) {
        if (eventId == null) {
            return null;
        }

        List<ExecutionArrangement> cached = cache.getIfPresent(eventId);
        if (log.isDebugEnabled()) {
            log.debug("[cache_x] get cache for eventId={}, hit={}", eventId, cached != null);
        }
        return cached;
    }

    @Override
    public void put(Integer eventId, List<ExecutionArrangement> arrangements) {
        if (eventId == null) {
            return;
        }

        cache.put(eventId, arrangements);
        if (log.isDebugEnabled()) {
            log.debug("[cache_x] put cache for eventId={}, size={}", eventId,
                    arrangements != null ? arrangements.size() : 0);
        }
    }

    @Override
    public void invalidate(Integer eventId) {
        if (eventId == null) {
            return;
        }

        cache.invalidate(eventId);
        if (log.isDebugEnabled()) {
            log.debug("[cache_x] invalidated cache for eventId={}", eventId);
        }
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
        log.info("[cache_x] invalidated all execution arrangement cache");
    }
}

