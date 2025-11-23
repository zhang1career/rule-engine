package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine;
import lab.zhang.rule.rule_engine.executor.impl.ApiQueryRuleExecutor;
import lab.zhang.rule.rule_engine.executor.impl.ExpressionRuleExecutor;
import lab.zhang.rule.rule_engine.executor.impl.ScriptRuleExecutor;
import lab.zhang.rule.rule_engine.executor.impl.SqlQueryRuleExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * Rule engine configuration class
 * 
 * @author rule-engine
 */
@Slf4j
@Configuration
public class RuleEngineConfig {
    
    @Autowired
    private RuleExecutionEngine ruleExecutionEngine;
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    /**
     * Register all rule executors
     */
    @PostConstruct
    public void registerExecutors() {
        // Register expression executor
        ruleExecutionEngine.registerExecutor(new ExpressionRuleExecutor());
        
        // Register script executor
        ruleExecutionEngine.registerExecutor(new ScriptRuleExecutor());
        
        // Register API query executor
        ruleExecutionEngine.registerExecutor(new ApiQueryRuleExecutor());
        
        // Register SQL query executor (if data source is configured)
        if (dataSource != null) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            ruleExecutionEngine.registerExecutor(new SqlQueryRuleExecutor(jdbcTemplate));
        } else {
            log.warn("DataSource not configured, SqlQueryRuleExecutor will not be available");
        }
        
        log.info("All rule executors registered");
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

