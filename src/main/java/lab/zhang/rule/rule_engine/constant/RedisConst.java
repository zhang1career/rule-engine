package lab.zhang.rule.rule_engine.constant;

public class RedisConst {
    /**
     * selected rules in A/B testing
     */
    public static final String SELECTED_RULE_KEY = "rule:gw:abt:";
    public static final long SELECTED_RULE_TTL = 24 * 60 * 60;

    /**
     * rule content
     */
    public static final String RULE_CONTENT_KEY = "rule:exp:";
    public static final long RULE_CONTENT_TTL = 10 * 60;
}
