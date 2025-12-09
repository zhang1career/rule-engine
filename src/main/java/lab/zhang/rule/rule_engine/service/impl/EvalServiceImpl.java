package lab.zhang.rule.rule_engine.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine;
import lab.zhang.rule.rule_engine.entity.EvalLogEntity;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.mapper.EvalLogMapper;
import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.service.KafkaService;
import lab.zhang.rule.rule_engine.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private EvalLogMapper evalLogMapper;

    @Autowired
    private KafkaService kafkaService;


    @Override
    public EvalResult eval(EvalRequest request) {
        log.info("Eval request received: userId={}, eventId={}, arguments={}, traceId={}",
                request.getUserId(), request.getEventId(), request.getArguments(), request.getTraceId());

        ExecutionTrace trace = new ExecutionTrace(request);

        RuleExecutionContext context = buildContext(request);

        extendArguments(request, context);
        log.info("Extended arguments: {}", context.getArguments());

        TypedValue result = ruleExecutionEngine.execute(request.getEventId(), context, trace);

        saveLog(request, trace);

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

    private void saveLog(EvalRequest request, ExecutionTrace trace) {
        // Write ExecutionTrace to database
        try {
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
            evalLogMapper.insert(evalLog);
            log.info("Eval log saved to database: traceId={}, eventId={}, userId={}",
                    request.getTraceId(), request.getEventId(), request.getUserId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ExecutionTrace to JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Database write failure does not affect main flow, only log
            log.error("Failed to save eval log to database: {}", e.getMessage(), e);
        }
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

