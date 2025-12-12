package lab.zhang.rule.rule_engine.controller.reactive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

/**
 * Reactive router configuration for eval endpoints
 * Provides non-blocking HTTP interface using Spring WebFlux
 * 
 * Note: In Spring Boot 3.x, when both spring-boot-starter-web and 
 * spring-boot-starter-webflux are present, RouterFunction beans are 
 * automatically registered and should work in Servlet environment.
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rule.eval.reactive.enabled", havingValue = "true", matchIfMissing = true)
public class EvalRouter {

    @Autowired
    private EvalHandler evalHandler;

    /**
     * Test route for WebFlux verification
     * Route: GET /flux/hello
     *
     * @return router function
     */
    @Bean
    public RouterFunction<ServerResponse> testRoutes() {
        log.info("[init] Registering test route: GET /flux/hello");
        return RouterFunctions
                .route(GET("/flux/hello"),
                        request -> ServerResponse.ok().bodyValue("Hello WebFlux"));
    }

    /**
     * Define reactive routes for eval endpoints
     * Route: POST /api/eval/reactive
     *
     * @return router function
     */
    @Bean
    public RouterFunction<ServerResponse> evalReactiveRoutes() {
        log.info("[init] Registering reactive route: POST /api/eval/reactive");
        return RouterFunctions.route(
                POST("/api/eval/reactive").and(contentType(MediaType.APPLICATION_JSON)),
                evalHandler::eval
        );
    }
}
