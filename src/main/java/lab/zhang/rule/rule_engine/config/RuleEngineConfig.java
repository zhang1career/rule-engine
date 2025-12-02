package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.executor.impl.SqlQueryRuleExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule engine configuration class
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
public class RuleEngineConfig {

    @Autowired
    private RuleExecutionEngine ruleExecutionEngine;

    @Autowired(required = false)
    private List<RuleExecutor> ruleExecutors;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ConfigurableApplicationContext configurableApplicationContext;

    /**
     * Register all rule executors
     */
    @PostConstruct
    public void registerExecutors() {
        // Check if SqlQueryRuleExecutor needs to be created manually
        try {
            DataSource dataSource = applicationContext.getBean(DataSource.class);
            // Check if SqlQueryRuleExecutor already exists
            try {
                applicationContext.getBean(SqlQueryRuleExecutor.class);
                log.debug("SqlQueryRuleExecutor already exists as a bean");
            } catch (Exception e) {
                // SqlQueryRuleExecutor doesn't exist, create it manually and register as singleton bean
                log.warn("SqlQueryRuleExecutor bean not found, creating it manually");
                try {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                    SqlQueryRuleExecutor sqlExecutor = new SqlQueryRuleExecutor(jdbcTemplate);
                    // Register as singleton bean
                    ConfigurableListableBeanFactory beanFactory = configurableApplicationContext.getBeanFactory();
                    beanFactory.registerSingleton("sqlQueryRuleExecutor", sqlExecutor);
                    log.info("SqlQueryRuleExecutor manually created and registered as singleton bean");
                } catch (Exception ex) {
                    log.error("Failed to create SqlQueryRuleExecutor manually: {}", ex.getMessage(), ex);
                }
            }
        } catch (Exception e) {
            log.error("DataSource not available: {}", e.getMessage());
        }

        // Re-inject ruleExecutors to include the newly created SqlQueryRuleExecutor
        // Note: This won't work because @Autowired is already done, we need to get it from context
        try {
            ruleExecutors = new ArrayList<>(applicationContext.getBeansOfType(RuleExecutor.class).values());
            log.info("Refreshed ruleExecutors list, now contains {} executors", ruleExecutors.size());
        } catch (Exception e) {
            log.warn("Failed to refresh ruleExecutors list: {}", e.getMessage());
        }

        if (ruleExecutors == null || ruleExecutors.isEmpty()) {
            log.warn("No rule executors found, please ensure RuleExecutor implementations are annotated with @Component or defined as @Bean");
            return;
        }

        // Log all executors for debugging
        log.info("Found {} rule executors:", ruleExecutors.size());
        for (RuleExecutor executor : ruleExecutors) {
            log.info("  - {}: {}", executor.getClass().getSimpleName(), executor.getSupportedRuleType());
        }

        // Register all rule executors
        for (RuleExecutor executor : ruleExecutors) {
            ruleExecutionEngine.registerExecutor(executor);
        }

        log.info("All rule executors registered: count={}", ruleExecutors.size());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Create SqlQueryRuleExecutor bean if DataSource is available
     * Note: This method will only be called if DataSource bean exists
     * The @ConditionalOnBean check happens during configuration class processing
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    public SqlQueryRuleExecutor sqlQueryRuleExecutor(DataSource dataSource) {
        log.info("sqlQueryRuleExecutor method called, DataSource type: {}", dataSource != null ? dataSource.getClass().getName() : "null");

        // Create JdbcTemplate from DataSource
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("Creating SqlQueryRuleExecutor bean with JdbcTemplate from DataSource: {}", dataSource.getClass().getName());
        SqlQueryRuleExecutor executor = new SqlQueryRuleExecutor(jdbcTemplate);
        log.info("SqlQueryRuleExecutor bean created successfully");
        return executor;
    }
}

