package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.util.LoggingContextUtil;
import lab.zhang.rule.rule_engine.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * Logging interceptor for trace ID management
 * Sets traceId in MDC for all requests and clears it after request completion
 *
 * @author Rongjin Zhang
 */
@Component
@Slf4j
public class TraceLoggingFilter implements HandlerInterceptor {

    // traceId is either specified in request parameter "traceId"
    private static final String TRACE_ID_PARAM = "traceId";
    // or by default in header "X-Request-Id"
    private static final String TRACE_ID_HEADER = "X-Request-Id";


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // extract traceId from parameter first, then from header
        String traceIdStr = request.getParameter(TRACE_ID_PARAM);
        if (StrUtil.isBlank(traceIdStr)) {
            traceIdStr = request.getHeader(TRACE_ID_HEADER);
        }

        if (StrUtil.isBlank(traceIdStr)) {
            LoggingContextUtil.setTraceId(BigInteger.ZERO);
            return true;
        }

        try {
            BigInteger traceId = new BigInteger(traceIdStr.trim());
            LoggingContextUtil.setTraceId(traceId);
        } catch (NumberFormatException e) {
            LoggingContextUtil.setTraceId(BigInteger.ZERO);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Always clear the MDC after request processing
        LoggingContextUtil.clear();
    }
}