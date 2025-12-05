package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleQO;
import lab.zhang.rule.rule_engine.service.RuleService;
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rule management REST API controller
 *
 * @author Rongjin Zhang
 */
@Slf4j
@RestController
@RequestMapping("/api/rules")
@Validated
public class RuleController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RuleStructMapper ruleStructMapper;


    /**
     * Get all rules
     * GET /api/rules
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<RuleDTO>>> getAllRules() {
        List<Rule> rules = ruleService.getAllRules();
        List<RuleDTO> dtos = rules.stream()
                .map(ruleStructMapper::modelToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDTO.success(dtos));
    }

    /**
     * Get rule by ID
     * GET /api/rules/{ruleId}
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<ApiResponseDTO<RuleDTO>> getRule(
            @PathVariable @NotNull @Min(value = 1, message = "Rule ID must be a positive integer") Long ruleId) {
        Rule rule = ruleService.getRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found for ID: " + ruleId);
        }

        RuleDTO dto = ruleStructMapper.modelToDTO(rule);
        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    /**
     * Create rule
     * POST /api/rules
     * Note: New rules are always created with OFFLINE status (status ID = 0).
     * The status field in the request body will be ignored.
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<RuleDTO>> createRule(
            @RequestBody @Validated(RuleQO.Create.class) RuleQO ruleQO) {
        Rule rule = ruleStructMapper.qoToModel(ruleQO);
        rule.setRuleStatus(RuleStatusEnum.OFFLINE); // New rules are created as OFFLINE
        ruleService.createRule(rule);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Update rule
     * PUT /api/rules/{ruleId}
     * <p>
     * Note:
     * - name, type, status, description, content are optional (only provided fields will be updated)
     * - type can be updated if a valid value is provided
     */
    @PutMapping("/{ruleId}")
    public ResponseEntity<ApiResponseDTO<RuleDTO>> updateRule(
            @PathVariable @NotNull @Min(value = 1, message = "Rule ID must be a positive integer") Long ruleId,
            @RequestBody @Validated(RuleQO.Update.class) RuleQO ruleQO) {
        Rule rule = ruleStructMapper.qoToModel(ruleQO);
        ruleService.updateRule(ruleId, rule);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Delete rule
     * DELETE /api/rules/{ruleId}
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteRule(
            @PathVariable @NotNull @Min(value = 1, message = "Rule ID must be a positive integer") Long ruleId) {
        ruleService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }
}

