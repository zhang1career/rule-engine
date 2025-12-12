package lab.zhang.rule.rule_engine.service;

/**
 * Cache warmup service interface
 * Pre-loads frequently accessed data into cache on application startup
 *
 * @author Rongjin Zhang
 */
public interface CacheWarmupService {

    /**
     * Warm up caches by preloading frequently accessed data
     * This method is called on application startup
     */
    void warmup();
}

