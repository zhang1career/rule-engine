package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.service.CacheWarmupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * Cache warmup configuration
 * Triggers cache warmup when application is ready
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
public class CacheWarmupConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired(required = false)
    private CacheWarmupService cacheWarmupService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (cacheWarmupService != null) {
            log.info("[warmup] application ready, starting cache warmup...");
            // Execute warmup asynchronously to avoid blocking application startup
            new Thread(() -> {
                try {
                    cacheWarmupService.warmup();
                } catch (Exception e) {
                    log.error("[warmup] cache warmup thread failed: {}", e.getMessage(), e);
                }
            }, "cache-warmup-thread").start();
        } else {
            log.info("[warmup] cache warmup service not available, skipping warmup");
        }
    }
}

