package lab.zhang.rule.rule_engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * WebFlux configuration
 * Note: When both spring-boot-starter-web and spring-boot-starter-webflux are present,
 * RouterFunction beans are automatically detected and registered.
 * No explicit @EnableWebFlux is needed in this hybrid setup.
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
public class WebFluxConfig {

    public WebFluxConfig() {
        log.info("[init] webflux RouterFunction support enabled for reactive endpoints");
    }
}
