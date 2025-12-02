package lab.zhang.rule.rule_engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Rule Engine Application
 * 
 * @author Rongjin Zhang
 */
@SpringBootApplication
@MapperScan("lab.zhang.rule.rule_engine.mapper")
public class RuleEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleEngineApplication.class, args);
    }
}

