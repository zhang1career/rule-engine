package lab.zhang.rule.rule_engine.controller.reactive;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.pojo.dto.EvalResultDTO;
import lab.zhang.rule.rule_engine.pojo.qo.EvalRequestQO;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.struct_mapper.EvalStructMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive handler for rule evaluation
 * Provides non-blocking HTTP interface using Spring WebFlux
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Component
public class EvalHandler {

    @Autowired
    private EvalService evalService;

    @Autowired
    private EvalStructMapper evalStructMapper;

    /**
     * Scheduler for executing blocking operations
     * Uses bounded elastic scheduler to avoid blocking reactive threads
     */
    private final Scheduler blockingScheduler = Schedulers.boundedElastic();

    /**
     * Handle reactive eval request
     *
     * @param request server request
     * @return reactive response
     */
    public Mono<ServerResponse> eval(ServerRequest request) {
        return request.bodyToMono(EvalRequestQO.class)
                .doOnNext(qo -> {
                    if (log.isDebugEnabled()) {
                        log.debug("[eval] reactive eval param: userId={}, eventId={}, traceId={}",
                                qo.getUserId(), qo.getEventId(), qo.getTraceId());
                    }
                })
                .flatMap(qo -> {
                    // Execute blocking service call on dedicated scheduler
                    return Mono.fromCallable(() -> {
                                EvalRequest evalRequest = evalStructMapper.qoToModel(qo);
                                return evalService.eval(evalRequest);
                            })
                            .subscribeOn(blockingScheduler)
                            .flatMap(evalResult -> {
                                EvalResultDTO dto = new EvalResultDTO(
                                        evalResult.getResult(),
                                        evalResult.getBriefSteps()
                                );
                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(dto);
                            });
                })
                .onErrorResume(e -> {
                    log.error("[eval] reactive eval failed: error={}", e.getMessage(), e);
                    
                    // Determine appropriate status code based on exception type
                    if (e instanceof IllegalArgumentException) {
                        String message = e.getMessage();
                        if (message != null && (message.toLowerCase().contains("not found") || 
                                                message.toLowerCase().contains("does not exist"))) {
                            return ServerResponse.status(org.springframework.http.HttpStatus.NOT_FOUND)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new ErrorResponse(message));
                        }
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new ErrorResponse(message));
                    }
                    
                    // For other exceptions, return 500
                    return ServerResponse.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new ErrorResponse("Internal server error: " + e.getMessage()));
                });
    }

    /**
     * Error response DTO
     */
    @Getter
    @AllArgsConstructor
    private static class ErrorResponse {
        private final String error;
    }
}
