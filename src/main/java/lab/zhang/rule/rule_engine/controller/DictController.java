package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.constant.CommonConst;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.util.EnumUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * <p>
     * According to project conventions, returns a map where:
     * - Key: enumeration class name
     * - Value: list of objects, each object has "name" (enumeration value) and "value" (enumeration ID)
     *
     * @param keys comma-separated enumeration class names (e.g., "RuleStatusEnum,RuleTypeEnum")
     * @return map with enumeration class names as keys, and their list of name-value pairs as values
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, List<Map<String, Object>>>>> getEnumValues(
            @RequestParam(value = "keys", required = false, defaultValue = CommonConst.EMPTY_STRING) String keys) {

        log.info("Query enumeration values: keys={}", keys);

        Map<String, List<Map<String, Object>>> result = EnumUtil.getEnumInfoMap(keys);

        return ResponseEntity.ok(ApiResponseDTO.success(result));

    }
}

