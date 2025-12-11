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
 * Log executor configuration
 * 
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class LogExecutorConfig {

    @Value("${rule.log.async.enabled:true}")
    private boolean isAsyncEnabled;

    @Value("${rule.log.async.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${rule.log.async.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * Executor for eval log writing
     * When isEnabled=true: returns a thread pool executor for async execution
     * When isEnabled=false: returns a synchronous executor that runs tasks in the calling thread
     *
     * @return Executor instance
     */
    @Bean(name = "logExecutor")
    public Executor logAsyncExecutor() {
        if (!isAsyncEnabled) {
            log.info("[init] log sync executor initialized");
            // Return a synchronous executor that executes tasks in the calling thread
            return Runnable::run;
        }

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
}

