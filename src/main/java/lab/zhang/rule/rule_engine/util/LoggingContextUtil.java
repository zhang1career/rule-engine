package lab.zhang.rule.rule_engine.util;

import org.slf4j.MDC;

import java.math.BigInteger;

/**
 * Logging context utility class
 * Used to store traceId in MDC for logging throughout the request lifecycle
 *
 * @author Rongjin Zhang
 */
public class LoggingContextUtil {

    /**
     * MDC key for trace ID
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * Set trace ID in MDC
     *
     * @param traceId trace ID, can be null
     */
    public static void setTraceId(BigInteger traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, String.valueOf(traceId));
        } else {
            MDC.put(TRACE_ID_KEY, "");
        }
    }

    /**
     * Get trace ID from MDC
     *
     * @return trace ID as Long, 0L if not set or invalid
     */
    public static BigInteger getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            return BigInteger.ZERO;
        }
        try {
            return new BigInteger(traceId);
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }

    /**
     * Clear trace ID from MDC
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}