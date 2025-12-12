package lab.zhang.rule.rule_engine.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

/**
 * Rule evaluation controller
 * Provides HTTP synchronous blocking interface
 *
 * @author Rongjin Zhang
 */
@Slf4j
@RestController
@RequestMapping("/api/eval")
public class EvalController {

    @Autowired
    private EvalService evalService;

    @Autowired
    private EvalStructMapper evalStructMapper;

    /**
     * Rule evaluation interface (HTTP synchronous blocking)
     *
     * @param qo evaluation request
     * @return evaluation result with execution trace
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<EvalResultDTO>> eval(
            @RequestHeader(value = "X-Request-ID", required = false) String requestIdStr,
            @RequestParam(value = "traceId", required = false) String traceIdStr,
            @RequestBody EvalRequestQO qo) {
        log.info("[eval] eval param: userId={}, eventId={}", qo.getUserId(), qo.getEventId());

        BigInteger traceId = TraceUtil.getTraceId(requestIdStr, traceIdStr);
        EvalRequest request = evalStructMapper.qoToModel(qo);

        EvalResult evalResult = evalService.eval(request, traceId);
        EvalResultDTO dto = new EvalResultDTO(evalResult.getValue(), evalResult.getBriefSteps());

        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

}

