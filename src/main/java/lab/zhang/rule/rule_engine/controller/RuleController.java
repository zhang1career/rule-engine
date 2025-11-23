package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.dto.EvalRequest;
import lab.zhang.rule.rule_engine.dto.EvalResponse;
import lab.zhang.rule.rule_engine.service.EvalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Rule evaluation controller
 * Provides HTTP synchronous blocking interface
 * 
 * @author rule-engine
 */
@Slf4j
@RestController
@RequestMapping("/rule")
public class RuleController {
    
    @Autowired
    private EvalService evalService;
    
    /**
     * Rule evaluation interface (HTTP synchronous blocking)
     * 
     * @param request evaluation request
     * @return evaluation result
     */
    @PostMapping("/eval")
    public ResponseEntity<EvalResponse> eval(@RequestBody EvalRequest request) {
        try {
            log.info("Received eval request: userId={}, eventId={}, traceId={}", 
                    request.getUserId(), request.getEventId(), request.getTraceId());
            
            TypedValue result = evalService.eval(request);
            
            EvalResponse response = new EvalResponse(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Eval request failed: {}", e.getMessage(), e);
            EvalResponse response = new EvalResponse(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}

