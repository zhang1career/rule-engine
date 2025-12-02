package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.util.EnumUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dictionary controller
 * Provides enumeration value query interface
 * 
 * @author Rongjin Zhang
 */
@Slf4j
@RestController
@RequestMapping("/api/dicts")
public class DictController {
    
    /**
     * Query enumeration values
     * GET /api/dicts?keys=RuleStatusEnum,RuleTypeEnum,EnvironmentEnum,ExecutionItemTypeEnum
     *
     * According to project conventions, returns a map where:
     * - Key: enumeration class name
     * - Value: list of objects, each object has "name" (enumeration value) and "value" (enumeration ID)
     *
     * @param keys comma-separated enumeration class names (e.g., "RuleStatusEnum,RuleTypeEnum")
     * @return map with enumeration class names as keys, and their list of name-value pairs as values
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, List<Map<String, Object>>>>> getEnumValues(
            @RequestParam(value = "keys", required = false, defaultValue = "") String keys) {
        try {
            log.info("Query enumeration values: keys={}", keys);
            
            Map<String, List<Map<String, Object>>> result = EnumUtil.getEnumInfoMap(keys);
            
            return ResponseEntity.ok(ApiResponseDTO.success(result));
        } catch (Exception e) {
            log.error("Failed to query enumeration values: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponseDTO.error(500, "Failed to query enumeration values: " + e.getMessage()));
        }
    }
}

