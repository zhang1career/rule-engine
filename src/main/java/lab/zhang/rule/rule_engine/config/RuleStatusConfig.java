package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.enums.EnvironmentEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.util.EnvUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rule status configuration class
 * Manages the mapping between environment types and allowed rule statuses
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Component
public class RuleStatusConfig {

    @Autowired
    private EnvUtil envUtil;

    /**
     * Mapping from environment enum to allowed rule statuses
     */
    private static final Map<EnvironmentEnum, Set<RuleStatusEnum>> ENV_RULE_STATUS_MAP = new HashMap<EnvironmentEnum, Set<RuleStatusEnum>>() {{
        put(EnvironmentEnum.TEST, new HashSet<>(Arrays.asList(RuleStatusEnum.TEST)));
        put(EnvironmentEnum.GRAY, new HashSet<>(Arrays.asList(RuleStatusEnum.GRAY, RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL)));
        put(EnvironmentEnum.PRODUCTION, new HashSet<>(Arrays.asList(RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL)));
    }};

    /**
     * Get allowed rule statuses based on current environment
     *
     * @return set of allowed rule statuses for current environment
     */
    public Set<RuleStatusEnum> getAllowedRuleStatuses() {
        EnvironmentEnum env = envUtil.getEnvEnum();
        Set<RuleStatusEnum> allowedStatuses = ENV_RULE_STATUS_MAP.get(env);
        if (allowedStatuses == null) {
            log.warn("No rule statuses configured for environment: {}, use default value instead", env);
            return new HashSet<>(Arrays.asList(RuleStatusEnum.OFFLINE) );
        }
        return allowedStatuses;
    }
}

