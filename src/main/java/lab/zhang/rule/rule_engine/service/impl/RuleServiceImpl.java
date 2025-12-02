package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lab.zhang.rule.rule_engine.engine.ExecutionItem;
import lab.zhang.rule.rule_engine.entity.*;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.mapper.*;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper;
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper;
import lab.zhang.rule.rule_engine.config.RuleStatusConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule service implementation
 * Uses MyBatis Plus for database operations
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class RuleServiceImpl implements RuleService {

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private RuleContentMapper ruleContentMapper;

    @Autowired
    private ExecutionEventRelationMapper executionEventRelationMapper;

    @Autowired
    private RuleGroupMapper ruleGroupMapper;

    @Autowired
    private RuleGroupRuleRelationMapper ruleGroupRuleRelationMapper;

    @Autowired
    private RuleGroupStructMapper ruleGroupStructMapper;

    @Autowired
    @Lazy
    private RuleGroupService ruleGroupService;

    @Autowired(required = false)
    @Lazy
    private List<RuleExecutor> ruleExecutors;

    @Autowired
    private RuleStatusConfig ruleStatusConfig;


    @Override
    public List<Rule> getAllRules() {
        List<RuleEntity> entities = ruleMapper.selectList(null);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(ruleStructMapper::entityToModel)
                .collect(Collectors.toList());
    }

    @Autowired
    private RuleStructMapper ruleStructMapper;

    /**
     * Gets a rule by its ID.
     *
     * @param ruleId the rule ID, must not be null
     * @return the rule, or null if not found
     * @throws IllegalArgumentException if ruleId is null
     */
    @Override
    public Rule getRuleById(Long ruleId) {
        RuleEntity ruleEntity = ruleMapper.selectById(ruleId);
        if (ruleEntity == null) {
            return null;
        }

        Rule rule = ruleStructMapper.entityToModel(ruleEntity);

        // Load rule content
        RuleContentEntity contentEntity = ruleContentMapper.selectById(ruleId);
        if (contentEntity != null && contentEntity.getContent() != null) {
            rule.setContent(contentEntity.getContent());
        } else {
            rule.setContent("");
        }

        return rule;
    }


    @Override
    public void createRule(Rule rule) {
        rule.setRuleStatus(RuleStatusEnum.OFFLINE); // New rules are created as OFFLINE
        RuleEntity ruleEntity = ruleStructMapper.modelToEntity(rule);
        ruleEntity.setTimeOnCreate();
        ruleMapper.insert(ruleEntity);

        // Update rule.id with the generated ID from database
        Long ruleId = ruleEntity.getId();
        rule.setId(ruleId);

        // skip if no content provided
        if (rule.getContent() == null) {
            log.info("Rule created without content: ruleId={}, ruleName={}, status=OFFLINE",
                    ruleEntity.getId(), ruleEntity.getName());
            return;
        }

        RuleContentEntity contentEntity = ruleStructMapper.modelToContentEntity(rule);
        // Ensure contentEntity.id is set to the generated rule ID
        contentEntity.setId(ruleId);
        contentEntity.setTimeOnCreate();
        ruleContentMapper.insert(contentEntity);

        log.info("Rule created: ruleId={}, ruleName={}, status=OFFLINE",
                ruleEntity.getId(), ruleEntity.getName());
    }

    @Override
    @Transactional
    public void updateRule(Long ruleId, Rule newRule) {
        // Load existing rule from database to get latest ruleGroupId
        RuleEntity existingEntity = ruleMapper.selectById(ruleId);
        if (existingEntity == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }
        Rule existingRule = ruleStructMapper.entityToModel(existingEntity);

        // Check on status transition
        RuleStatusEnum oldStatus = existingRule.getRuleStatus();
        RuleStatusEnum newStatus = newRule.getRuleStatus();
        // If rule status is not provided, keep the old status
        if (newStatus == null) {
            newRule.setRuleStatus(oldStatus);
            newStatus = oldStatus;
        }
        if (oldStatus != newStatus && !oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s. Allowed transitions from %s: %s",
                            oldStatus, newStatus, oldStatus, oldStatus.getAllowedTargetStatuses()));
        }

        // Handle rule status change and get ruleGroupId directly from the method return
        // This avoids the need to re-query the database, solving concurrency consistency issues
        Long ruleGroupId = null;
        if (oldStatus != newStatus) {
            ruleGroupId = handleRuleStatusChange(existingRule, newRule);
            // Set ruleGroupId from handleRuleStatusChange return value
            // Rule was removed from group (AB_TEST -> other status), set to null
            if (ruleGroupId != null) {
                newRule.setRuleGroupId(ruleGroupId);
            }
        }

        newRule.setId(existingRule.getId());
        // Save user-provided values before copying from existingRule
        // Use a special marker to indicate that we're tracking original values
        ContentTypeEnum originalContentType = newRule.getContentType();
        String originalContent = newRule.getContent();
        // Use a special marker object to distinguish between "user provided null" and "not called from updateRule"
        final ContentTypeEnum MARKER_CONTENT_TYPE = ContentTypeEnum.EXPRESSION; // Use a sentinel value that won't conflict
        if (newRule.getName() == null || newRule.getName().trim().isEmpty()) {
            newRule.setName(existingRule.getName());
        }
        if (newRule.getDescription() == null) {
            newRule.setDescription(existingRule.getDescription());
        }
        if (newRule.getContentType() == null) {
            newRule.setContentType(existingRule.getContentType());
        }
        if (newRule.getContent() == null) {
            newRule.setContent(existingRule.getContent());
        }
        newRule.setCreateTime(existingRule.getCreateTime());
        // Use doUpdateRule to perform the actual database update
        // Pass original user-provided values to determine if validation is needed
        // If originalContentType is null, pass a special marker to indicate "called from updateRule but user didn't provide"
        doUpdateRule(existingRule, newRule, originalContentType, originalContent, true);
    }

    /**
     * Update rule in database without handling status change logic.
     * This is a pure database update method that does not trigger status change handlers.
     *
     * @param existingRule the existing rule
     * @param newRule      the new rule data
     * @throws IllegalArgumentException if rule not found
     */
    @Override
    @Transactional
    public void doUpdateRule(Rule existingRule, Rule newRule) {
        // Call the overloaded method with null original values and fromUpdateRule=false
        // This indicates direct call (not from updateRule), use original logic
        doUpdateRule(existingRule, newRule, null, null, false);
    }

    /**
     * Internal method to update rule with original user-provided values.
     * This allows us to distinguish between user-provided values and values copied from existingRule.
     *
     * @param existingRule the existing rule
     * @param newRule the new rule data (may have been modified by copying from existingRule)
     * @param originalContentType the original contentType provided by user (null if not provided)
     * @param originalContent the original content provided by user (null if not provided)
     * @param fromUpdateRule true if called from updateRule, false if called directly
     */
    private void doUpdateRule(Rule existingRule, Rule newRule, ContentTypeEnum originalContentType, String originalContent, boolean fromUpdateRule) {
        // Validate contentType and content before updating
        // Only update contentType and content if both are provided and both are valid
        boolean shouldUpdateContent = false;
        
        // Determine if user provided contentType and content
        boolean userProvidedContentType;
        boolean userProvidedContent;
        
        if (fromUpdateRule) {
            // Called from updateRule
            // User provided contentType if originalContentType is not null
            userProvidedContentType = originalContentType != null;
            // User provided content if originalContent is not null and not empty
            userProvidedContent = originalContent != null && !originalContent.trim().isEmpty();
        } else {
            // Called directly (not from updateRule), use original logic:
            // If both contentType and content are provided, validate both
            userProvidedContentType = newRule.getContentType() != null;
            userProvidedContent = newRule.getContent() != null && !newRule.getContent().trim().isEmpty();
        }
        
        // If both contentType and content are provided by user, validate both
        if (userProvidedContentType && userProvidedContent) {
            // Validate contentType
            ContentTypeEnum validatedContentType = ContentTypeEnum.fromId(newRule.getContentType().getId());
            // Find the corresponding RuleExecutor based on contentType
            RuleExecutor executor = findRuleExecutor(validatedContentType);
            if (executor == null) {
                throw new IllegalArgumentException("No executor found for content type: " + validatedContentType);
            }
            // Validate content using RuleExecutor
            executor.validate(newRule.getContent());
            // Both are valid, can update both
            shouldUpdateContent = true;
        }

        // Build entity for update
        RuleEntity newEntity = ruleStructMapper.modelToEntity(newRule);
        // Set ID for updateById (mapper ignores ID field)
        newEntity.setId(newRule.getId());
        if (shouldUpdateContent) {
            newEntity.setContentTypeEnum(ContentTypeEnum.fromId(newRule.getContentType().getId()));
        } else {
            newEntity.setContentTypeEnum(existingRule.getContentType());
        }
        newEntity.setTimeOnUpdate();
        ruleMapper.updateById(newEntity);

        // Only update content if validation passed
        if (shouldUpdateContent) {
            RuleContentEntity contentEntity = ruleStructMapper.modelToContentEntity(newRule);
            contentEntity.setTimeOnUpdate();
            ruleContentMapper.updateById(contentEntity);
        }

        log.info("Rule updated: ruleId={}, ruleName={}, status={}",
                newEntity.getId(), newEntity.getName(), newRule.getRuleStatus());
    }

    /**
     * Delete a rule.
     * Only rules with OFFLINE status can be deleted.
     *
     * @param ruleId the rule ID to delete
     * @throws IllegalArgumentException if ruleId is null, rule not found, or rule is not in OFFLINE status
     */
    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        Rule rule = getRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }

        if (rule.getRuleStatus() != RuleStatusEnum.OFFLINE) {
            throw new IllegalArgumentException(
                    String.format("Rule can only be deleted when status is OFFLINE. Current status: %s", rule.getRuleStatus()));
        }

        // Delete rule content
        ruleContentMapper.deleteById(ruleId);

        // Delete rule entity
        ruleMapper.deleteById(ruleId);

        // Delete execution event relations for this rule
        LambdaQueryWrapper<ExecutionEventRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE.getId())
                .eq(ExecutionEventRelationEntity::getItemId, ruleId);
        executionEventRelationMapper.delete(deleteWrapper);

        log.info("Rule deleted: ruleId={}, ruleName={}", ruleId, rule.getName());
    }

    /**
     * Handle rule status change and update rule groups accordingly
     *
     * @param oldRule the old rule state
     * @param newRule the new rule state
     * @return the ruleGroupId if rule is added to a group, null if removed from group or no group change
     */
    private Long handleRuleStatusChange(Rule oldRule, Rule newRule) {
        RuleStatusEnum oldStatus = oldRule.getRuleStatus();
        RuleStatusEnum newStatus = newRule.getRuleStatus();

        // Case 1: Rule changes from other status to AB_TEST
        // Business Logic 1 is handled in RuleGroupService.createRuleGroup() and addRuleToGroup()
        if (oldStatus != RuleStatusEnum.AB_TEST && newStatus == RuleStatusEnum.AB_TEST) {
            return changeRuleStatusToABTest(newRule);
        }

        // Case 2: Rule changes from AB_TEST to other status (OFFLINE, TEST, FULL)
        // Business Logic 3 and 4 are handled in removeRuleFromGroup()
        if (oldStatus == RuleStatusEnum.AB_TEST && newStatus != RuleStatusEnum.AB_TEST) {
            return changeRuleStatusFromABTest(oldRule, newRule, newStatus);
        }

        // No status change or no group change, return null
        return null;
    }

    private Long changeRuleStatusToABTest(Rule newRule) {
        if (newRule.getRuleGroupId() != null && newRule.getRuleGroupId() != 0L) {
            // Check if the group actually exists before trying to add to it
            // If group doesn't exist (e.g., was deleted when rule changed from AB_TEST to FULL),
            // create a new group instead
            RuleGroupEntity groupEntity = ruleGroupMapper.selectById(newRule.getRuleGroupId());
            if (groupEntity != null) {
                // Add to existing group (Business Logic 2 is handled in addRuleToGroup)
                ruleGroupService.addRuleToGroup(newRule, newRule.getRuleGroupId());
                // Return the groupId directly from the parameter
                return newRule.getRuleGroupId();
            } else {
                // Group doesn't exist, clear ruleGroupId and create new group
                newRule.setRuleGroupId(null);
                RuleGroup createdGroup = ruleGroupService.createRuleGroup(newRule);
                // Return the newly created group's ID
                return createdGroup != null ? createdGroup.getId() : null;
            }
        } else {
            // Create new group (Business Logic 1 is handled in createRuleGroup)
            RuleGroup createdGroup = ruleGroupService.createRuleGroup(newRule);
            // Return the newly created group's ID
            return createdGroup != null ? createdGroup.getId() : null;
        }
    }

    private Long changeRuleStatusFromABTest(Rule oldRule, Rule newRule, RuleStatusEnum newStatus) {
        // Validate target status
        if (newStatus != RuleStatusEnum.OFFLINE && newStatus != RuleStatusEnum.FULL) {
            throw new IllegalArgumentException(String.format("Invalid status transition from AB_TEST to %s. Allowed target statuses: OFFLINE, FULL", newStatus));
        }
        // Set other rules in group to OFFLINE
        if (newStatus == RuleStatusEnum.FULL) {
            ruleGroupService.setOtherRulesOffline(oldRule);
        }
        // Delete the rule group and all its associations
        ruleGroupService.doDeleteRuleGroup(oldRule.getRuleGroupId());
        // Return 0 to indicate rule was removed from group
        return RuleGroup.DEFAULT_RULE_GROUP_ID;
    }

    @Override
    @Transactional
    public void saveExecutionSequence(Long eventId, List<Long> ruleIds) {
        if (eventId == null) {
            throw new IllegalArgumentException("EventId cannot be null");
        }

        // Get old relations from database
        LambdaQueryWrapper<ExecutionEventRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                .eq(ExecutionEventRelationEntity::getItemType, ExecutionItemTypeEnum.RULE.getId());
        List<ExecutionEventRelationEntity> oldRelations = executionEventRelationMapper.selectList(queryWrapper);

        Set<Long> oldRuleIds = oldRelations != null
                ? oldRelations.stream().map(ExecutionEventRelationEntity::getItemId).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<Long> newRuleIds = ruleIds != null ? new HashSet<>(ruleIds) : Collections.emptySet();

        // Remove eventId associations for rules that are no longer in the sequence
        for (Long ruleId : oldRuleIds) {
            if (!newRuleIds.contains(ruleId)) {
                Rule rule = getRuleById(ruleId);
                if (rule != null && rule.getRuleStatus() == RuleStatusEnum.AB_TEST && rule.getRuleGroupId() != null) {
                    // Remove group-eventId association
                    ruleGroupService.removeGroupEventIdAssociation(rule.getRuleGroupId(), eventId);
                }
            }
        }

        // Delete old relations for this eventId
        LambdaQueryWrapper<ExecutionEventRelationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId);
        executionEventRelationMapper.delete(deleteWrapper);

        // Insert new relations
        if (ruleIds != null && !ruleIds.isEmpty()) {
            long currentTime = System.currentTimeMillis() / 1000;
            for (int i = 0; i < ruleIds.size(); i++) {
                ExecutionEventRelationEntity relationEntity = new ExecutionEventRelationEntity();
                relationEntity.setEventId(eventId);
                relationEntity.setItemType(ExecutionItemTypeEnum.RULE.getId());
                relationEntity.setItemId(ruleIds.get(i));
                relationEntity.setExecutionOrder(i + 1);
                relationEntity.setCt((int) currentTime);
                relationEntity.setUt((int) currentTime);
                executionEventRelationMapper.insert(relationEntity);

                // Add eventId associations for new rules
                Long ruleId = ruleIds.get(i);
                if (!oldRuleIds.contains(ruleId)) {
                    Rule rule = getRuleById(ruleId);
                    if (rule != null) {
                        // If rule is in AB_TEST status and has a group, also associate the group
                        if (rule.getRuleStatus() == RuleStatusEnum.AB_TEST && rule.getRuleGroupId() != null) {
                            ruleGroupService.associateGroupWithEventId(rule.getRuleGroupId(), eventId);
                        }
                    }
                }
            }
        }

        log.info("Execution event relation saved: eventId={}, ruleIds={}", eventId, ruleIds);
    }

    /**
     * Find RuleExecutor by ContentTypeEnum
     *
     * @param contentType content type enum
     * @return RuleExecutor for the content type, or null if not found
     */
    private RuleExecutor findRuleExecutor(ContentTypeEnum contentType) {
        if (ruleExecutors == null || contentType == null) {
            return null;
        }
        return ruleExecutors.stream()
                .filter(executor -> executor.getSupportedRuleType() == contentType)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ExecutionItem> getExecutionItemsByEventId(Long eventId) {
        // Query execution event relations from database
        LambdaQueryWrapper<ExecutionEventRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExecutionEventRelationEntity::getEventId, eventId)
                .orderByAsc(ExecutionEventRelationEntity::getExecutionOrder);
        List<ExecutionEventRelationEntity> relationEntities = executionEventRelationMapper.selectList(queryWrapper);
        if (relationEntities == null || relationEntities.isEmpty()) {
            log.warn("No execution event relations found for eventId: {}", eventId);
            return Collections.emptyList();
        }

        // Collect rule IDs and group IDs
        Set<Long> ruleIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        for (ExecutionEventRelationEntity relationEntity : relationEntities) {
            ExecutionItemTypeEnum itemType = relationEntity.getItemTypeEnum();
            Long itemId = relationEntity.getItemId();
            if (itemType == ExecutionItemTypeEnum.RULE) {
                ruleIds.add(itemId);
            } else if (itemType == ExecutionItemTypeEnum.RULE_GROUP) {
                groupIds.add(itemId);
            }
        }

        // Get allowed rule statuses (used for both rules and rule groups)
        Set<RuleStatusEnum> allowedStatuses = ruleStatusConfig.getAllowedRuleStatuses();

        // Batch load rules from database
        Map<Long, Rule> ruleMap = new HashMap<>();
        if (!ruleIds.isEmpty()) {
            ruleMap = getRulesOnStatus(ruleIds, allowedStatuses);
        }

        // Batch load rule groups from database
        Map<Long, RuleGroup> groupMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            groupMap = getRuleGroupsWithRules(groupIds, allowedStatuses);
        }

        // Process each relation entity and build execution items
        List<ExecutionItem> items = new ArrayList<>();
        for (ExecutionEventRelationEntity relationEntity : relationEntities) {
            ExecutionItemTypeEnum itemType = relationEntity.getItemTypeEnum();
            Long itemId = relationEntity.getItemId();

            if (itemType == ExecutionItemTypeEnum.RULE) {
                Rule rule = ruleMap.get(itemId);
                // Skip rules that are not in ruleMap (filtered out by status)
                if (rule == null) {
                    continue;
                }
                items.add(ExecutionItem.forRule(rule));
            } else if (itemType == ExecutionItemTypeEnum.RULE_GROUP) {
                // Direct rule group reference
                RuleGroup group = groupMap.get(itemId);
                if (group == null) {
                    continue;
                }
                items.add(ExecutionItem.forRuleGroup(group));
            }
        }

        return items;
    }

    private Map<Long, Rule> getRulesOnStatus(Set<Long> ruleIds, Set<RuleStatusEnum> allowedStatuses) {
        // Batch load rule entities
        List<RuleEntity> ruleEntities = ruleMapper.selectBatchIds(new ArrayList<>(ruleIds));
        if (ruleEntities == null || ruleEntities.isEmpty()) {
            log.warn("No rules found for IDs: {}", ruleIds);
            return new HashMap<>();
        }
        // Filter rule entities by allowed statuses
        List<RuleEntity> allowedRuleEntities = ruleEntities.stream()
                .filter(entity -> allowedStatuses.contains(entity.getRuleStatusEnum()))
                .collect(Collectors.toList());
        if (allowedRuleEntities.isEmpty()) {
            log.warn("No rules found with allowed statuses: {} for IDs: {}", allowedStatuses, ruleIds);
            return new HashMap<>();
        }

        // Get allowed rule IDs
        Set<Long> allowedRuleIds = allowedRuleEntities.stream()
                .map(RuleEntity::getId)
                .collect(Collectors.toSet());

        // Batch load rule contents
        Map<Long, String> contentMap = batchGetContentMap(allowedRuleIds);

        return buildRuleMap(allowedRuleEntities, contentMap);
    }

    private Map<Long, RuleGroup> getRuleGroupsWithRules(Set<Long> groupIds, Set<RuleStatusEnum> allowedStatuses) {
        // Batch load rule group entities
        List<RuleGroupEntity> groupEntities = ruleGroupMapper.selectBatchIds(new ArrayList<>(groupIds));
        if (groupEntities == null || groupEntities.isEmpty()) {
            log.warn("No rule groups found for IDs: {}", groupIds);
            return new HashMap<>();
        }
        // Batch load rule group relations
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> relationQueryWrapper = new LambdaQueryWrapper<>();
        relationQueryWrapper.in(RuleGroupRuleRelationEntity::getGroupId, groupIds);
        List<RuleGroupRuleRelationEntity> allRelations = ruleGroupRuleRelationMapper.selectList(relationQueryWrapper);
        // Build relations map: groupId -> List<RuleGroupRuleRelationEntity>
        Map<Long, List<RuleGroupRuleRelationEntity>> relationsMap = allRelations != null
                ? allRelations.stream()
                .collect(Collectors.groupingBy(RuleGroupRuleRelationEntity::getGroupId))
                : Collections.emptyMap();

        // Collect all rule IDs from all groups
        Set<Long> allRuleIds = allRelations != null
                ? allRelations.stream()
                .map(RuleGroupRuleRelationEntity::getRuleId)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        // Batch load rule entities and contents, filtered by allowed statuses
        Map<Long, Rule> ruleMap = new HashMap<>();
        if (!allRuleIds.isEmpty()) {
            ruleMap = getRulesOnStatus(allRuleIds, allowedStatuses);
        }

        Map<Long, RuleGroup> groupMap = new HashMap<>();
        // Convert entities to models and set rules map
        for (RuleGroupEntity groupEntity : groupEntities) {
            Map<Long, List<RuleGroupRuleRelationEntity>> groupRelationsMap = new HashMap<>();
            List<RuleGroupRuleRelationEntity> groupRelations = relationsMap.getOrDefault(groupEntity.getId(), Collections.emptyList());
            if (!groupRelations.isEmpty()) {
                groupRelationsMap.put(groupEntity.getId(), groupRelations);
            }
            RuleGroup group = ruleGroupStructMapper.entityToModel(groupEntity, groupRelationsMap);
            if (group == null) {
                continue;
            }
            // Build rules map: ruleId -> Pair<Rule, Integer>
            Map<Long, Pair<Rule, Integer>> rulesMap = new HashMap<>();
            if (!groupRelations.isEmpty()) {
                for (RuleGroupRuleRelationEntity relation : groupRelations) {
                    Long ruleId = relation.getRuleId();
                    Rule rule = ruleMap.get(ruleId);
                    if (rule == null) {
                        continue;
                    }
                    Integer ratio = relation.getAbTestRatio() != null ? relation.getAbTestRatio() : 0;
                    rulesMap.put(ruleId, Pair.of(rule, ratio));
                }
            }
            group.setRules(rulesMap);
            groupMap.put(groupEntity.getId(), group);
        }

        return groupMap;
    }

    private Map<Long, String> batchGetContentMap(Set<Long> allowedRuleIds) {
        List<RuleContentEntity> contentEntities = ruleContentMapper.selectBatchIds(new ArrayList<>(allowedRuleIds));
        return contentEntities != null
                ? contentEntities.stream()
                .filter(content -> content != null && content.getContent() != null)
                .collect(Collectors.toMap(RuleContentEntity::getId, RuleContentEntity::getContent))
                : Collections.emptyMap();
    }

    private Map<Long, Rule> buildRuleMap(List<RuleEntity> ruleEntities, Map<Long, String> contentMap) {
        Map<Long, Rule> ruleMap = new HashMap<>();
        for (RuleEntity ruleEntity : ruleEntities) {
            Rule rule = ruleStructMapper.entityToModel(ruleEntity);
            if (rule == null) {
                continue;
            }
            String content = contentMap.getOrDefault(ruleEntity.getId(), "");
            rule.setContent(content);
            ruleMap.put(rule.getId(), rule);
        }
        return ruleMap;
    }
}

