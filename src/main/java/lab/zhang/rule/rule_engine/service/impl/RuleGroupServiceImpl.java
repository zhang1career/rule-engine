package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lab.zhang.rule.rule_engine.entity.*;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.mapper.*;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.service.RuleSelectionCacheService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper;
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
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
    private RuleGroupRuleRelationMapper ruleGroupRuleRelationMapper;

    @Autowired
    private ExecutionEventRelationMapper executionEventRelationMapper;

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private RuleContentMapper ruleContentMapper;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RuleGroupStructMapper ruleGroupStructMapper;

    @Autowired
    private RuleStructMapper ruleStructMapper;

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

        // Batch load all rule group relations in one query
        Map<Long, List<RuleGroupRuleRelationEntity>> relationsMap = Collections.emptyMap();
        if (!groupIds.isEmpty()) {
            LambdaQueryWrapper<RuleGroupRuleRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(RuleGroupRuleRelationEntity::getGroupId, groupIds);
            List<RuleGroupRuleRelationEntity> allRelations = ruleGroupRuleRelationMapper.selectList(queryWrapper);

            // Build map: groupId -> List<RuleGroupRuleEntity>
            relationsMap = allRelations != null
                    ? allRelations.stream()
                    .collect(Collectors.groupingBy(RuleGroupRuleRelationEntity::getGroupId))
                    : Collections.emptyMap();
        }

        // Convert entities to models using MapStruct with relations map as context
        final Map<Long, List<RuleGroupRuleRelationEntity>> finalRelationsMap = relationsMap;
        return entities.stream()
                .map(entity -> ruleGroupStructMapper.entityToModel(entity, finalRelationsMap))
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
            return null;
        }

        // Load rule IDs from rule_group_rule_rel table
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId);
        List<RuleGroupRuleRelationEntity> relationEntities = ruleGroupRuleRelationMapper.selectList(queryWrapper);
        log.debug("Loaded {} relations for groupId={}", relationEntities != null ? relationEntities.size() : 0, groupId);

        // Build relations map for MapStruct context
        Map<Long, List<RuleGroupRuleRelationEntity>> relationsMap = new HashMap<>();
        if (relationEntities != null && !relationEntities.isEmpty()) {
            relationsMap.put(groupId, relationEntities);
            log.debug("Relations map built for groupId={}, ruleIds={}, entity.id={}", groupId,
                    relationEntities.stream().map(RuleGroupRuleRelationEntity::getRuleId).collect(java.util.stream.Collectors.toList()),
                    entity != null ? entity.getId() : null);
        }

        // Convert entity to model using MapStruct
        // Note: ruleRatios are loaded separately when needed (e.g., in selectRuleFromGroup)
        // They are stored in rule_group_rule_rel table, not in RuleGroup model
        RuleGroup group = ruleGroupStructMapper.entityToModel(entity, relationsMap);

        // Rules map will be loaded separately when needed (e.g., in getRuleGroupsWithRules)
        // For now, just log the conversion
        log.debug("Converted RuleGroup: groupId={}, ruleIds={}", group != null ? group.getId() : null,
                group != null && group.getRuleIds() != null ? group.getRuleIds() : "null");
        return group;
    }

    /**
     * Create a new rule group and add the rule to it.
     * Business Logic 1: When a rule changes from other status to AB_TEST,
     * create a rule group and copy rule-event associations to group-event associations.
     *
     * @param rule the rule to add to the new group
     * @return the created rule group
     */
    @Override
    @Transactional
    public RuleGroup createRuleGroup(Rule rule) {
        if (rule.getRuleStatus() != RuleStatusEnum.AB_TEST) {
            throw new IllegalArgumentException("Rule must be in AB_TEST status to create a group");
        }

        // Create rule group entity
        RuleGroupEntity entity = new RuleGroupEntity();
        entity.setTimeOnCreate();
        ruleGroupMapper.insert(entity);

        Long groupId = entity.getId();

        // Add rule to group with A/B test ratio from rule (if exists, otherwise default to 0)
        addRuleToGroupRelation(groupId, rule.getId(), 0);

        // Business Logic 1: Copy rule-event associations to group-event associations
        copyRuleEventAssociationsToGroup(rule.getId(), groupId);

        // Update rule's groupId directly in database to avoid circular dependency
        // Don't call ruleService.createRule() as it would trigger handleRuleStatusChange()
        // which would call createRuleGroup() again, causing infinite recursion
        RuleEntity ruleEntity = ruleMapper.selectById(rule.getId());
        if (ruleEntity != null) {
            ruleEntity.setRuleGroupId(groupId);
            ruleEntity.setUt((int) (System.currentTimeMillis() / 1000));
            ruleMapper.updateById(ruleEntity);
            // Also update the rule object's groupId for consistency
            rule.setRuleGroupId(groupId);
        }

        log.info("Rule group created: groupId={}, ruleId={}", groupId, rule.getId());

        return RuleGroup.builder()
                .id(groupId)
                .rules(Collections.singletonMap(rule.getId(), Pair.of(rule, 0)))
                .build();
    }

    @Override
    @Transactional
    public void deleteRuleGroup(Long groupId) {
        // Verify group exists
        RuleGroup group = getRuleGroup(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Rule group not found: " + groupId);
        }

        // Set all rules in group to OFFLINE and remove from group
        List<Long> ruleIds = group.getRuleIds();
        for (Long ruleId : ruleIds) {
            RuleEntity ruleEntity = ruleMapper.selectById(ruleId);
            if (ruleEntity != null) {
                ruleEntity.setRuleStatusEnum(RuleStatusEnum.OFFLINE);
                ruleEntity.setRuleGroupId(RuleGroup.DEFAULT_RULE_GROUP_ID);
                ruleEntity.setTimeOnUpdate();
                ruleMapper.updateById(ruleEntity);
            }
        }

        // Call doDeleteRuleGroup to delete the group and all its associations
        doDeleteRuleGroup(groupId);

        log.info("Rule group deleted: groupId={}, affected rules={}", groupId, ruleIds);
    }

    /**
     * Delete rule group and all its associations.
     * This method deletes:
     * 1. All rule-group associations (rule_group_rule_rel table)
     * 2. All group-event associations (execution_event_rel table)
     * 3. The rule group entity itself (rule_group table)
     *
     * @param groupId the group ID to delete
     */
    @Transactional
    public void doDeleteRuleGroup(Long groupId) {
        if (groupId == null) {
            log.warn("GroupId is null, cannot delete rule group");
            return;
        }

        // Delete all rule-group associations
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> ruleRelationDeleteWrapper = new LambdaQueryWrapper<>();
        ruleRelationDeleteWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId);
        ruleGroupRuleRelationMapper.delete(ruleRelationDeleteWrapper);

        // Delete all group-event associations
        LambdaQueryWrapper<ExecutionEventRelationEntity> eventRelationDeleteWrapper = new LambdaQueryWrapper<>();
        eventRelationDeleteWrapper.eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE_GROUP.getId())
                .eq(ExecutionEventRelationEntity::getItemId, groupId);
        executionEventRelationMapper.delete(eventRelationDeleteWrapper);

        // Delete rule group entity
        ruleGroupMapper.deleteById(groupId);

        log.info("Rule group and all associations deleted: groupId={}", groupId);
    }

    @Override
    @Transactional
    public RuleGroup createRuleGroupWithRules(Map<Long, Integer> rules) {
        // Validate all rules exist first, collect non-existent rule IDs
        List<Long> nonExistentRuleIds = new ArrayList<>();
        for (Long ruleId : rules.keySet()) {
            Rule rule = ruleService.getRuleById(ruleId);
            if (rule == null) {
                nonExistentRuleIds.add(ruleId);
            }
        }
        if (!nonExistentRuleIds.isEmpty()) {
            throw new IllegalArgumentException("Rule IDs do not exist in database: " + nonExistentRuleIds);
        }
        
        // Validate all rules can transition to AB_TEST and check if rules belong to other groups
        // Note: Ratio validation (0-100 range and sum <= 100) is handled by @ValidRuleRatios annotation in RuleGroupQO
        for (Map.Entry<Long, Integer> entry : rules.entrySet()) {
            Long ruleId = entry.getKey();
            RuleStatusEnum currentStatus = queryRuleStatus(ruleId);
            // Validate 2: Check if rule already belongs to another group
            validateRuleAlreadyGrouped(ruleId);
            // Rules in A/B Test status cannot be added to new group
            if (currentStatus == RuleStatusEnum.AB_TEST) {
                throw new IllegalArgumentException("Rule " + ruleId + " is already in AB_TEST status and cannot be added to a new group");
            }
            // Check if rule can transition to AB_TEST
            if (!currentStatus.canTransitionTo(RuleStatusEnum.AB_TEST)) {
                throw new IllegalArgumentException(
                        String.format("Rule %d cannot transition from %s to AB_TEST. Allowed transitions from %s: %s",
                                ruleId, currentStatus, currentStatus, currentStatus.getAllowedTargetStatuses()));
            }
        }

        // Create rule group entity
        RuleGroupEntity ruleGroupEntity = new RuleGroupEntity();
        ruleGroupEntity.setTimeOnCreate();
        ruleGroupMapper.insert(ruleGroupEntity);
        Long groupId = ruleGroupEntity.getId();

        long currentTime = System.currentTimeMillis() / 1000;

        // Add rules to group and update their status to AB_TEST
        List<Long> ruleIdList = new ArrayList<>(rules.keySet());
        batchUpdateRuleToABTest(rules, ruleIdList, groupId, currentTime);

        // Batch query existing relations (should be empty for new group, but check anyway)
        List<Long> ruleIds = new ArrayList<>(rules.keySet());
        Map<Long, RuleGroupRuleRelationEntity> existingRelationMap = getExistingRuleGroupRelationMap(groupId, ruleIds);

        // Batch process relations (insert new)
        List<RuleGroupRuleRelationEntity> relationsToInsert = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : rules.entrySet()) {
            Long ruleId = entry.getKey();
            Integer ratio = entry.getValue();
            RuleGroupRuleRelationEntity existing = existingRelationMap.get(ruleId);
            if (existing == null) {
                RuleGroupRuleRelationEntity relation = new RuleGroupRuleRelationEntity();
                relation.setGroupId(groupId);
                relation.setRuleId(ruleId);
                relation.setAbTestRatio(ratio != null ? ratio : 0);
                relation.setCt((int) currentTime);
                relation.setUt((int) currentTime);
                relationsToInsert.add(relation);
            }
        }

        // Batch insert new relations
        if (!relationsToInsert.isEmpty()) {
            for (RuleGroupRuleRelationEntity relation : relationsToInsert) {
                ruleGroupRuleRelationMapper.insert(relation);
            }
        }

        log.info("Rule group created with rules: groupId={}, ruleIds={}", groupId, ruleIdList);

        return RuleGroup.builder()
                .id(groupId)
                .rules(rules.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    RuleEntity ruleEntity = ruleMapper.selectById(entry.getKey());
                                    Rule ruleModel = ruleStructMapper.entityToModel(ruleEntity);
                                    return Pair.of(ruleModel, entry.getValue());
                                }
                        )))
                .build();
    }

    private Map<Long, RuleGroupRuleRelationEntity> getExistingRuleGroupRelationMap(Long groupId, List<Long> ruleIds) {
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId)
                .in(RuleGroupRuleRelationEntity::getRuleId, ruleIds);
        List<RuleGroupRuleRelationEntity> existingRelations = ruleGroupRuleRelationMapper.selectList(relationQueryWrapper);
        return existingRelations != null
                ? existingRelations.stream()
                .collect(Collectors.toMap(RuleGroupRuleRelationEntity::getRuleId, relation -> relation))
                : Collections.emptyMap();
    }

    @Override
    @Transactional
    public void updateRuleGroupWithRules(Long groupId, Map<Long, Integer> rules) {
        // Verify group exists
        RuleGroup existingGroup = getRuleGroup(groupId);
        if (existingGroup == null) {
            throw new IllegalArgumentException("Rule group not found: " + groupId);
        }

        // Validate all rules exist first, collect non-existent rule IDs
        List<Long> nonExistentRuleIds = new ArrayList<>();
        for (Long ruleId : rules.keySet()) {
            Rule rule = ruleService.getRuleById(ruleId);
            if (rule == null) {
                nonExistentRuleIds.add(ruleId);
            }
        }
        if (!nonExistentRuleIds.isEmpty()) {
            throw new IllegalArgumentException("Rule IDs do not exist in database: " + nonExistentRuleIds);
        }
        
        // Validate all rules can transition to AB_TEST (or are already AB_TEST) and check if rules belong to other groups
        // Note: Ratio validation (0-100 range and sum <= 100) is handled by @ValidRuleRatios annotation in RuleGroupQO
        for (Map.Entry<Long, Integer> entry : rules.entrySet()) {
            Long ruleId = entry.getKey();
            RuleStatusEnum currentStatus = queryRuleStatus(ruleId);
            // Validate 2: Check if rule already belongs to another group (excluding current group)
            validateRuleAlreadyGrouped(groupId, ruleId);
            // If already AB_TEST, skip validation (allow)
            if (currentStatus == RuleStatusEnum.AB_TEST) {
                continue;
            }
            // Check if rule can transition to AB_TEST
            if (!currentStatus.canTransitionTo(RuleStatusEnum.AB_TEST)) {
                throw new IllegalArgumentException(
                        String.format("Rule %d cannot transition from %s to AB_TEST. Allowed transitions from %s: %s",
                                ruleId, currentStatus, currentStatus, currentStatus.getAllowedTargetStatuses()));
            }
        }

        long currentTime = System.currentTimeMillis() / 1000;

        // Get existing rules in group
        List<Long> existingRuleIds = existingGroup.getRuleIds();
        Set<Long> newRuleIds = new HashSet<>(rules.keySet());

        // Remove rules that are not in the new rules map
        List<Long> rulesToRemove = new ArrayList<>();
        for (Long existingRuleId : existingRuleIds) {
            if (!newRuleIds.contains(existingRuleId)) {
                rulesToRemove.add(existingRuleId);
            }
        }
        // Batch remove rules
        if (!rulesToRemove.isEmpty()) {
            batchRemoveRulesFromGroup(groupId, rulesToRemove, currentTime);
            log.info("Rules removed from group and set to OFFLINE: groupId={}, ruleIds={}", groupId, rulesToRemove);
        }

        // Add or update rules in group
        batchAddOrUpdateRuleToGroup(groupId, rules, currentTime);

        // Update group update time
        RuleGroupEntity groupEntity = ruleGroupMapper.selectById(groupId);
        if (groupEntity != null) {
            groupEntity.setUt((int) currentTime);
            ruleGroupMapper.updateById(groupEntity);
        }

        log.info("Rule group updated: groupId={}, rules={}", groupId, rules.keySet());
    }

    @Transactional
    public void batchRemoveRulesFromGroup(Long groupId, List<Long> rulesToRemove, long currentTime) {
        // Batch query rule entities
        List<RuleEntity> ruleEntitiesToUpdate = ruleMapper.selectBatchIds(rulesToRemove);
        if (ruleEntitiesToUpdate != null && !ruleEntitiesToUpdate.isEmpty()) {
            // Batch update rules to OFFLINE and remove from group
            for (RuleEntity ruleEntity : ruleEntitiesToUpdate) {
                ruleEntity.setRuleStatusEnum(RuleStatusEnum.OFFLINE);
                ruleEntity.setRuleGroupId(RuleGroup.DEFAULT_RULE_GROUP_ID);
                ruleEntity.setUt((int) currentTime);
            }
            ruleMapper.updateBatchById(ruleEntitiesToUpdate);
        }

        // Batch delete group relations
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId)
                .in(RuleGroupRuleRelationEntity::getRuleId, rulesToRemove);
        ruleGroupRuleRelationMapper.delete(deleteWrapper);
    }

    @Transactional
    public void batchAddOrUpdateRuleToGroup(Long groupId, Map<Long, Integer> rules, long currentTime) {
        List<Long> ruleIds = new ArrayList<>(rules.keySet());
        // Batch query all rule entities to get current status
        batchUpdateRuleToABTest(rules, ruleIds, groupId, currentTime);

        // Batch query existing relations
        Map<Long, RuleGroupRuleRelationEntity> existingRelationMap = getExistingRuleGroupRelationMap(groupId, ruleIds);

        // Batch process relations (insert new or update existing)
        List<RuleGroupRuleRelationEntity> relationsToInsert = new ArrayList<>();
        List<RuleGroupRuleRelationEntity> relationsToUpdate = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : rules.entrySet()) {
            Long ruleId = entry.getKey();
            Integer ratio = entry.getValue();
            RuleGroupRuleRelationEntity existing = existingRelationMap.get(ruleId);
            if (existing == null) {
                RuleGroupRuleRelationEntity relation = new RuleGroupRuleRelationEntity();
                relation.setGroupId(groupId);
                relation.setRuleId(ruleId);
                relation.setAbTestRatio(ratio != null ? ratio : 0);
                relation.setCt((int) currentTime);
                relation.setUt((int) currentTime);
                relationsToInsert.add(relation);
            } else {
                // Update ratio if provided
                if (ratio != null && !ratio.equals(existing.getAbTestRatio())) {
                    existing.setAbTestRatio(ratio);
                    existing.setUt((int) currentTime);
                    relationsToUpdate.add(existing);
                }
            }
        }
        // Batch insert new relations
        if (!relationsToInsert.isEmpty()) {
            for (RuleGroupRuleRelationEntity relation : relationsToInsert) {
                ruleGroupRuleRelationMapper.insert(relation);
            }
        }
        // Batch update existing relations
        if (!relationsToUpdate.isEmpty()) {
            ruleGroupRuleRelationMapper.updateBatchById(relationsToUpdate);
        }
    }

    @Transactional
    public void batchUpdateRuleToABTest(Map<Long, Integer> rules, List<Long> ruleIdList, Long groupId, long currentTime) {
        // Batch query all rule entities to get current status
        List<RuleEntity> allRuleEntities = ruleMapper.selectBatchIds(ruleIdList);
        Map<Long, RuleEntity> ruleEntityMap = allRuleEntities != null
                ? allRuleEntities.stream()
                .collect(Collectors.toMap(RuleEntity::getId, entity -> entity))
                : Collections.emptyMap();

        // Collect rules that need status update to AB_TEST
        List<RuleEntity> rulesToUpdate = new ArrayList<>();
        List<Long> rulesToCopyEventAssociations = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : rules.entrySet()) {
            Long ruleId = entry.getKey();
            RuleEntity ruleEntity = ruleEntityMap.get(ruleId);
            if (ruleEntity == null) {
                continue;
            }
            RuleStatusEnum currentStatus = ruleEntity.getRuleStatusEnum();
            if (currentStatus != RuleStatusEnum.AB_TEST) {
                ruleEntity.setRuleStatusEnum(RuleStatusEnum.AB_TEST);
                ruleEntity.setRuleGroupId(groupId);
                ruleEntity.setUt((int) currentTime);
                rulesToUpdate.add(ruleEntity);
                rulesToCopyEventAssociations.add(ruleId);
            }
        }
        // Batch update rules that need status change
        if (!rulesToUpdate.isEmpty()) {
            ruleMapper.updateBatchById(rulesToUpdate);
        }

        // Batch copy rule-event associations to group-event associations
        for (Long ruleId : rulesToCopyEventAssociations) {
            copyRuleEventAssociationsToGroup(ruleId, groupId);
        }
    }

    private RuleStatusEnum queryRuleStatus(Long ruleId) {
        Rule rule = ruleService.getRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found for ID: " + ruleId);
        }
        RuleStatusEnum currentStatus = rule.getRuleStatus();
        if (currentStatus == null) {
            throw new IllegalArgumentException("Rule status is null for rule: " + ruleId);
        }
        return currentStatus;
    }

    private void validateRuleAlreadyGrouped(Long ruleId) {
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(RuleGroupRuleRelationEntity::getRuleId, ruleId);
        RuleGroupRuleRelationEntity existingRelation = ruleGroupRuleRelationMapper.selectOne(checkWrapper);
        if (existingRelation != null) {
            throw new IllegalArgumentException("Rule " + ruleId + " already belongs to another rule group: " + existingRelation.getGroupId());
        }
    }

    private void validateRuleAlreadyGrouped(Long ruleId, Long selfGroupId) {
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(RuleGroupRuleRelationEntity::getRuleId, ruleId);
        RuleGroupRuleRelationEntity existingRelation = ruleGroupRuleRelationMapper.selectOne(checkWrapper);
        if (existingRelation != null && !existingRelation.getGroupId().equals(selfGroupId)) {
            throw new IllegalArgumentException("Rule " + ruleId + " already belongs to another rule group: " + existingRelation.getGroupId());
        }
    }

    /**
     * Add rule to existing rule group.
     * Business Logic 2: When adding a rule to an existing group,
     * copy group-event associations to rule-event associations.
     *
     * @param rule    the rule to add
     * @param groupId the existing group ID
     */
    @Override
    @Transactional
    public void addRuleToGroup(Rule rule, Long groupId) {
        if (rule == null || rule.getId() == null) {
            throw new IllegalArgumentException("Rule or ruleId cannot be null");
        }
        if (rule.getRuleStatus() != RuleStatusEnum.AB_TEST) {
            throw new IllegalArgumentException("Rule must be in AB_TEST status to add to a group");
        }
        if (groupId == null) {
            throw new IllegalArgumentException("GroupId cannot be null");
        }

        RuleGroupEntity groupEntity = ruleGroupMapper.selectById(groupId);
        if (groupEntity == null) {
            throw new IllegalArgumentException("Rule group not found: " + groupId);
        }

        // Check if rule already belongs to another group
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(RuleGroupRuleRelationEntity::getRuleId, rule.getId());
        RuleGroupRuleRelationEntity existingRelation = ruleGroupRuleRelationMapper.selectOne(checkWrapper);
        if (existingRelation != null && !existingRelation.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("Rule already belongs to another group: " + existingRelation.getGroupId());
        }

        // Add rule to group with A/B test ratio from rule (if exists, otherwise default to 0)
        Integer abTestRatio = 0;
        addRuleToGroupRelation(groupId, rule.getId(), abTestRatio);

        // Business Logic 2: Copy group-event associations to rule-event associations
        copyGroupEventAssociationsToRule(groupId, rule.getId());

        // Update rule's groupId directly in database to avoid circular dependency
        // Don't call ruleService.createRule() as it would trigger handleRuleStatusChange()
        // which would call addRuleToGroup() again, causing infinite recursion
        RuleEntity ruleEntity = ruleMapper.selectById(rule.getId());
        if (ruleEntity != null) {
            ruleEntity.setRuleGroupId(groupId);
            ruleEntity.setUt((int) (System.currentTimeMillis() / 1000));
            ruleMapper.updateById(ruleEntity);
            // Also update the rule object's groupId for consistency
            rule.setRuleGroupId(groupId);
        }

        log.info("Rule added to group: groupId={}, ruleId={}", groupId, rule.getId());
    }

    /**
     * Business Logic 1: Copy rule-event associations to group-event associations.
     * When a rule changes from other status to AB_TEST, copy all its event associations
     * to the group, including execution order.
     */
    private void copyRuleEventAssociationsToGroup(Long ruleId, Long groupId) {
        // Get all rule-event associations
        LambdaQueryWrapper<ExecutionEventRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE.getId())
                .eq(ExecutionEventRelationEntity::getItemId, ruleId);
        List<ExecutionEventRelationEntity> ruleRelations = executionEventRelationMapper.selectList(queryWrapper);

        if (ruleRelations == null || ruleRelations.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis() / 1000;

        // Collect all event IDs from rule relations
        List<Long> eventIds = ruleRelations.stream()
                .map(ExecutionEventRelationEntity::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            return;
        }

        // Batch query existing group-event associations
        LambdaQueryWrapper<ExecutionEventRelationEntity> existingQueryWrapper = new LambdaQueryWrapper<>();
        existingQueryWrapper.eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE_GROUP.getId())
                .eq(ExecutionEventRelationEntity::getItemId, groupId)
                .in(ExecutionEventRelationEntity::getEventId, eventIds);
        List<ExecutionEventRelationEntity> existingGroupRelations = executionEventRelationMapper.selectList(existingQueryWrapper);

        // Build a set of existing event IDs for quick lookup
        Set<Long> existingEventIds = existingGroupRelations != null
                ? existingGroupRelations.stream()
                .map(ExecutionEventRelationEntity::getEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        // Collect new group-event associations to insert
        List<ExecutionEventRelationEntity> groupRelationsToInsert = new ArrayList<>();
        for (ExecutionEventRelationEntity ruleRelation : ruleRelations) {
            Long eventId = ruleRelation.getEventId();
            if (eventId == null || existingEventIds.contains(eventId)) {
                continue;
            }
            // Create new group-event association with same execution order
            ExecutionEventRelationEntity groupRelation = new ExecutionEventRelationEntity();
            groupRelation.setEventId(eventId);
            groupRelation.setItemType(ExecutionItemTypeEnum.RULE_GROUP.getId());
            groupRelation.setItemId(groupId);
            groupRelation.setExecutionOrder(ruleRelation.getExecutionOrder());
            groupRelation.setCt((int) currentTime);
            groupRelation.setUt((int) currentTime);
            groupRelationsToInsert.add(groupRelation);
        }

        // Batch insert new group-event associations
        if (!groupRelationsToInsert.isEmpty()) {
            for (ExecutionEventRelationEntity groupRelation : groupRelationsToInsert) {
                executionEventRelationMapper.insert(groupRelation);
            }
        }
    }

    /**
     * Business Logic 2: Copy group-event associations to rule-event associations.
     * When adding a rule to an existing group, copy all group's event associations
     * to the rule, including execution order.
     */
    private void copyGroupEventAssociationsToRule(Long groupId, Long ruleId) {
        // Get all group-event associations
        LambdaQueryWrapper<ExecutionEventRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE_GROUP.getId())
                .eq(ExecutionEventRelationEntity::getItemId, groupId);
        List<ExecutionEventRelationEntity> groupRelations = executionEventRelationMapper.selectList(queryWrapper);

        if (groupRelations == null || groupRelations.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis() / 1000;

        // Copy each association to rule
        List<ExecutionEventRelationEntity> ruleRelationsToInsert = new ArrayList<>();
        for (ExecutionEventRelationEntity groupRelation : groupRelations) {
            // Check if rule-event association already exists
            LambdaQueryWrapper<ExecutionEventRelationEntity> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(ExecutionEventRelationEntity::getEventId, groupRelation.getEventId())
                    .eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE.getId())
                    .eq(ExecutionEventRelationEntity::getItemId, ruleId);
            ExecutionEventRelationEntity existing = executionEventRelationMapper.selectOne(checkWrapper);

            if (existing != null) {
                continue;
            }
            // Create new rule-event association with same execution order
            ExecutionEventRelationEntity ruleRelation = new ExecutionEventRelationEntity();
            ruleRelation.setEventId(groupRelation.getEventId());
            ruleRelation.setItemType(ExecutionItemTypeEnum.RULE.getId());
            ruleRelation.setItemId(ruleId);
            ruleRelation.setExecutionOrder(groupRelation.getExecutionOrder());
            ruleRelation.setCt((int) currentTime);
            ruleRelation.setUt((int) currentTime);
            ruleRelationsToInsert.add(ruleRelation);
        }

        // Batch insert new rule-event associations
        if (!ruleRelationsToInsert.isEmpty()) {
            for (ExecutionEventRelationEntity ruleRelation : ruleRelationsToInsert) {
                executionEventRelationMapper.insert(ruleRelation);
            }
        }
    }

    /**
     * Set all other rules in the group to OFFLINE status.
     *
     * @param rule the rule that changed to FULL status
     */
    @Override
    @Transactional
    public void setOtherRulesOffline(@NotNull Rule rule) {
        if (rule.getId() == null) {
            log.warn("Rule ID is null, cannot set other rules offline");
            return;
        }

        Long groupId = rule.getRuleGroupId();
        if (groupId == null) {
            log.warn("Rule group ID is null for ruleId={}, cannot set other rules offline", rule.getId());
            return;
        }

        RuleGroup group = getRuleGroup(groupId);
        if (group == null) {
            log.warn("Rule group not found for groupId={}, cannot set other rules offline", groupId);
            return;
        }

        // Set all other rules in the group to OFFLINE
        for (Long ruleId : group.getRuleIds()) {
            if (ruleId.equals(rule.getId())) {
                continue;
            }
            Rule otherRule = ruleService.getRuleById(ruleId);
            if (otherRule == null || otherRule.getRuleStatus() != RuleStatusEnum.AB_TEST) {
                continue;
            }
            // Create a new Rule object with OFFLINE status for update
            Rule ruleToUpdate = Rule.builder()
                    .id(otherRule.getId())
                    .name(otherRule.getName())
                    .contentType(otherRule.getContentType())
                    .content(otherRule.getContent())
                    .description(otherRule.getDescription())
                    .ruleStatus(RuleStatusEnum.OFFLINE)
                    .ruleGroupId(0L)
                    .build();
            ruleToUpdate.setCreateTime(otherRule.getCreateTime());
            // Update rule status to OFFLINE in database without triggering status change handlers
            ruleService.doUpdateRule(otherRule, ruleToUpdate);
            log.info("Rule set to OFFLINE: ruleId={}, groupId={}", ruleId, groupId);
        }
    }

    /**
     * Delete rule group if it's empty.
     * Business Logic 4: When a rule group has no rules,
     * delete the group and its event associations.
     *
     * @param groupId the group ID to check and delete
     */
    @Override
    @Transactional
    public void deleteRuleGroupIfEmpty(Long groupId) {
        if (groupId == null) {
            return;
        }

        RuleGroup group = getRuleGroup(groupId);
        if (group == null || !group.isEmpty()) {
            return;
        }

        // Delete group-event associations
        LambdaQueryWrapper<ExecutionEventRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE_GROUP.getId())
                .eq(ExecutionEventRelationEntity::getItemId, groupId);
        executionEventRelationMapper.delete(deleteWrapper);

        // Delete rule group
        ruleGroupMapper.deleteById(groupId);

        log.info("Empty rule group deleted: groupId={}", groupId);
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
        Long eventId = context.getEventId();

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
     * Loads rules map for a rule group if not already loaded.
     *
     * @param group the rule group to load rules for
     */
    private void loadRulesMapForGroup(RuleGroup group) {
        List<Long> ruleIds = group.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            log.warn("Rule group has no rules: groupId={}", group.getId());
            group.setRules(Collections.emptyMap());
            return;
        }

        // Batch load ratios from relation table
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> ratioWrapper = new LambdaQueryWrapper<>();
        ratioWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, group.getId())
                .in(RuleGroupRuleRelationEntity::getRuleId, ruleIds);
        List<RuleGroupRuleRelationEntity> relationEntities = ruleGroupRuleRelationMapper.selectList(ratioWrapper);
        Map<Long, RuleGroupRuleRelationEntity> relationEntityMap = relationEntities != null
                ? relationEntities.stream()
                .collect(Collectors.toMap(RuleGroupRuleRelationEntity::getRuleId, relation -> relation, (existing, replacement) -> existing))
                : Collections.emptyMap();

        // Batch load rule entities
        List<RuleEntity> ruleEntities = ruleMapper.selectBatchIds(ruleIds);
        Map<Long, RuleEntity> ruleEntityMap = ruleEntities != null
                ? ruleEntities.stream()
                .collect(Collectors.toMap(RuleEntity::getId, entity -> entity))
                : Collections.emptyMap();

        // Build rules map: ruleId -> Pair<Rule, Integer>
        // Note: rule content is not loaded here, it will be loaded after rule selection
        Map<Long, Pair<Rule, Integer>> rulesMap = new HashMap<>();
        for (Long ruleId : ruleIds) {
            RuleGroupRuleRelationEntity relation = relationEntityMap.get(ruleId);
            int ratio = (relation != null && relation.getAbTestRatio() != null)
                    ? relation.getAbTestRatio() : 0;

            RuleEntity ruleEntity = ruleEntityMap.get(ruleId);
            if (ruleEntity == null || ruleEntity.getRuleStatusEnum() != RuleStatusEnum.AB_TEST) {
                log.warn("Rule in group is not in AB_TEST status or not found: groupId={}, ruleId={}", group.getId(), ruleId);
                continue;
            }

            Rule rule = ruleStructMapper.entityToModel(ruleEntity);
            if (rule == null) {
                log.warn("Rule entity could not be mapped to model: ruleId={}", ruleId);
                continue;
            }
            // Content is not set here, will be loaded after rule selection
            rulesMap.put(ruleId, Pair.of(rule, ratio));
        }

        group.setRules(rulesMap);
    }

    @Override
    @Transactional
    public void associateGroupWithEventId(Long groupId, Long eventId) {
        if (groupId == null || eventId == null) {
            return;
        }

        // Check if association already exists
        LambdaQueryWrapper<ExecutionEventRelationEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                .eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE_GROUP.getId())
                .eq(ExecutionEventRelationEntity::getItemId, groupId);
        ExecutionEventRelationEntity existing = executionEventRelationMapper.selectOne(checkWrapper);

        if (existing == null) {
            // Get max execution order for this event
            LambdaQueryWrapper<ExecutionEventRelationEntity> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                    .orderByDesc(ExecutionEventRelationEntity::getExecutionOrder)
                    .last("LIMIT 1");
            ExecutionEventRelationEntity lastRelation = executionEventRelationMapper.selectOne(orderWrapper);
            int executionOrder = lastRelation != null ? lastRelation.getExecutionOrder() + 1 : 1;

            // Create new association
            ExecutionEventRelationEntity relation = new ExecutionEventRelationEntity();
            relation.setEventId(eventId);
            relation.setItemType(ExecutionItemTypeEnum.RULE_GROUP.getId());
            relation.setItemId(groupId);
            relation.setExecutionOrder(executionOrder);
            long currentTime = System.currentTimeMillis() / 1000;
            relation.setCt((int) currentTime);
            relation.setUt((int) currentTime);
            executionEventRelationMapper.insert(relation);
        }

        log.debug("Rule group {} associated with eventId {}", groupId, eventId);
    }

    @Override
    @Transactional
    public void removeGroupEventIdAssociation(Long groupId, Long eventId) {
        if (groupId == null || eventId == null) {
            return;
        }

        LambdaQueryWrapper<ExecutionEventRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                .eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE_GROUP.getId())
                .eq(ExecutionEventRelationEntity::getItemId, groupId);
        executionEventRelationMapper.delete(deleteWrapper);

        log.debug("Rule group {} disassociated from eventId {}", groupId, eventId);
    }

    @Override
    public void validateAndCleanInvalidCache() {
        log.info("Starting cache validation and cleanup task");
        java.util.Set<String> cacheKeys = ruleSelectionCacheService.getAllCacheKeys();
        if (cacheKeys == null || cacheKeys.isEmpty()) {
            log.debug("No cache keys found for validation");
            return;
        }

        int totalKeys = cacheKeys.size();
        int invalidCount = 0;
        int validCount = 0;

        for (String cacheKey : cacheKeys) {
            try {
                // Parse cache key: rule:gw:abt:{userId}:{eventId}:{groupId}
                String prefix = "rule:gw:abt:";
                if (!cacheKey.startsWith(prefix)) {
                    log.warn("Invalid cache key format (missing prefix): {}", cacheKey);
                    continue;
                }
                String[] parts = cacheKey.substring(prefix.length()).split(":");
                if (parts.length != 3) {
                    log.warn("Invalid cache key format (wrong number of parts): {}", cacheKey);
                    continue;
                }

                Long userId = Long.parseLong(parts[0]);
                Long eventId = Long.parseLong(parts[1]);
                Long groupId = Long.parseLong(parts[2]);

                Long cachedRuleId = ruleSelectionCacheService.get(userId, eventId, groupId);
                if (cachedRuleId == null) {
                    continue;
                }

                RuleGroup group = getRuleGroup(groupId);
                if (group == null) {
                    // Group not found, remove cache entry
                    ruleSelectionCacheService.remove(userId, eventId, groupId);
                    invalidCount++;
                    log.debug("Removed invalid cache entry: group not found: userId={}, eventId={}, groupId={}, ruleId={}",
                            userId, eventId, groupId, cachedRuleId);
                    continue;
                }

                // Ensure rules map is loaded for validation
                if (group.getRules() == null || group.getRules().isEmpty()) {
                    loadRulesMapForGroup(group);
                }

                // Check if cached rule exists in the group's rules map
                // If it exists in the map, it means the rule is valid and in AB_TEST status
                if (!isCachedABTestRule(cachedRuleId, group)) {
                    // Invalid cache entry, remove it
                    ruleSelectionCacheService.remove(userId, eventId, groupId);
                    invalidCount++;
                    log.debug("Removed invalid cache entry: userId={}, eventId={}, groupId={}, ruleId={}",
                            userId, eventId, groupId, cachedRuleId);
                } else {
                    validCount++;
                }
            } catch (Exception e) {
                log.error("Error validating cache key: {}", cacheKey, e);
            }
        }

        log.info("Cache validation completed: total={}, valid={}, invalid={}", totalKeys, validCount, invalidCount);
    }

    /**
     * Check if a cached a/b test rule is valid
     * Validates by checking if the rule exists in the group's rules map.
     * If it exists in the map, it means the rule is valid and in AB_TEST status
     * (loadRulesMapForGroup already filters out non-AB_TEST rules).
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

        return cachedRule.getRuleStatus() == RuleStatusEnum.AB_TEST &&
                cachedRule.getRuleGroupId() != null &&
                cachedRule.getRuleGroupId().equals(group.getId());
    }

    /**
     * Add rule to group relation in database
     *
     * @param groupId     rule group ID
     * @param ruleId      rule ID
     * @param abTestRatio A/B test ratio (0-100), default 0 if null
     */
    private void addRuleToGroupRelation(Long groupId, Long ruleId, Integer abTestRatio) {
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId)
                .eq(RuleGroupRuleRelationEntity::getRuleId, ruleId);
        RuleGroupRuleRelationEntity existing = ruleGroupRuleRelationMapper.selectOne(checkWrapper);

        if (existing == null) {
            RuleGroupRuleRelationEntity relation = new RuleGroupRuleRelationEntity();
            relation.setGroupId(groupId);
            relation.setRuleId(ruleId);
            relation.setAbTestRatio(abTestRatio != null ? abTestRatio : 0);
            long currentTime = System.currentTimeMillis() / 1000;
            relation.setCt((int) currentTime);
            relation.setUt((int) currentTime);
            try {
                int insertResult = ruleGroupRuleRelationMapper.insert(relation);
                if (insertResult <= 0) {
                    log.error("Failed to insert rule-group relation: groupId={}, ruleId={}, insertResult={}", groupId, ruleId, insertResult);
                    throw new IllegalStateException("Failed to insert rule-group relation: groupId=" + groupId + ", ruleId=" + ruleId);
                }
                log.debug("Successfully inserted rule-group relation: groupId={}, ruleId={}", groupId, ruleId);
            } catch (Exception e) {
                log.error("Exception while inserting rule-group relation: groupId={}, ruleId={}", groupId, ruleId, e);
                throw e;
            }
        } else {
            // Update ratio if provided
            if (abTestRatio != null) {
                existing.setAbTestRatio(abTestRatio);
                existing.setUt((int) (System.currentTimeMillis() / 1000));
                // For composite primary key, use update with query wrapper instead of updateById
                LambdaQueryWrapper<RuleGroupRuleRelationEntity> updateWrapper = new LambdaQueryWrapper<>();
                updateWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId)
                        .eq(RuleGroupRuleRelationEntity::getRuleId, ruleId);
                ruleGroupRuleRelationMapper.update(existing, updateWrapper);
            }
        }
    }
}
