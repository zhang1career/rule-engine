package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO;
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleGroupQO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleQO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleRatioQO;
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
     * Update a rule group (deprecated, use updateRuleGroupRatios instead)
     * PUT /api/rule-groups/{groupId}
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponseDTO<Void>> updateRuleGroup(
            @PathVariable @NotNull @Min(value = 1, message = "Rule Group ID must be a positive integer") Long groupId,
            @RequestBody @Valid RuleGroupQO qo) {
        Map<Long, Integer> ruleRatioMap = qo.getRuleRatios().stream()
                .filter(ruleRatioQO -> ruleRatioQO != null && ruleRatioQO.getRuleId() != null && ruleRatioQO.getRatio() != null)
                .collect(Collectors.toMap(RuleRatioQO::getRuleId, RuleRatioQO::getRatio));
        ruleGroupService.updateRuleGroupRatios(groupId, ruleRatioMap);
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

