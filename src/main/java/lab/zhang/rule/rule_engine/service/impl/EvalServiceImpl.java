package lab.zhang.rule.rule_engine.service.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.service.MessageQueueService;
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
    private MessageQueueService messageQueueService;

    @Override
    public TypedValue eval(EvalDTO dto, ExecutionTrace trace) {
        log.info("Eval request received: userId={}, eventId={}, arguments={}, traceId={}",
                dto.getUserId(), dto.getEventId(), dto.getArguments(), dto.getTraceId());

        // Build execution context
        RuleExecutionContext context = new RuleExecutionContext(
                dto.getUserId(),
                dto.getEventId(),
                dto.getTraceId(),
                dto.getArguments()
        );
        // Calculate userHash using murmur-hash and store in arguments
        if (dto.getUserId() != null) {
            // hash of userId
            String userHash = HashUtil.murmurHash3(dto.getUserId().toString());
            context.putArgument(EvalArgumentConst.ARG_USER_HASH, new TypedValue(userHash, ValueTypeEnum.STRING));
            // random int of userId
            int userHashInt = HashUtil.hashToIntRange(userHash, 1, 100);
            context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(userHashInt, ValueTypeEnum.INTEGER));
            if (log.isDebugEnabled()) {
                log.debug("User hash calculated and stored: userId={}, userHash={}, userHashInt={}",
                        context.getUserId(), userHash, userHashInt);
            }
        }

        // Execute rule evaluation with trace
        TypedValue result = ruleExecutionEngine.execute(dto.getEventId(), context, trace);

        // Asynchronously send message to RabbitMQ
        try {
            messageQueueService.sendEvalResult(dto, result);
        } catch (Exception e) {
            // Message sending failure does not affect main flow, only log
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
        }

        return result;
    }
}

