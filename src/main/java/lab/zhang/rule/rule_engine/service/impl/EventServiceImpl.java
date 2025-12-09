package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lab.zhang.rule.rule_engine.config.RuleStatusConfig;
import lab.zhang.rule.rule_engine.constant.CommonConst;
import lab.zhang.rule.rule_engine.entity.EventEntity;
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.mapper.EventMapper;
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper;
import lab.zhang.rule.rule_engine.mapper.RuleMapper;
import lab.zhang.rule.rule_engine.model.Event;
import lab.zhang.rule.rule_engine.model.ExecutionArrangement;
import lab.zhang.rule.rule_engine.service.EventService;
import lab.zhang.rule.rule_engine.struct_mapper.EventStructMapper;
import lab.zhang.rule.rule_engine.struct_mapper.ExecutionArrangementStructMapper;
import lab.zhang.rule.rule_engine.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Event service implementation
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private ExecutionArrangementMapper executionArrangementMapper;

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private EventStructMapper eventStructMapper;

    @Autowired
    private ExecutionArrangementStructMapper executionArrangementStructMapper;

    @Autowired
    private RuleStatusConfig ruleStatusConfig;

    @Override
    public List<Event> getAllEvents() {
        List<EventEntity> eventEntities = eventMapper.selectList(null);
        if (eventEntities == null || eventEntities.isEmpty()) {
            return Collections.emptyList();
        }
        return eventEntities.stream()
                .map(eventStructMapper::entityToModel)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Event getEventById(Integer eventId) {
        EventEntity eventEntity = eventMapper.selectById(eventId);
        if (eventEntity == null) {
            return null;
        }
        return eventStructMapper.entityToModel(eventEntity);
    }


    @Override
    @Transactional
    public Event createEvent(Event event) {
        if (event == null || event.getId() == null) {
            throw new IllegalArgumentException("event, and its id cannot be null");
        }

        Integer id = event.getId();
        // Check if event with same id already exists
        EventEntity existingEvent = eventMapper.selectById(id);
        if (existingEvent != null) {
            throw new IllegalArgumentException("event with ID " + id + " already exists");
        }

        Event eventToCreate = Event.builder()
                .id(event.getId())
                .name(event.getName() != null ? event.getName().trim() : CommonConst.EMPTY_STRING)
                .description(event.getDescription() != null ? event.getDescription().trim() : CommonConst.EMPTY_STRING)
                .build();
        EventEntity eventEntity = eventStructMapper.modelToEntity(eventToCreate);
        eventEntity.setTimeOnCreate();
        eventMapper.insert(eventEntity);

        log.info("[event] event created: eventId={}, name={}", event.getId(), event.getName());

        return eventToCreate;
    }


    @Override
    @Transactional
    public Event updateEvent(Integer eventId, String name, String description) {
        EventEntity eventEntity = eventMapper.selectById(eventId);
        if (eventEntity == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        Event event = eventStructMapper.entityToModel(eventEntity);

        // Update name if provided
        if (name != null && !name.trim().isEmpty()) {
            event.setName(name.trim());
        }
        // If name is null or empty, preserve existing name

        // Update description if provided
        if (description != null) {
            event.setDescription(description.trim());
        }
        // If description is null, preserve existing description

        // Convert back to entity and update
        EventEntity updatedEntity = eventStructMapper.modelToEntity(event);
        // Set update time (UNIX timestamp in seconds)
        updatedEntity.setUt((int) TimeUtil.getCurrentTime());
        eventMapper.updateById(updatedEntity);

        log.info("Event updated: eventId={}, name={}, description={}", eventId, event.getName(), event.getDescription());
        return event;
    }

    @Override
    @Transactional
    public void deleteEvent(Integer eventId) {
        EventEntity event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        // Delete all execution event relations
        LambdaQueryWrapper<ExecutionArrangementEntity> arrangementOfEventWrapper = new LambdaQueryWrapper<>();
        arrangementOfEventWrapper.eq(ExecutionArrangementEntity::getEventId, eventId);
        executionArrangementMapper.delete(arrangementOfEventWrapper);

        // Delete event
        eventMapper.deleteById(eventId);

        log.info("Event deleted: eventId={}", eventId);
    }

    @Override
    public List<ExecutionArrangement> getExecutionItems(@NotNull Integer eventId) {
        // Get allowed rule statuses
        Set<RuleStatusEnum> allowedStatusSet = ruleStatusConfig.getEvalAvailableRuleStatuses();
        List<Integer> allowedStatusIdList = allowedStatusSet.stream()
                .map(RuleStatusEnum::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (allowedStatusIdList.isEmpty()) {
            log.warn("No allowed rule statuses configured, returning empty list");
            return Collections.emptyList();
        }

        // Query execution arrangements with rule status filter using JOIN query
        List<ExecutionArrangementEntity> entityList = executionArrangementMapper.selectByEventIdOnRuleStatus(eventId, allowedStatusIdList);
        if (entityList == null || entityList.isEmpty()) {
            log.warn("No execution arrangements found for eventId={} with allowed rule statuses={}", eventId, allowedStatusIdList);
            return Collections.emptyList();
        }

        return entityList.stream()
                .map(executionArrangementStructMapper::entityToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setExecutionArrangements(@NotNull Integer eventId, List<Long> ruleIdList) {
        // Validate event existence
        EventEntity event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        // Handle null or empty list - treat as clearing all execution arrangements
        if (ruleIdList == null) {
            ruleIdList = Collections.emptyList();
        }

        // Collect all rule IDs for batch validation
        Set<Long> inputRuleIdSet = new HashSet<>(ruleIdList);
        inputRuleIdSet.removeIf(Objects::isNull);
        // Validate rule existence
        if (!inputRuleIdSet.isEmpty()) {
            validateRuleIdExistence(inputRuleIdSet);
        }

        // Get existing arrangements by event_id
        LambdaQueryWrapper<ExecutionArrangementEntity> arrangementOfEventWrapper = new LambdaQueryWrapper<>();
        arrangementOfEventWrapper.eq(ExecutionArrangementEntity::getEventId, eventId);
        List<ExecutionArrangementEntity> allExistingArrangementList = executionArrangementMapper.selectList(arrangementOfEventWrapper);
        if (allExistingArrangementList == null) {
            allExistingArrangementList = Collections.emptyList();
        }
        Set<Long> existingRuleIdSet = allExistingArrangementList.stream()
                .map(ExecutionArrangementEntity::getRuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long currentTime = TimeUtil.getCurrentTime();

        // 1. Insert/update: ruleId in input list but not in database (by event_id and rule_id)
        for (int i = 0; i < ruleIdList.size(); i++) {
            Long ruleId = ruleIdList.get(i);
            if (ruleId == null) {
                continue;
            }
            if (existingRuleIdSet.contains(ruleId)) {
                // Update all records matching event_id and rule_id (regardless of group_id)
                LambdaUpdateWrapper<ExecutionArrangementEntity> arrangementWrapper = new LambdaUpdateWrapper<>();
                arrangementWrapper.eq(ExecutionArrangementEntity::getEventId, eventId)
                        .eq(ExecutionArrangementEntity::getRuleId, ruleId)
                        .set(ExecutionArrangementEntity::getExeOrder, i)
                        .set(ExecutionArrangementEntity::getUt, (int) currentTime);
                executionArrangementMapper.update(null, arrangementWrapper);
            } else {
                // Create new relation
                ExecutionArrangementEntity relation = new ExecutionArrangementEntity();
                relation.setEventId(eventId);
                relation.setRuleId(ruleId);
                relation.setGroupId(0L); // Default group_id = 0 for standalone rules
                relation.setExeOrder(i);
                relation.setAbRatio(0); // Default abRatio = 0
                relation.setCt((int) currentTime);
                relation.setUt((int) currentTime);
                executionArrangementMapper.insert(relation);
            }
        }

        // 2. Delete: ruleId in database but not in input list (delete all matching records by event_id and rule_id)
        for (Long ruleId : existingRuleIdSet) {
            if (inputRuleIdSet.contains(ruleId)) {
                continue;
            }
            LambdaQueryWrapper<ExecutionArrangementEntity> arrangementWrapper = new LambdaQueryWrapper<>();
            arrangementWrapper.eq(ExecutionArrangementEntity::getEventId, eventId)
                    .eq(ExecutionArrangementEntity::getRuleId, ruleId);
            executionArrangementMapper.delete(arrangementWrapper);
        }

        if (log.isDebugEnabled()) {
            log.debug("set execution arrangements: eventId={}, itemCount={}", eventId, ruleIdList.size());
        }
    }

    private void validateRuleIdExistence(Set<Long> ruleIdSet) {
        List<RuleEntity> ruleEntityList = ruleMapper.selectBatchIds(new ArrayList<>(ruleIdSet));
        Set<Long> foundRuleIdSet = ruleEntityList != null
                ? ruleEntityList.stream()
                .map(RuleEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                : Collections.emptySet();
        Set<Long> notFoundRuleIdSet = ruleIdSet.stream()
                .filter(ruleId -> !foundRuleIdSet.contains(ruleId))
                .collect(Collectors.toSet());
        if (!notFoundRuleIdSet.isEmpty()) {
            throw new IllegalArgumentException("Rules not found in database: " + notFoundRuleIdSet);
        }
    }
}

