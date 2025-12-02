package lab.zhang.rule.rule_engine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lab.zhang.rule.rule_engine.entity.RuleGroupRuleRelationEntity;
import lab.zhang.rule.rule_engine.mapper.RuleGroupRuleRelationMapper;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleGroupQO;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper;
import lab.zhang.rule.rule_engine.util.RatioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rule group REST API controller
 *
 * @author Rongjin Zhang
 */
@Slf4j
@RestController
@RequestMapping("/api/rule-groups")
@Validated
public class RuleGroupController {

    @Autowired
    private RuleGroupService ruleGroupService;

    @Autowired
    private RuleGroupStructMapper ruleGroupStructMapper;

    @Autowired
    private RuleGroupRuleRelationMapper ruleGroupRuleRelationMapper;

    /**
     * Get all rule groups
     * GET /api/rule-groups
     * Note: rules field is not included in the response
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<RuleGroupDTO>>> getAllRuleGroups() {
        List<RuleGroup> groups = ruleGroupService.getAllRuleGroups();
        List<RuleGroupDTO> dtos = groups.stream()
                .map(ruleGroupStructMapper::modelToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDTO.success(dtos));
    }

    /**
     * Get rule group by ID
     * GET /api/rule-groups/{groupId}
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponseDTO<RuleGroupDTO>> getRuleGroup(
            @PathVariable @NotNull @Min(value = 1, message = "Rule Group ID must be a positive integer") Long groupId) {
        RuleGroup group = ruleGroupService.getRuleGroup(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Rule group not found for ID: " + groupId);
        }

        // Batch load all rule ratios for this group in one query
        Map<String, Integer> ratiosMap = buildRatiosMap(groupId, group.getRuleIds());

        RuleGroupDTO dto = ruleGroupStructMapper.modelToDTOWithRules(group, ratiosMap);
        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    /**
     * Create a new rule group
     * POST /api/rule-groups
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<RuleGroupDTO>> createRuleGroup(
            @RequestBody @Valid RuleGroupQO qo) {
        ruleGroupService.createRuleGroupWithRules(qo.getRules());
        return ResponseEntity.ok(ApiResponseDTO.success(null));

    }

    /**
     * Update a rule group
     * PUT /api/rule-groups/{groupId}
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponseDTO<Void>> updateRuleGroup(
            @PathVariable @NotNull @Min(value = 1, message = "Rule Group ID must be a positive integer") Long groupId,
            @RequestBody @Valid RuleGroupQO qo) {
        ruleGroupService.updateRuleGroupWithRules(groupId, qo.getRules());
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Delete a rule group
     * DELETE /api/rule-groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteRuleGroup(
            @PathVariable @NotNull @Min(value = 1, message = "Rule Group ID must be a positive integer") Long groupId) {
        ruleGroupService.deleteRuleGroup(groupId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Build ratios map for a rule group
     * Key format: "groupId:ruleId", value: A/B test ratio
     *
     * @param groupId rule group ID
     * @param ruleIds list of rule IDs
     * @return map of "groupId:ruleId" to ratio
     */
    private Map<String, Integer> buildRatiosMap(Long groupId, List<Long> ruleIds) {
        if (groupId == null || ruleIds == null || ruleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Batch query all relations for this group in one query
        LambdaQueryWrapper<RuleGroupRuleRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RuleGroupRuleRelationEntity::getGroupId, groupId)
                .in(RuleGroupRuleRelationEntity::getRuleId, ruleIds);
        List<RuleGroupRuleRelationEntity> relations = ruleGroupRuleRelationMapper.selectList(queryWrapper);

        // Build map: "groupId:ruleId" -> ratio
        Map<String, Integer> ratiosMap = new HashMap<>();
        if (relations != null) {
            for (RuleGroupRuleRelationEntity relation : relations) {
                String key = RatioUtil.buildRatioKey(relation.getGroupId(), relation.getRuleId());
                Integer ratio = relation.getAbTestRatio() != null ? relation.getAbTestRatio() : 0;
                ratiosMap.put(key, ratio);
            }
        }

        return ratiosMap;
    }

}

