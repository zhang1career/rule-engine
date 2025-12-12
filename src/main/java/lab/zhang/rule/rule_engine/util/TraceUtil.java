package lab.zhang.rule.rule_engine.util;

import java.math.BigInteger;

public class TraceUtil {
    public static BigInteger getTraceId(String requestIdStr, String traceIdStr) {
        if (StrUtil.isBlank(traceIdStr)) {
            traceIdStr = requestIdStr;
        }
        BigInteger traceId = BigInteger.ZERO;
        if (!StrUtil.isBlank(traceIdStr)) {
            traceId = new BigInteger(traceIdStr.trim());
        }
        return traceId;
    }
}
