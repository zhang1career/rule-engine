package lab.zhang.rule.rule_engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Log async executor configuration
 * 
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class LogAsyncExecutorConfig {

    @Value("${rule.log.async.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${rule.log.async.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * Thread pool executor for async eval log writing
     */
    @Bean(name = "logAsnycExecutor")
    public Executor logAsnycExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("eval-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("[init] log executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                threadPoolSize, threadPoolSize * 2, queueCapacity);
        return executor;
    }
}

