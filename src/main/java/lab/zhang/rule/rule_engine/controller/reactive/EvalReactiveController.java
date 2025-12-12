package lab.zhang.rule.rule_engine.controller.reactive;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.EvalResultDTO;
import lab.zhang.rule.rule_engine.pojo.qo.EvalRequestQO;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.struct_mapper.EvalStructMapper;
import lab.zhang.rule.rule_engine.util.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;

/**
 * Reactive controller for rule evaluation
 * Provides non-blocking HTTP interface using reactive programming
 * Works in Servlet environment with reactive support
 *
 * @author Rongjin Zhang
 */
@Slf4j
@RestController
@RequestMapping("/api/eval")
public class EvalReactiveController {

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
     * Reactive rule evaluation interface
     * Route: POST /api/eval/reactive
     *
     * @param requestIdStr request ID from header
     * @param traceIdStr   trace ID from query parameter
     * @param qo           evaluation request
     * @return reactive response
     */
    @PostMapping("/reactive")
    public Mono<ResponseEntity<ApiResponseDTO<EvalResultDTO>>> evalReactive(
            @RequestHeader(value = "X-Request-ID", required = false) String requestIdStr,
            @RequestParam(value = "traceId", required = false) String traceIdStr,
            @RequestBody EvalRequestQO qo) {

        if (log.isDebugEnabled()) {
            log.debug("[eval] reactive eval param: userId={}, eventId={}", qo.getUserId(), qo.getEventId());
        }

        return Mono.fromCallable(() -> {
                    EvalRequest evalRequest = evalStructMapper.qoToModel(qo);
                    BigInteger traceId = TraceUtil.getTraceId(requestIdStr, traceIdStr);
                    EvalResult evalResult = evalService.eval(evalRequest, traceId);
                    EvalResultDTO dto = new EvalResultDTO(evalResult.getValue(), evalResult.getBriefSteps());
                    return ResponseEntity.ok(ApiResponseDTO.success(dto));
                })
                .subscribeOn(blockingScheduler)
                .onErrorResume(e -> {
                    log.error("[eval] reactive eval failed: error={}", e.getMessage(), e);

                    // Determine appropriate status code based on exception type
                    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                    if (e instanceof IllegalArgumentException) {
                        String message = e.getMessage();
                        if (message != null && (message.toLowerCase().contains("not found") ||
                                message.toLowerCase().contains("does not exist"))) {
                            status = HttpStatus.NOT_FOUND;
                        } else {
                            status = HttpStatus.BAD_REQUEST;
                        }
                    }

                    // Return error response with appropriate status
                    return Mono.just(ResponseEntity.ok(
                            ApiResponseDTO.error(status.value(), e.getMessage())));
                });
    }
}
