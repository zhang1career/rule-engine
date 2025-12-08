package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lab.zhang.rule.rule_engine.cache.RuleContentCacheService;
import lab.zhang.rule.rule_engine.cache.RuleSelectionCacheService;
import lab.zhang.rule.rule_engine.engine.ExecutionItem;
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity;
import lab.zhang.rule.rule_engine.entity.RuleContentEntity;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper;
import lab.zhang.rule.rule_engine.mapper.RuleContentMapper;
import lab.zhang.rule.rule_engine.mapper.RuleMapper;
import lab.zhang.rule.rule_engine.model.ExecutionArrangement;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.service.EventService;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper;
import lab.zhang.rule.rule_engine.util.TimeUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static lab.zhang.rule.rule_engine.constant.EvalArgumentConst.ARG_USER_HASH_INT;
import static lab.zhang.rule.rule_engine.model.RuleGroup.DEFAULT_RULE_GROUP_ID;

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
    private ExecutionArrangementMapper executionArrangementMapper;

    @Autowired
    private EventService eventService;

    @Autowired
    @Lazy
    private RuleGroupService ruleGroupService;

    @Autowired(required = false)
    @Lazy
    private List<RuleExecutor> ruleExecutors;

    @Autowired(required = false)
    private RuleSelectionCacheService ruleSelectionCacheService;

    @Autowired(required = false)
    private RuleContentCacheService ruleContentCacheService;

    @Value("${rule.content.cache.enabled:true}")
    private boolean ruleContentCacheEnabled;


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
    public void doCreateRule(Rule rule, long currentTimeSeconds) {
        // Set default status to OFFLINE if not provided
        if (rule.getRuleStatus() == null) {
            rule.setRuleStatus(RuleStatusEnum.OFFLINE);
        }

        RuleEntity ruleEntity = ruleStructMapper.modelToEntity(rule);
        if (currentTimeSeconds != 0) {
            ruleEntity.setCt((int) currentTimeSeconds);
            ruleEntity.setUt((int) currentTimeSeconds);
        } else {
            ruleEntity.setTimeOnCreate();
        }
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
    public void createRule(Rule rule) {
        doCreateRule(rule, 0);
    }

    @Override
    @Transactional
    public void updateRule(Long ruleId, Rule newRule) {
        // Load existing rule from database
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
        if (oldStatus != newStatus && !oldStatus.canChangeTo(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s. Allowed transitions from %s: %s",
                            oldStatus, newStatus, oldStatus, oldStatus.getAllowedTargetStatuses()));
        }

        // Validate that rule must be associated with at least one event before transitioning to TEST/GRAY/ONLINE status
        if (newStatus == RuleStatusEnum.TEST || newStatus == RuleStatusEnum.GRAY || newStatus == RuleStatusEnum.ONLINE) {
            LambdaQueryWrapper<ExecutionArrangementEntity> arrangementQuery = new LambdaQueryWrapper<>();
            arrangementQuery.eq(ExecutionArrangementEntity::getRuleId, ruleId);
            List<ExecutionArrangementEntity> arrangementList = executionArrangementMapper.selectList(arrangementQuery);
            if (arrangementList == null || arrangementList.isEmpty()) {
                throw new IllegalStateException("Rule must be associated with at least one event before transitioning to " + newStatus + " status");
            }
        }

        // Handle rule status change
        if (oldStatus != newStatus) {
            changeRuleStatusAboutOnline(existingRule, newRule);
        }

        newRule.setId(existingRule.getId());
        // Save user-provided values before copying from existingRule
        // Use a special marker to indicate that we're tracking original values
        ContentTypeEnum originalContentType = newRule.getContentType();
        String originalContent = newRule.getContent();
        // Use a special marker object to distinguish between "user provided null" and "not called from updateRule"
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
     * @param existingRule        the existing rule
     * @param newRule             the new rule data (may have been modified by copying from existingRule)
     * @param originalContentType the original contentType provided by user (null if not provided)
     * @param originalContent     the original content provided by user (null if not provided)
     * @param fromUpdateRule      true if called from updateRule, false if called directly
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
        LambdaQueryWrapper<ExecutionArrangementEntity> arrangementWrapper = new LambdaQueryWrapper<>();
        arrangementWrapper.eq(ExecutionArrangementEntity::getRuleId, ruleId);
        executionArrangementMapper.delete(arrangementWrapper);

        log.info("Rule deleted: ruleId={}, ruleName={}", ruleId, rule.getName());
    }

    /**
     * Handle rule status about online. Change and update rule groups accordingly
     *
     * @param oldRule the old rule state
     * @param newRule the new rule state
     */
    private void changeRuleStatusAboutOnline(Rule oldRule, Rule newRule) {
        RuleStatusEnum oldStatus = oldRule.getRuleStatus();
        RuleStatusEnum newStatus = newRule.getRuleStatus();

        // Case 1: Rule changes from other status to ONLINE
        // When a rule becomes ONLINE, create a rule group for each event associated with the rule
        if (oldStatus != RuleStatusEnum.ONLINE && newStatus == RuleStatusEnum.ONLINE) {
            changeRuleStatusToOnline(newRule);
            return;
        }

        // Case 2: Rule changes from ONLINE to any other status (not ONLINE)
        // When a rule leaves ONLINE status, remove it from rule groups
        // For OFFLINE, also delete event associations
        if (oldStatus == RuleStatusEnum.ONLINE && newStatus != RuleStatusEnum.ONLINE) {
            if (newStatus == RuleStatusEnum.OFFLINE) {
                changeRuleStatusFromOnlineToOffline(oldRule);
            } else {
                // For other statuses (TEST, GRAY), just remove from groups but keep event associations
                changeRuleStatusFromOnlineToOther(oldRule);
            }
        }
    }

    private void changeRuleStatusToOnline(Rule newRule) {
        // Get all event IDs associated with this rule (with group_id = 0, standalone rules)
        LambdaQueryWrapper<ExecutionArrangementEntity> singleArrangementWrapper = new LambdaQueryWrapper<>();
        singleArrangementWrapper.eq(ExecutionArrangementEntity::getGroupId, 0)
                .eq(ExecutionArrangementEntity::getRuleId, newRule.getId());
        List<ExecutionArrangementEntity> singleArrangementEntityList = executionArrangementMapper.selectList(singleArrangementWrapper);
        if (singleArrangementEntityList == null || singleArrangementEntityList.isEmpty()) {
            throw new IllegalArgumentException("Rule must be associated with at least one event before transitioning to ONLINE status");
        }

        // Create rule group for each event
        for (ExecutionArrangementEntity singleArrangementEntity : singleArrangementEntityList) {
            Integer eventId = singleArrangementEntity.getEventId();
            if (eventId == null) {
                log.warn("Skipping arrangement with null eventId for rule {}", newRule.getId());
                continue;
            }
            // create rule group
            RuleGroup ruleGroup = ruleGroupService.createRuleGroup(newRule, eventId);
            // update execution arrangement
            singleArrangementEntity.setGroupId(ruleGroup.getId());
            singleArrangementEntity.setTimeOnUpdate();
            executionArrangementMapper.updateByPrimaryKey(singleArrangementEntity);
        }
    }

    private void changeRuleStatusFromOnlineToOffline(Rule oldRule) {
        // Get all records from table x associated with this rule (with non-zero group_id)
        LambdaQueryWrapper<ExecutionArrangementEntity> groupedArrangementWrapper = new LambdaQueryWrapper<>();
        groupedArrangementWrapper.eq(ExecutionArrangementEntity::getRuleId, oldRule.getId())
                .ne(ExecutionArrangementEntity::getGroupId, 0);
        List<ExecutionArrangementEntity> relations = executionArrangementMapper.selectList(groupedArrangementWrapper);

        if (relations != null && !relations.isEmpty()) {
            doChangeRuleStatusFromOnline(relations);
        }

        // Delete all event-rule associations (with group_id = 0)
        LambdaQueryWrapper<ExecutionArrangementEntity> singleArrangementWrapper = new LambdaQueryWrapper<>();
        singleArrangementWrapper.eq(ExecutionArrangementEntity::getGroupId, 0)
                .eq(ExecutionArrangementEntity::getRuleId, oldRule.getId());
        executionArrangementMapper.delete(singleArrangementWrapper);
    }

    /**
     * Handle rule status change from ONLINE to other status (TEST, GRAY, etc.)
     * Removes the rule from rule groups but keeps event associations
     *
     * @param oldRule the old rule state
     */
    private void changeRuleStatusFromOnlineToOther(Rule oldRule) {
        log.info("Removing rule {} from groups (transitioning from ONLINE to other status)", oldRule.getId());
        // Get all records from table x associated with this rule (with non-zero group_id)
        LambdaQueryWrapper<ExecutionArrangementEntity> groupedArrangementWrapper = new LambdaQueryWrapper<>();
        groupedArrangementWrapper.eq(ExecutionArrangementEntity::getRuleId, oldRule.getId())
                .ne(ExecutionArrangementEntity::getGroupId, 0);
        List<ExecutionArrangementEntity> relations = executionArrangementMapper.selectList(groupedArrangementWrapper);
        log.info("Found {} relations with non-zero group_id for rule {}",
                relations != null ? relations.size() : 0, oldRule.getId());

        if (relations == null || relations.isEmpty()) {
            return;
        }
        doChangeRuleStatusFromOnline(relations);
    }

    private void doChangeRuleStatusFromOnline(List<ExecutionArrangementEntity> arrangementEntityList) {
        // Group by groupId to handle each group
        Map<Long, List<ExecutionArrangementEntity>> arrangementEntityMap = arrangementEntityList.stream()
                .collect(Collectors.groupingBy(ExecutionArrangementEntity::getGroupId));

        for (Map.Entry<Long, List<ExecutionArrangementEntity>> entry : arrangementEntityMap.entrySet()) {
            Long groupId = entry.getKey();
            // Update records in table x: set group_id from groupId to 0 (standalone rules)
            for (ExecutionArrangementEntity arrangementEntity : entry.getValue()) {
                LambdaUpdateWrapper<ExecutionArrangementEntity> arrangementWrapper = new LambdaUpdateWrapper<>();
                arrangementWrapper.eq(ExecutionArrangementEntity::getEventId, arrangementEntity.getEventId())
                        .eq(ExecutionArrangementEntity::getRuleId, arrangementEntity.getRuleId())
                        .set(ExecutionArrangementEntity::getGroupId, 0L)
                        .set(ExecutionArrangementEntity::getAbRatio, 0)
                        .set(ExecutionArrangementEntity::getUt, (int) TimeUtil.getCurrentTime());
                executionArrangementMapper.update(null, arrangementWrapper);
            }

            // Check if group is empty, if so, delete the group
            RuleGroup group = ruleGroupService.getRuleGroup(groupId);
            if (group == null) {
                log.warn("Rule group not found when removing rule from group: groupId={}", groupId);
                return;
            }
            if (group.getRules() != null && !group.getRules().isEmpty()) {
                continue;
            }
            ruleGroupService.deleteRuleGroup(groupId);
        }
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
    public List<ExecutionItem> getExecutionItemsByEventId(@NotNull Integer eventId, @NotNull RuleExecutionContext context) {
        // Get arguments
        Long userId = context.getUserId();
        Integer userHashInt = (Integer) context.getArgument(ARG_USER_HASH_INT).getValue();
        if (userId == null || userHashInt == null) {
            log.warn("User ID is null in context: {}", context);
            return Collections.emptyList();
        }

        // Step 1: Query all records from table x associated with event_id
        List<ExecutionArrangement> arrangementList = eventService.getExecutionItems(eventId);
        if (arrangementList == null || arrangementList.isEmpty()) {
            log.warn("No execution event relations found for eventId: {}", eventId);
            return Collections.emptyList();
        }

        // Step 2: Build ruleIds array using exe_order as array index
        DraftResult draftResult = draftExecutionQueue(eventId, arrangementList);
        Long[] executionQueue = draftResult.getExecutionQueue();  // ruleId list in execution order
        Map<Long, List<ExecutionArrangement>> groupMap = draftResult.getGroupMap();
        Map<Long, Long> ruleGroupMap = draftResult.getArrangementGroupMap();

        // Step 3: Select rule from each group based on userHashInt
        selectRuleFromGroupByRatio(eventId, userId, userHashInt, groupMap, executionQueue);

        // Step 4: Collect all rule IDs from array (excluding nulls)
        List<Long> finalRuleIds = Arrays.stream(executionQueue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (finalRuleIds.isEmpty()) {
            log.warn("No rules to execute for eventId: {}", eventId);
            return Collections.emptyList();
        }

        // Step 5: Batch query rules and rule contents
        Map<Long, Rule> ruleMap = batchGetRuleWithContent(new HashSet<>(finalRuleIds));

        // Step 6: Build execution items in order
        List<ExecutionItem> items = new ArrayList<>();
        for (Long ruleId : finalRuleIds) {
            Rule rule = ruleMap.get(ruleId);
            if (rule == null) {
                throw new IllegalStateException("Rule not found or not in allowed status for ruleId: " + ruleId);
            }
            Long groupId = null;
            if (ruleGroupMap.containsKey(ruleId)) {
                groupId = ruleGroupMap.get(ruleId);
            }
            items.add(ExecutionItem.forRule(rule, groupId));
        }

        return items;
    }

    @Data
    static class DraftResult {
        /**
         * Execution queue array, index = exe_order, value = rule_id
         */
        private Long[] executionQueue;
        /**
         * Map of group_id to list of ExecutionArrangement in that group
         */
        private Map<Long, List<ExecutionArrangement>> groupMap;
        /**
         * Map of rule_id to group_id
         */
        private Map<Long, Long> arrangementGroupMap;

        DraftResult(int queueSize) {
            this.executionQueue = new Long[queueSize];
            this.groupMap = new HashMap<>();
            this.arrangementGroupMap = new HashMap<>();
        }

        void addArrangementInQueue(ExecutionArrangement arrangement) {
            Integer exeOrder = arrangement.getExeOrder();
            if (exeOrder == null || exeOrder < 0 || exeOrder >= executionQueue.length) {
                throw new IllegalStateException("Invalid exe_order for arrangement: " + arrangement);
            }
            Long ruleId = arrangement.getRuleId();
            if (ruleId == null) {
                throw new IllegalStateException("Null ruleId for standalone rule: " + arrangement);
            }
            executionQueue[exeOrder] = ruleId;
        }

        void addArrangementInGroup(ExecutionArrangement arrangement) {
            Long groupId = arrangement.getGroupId();
            if (groupId == null) {
                return;
            }
            groupMap.computeIfAbsent(groupId, k -> new ArrayList<>()).add(arrangement);
        }

        void indexGroupByArrangement(ExecutionArrangement arrangement) {
            arrangementGroupMap.put(arrangement.getRuleId(), arrangement.getGroupId());
        }
    }


    private DraftResult draftExecutionQueue(Integer eventId,
                                            List<ExecutionArrangement> arrangementList) {
        int maxExeOrder = arrangementList.stream()
                .mapToInt(entity -> entity.getExeOrder() != null ? entity.getExeOrder() : 0)
                .max()
                .orElse(-1);
        if (maxExeOrder < 0 || maxExeOrder != (arrangementList.size() - 1)) {
            throw new IllegalStateException("Invalid exe_order values for eventId: " + eventId);
        }

        DraftResult result = new DraftResult(maxExeOrder + 1);
        // Separate records: group_id = 0 (standalone rules) and group_id != 0 (rules in groups)
        for (ExecutionArrangement arrangement : arrangementList) {
            Long groupId = arrangement.getGroupId();
            if (groupId == null || groupId == 0) {
                // Standalone rule, add to array directly
                result.addArrangementInQueue(arrangement);
            } else {
                // Rule in group, group by group_id
                result.addArrangementInGroup(arrangement);
            }
            result.indexGroupByArrangement(arrangement);
        }
        return result;
    }


    private void selectRuleFromGroupByRatio(Integer eventId,
                                            Long userId,
                                            Integer userHashInt,
                                            Map<Long, List<ExecutionArrangement>> groupMap,
                                            Long[] executionItemIds) {
        for (Map.Entry<Long, List<ExecutionArrangement>> entry : groupMap.entrySet()) {
            Long groupId = entry.getKey();
            if (groupId == null) {
                throw new IllegalStateException("Invalid groupId (null) in groupRulesMap");
            }
            List<ExecutionArrangement> arrangementListInGroup = entry.getValue();
            if (arrangementListInGroup == null || arrangementListInGroup.isEmpty()) {
                throw new IllegalStateException("Empty arrangement list for groupId: " + groupId);
            }
            // Build rule_id -> ab_ratio map for this group
            Map<Long, Integer> ruleRatioMap = new HashMap<>();
            for (ExecutionArrangement arrangement : arrangementListInGroup) {
                Long ruleId = arrangement.getRuleId();
                Integer abRatio = arrangement.getAbRatio() != null ? arrangement.getAbRatio() : 0;
                ruleRatioMap.put(ruleId, abRatio);
            }

            Long selectedRuleId = getCachedRuleId(userId, userHashInt, eventId, groupId, ruleRatioMap);
            if (selectedRuleId == null) {
                // leave the queue blank if no rule selected
                continue;
            }
            // Put selected rule_id into array at exe_order position
            try {
                setExecutionQueue(arrangementListInGroup, selectedRuleId, executionItemIds);
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + ", group=" + groupId);
            }
        }
    }


    private Long getCachedRuleId(Long userId,
                                 Integer userHashInt,
                                 Integer eventId,
                                 Long groupId,
                                 Map<Long, Integer> ruleRatioMap) {
        // Check if caching is enabled
        if (ruleSelectionCacheService == null) {
            log.error("RuleSelectionCacheService is not configured, skipping cache for rule selection");
            return null;
        }
        // If in cache, return cached value
        Long selectedRuleId = ruleSelectionCacheService.get(userId, eventId, groupId);
        if (selectedRuleId != null) {
            if (log.isDebugEnabled()) {
                log.debug("[drawRule] hit cache: userId={}, eventId={}, groupId={}, selectedRuleId={}",
                        userId, eventId, groupId, selectedRuleId);
            }
            return selectedRuleId;
        }
        // If not in cache or invalid, select based on userHashInt
        selectedRuleId = selectRuleFromGroupByRatio(ruleRatioMap, userHashInt);
        if (selectedRuleId == null) {
            // no-rule-selected will not be cached
            if (log.isDebugEnabled()) {
                log.debug("[drawRule] no rule selected: userId={}, eventId={}, groupId={}",
                        userId, eventId, groupId);
            }
            return null;
        }
        // Cache the selection
        ruleSelectionCacheService.put(userId, eventId, groupId, selectedRuleId);

        return selectedRuleId;
    }


    /**
     * Select rule from group based on probability distribution (ab_ratio)
     *
     * @param ruleRatioMap map of rule_id -> ab_ratio
     * @param userHashInt  hash integer in range [1, 100]
     * @return selected rule ID, or null if no rule should be executed
     * todo: userHashInt做一次随机偏移（每一条规则有一个固定的偏移值），以均衡不同用户的分布
     */
    private Long selectRuleFromGroupByRatio(@NotEmpty Map<Long, Integer> ruleRatioMap, @NotNull Integer userHashInt) {
        // Calculate total ratio
        int totalRatio = ruleRatioMap.values().stream()
                .mapToInt(ratio -> ratio != null ? ratio : 0)
                .sum();
        // Validate total ratio
        if (totalRatio < 0) {
            throw new IllegalArgumentException("Total ratio must be positive in rule group: " + ruleRatioMap);
        }

        // Select rule based on probability distribution
        // Sort rule IDs to ensure consistent ordering
        List<Long> sortedRuleIds = new ArrayList<>(ruleRatioMap.keySet());
        sortedRuleIds.sort(Long::compareTo);

        int cumulativeRatio = 0;
        for (Long ruleId : sortedRuleIds) {
            Integer ratio = ruleRatioMap.get(ruleId);
            if (ratio == null) {
                continue;
            }
            cumulativeRatio += ratio;
            if (userHashInt <= cumulativeRatio) {
                return ruleId;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[drawRule] bypass due to unclosed totalRatio: userHashInt={}, totalRatio={}",
                    userHashInt, totalRatio);
        }
        return null;
    }


    /**
     * Set execution item into execution queue
     * It is NOT thread safe, should be called in single-threaded context
     *
     * @param arrangementListInGroup arrangements in the group
     * @param selectedItemId         the selected rule ID
     * @param executionQueue         the execution queue
     */
    private void setExecutionQueue(List<ExecutionArrangement> arrangementListInGroup,
                                   Long selectedItemId,
                                   Long[] executionQueue) {
        Integer index = arrangementListInGroup.get(0).getExeOrder();
        if (index == null || index < 0 || index >= executionQueue.length) {
            throw new IllegalStateException("[drawRule] invalid exe_order");
        }
        for (ExecutionArrangement arrangement : arrangementListInGroup) {
            if (!arrangement.getRuleId().equals(selectedItemId)) {
                continue;
            }
            Integer exeOrder = arrangement.getExeOrder();
            if (exeOrder == null || !exeOrder.equals(index)) {
                throw new IllegalStateException("[drawRule] exe_order mismatch for selected rule");
            }
            executionQueue[index] = selectedItemId;
            break;
        }
        // validate that selected rule was placed
        if (executionQueue[index] == null || !executionQueue[index].equals(selectedItemId)) {
            throw new IllegalStateException("[drawRule] selected rule not placed in ruleIds array");
        }
    }


    private Map<Long, Rule> batchGetRuleWithContent(Set<Long> ruleIds) {
        // Batch load rule entities
        List<RuleEntity> ruleEntities = ruleMapper.selectBatchIds(new ArrayList<>(ruleIds));
        if (ruleEntities == null || ruleEntities.isEmpty()) {
            log.warn("No rules found for IDs: {}", ruleIds);
            return new HashMap<>();
        }

        // Get rule IDs
        Set<Long> foundRuleIds = ruleEntities.stream()
                .map(RuleEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Batch load rule contents with cache support (configurable)
        Map<Long, String> contentMap = ruleContentCacheEnabled
                ? batchGetContentMapWithCache(foundRuleIds)
                : batchGetContentMap(foundRuleIds);

        return buildRuleMap(ruleEntities, contentMap);
    }


    private Map<Long, String> batchGetContentMapWithCache(Set<Long> ruleIdSet) {
        // Check if cache service is available
        if (ruleContentCacheService == null) {
            log.warn("RuleContentCacheService is not configured, fetching from database directly");
            return batchGetContentMap(ruleIdSet);
        }

        // First, try to get content from cache in batch
        Map<Long, String> cachedContentMap = ruleContentCacheService.getBatch(ruleIdSet);

        // Find uncached rule IDs
        Set<Long> uncachedRuleIds = ruleIdSet.stream()
                .filter(ruleId -> !cachedContentMap.containsKey(ruleId))
                .collect(Collectors.toSet());
        if (uncachedRuleIds.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("[eval] all rules from cache: {} total", ruleIdSet.size());
            }
            return cachedContentMap;
        }
        if (log.isDebugEnabled()) {
            log.debug("[eval] rule content cache: {} hits, {} misses out of {} total",
                    cachedContentMap.size(), uncachedRuleIds.size(), ruleIdSet.size());
        }

        Map<Long, String> contentMap = new HashMap<>(cachedContentMap);

        // If there are uncached rule IDs, fetch from database
        Map<Long, String> dbContentMap = batchGetContentMap(uncachedRuleIds);
        contentMap.putAll(dbContentMap);

        // Update cache with newly fetched content
        ruleContentCacheService.putBatch(dbContentMap);

        if (log.isDebugEnabled()) {
            log.debug("[eval] fetched {} rule contents from database and updated cache", dbContentMap.size());
        }

        return contentMap;
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

