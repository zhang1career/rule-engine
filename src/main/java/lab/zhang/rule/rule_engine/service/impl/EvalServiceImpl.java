package lab.zhang.rule.rule_engine.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine;
import lab.zhang.rule.rule_engine.entity.EvalLogEntity;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.service.EvalLogService;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.service.KafkaService;
import lab.zhang.rule.rule_engine.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Rule evaluation service implementation
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class EvalServiceImpl implements EvalService {

    @Autowired
    private RuleExecutionEngine ruleExecutionEngine;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvalLogService evalLogService;

    @Autowired
    private KafkaService kafkaService;


    @Override
    public EvalResult eval(EvalRequest request) {
        ExecutionTrace trace = new ExecutionTrace(request);
        RuleExecutionContext context = buildContext(request);

        extendArguments(request, context);
        log.info("[eval] extended arguments={}", context.getArguments());

        TypedValue result = ruleExecutionEngine.execute(request.getEventId(), context, trace);

        saveLogAsync(request, trace);

        sendMessage(request, result);

        return new EvalResult(result, trace);
    }

    private static RuleExecutionContext buildContext(EvalRequest request) {
        Integer eventId = request.getEventId() != null ? request.getEventId().intValue() : null;
        return new RuleExecutionContext(
                request.getUserId(),
                eventId,
                request.getTraceId(),
                request.getArguments()
        );
    }

    private void extendArguments(EvalRequest request, RuleExecutionContext context) {
        // Calculate userHash using murmur-hash and store in arguments
        if (request.getUserId() != null) {
            // hash of userId
            String userHash = HashUtil.murmurHash3(request.getUserId().toString());
            context.putArgument(EvalArgumentConst.ARG_USER_HASH, new TypedValue(userHash, ValueTypeEnum.STRING));
            // random int of userId
            int userHashInt = HashUtil.hashToIntRange(userHash, 1, 100);
            context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(userHashInt, ValueTypeEnum.INTEGER));
        }
    }

    /**
     * Save eval log asynchronously using batch service
     * This method is called asynchronously to avoid blocking the main execution flow
     *
     * @param request eval request
     * @param trace execution trace
     */
    @Async("logAsyncExecutor")
    public void saveLogAsync(EvalRequest request, ExecutionTrace trace) {
        try {
            EvalLogEntity evalLog = buildEvalLogEntity(request, trace);
            evalLogService.addLog(evalLog);
            if (log.isDebugEnabled()) {
                log.debug("Eval log added to batch: traceId={}, eventId={}, userId={}",
                        request.getTraceId(), request.getEventId(), request.getUserId());
            }
        } catch (JsonProcessingException e) {
            log.error("[eval] failed to serialize ExecutionTrace to JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Database write failure does not affect main flow, only log
            log.error("[eval] failed to add eval log to batch: {}", e.getMessage(), e);
        }
    }

    /**
     * Build EvalLogEntity from request and trace
     *
     * @param request eval request
     * @param trace execution trace
     * @return EvalLogEntity
     * @throws JsonProcessingException if JSON serialization fails
     */
    private EvalLogEntity buildEvalLogEntity(EvalRequest request, ExecutionTrace trace) throws JsonProcessingException {
        EvalLogEntity evalLog = new EvalLogEntity();
        evalLog.setTraceId(request.getTraceId());
        evalLog.setEventId(request.getEventId());
        evalLog.setUserId(request.getUserId());

        // Serialize arguments to JSON string
        if (trace.getArguments() != null) {
            String argumentsJson = objectMapper.writeValueAsString(trace.getArguments());
            evalLog.setArguments(argumentsJson);
        }

        // Serialize steps to JSON string
        if (trace.getSteps() != null) {
            String stepsJson = objectMapper.writeValueAsString(trace.getSteps());
            evalLog.setSteps(stepsJson);
        }

        evalLog.setTimeOnCreate();
        return evalLog;
    }

    private void sendMessage(EvalRequest request, TypedValue result) {
        // Send EvalDTO and result to Kafka
        try {
            kafkaService.sendEvalResult(request, result);
        } catch (Exception e) {
            // Message sending failure does not affect main flow, only log
            log.error("Failed to send message to Kafka: {}", e.getMessage(), e);
        }
    }
}

