package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO;
import lab.zhang.rule.rule_engine.pojo.dto.EvalResultDTO;
import lab.zhang.rule.rule_engine.pojo.qo.EvalQO;
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
            @RequestBody EvalQO qo) {
        log.info("Received eval request: userId={}, eventId={}, traceId={}",
                qo.getUserId(), qo.getEventId(), qo.getTraceId());

        EvalDTO dto = evalStructMapper.qoToDto(qo);
        ExecutionTrace trace = new ExecutionTrace();
        TypedValue result = evalService.eval(dto, trace);

        return ResponseEntity.ok(new EvalResultDTO(result, trace));
    }
}

