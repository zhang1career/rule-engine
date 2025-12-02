package lab.zhang.rule.rule_engine.util;

public class RatioUtil {
    public static String buildRatioKey(Long groupId, Long ruleId) {
        return groupId + ":" + ruleId;
    }
}
