package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO;
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleGroupQO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleQO;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper;
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

import static lab.zhang.rule.rule_engine.util.MapUtil.getOneEntry;

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
    private RuleService ruleService;

    @Autowired
    private RuleGroupStructMapper ruleGroupStructMapper;

    @Autowired
    private RuleStructMapper ruleStructMapper;

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

        RuleGroupDTO dto = ruleGroupStructMapper.modelToDTO(group);
        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    /**
     * Create a new rule group
     * POST /api/rule-groups
     * Note: According to new design, rule groups are created automatically when rules transition to ONLINE status.
     * This endpoint is deprecated and may be removed in future versions.
     */
    @PostMapping
    @Deprecated
    public ResponseEntity<ApiResponseDTO<RuleGroupDTO>> createRuleGroup(
            @RequestBody @Valid RuleGroupQO qo,
            @RequestBody @NotNull @Min(value = 1, message = "Event ID must be a positive integer") Integer eventId) {
        Pair<Long, Integer> ruleRatioPair = getOneEntry(qo.getRuleRatios());
        if (ruleRatioPair == null) {
            throw new IllegalArgumentException("At least one rule must be provided to create a rule group.");
        }
        Long ruleId = ruleRatioPair.getLeft();
        Integer ruleRatio = ruleRatioPair.getRight();
        if (ruleId == null || ruleRatio == null) {
            throw new IllegalArgumentException("Invalid rule ID or ratio provided.");
        }
        Rule rule = ruleService.getRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found for ID: " + ruleId);
        }
        RuleGroup ruleGroup = ruleGroupService.createRuleGroup(rule, eventId);
        if (ruleGroup == null) {
            throw new IllegalArgumentException("Failed to create rule group for rule ID: " + ruleId);
        }
        RuleGroupDTO ruleGroupDTO = ruleGroupStructMapper.modelToDTO(ruleGroup);
        return ResponseEntity.ok(ApiResponseDTO.success(ruleGroupDTO));
    }

    /**
     * Update a rule group (deprecated, use updateRuleGroupRatios instead)
     * PUT /api/rule-groups/{groupId}
     */
    @PutMapping("/{groupId}")
    @Deprecated
    public ResponseEntity<ApiResponseDTO<Void>> updateRuleGroup(
            @PathVariable @NotNull @Min(value = 1, message = "Rule Group ID must be a positive integer") Long groupId,
            @RequestBody @Valid RuleGroupQO qo) {
        ruleGroupService.updateRuleGroupRatios(groupId, qo.getRuleRatios());
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Copy a rule within a rule group
     * POST /api/rule-groups/rules/{ruleId}/copy
     */
    @PostMapping("/{groupId}/rules/{ruleId}/copy")
    public ResponseEntity<ApiResponseDTO<RuleDTO>> copyRuleInGroup(
            @PathVariable @NotNull @Min(value = 1, message = "Rule ID must be a positive integer") Long ruleId,
            @RequestBody @Validated(RuleQO.Copy.class) RuleQO qo) {
        RuleDTO ruleDTO = ruleStructMapper.qoToDto(qo);
        Rule resultRule = ruleGroupService.copyRuleInGroup(ruleId, ruleDTO);
        RuleDTO resultDto = ruleStructMapper.modelToDTO(resultRule);
        return ResponseEntity.ok(ApiResponseDTO.success(resultDto));
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
}

