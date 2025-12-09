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
import lab.zhang.rule.rule_engine.model.EvalResult;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.service.KafkaService;
import lab.zhang.rule.rule_engine.cache.EvalCacheService;
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

    @Autowired
    private EvalCacheService evalCacheService;


    @Override
    public EvalResult eval(EvalDTO dto) {
        log.info("Eval request received: userId={}, eventId={}, arguments={}, traceId={}",
                dto.getUserId(), dto.getEventId(), dto.getArguments(), dto.getTraceId());

        ExecutionTrace trace = new ExecutionTrace(dto);

        RuleExecutionContext context = buildContext(dto);

        extendArguments(dto, context);
        log.info("Extended arguments: {}", context.getArguments());

        TypedValue result = ruleExecutionEngine.execute(dto.getEventId(), context, trace);

        saveLog(dto, trace);

        sendMessage(dto, result);

        return new EvalResult(result, trace);
    }

    private static RuleExecutionContext buildContext(EvalDTO dto) {
        Integer eventId = dto.getEventId() != null ? dto.getEventId().intValue() : null;
        return new RuleExecutionContext(
                dto.getUserId(),
                eventId,
                dto.getTraceId(),
                dto.getArguments()
        );
    }

    private void extendArguments(EvalDTO dto, RuleExecutionContext context) {
        // Calculate userHash using murmur-hash and store in arguments
        if (dto.getUserId() != null) {
            // hash of userId
            String userHash = HashUtil.murmurHash3(dto.getUserId().toString());
            context.putArgument(EvalArgumentConst.ARG_USER_HASH, new TypedValue(userHash, ValueTypeEnum.STRING));
            // random int of userId
            int userHashInt = HashUtil.hashToIntRange(userHash, 1, 100);
            context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(userHashInt, ValueTypeEnum.INTEGER));
        }
    }

    private void saveLog(EvalDTO dto, ExecutionTrace trace) {
        // Write ExecutionTrace to database
        try {
            EvalLogEntity evalLog = new EvalLogEntity();
            evalLog.setTraceId(dto.getTraceId());
            evalLog.setEventId(dto.getEventId());
            evalLog.setUserId(dto.getUserId());

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
                    dto.getTraceId(), dto.getEventId(), dto.getUserId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ExecutionTrace to JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Database write failure does not affect main flow, only log
            log.error("Failed to save eval log to database: {}", e.getMessage(), e);
        }
    }

    private void sendMessage(EvalDTO dto, TypedValue result) {
        // Send EvalDTO and result to Kafka
        try {
            kafkaService.sendEvalResult(dto, result);
        } catch (Exception e) {
            // Message sending failure does not affect main flow, only log
            log.error("Failed to send message to Kafka: {}", e.getMessage(), e);
        }
    }
}

