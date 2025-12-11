package lab.zhang.rule.rule_engine.controller.reactive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

/**
 * Reactive router configuration for eval endpoints
 * Provides non-blocking HTTP interface using Spring WebFlux
 *
 * @author Rongjin Zhang
 */
@Configuration
public class EvalRouter {

    @Autowired
    private EvalHandler evalHandler;

    /**
     * Define reactive routes for eval endpoints
     * Route: POST /api/eval/reactive
     *
     * @return router function
     */
    @Bean
    public RouterFunction<ServerResponse> evalReactiveRoutes() {
        return RouterFunctions.route(
                POST("/api/eval/reactive").and(contentType(MediaType.APPLICATION_JSON)),
                evalHandler::eval
        );
    }
}
