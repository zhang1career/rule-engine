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
    private static final Map<EnvironmentEnum, Set<RuleStatusEnum>> EVAL_RULE_STATUS_MAP = new HashMap<EnvironmentEnum, Set<RuleStatusEnum>>() {{
        put(EnvironmentEnum.TEST, new HashSet<>(Arrays.asList(RuleStatusEnum.TEST)));
        put(EnvironmentEnum.GRAY, new HashSet<>(Arrays.asList(RuleStatusEnum.GRAY, RuleStatusEnum.ONLINE)));
        put(EnvironmentEnum.PRODUCTION, new HashSet<>(Arrays.asList(RuleStatusEnum.ONLINE)));
    }};

    /**
     * State transition configuration map
     * Maps each status to the set of allowed target statuses for transition
     */
    public static final Map<RuleStatusEnum, Set<RuleStatusEnum>> RULE_STATUS_CHANGE_MAP = new HashMap<RuleStatusEnum, Set<RuleStatusEnum>>() {{
        put(RuleStatusEnum.OFFLINE, new HashSet<>(Arrays.asList(RuleStatusEnum.TEST)));
        put(RuleStatusEnum.TEST, new HashSet<>(Arrays.asList(RuleStatusEnum.OFFLINE, RuleStatusEnum.GRAY)));
        put(RuleStatusEnum.GRAY, new HashSet<>(Arrays.asList(RuleStatusEnum.OFFLINE, RuleStatusEnum.ONLINE)));
        put(RuleStatusEnum.ONLINE, new HashSet<>(Arrays.asList(RuleStatusEnum.OFFLINE)));
    }};

    /**
     * Get allowed rule statuses based on current environment
     *
     * @return set of allowed rule statuses for current environment
     */
    public Set<RuleStatusEnum> getEvalAvailableRuleStatuses() {
        EnvironmentEnum env = envUtil.getEnvEnum();
        Set<RuleStatusEnum> allowedStatuses = EVAL_RULE_STATUS_MAP.get(env);
        if (allowedStatuses == null) {
            log.warn("No rule statuses configured for environment: {}, use default value instead", env);
            return new HashSet<>(Collections.singletonList(RuleStatusEnum.OFFLINE));
        }
        return allowedStatuses;
    }
}

