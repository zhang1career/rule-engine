package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lab.zhang.rule.rule_engine.entity.EventEntity;
import lab.zhang.rule.rule_engine.entity.ExecutionEventRelationEntity;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lab.zhang.rule.rule_engine.mapper.EventMapper;
import lab.zhang.rule.rule_engine.mapper.ExecutionEventRelationMapper;
import lab.zhang.rule.rule_engine.mapper.RuleGroupMapper;
import lab.zhang.rule.rule_engine.mapper.RuleMapper;
import lab.zhang.rule.rule_engine.model.Event;
import lab.zhang.rule.rule_engine.pojo.qo.ExecutionItemQO;
import lab.zhang.rule.rule_engine.service.EventService;
import lab.zhang.rule.rule_engine.struct_mapper.EventStructMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private ExecutionEventRelationMapper executionEventRelationMapper;

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private RuleGroupMapper ruleGroupMapper;

    @Autowired
    private EventStructMapper eventStructMapper;

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
    public Event getEventById(Long eventId) {
        EventEntity eventEntity = eventMapper.selectById(eventId);
        if (eventEntity == null) {
            return null;
        }
        return eventStructMapper.entityToModel(eventEntity);
    }

    @Override
    @Transactional
    public Event createEvent(Long id, String name, String description) {
        // Check if event with same id already exists
        EventEntity existingEvent = eventMapper.selectById(id);
        if (existingEvent != null) {
            throw new IllegalArgumentException("Event with ID " + id + " already exists");
        }

        Event event = Event.builder()
                .id(id)
                .name(name != null ? name.trim() : "")
                .description(description != null ? description.trim() : "")
                .build();

        EventEntity eventEntity = eventStructMapper.modelToEntity(event);
        // Set time fields (UNIX timestamp in seconds)
        long currentTime = System.currentTimeMillis() / 1000;
        // New entity, set create time and update time
        eventEntity.setCt((int) currentTime);
        eventEntity.setUt((int) currentTime);
        eventMapper.insert(eventEntity);

        log.info("Event created: eventId={}, name={}", event.getId(), event.getName());
        return event;
    }

    @Override
    @Transactional
    public Event updateEvent(Long eventId, String name, String description) {
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
        updatedEntity.setUt((int) (System.currentTimeMillis() / 1000));
        eventMapper.updateById(updatedEntity);

        log.info("Event updated: eventId={}, name={}, description={}", eventId, event.getName(), event.getDescription());
        return event;
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        EventEntity event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        // Delete all execution event relations
        LambdaQueryWrapper<ExecutionEventRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId);
        executionEventRelationMapper.delete(deleteWrapper);

        // Delete event
        eventMapper.deleteById(eventId);

        log.info("Event deleted: eventId={}", eventId);
    }

    @Override
    public List<ExecutionEventRelationEntity> getExecutionEventRelations(Long eventId) {
        LambdaQueryWrapper<ExecutionEventRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                .orderByAsc(ExecutionEventRelationEntity::getExecutionOrder);
        List<ExecutionEventRelationEntity> relations = executionEventRelationMapper.selectList(queryWrapper);
        return relations != null ? relations : Collections.emptyList();
    }

    @Override
    @Transactional
    public void batchSetExecutionItems(Long eventId, List<ExecutionItemQO> executionItems) {
        // Validate event existence
        EventEntity event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        // Validate basic parameters and group items by itemType
        Map<ExecutionItemTypeEnum, Set<Long>> itemsByType = new HashMap<>();
        for (ExecutionItemQO itemQO : executionItems) {
            ExecutionItemTypeEnum itemType = ExecutionItemTypeEnum.fromId(itemQO.getItemType());
            itemsByType.computeIfAbsent(itemType, k -> new HashSet<>()).add(itemQO.getItemId());
        }
        // Batch validate existence: query database by itemType groups
        Map<ExecutionItemTypeEnum, Set<Long>> notFoundItemsByType = new HashMap<>();
        for (Map.Entry<ExecutionItemTypeEnum, Set<Long>> entry : itemsByType.entrySet()) {
            ExecutionItemTypeEnum itemType = entry.getKey();
            Set<Long> uniqueItemIds = entry.getValue();

            Set<Long> foundItemIds;
            if (itemType == ExecutionItemTypeEnum.RULE) {
                // Batch query rules
                List<RuleEntity> ruleEntities = ruleMapper.selectBatchIds(uniqueItemIds);
                foundItemIds = ruleEntities != null
                        ? ruleEntities.stream()
                        .map(RuleEntity::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
                        : Collections.emptySet();
            } else if (itemType == ExecutionItemTypeEnum.RULE_GROUP) {
                // Batch query rule groups
                List<RuleGroupEntity> ruleGroupEntities = ruleGroupMapper.selectBatchIds(uniqueItemIds);
                foundItemIds = ruleGroupEntities != null
                        ? ruleGroupEntities.stream()
                        .map(RuleGroupEntity::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
                        : Collections.emptySet();
            } else {
                // Unknown itemType, treat all as not found
                foundItemIds = Collections.emptySet();
            }

            // Find items that don't exist
            Set<Long> notFoundItemIds = uniqueItemIds.stream()
                    .filter(itemId -> !foundItemIds.contains(itemId))
                    .collect(Collectors.toSet());

            if (!notFoundItemIds.isEmpty()) {
                notFoundItemsByType.put(itemType, notFoundItemIds);
            }
        }
        // If any items not found, throw exception with detailed message
        if (!notFoundItemsByType.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Execution items not found in database: ");
            List<String> errorParts = new ArrayList<>();
            for (Map.Entry<ExecutionItemTypeEnum, Set<Long>> entry : notFoundItemsByType.entrySet()) {
                ExecutionItemTypeEnum itemType = entry.getKey();
                List<Long> notFoundIds = entry.getValue().stream().sorted().collect(Collectors.toList());
                String typeName = itemType == ExecutionItemTypeEnum.RULE ? "Rule" : "Rule group";
                errorParts.add(typeName + " IDs: " + notFoundIds);
            }
            errorMessage.append(String.join("; ", errorParts));
            throw new IllegalArgumentException(errorMessage.toString());
        }

        // Get existing relations
        LambdaQueryWrapper<ExecutionEventRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId);
        List<ExecutionEventRelationEntity> existingRelations = executionEventRelationMapper.selectList(queryWrapper);

        // Build map of existing relations: (itemType, itemId) -> ExecutionEventRelationEntity
        Map<String, ExecutionEventRelationEntity> existingMap = new HashMap<>();
        if (existingRelations != null) {
            for (ExecutionEventRelationEntity relation : existingRelations) {
                String key = relation.getItemType() + ":" + relation.getItemId();
                existingMap.put(key, relation);
            }
        }

        // Build set of new relations: (itemType, itemId)
        Set<String> newItemKeys = new HashSet<>();
        long currentTime = System.currentTimeMillis() / 1000;

        // Process new execution items
        for (int i = 0; i < executionItems.size(); i++) {
            ExecutionItemQO itemQO = executionItems.get(i);
            // Execution order is determined by the list position (starting from 1)
            Integer executionOrder = i + 1;
            // Build key using itemType ID and itemId
            String key = itemQO.getItemType() + ":" + itemQO.getItemId();
            newItemKeys.add(key);

            // Convert itemType ID to enum (already validated above)
            ExecutionItemTypeEnum itemType = ExecutionItemTypeEnum.fromId(itemQO.getItemType());

            // Check if relation already exists
            ExecutionEventRelationEntity existing = existingMap.get(key);

            if (existing != null) {
                // Update existing relation
                LambdaUpdateWrapper<ExecutionEventRelationEntity> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                        .eq(ExecutionEventRelationEntity::getItemType, itemType.getId())
                        .eq(ExecutionEventRelationEntity::getItemId, itemQO.getItemId())
                        .set(ExecutionEventRelationEntity::getExecutionOrder, executionOrder)
                        .set(ExecutionEventRelationEntity::getUt, (int) currentTime);
                executionEventRelationMapper.update(null, updateWrapper);
            } else {
                // Create new relation
                ExecutionEventRelationEntity relation = new ExecutionEventRelationEntity();
                relation.setEventId(eventId);
                relation.setItemTypeEnum(itemType);
                relation.setItemId(itemQO.getItemId());
                relation.setExecutionOrder(executionOrder);
                relation.setCt((int) currentTime);
                relation.setUt((int) currentTime);
                executionEventRelationMapper.insert(relation);
            }
        }

        // Delete relations that are not in the new list
        for (Map.Entry<String, ExecutionEventRelationEntity> entry : existingMap.entrySet()) {
            if (!newItemKeys.contains(entry.getKey())) {
                ExecutionEventRelationEntity relation = entry.getValue();
                LambdaQueryWrapper<ExecutionEventRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(ExecutionEventRelationEntity::getEventId, relation.getEventId())
                        .eq(ExecutionEventRelationEntity::getItemType, relation.getItemType())
                        .eq(ExecutionEventRelationEntity::getItemId, relation.getItemId());
                executionEventRelationMapper.delete(deleteWrapper);
            }
        }

        log.info("Batch set execution items for event: eventId={}, itemCount={}", eventId, executionItems.size());
    }
}

