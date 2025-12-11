package lab.zhang.rule.rule_engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Log executor configuration
 * 
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class LogExecutorConfig {

    @Value("${rule.log.async.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${rule.log.async.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * Async executor for eval log writing
     * Only created when rule.log.async.enabled=true (default)
     *
     * @return ThreadPoolTaskExecutor instance for async execution
     */
    @Bean(name = "logExecutor")
    @ConditionalOnProperty(name = "rule.log.async.enabled", havingValue = "true", matchIfMissing = true)
    public Executor logAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("eval-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("[init] log async executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                threadPoolSize, threadPoolSize * 2, queueCapacity);
        return executor;
    }

    /**
     * Synchronous executor for eval log writing
     * Only created when rule.log.async.enabled=false
     * This executor executes tasks synchronously in the calling thread
     *
     * @return Synchronous Executor instance
     */
    @Bean(name = "logExecutor")
    @ConditionalOnProperty(name = "rule.log.async.enabled", havingValue = "false")
    public Executor logSyncExecutor() {
        log.info("[init] log sync executor initialized (async disabled)");
        // Return a synchronous executor that executes tasks in the calling thread
        return Runnable::run;
    }
}

