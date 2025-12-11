package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;
import lab.zhang.rule.rule_engine.pojo.dto.EvalResultDTO;
import lab.zhang.rule.rule_engine.pojo.qo.EvalRequestQO;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.struct_mapper.EvalStructMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<EvalResultDTO> eval(
            @RequestBody EvalRequestQO qo) {
        log.info("[eval] eval param: userId={}, eventId={}, traceId={}", qo.getUserId(), qo.getEventId(), qo.getTraceId());

        EvalRequest request = evalStructMapper.qoToModel(qo);
        EvalResult evalResult = evalService.eval(request);

        return ResponseEntity.ok(new EvalResultDTO(evalResult.getResult(), evalResult.getBriefSteps()));
    }
}

