package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity;
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper;
import lab.zhang.rule.rule_engine.mapper.RuleGroupMapper;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.service.RuleSelectionCacheService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper;
import lab.zhang.rule.rule_engine.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static lab.zhang.rule.rule_engine.constant.EvalArgumentConst.ARG_USER_HASH_INT;

/**
 * Rule group service implementation
 * Uses MyBatis Plus for database operations
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class RuleGroupServiceImpl implements RuleGroupService {

    @Autowired
    private RuleGroupMapper ruleGroupMapper;

    @Autowired
    private ExecutionArrangementMapper executionArrangementMapper;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RuleGroupStructMapper ruleGroupStructMapper;

    @Autowired
    private RuleSelectionCacheService ruleSelectionCacheService;

    @Override
    public List<RuleGroup> getAllRuleGroups() {
        List<RuleGroupEntity> entities = ruleGroupMapper.selectList(null);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all group IDs
        List<Long> groupIds = entities.stream()
                .map(RuleGroupEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Convert entities to models using MapStruct with relations map as context
        return entities.stream()
                .map(entity -> ruleGroupStructMapper.entityToModel(entity))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Gets a rule group by its ID.
     *
     * @param groupId the group ID, must not be null
     * @return the rule group, or null if not found
     * @throws IllegalArgumentException if groupId is null
     */
    @Override
    public RuleGroup getRuleGroup(Long groupId) {
        RuleGroupEntity entity = ruleGroupMapper.selectById(groupId);
        if (entity == null) {
            log.warn("Rule group not found: groupId={}", groupId);
            return null;
        }

        // Load execution arrangements from table x
        LambdaQueryWrapper<ExecutionArrangementEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionArrangementEntity::getGroupId, groupId);
        List<ExecutionArrangementEntity> entityList = executionArrangementMapper.selectList(queryWrapper);
        if (log.isDebugEnabled()) {
            log.debug("Loaded {} execution arrangements for groupId={}, ruleIds={}",
                    entityList != null ? entityList.size() : 0,
                    groupId,
                    entityList != null ? entityList.stream()
                            .map(ExecutionArrangementEntity::getRuleId)
                            .collect(Collectors.toList()) : "null");
        }

        return ruleGroupStructMapper.entityToModelWithRuleRatios(entity, entityList);
    }

    /**
     * Create a new rule group and add the rule to it.
     * Called when a rule changes from other status to ONLINE status.
     * <p>
     * Business Logic:
     * 1. Check if this rule+event combination already exists in another rule group, if so, exit
     * 2. Create new rule group
     * 3. Update record in table x: set group_id from 0 to new groupId, set ab_ratio to 0
     *
     * @param rule    the rule to add to the new group (must be in ONLINE status)
     * @param eventId the event ID to associate with the rule group
     * @return the created rule group
     * @throws IllegalArgumentException if rule is not in ONLINE status, or rule+event combination already exists in another group
     */
    @Override
    @Transactional
    public RuleGroup createRuleGroup(@NotNull Rule rule, @NotNull Integer eventId) {
        if (rule.getId() == null) {
            throw new IllegalArgumentException("Rule Id cannot be null");
        }
        if (rule.getRuleStatus() != RuleStatusEnum.ONLINE) {
            throw new IllegalArgumentException("Rule must be in ONLINE status to create a group");
        }

        // Step 1: Check if this event-rule combination already exists in another rule group
        LambdaQueryWrapper<ExecutionArrangementEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ExecutionArrangementEntity::getEventId, eventId)
                .eq(ExecutionArrangementEntity::getRuleId, rule.getId());
        ExecutionArrangementEntity existingEntity = executionArrangementMapper.selectOne(checkWrapper);
        if (existingEntity == null) {
            throw new IllegalStateException("Event-Rule relation not found for ruleId=" + rule.getId() + ", eventId=" + eventId);
        }
        if (existingEntity.getGroupId() != null && existingEntity.getGroupId() != 0) {
            throw new IllegalStateException("Rule is already in a rule group, event=" + eventId + ", rule=" + rule.getId() + ", group=" + existingEntity.getGroupId());
        }

        long currentTime = TimeUtil.getCurrentTime();

        // Step 2: Create new rule group
        RuleGroupEntity ruleGroupEntity = new RuleGroupEntity();
        ruleGroupEntity.setCt((int) currentTime);
        ruleGroupEntity.setUt((int) currentTime);
        ruleGroupMapper.insert(ruleGroupEntity);
        Long groupId = ruleGroupEntity.getId();

        // Step 3: Update record in table x: set group_id from 0 to new groupId
        // Create entity with primary key fields set for identification
        ExecutionArrangementEntity updateEntity = new ExecutionArrangementEntity();
        updateEntity.setEventId(eventId);
        updateEntity.setRuleId(rule.getId());
        // Set fields to update (non-primary key fields)
        updateEntity.setGroupId(groupId); // New group_id
        updateEntity.setUt((int) currentTime);
        // Update by primary key, passing original group_id (0) separately for WHERE condition
        int updatedRows = executionArrangementMapper.updateByPrimaryKey(updateEntity);
        if (updatedRows <= 0) {
            throw new IllegalStateException("Event-Rule relation not found for ruleId=" + rule.getId() + ", eventId=" + eventId);
        }

        if (log.isDebugEnabled()) {
            log.debug("Rule group created: groupId={}, ruleId={}, eventId={}", groupId, rule.getId(), eventId);
        }

        return RuleGroup.builder()
                .id(groupId)
                .rules(Collections.singletonMap(rule.getId(), Pair.of(rule, 0)))
                .build();
    }

    /**
     * Delete rule group if it has no associations.
     *
     * @param groupId the group ID to delete
     */
    @Override
    @Transactional
    public void deleteRuleGroup(@NotNull Long groupId) {
        // Verify group exists
        RuleGroup group = getRuleGroup(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Rule group not found: " + groupId);
        }

        List<Long> ruleIds = group.getRuleIds();
        if (ruleIds != null && !ruleIds.isEmpty()) {
            throw new IllegalStateException("Deleting non-empty rule group is not allowed: groupId=" + groupId);
        }

        // Delete rule group entity
        ruleGroupMapper.deleteById(groupId);

        if (log.isDebugEnabled()) {
            log.debug("Rule group deleted: groupId={}", groupId);
        }
    }

    /**
     * Update rule group traffic control ratios.
     * Only receives ratios parameter for editing.
     * <p>
     * Validation:
     * - Key must be Long positive integer (rule ID)
     * - Value must be non-negative integer (abTestRatio, 0-100)
     * - Sum of all values must not exceed 100
     * - All rule IDs must exist in the group
     *
     * @param groupId    rule group ID
     * @param ruleRatios map of rule ID to A/B test ratio (0-100)
     * @throws IllegalArgumentException if group not found, validation fails, or rule not in group
     */
    @Override
    @Transactional
    public void updateRuleGroupRatios(@NotNull Long groupId, @NotEmpty Map<Long, Integer> ruleRatios) {
        // Verify group exists
        RuleGroup existingGroup = getRuleGroup(groupId);
        if (existingGroup == null) {
            throw new IllegalArgumentException("Rule group not found: " + groupId);
        }

        // Validate all rule IDs exist in the group
        Set<Long> existingRuleIdSet = new HashSet<>(existingGroup.getRuleIds());
        for (Long ruleId : ruleRatios.keySet()) {
            if (!existingRuleIdSet.contains(ruleId)) {
                throw new IllegalArgumentException("Rule " + ruleId + " does not exist in group " + groupId);
            }
        }

        // Get all records for this group from table x
        LambdaQueryWrapper<ExecutionArrangementEntity> groupRelationQueryWrapper = new LambdaQueryWrapper<>();
        groupRelationQueryWrapper.eq(ExecutionArrangementEntity::getGroupId, groupId);
        List<ExecutionArrangementEntity> groupRelations = executionArrangementMapper.selectList(groupRelationQueryWrapper);
        if (groupRelations == null || groupRelations.isEmpty()) {
            throw new IllegalStateException("Rule group " + groupId + " has no associated rules");
        }
        
        // Group by eventId to check if group is associated with multiple events
        Map<Integer, List<ExecutionArrangementEntity>> relationsByEvent = groupRelations.stream()
                .collect(Collectors.groupingBy(ExecutionArrangementEntity::getEventId));
        if (relationsByEvent.size() > 1) {
            throw new IllegalStateException("Rule group " + groupId + " is associated with multiple events, which violates the design constraint");
        }

        long currentTime = TimeUtil.getCurrentTime();

        // Update ratios in table x
        for (Map.Entry<Long, Integer> entry : ruleRatios.entrySet()) {
            Long ruleId = entry.getKey();
            Integer ratio = entry.getValue();

            // Find the relation for this rule in this group
            ExecutionArrangementEntity relation = groupRelations.stream()
                    .filter(r -> r.getRuleId().equals(ruleId))
                    .findFirst()
                    .orElse(null);
            
            if (relation == null) {
                throw new IllegalStateException("Rule " + ruleId + " not found in group " + groupId);
            }
            
            // Update ab_ratio
            LambdaUpdateWrapper<ExecutionArrangementEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ExecutionArrangementEntity::getEventId, relation.getEventId())
                    .eq(ExecutionArrangementEntity::getRuleId, ruleId)
                    .eq(ExecutionArrangementEntity::getGroupId, groupId)
                    .set(ExecutionArrangementEntity::getAbRatio, ratio)
                    .set(ExecutionArrangementEntity::getUt, (int) currentTime);
            executionArrangementMapper.update(null, updateWrapper);
        }

        // Update group update time
        RuleGroupEntity groupEntity = ruleGroupStructMapper.modelToEntity(existingGroup);
        if (groupEntity != null) {
            groupEntity.setUt((int) currentTime);
            ruleGroupMapper.updateById(groupEntity);
        }

        if (log.isDebugEnabled()) {
            log.info("Rule group ratios updated: groupId={}, ratios={}", groupId, ruleRatios);
        }
    }

    /**
     * Copy a rule within a rule group.
     * Creates a new rule with the same content, event association, and rule group association as the original.
     * The copied rule's abTestRatio is set to 0.
     *
     * @param ruleId the rule ID to copy
     * @param ruleDTO   the new rule data (name, description, contentType, content)
     * @return the copied rule
     * @throws IllegalArgumentException if rule not found or rule not in any group
     */
    @Override
    @Transactional
    public Rule copyRuleInGroup(@NotNull Long ruleId, @NotNull RuleDTO ruleDTO) {
        // Get original rule
        Rule originalRule = ruleService.getRuleById(ruleId);
        if (originalRule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }

        // Check if rule is in a group (query table x)
        LambdaQueryWrapper<ExecutionArrangementEntity> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(ExecutionArrangementEntity::getRuleId, ruleId)
                .ne(ExecutionArrangementEntity::getGroupId, 0);
        List<ExecutionArrangementEntity> relations = executionArrangementMapper.selectList(relationQueryWrapper);
        if (relations == null || relations.isEmpty()) {
            throw new IllegalArgumentException("Rule " + ruleId + " is not in any group");
        }

        // Get the first relation (assuming rule is in one group for one event)
        ExecutionArrangementEntity relation = relations.get(0);
        Long groupId = relation.getGroupId();
        Integer eventId = relation.getEventId();
        Integer exeOrder = relation.getExeOrder();

        // Prepare new rule data
        String newName = ruleDTO.getName() != null ? ruleDTO.getName() : originalRule.getName() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String newDescription = ruleDTO.getDescription() != null ? ruleDTO.getDescription() : originalRule.getDescription();
        Integer newContentType = ruleDTO.getContentType() != null ? ruleDTO.getContentType() : originalRule.getContentType().getId();
        String newContent = ruleDTO.getContent() != null ? ruleDTO.getContent() : originalRule.getContent();

        long currentTime = TimeUtil.getCurrentTime();

        // Create new rule
        Rule newRule = Rule.builder()
                .name(newName)
                .contentType(ContentTypeEnum.fromId(newContentType))
                .content(newContent)
                .description(newDescription)
                .ruleStatus(RuleStatusEnum.ONLINE) // Copied rule is in ONLINE status
                .build();

        // Create rule in database (status will be set to OFFLINE by createRule)
        ruleService.doCreateRule(newRule, currentTime);
        Long newRuleId = newRule.getId();

        // Create record in table x for new rule (same group and event, ratio = 0)
        // According to new design, when rule becomes ONLINE, createRuleGroup is called
        // But since we want the new rule in the same group, we directly create record in table x
        ExecutionArrangementEntity newRelation = new ExecutionArrangementEntity();
        newRelation.setEventId(eventId);
        newRelation.setRuleId(newRuleId);
        newRelation.setGroupId(groupId);
        newRelation.setExeOrder(exeOrder != null ? exeOrder : 0);
        newRelation.setAbRatio(0); // Default ratio is 0
        newRelation.setCt((int) currentTime);
        newRelation.setUt((int) currentTime);
        executionArrangementMapper.insert(newRelation);

        // Note: According to new design, when rule becomes ONLINE:
        // - createRuleGroup creates event-rule group relation and deletes event-rule relation
        // - But since we're adding to existing group, we don't need to create event-rule group relation again
        // - And event-rule relation should already be deleted when rule status changed to ONLINE
        if (log.isDebugEnabled()) {
            log.debug("Rule copied in group: originalRuleId={}, newRuleId={}, groupId={}, eventId={}",
                    ruleId, newRuleId, groupId, eventId);
        }

        return newRule;
    }

    /**
     * Select one rule from rule group based on probability distribution.
     * Uses cache to ensure consistent rule selection for the same user-event-group combination.
     * Cache key format: rule:gw:abt:{userId}:{eventId}:{groupId}
     * Since there can be multiple rule groups for a single event, the cache records
     * which specific rule was selected from each rule group.
     *
     * @param group   the rule group to select from
     * @param context the rule execution context (contains userId, eventId, and userHash)
     * @return the selected rule ID, or 0L if no rule should be executed
     */
    @Override
    public Rule selectRuleFromGroup(RuleGroup group, RuleExecutionContext context) {
        // Get userId and EventId
        Long userId = context.getUserId();
        Integer eventId = context.getEventId();

        // Check cache first
        Long cachedRuleId = ruleSelectionCacheService.get(userId, eventId, group.getId());
        if (cachedRuleId != null && isCachedABTestRule(cachedRuleId, group)) {
            Rule cachedRule = group.getRules().get(cachedRuleId).getLeft();
            if (log.isDebugEnabled()) {
                log.debug("Using cached rule selection: userId={}, eventId={}, groupId={}, ruleId={}",
                        userId, eventId, group.getId(), cachedRuleId);
            }
            return cachedRule;
        }

        // Get hashInt from context (calculated during context construction)
        Integer userHashInt = (Integer) context.getArgument(ARG_USER_HASH_INT).getValue();
        // Use drawRandomRule to select rule based on probability distribution
        Rule selectedRule = group.drawRandomRule(userHashInt);
        if (selectedRule == null) {
            log.warn("Rule group skipped: groupId={}, hashInt={}", group.getId(), userHashInt);
            return null;
        }

        // Cache the selection for this user-event-group combination
        ruleSelectionCacheService.put(userId, eventId, group.getId(), selectedRule.getId());

        // Load rule content after rule selection
        if (log.isDebugEnabled()) {
            log.debug("Rule selected from group: groupId={}, ruleId={}, userHashInt={}", group.getId(), selectedRule.getId(), userHashInt);
        }
        return selectedRule;
    }

    /**
     * Check if a cached a/b test rule is valid
     * Validates by checking if the rule exists in the group's rules map.
     * If it exists in the map, it means the rule is valid and in AB_TEST status
     * (loadRulesMapForGroup already filters out non-ONLINE rules).
     *
     * @param cachedRuleId the cached rule ID
     * @param group        the rule group (rules map must be loaded)
     * @return true if the cached rule is valid, false otherwise
     */
    private boolean isCachedABTestRule(Long cachedRuleId, RuleGroup group) {
        if (group.getRules() == null || !group.getRules().containsKey(cachedRuleId)) {
            log.warn("Cached rule not found in group's rules map: groupId={}, ruleId={}", group.getId(), cachedRuleId);
            return false;
        }

        Pair<Rule, Integer> ruleRatioPair = group.getRules().get(cachedRuleId);
        if (ruleRatioPair == null || ruleRatioPair.getLeft() == null) {
            log.warn("Cached rule pair is null in group's rules map: groupId={}, ruleId={}", group.getId(), cachedRuleId);
            return false;
        }

        Rule cachedRule = ruleRatioPair.getLeft();

        return cachedRule.getRuleStatus() == RuleStatusEnum.ONLINE;
    }
}
