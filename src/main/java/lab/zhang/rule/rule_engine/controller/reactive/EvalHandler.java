package lab.zhang.rule.rule_engine.controller.reactive;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.EvalResultDTO;
import lab.zhang.rule.rule_engine.pojo.qo.EvalRequestQO;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.struct_mapper.EvalStructMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;

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
                        log.debug("[eval] reactive eval param: userId={}, eventId={}", qo.getUserId(), qo.getEventId());
                    }
                })
                .flatMap(qo -> {
                    // Execute blocking service call on dedicated scheduler
                    return Mono.fromCallable(() -> {
                                EvalRequest evalRequest = evalStructMapper.qoToModel(qo);
                                return evalService.eval(evalRequest, BigInteger.ZERO);
                            })
                            .subscribeOn(blockingScheduler)
                            .flatMap(evalResult -> {
                                EvalResultDTO dto = new EvalResultDTO(
                                        evalResult.getValue(),
                                        evalResult.getBriefSteps()
                                );
                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(ApiResponseDTO.success(dto));
                            });
                })
                .onErrorResume(e -> {
                    log.error("[eval] reactive eval failed: error={}", e.getMessage(), e);

                    String message = e.getMessage();
                    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                    // Determine appropriate status code based on exception type
                    if (e instanceof IllegalArgumentException) {
                        if (message != null && (message.toLowerCase().contains("not found") || message.toLowerCase().contains("does not exist"))) {
                            status = HttpStatus.NOT_FOUND;
                        } else {
                            status = HttpStatus.BAD_REQUEST;
                        }
                    }
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponseDTO.error(status.value(), message != null ? message : "Unknown error"));
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
