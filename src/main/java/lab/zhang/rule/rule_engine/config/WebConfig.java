package lab.zhang.rule.rule_engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 *
 * @author Rongjin Zhang
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TraceLoggingFilter traceLoggingFilter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceLoggingFilter)
                .addPathPatterns("/api/**")  // Apply to all API endpoints
                .excludePathPatterns("/api/dicts"); // Exclude dict endpoint if needed
    }
}