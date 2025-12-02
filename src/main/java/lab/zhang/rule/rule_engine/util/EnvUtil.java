package lab.zhang.rule.rule_engine.util;

import lab.zhang.rule.rule_engine.enums.EnvironmentEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EnvUtil {
    /**
     * Current environment (read from configuration, default is test environment)
     */
    @Value("${rule.engine.environment:TEST}")
    private String environment;


    public EnvironmentEnum getEnvEnum() {
        EnvironmentEnum envEnum;
        try {
            envEnum = EnvironmentEnum.valueOf(environment.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid environment value: {}, use default value instead", environment);
            envEnum = EnvironmentEnum.UNDEFINED;
        }
        return envEnum;
    }
}
