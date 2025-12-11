package lab.zhang.rule.rule_engine.service.impl;

import lab.zhang.rule.rule_engine.cache.EventRulesCacheService;
import lab.zhang.rule.rule_engine.cache.RuleContentCacheService;
import lab.zhang.rule.rule_engine.entity.RuleContentEntity;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import lab.zhang.rule.rule_engine.mapper.RuleContentMapper;
import lab.zhang.rule.rule_engine.mapper.RuleMapper;
import lab.zhang.rule.rule_engine.model.Event;
import lab.zhang.rule.rule_engine.service.CacheWarmupService;
import lab.zhang.rule.rule_engine.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cache warmup service implementation
 * Preloads frequently accessed execution arrangements and rule contents into cache
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rule.cache.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class CacheWarmupServiceImpl implements CacheWarmupService {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRulesCacheService eventRulesCacheService;

    @Autowired(required = false)
    private RuleContentCacheService ruleContentCacheService;

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private RuleContentMapper ruleContentMapper;

    @Value("${rule.cache.warmup.event-limit:100}")
    private int eventLimit;

    @Value("${rule.cache.warmup.rule-limit:1000}")
    private int ruleLimit;

    @Override
    public void warmup() {
        log.info("[warmup] starting cache warmup...");

        long startTime = System.currentTimeMillis();
        int warmedEventCount = 0;
        int warmedRuleCount = 0;

        try {
            // Warm up event rules cache
            if (eventRulesCacheService != null) {
                warmedEventCount = warmupEventRulesCache();
            }

            // Warm up rule content cache
            if (ruleContentCacheService != null) {
                warmedRuleCount = warmupRuleContentCache();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[warmup] cache warmup completed: events={}, rules={}, duration={}ms",
                    warmedEventCount, warmedRuleCount, duration);
        } catch (Exception e) {
            log.error("[warmup] cache warmup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Warm up event rules cache by preloading execution arrangements for top N events
     *
     * @return number of events warmed up
     */
    private int warmupEventRulesCache() {
        try {
            List<Event> events = eventService.getAllEvents();
            if (events == null || events.isEmpty()) {
                log.info("[warmup] no events found, skipping event rules cache warmup");
                return 0;
            }

            int limit = Math.min(eventLimit, events.size());
            int warmedCount = 0;

            for (int i = 0; i < limit; i++) {
                Event event = events.get(i);
                if (event == null || event.getId() == null) {
                    continue;
                }

                try {
                    // Call getExecutionItems which will load and cache the data
                    eventService.getExecutionItems(event.getId());
                    warmedCount++;
                } catch (Exception e) {
                    log.warn("[warmup] failed to warmup event rules cache for eventId={}: {}",
                            event.getId(), e.getMessage());
                }
            }

            log.info("[warmup] event rules cache warmed up: {}/{} events", warmedCount, limit);
            return warmedCount;
        } catch (Exception e) {
            log.error("[warmup] failed to warmup event rules cache: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Warm up rule content cache by preloading contents for top N rules
     *
     * @return number of rules warmed up
     */
    private int warmupRuleContentCache() {
        try {
            List<RuleEntity> ruleEntities = ruleMapper.selectList(null);
            if (ruleEntities == null || ruleEntities.isEmpty()) {
                log.info("[warmup] no rules found, skipping rule content cache warmup");
                return 0;
            }

            int limit = Math.min(ruleLimit, ruleEntities.size());
            List<Long> ruleIds = ruleEntities.stream()
                    .limit(limit)
                    .map(RuleEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (ruleIds.isEmpty()) {
                return 0;
            }

            // Batch load rule contents
            List<RuleContentEntity> contentEntities = ruleContentMapper.selectBatchIds(ruleIds);
            if (contentEntities == null || contentEntities.isEmpty()) {
                log.info("[warmup] no rule contents found");
                return 0;
            }

            // Build content map and put into cache
            Map<Long, String> contentMap = new HashMap<>();
            for (RuleContentEntity entity : contentEntities) {
                if (entity != null && entity.getId() != null && entity.getContent() != null) {
                    contentMap.put(entity.getId(), entity.getContent());
                }
            }

            if (!contentMap.isEmpty()) {
                ruleContentCacheService.putBatch(contentMap);
                log.info("[warmup] rule content cache warmed up: {} rules", contentMap.size());
                return contentMap.size();
            }

            return 0;
        } catch (Exception e) {
            log.error("[warmup] failed to warmup rule content cache: {}", e.getMessage(), e);
            return 0;
        }
    }
}

