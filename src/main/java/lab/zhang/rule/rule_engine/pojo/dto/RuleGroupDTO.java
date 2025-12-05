package lab.zhang.rule.rule_engine.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Rule group DTO for REST API
 * 
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleGroupDTO {
    
    /**
     * Rule group ID
     */
    private Long id;

    /**
     * Rule name
     */
    private String name;

    /**
     * Rule description
     */
    private String description;

    /**
     * Map of rule ID to A/B test ratio (0-100)
     */
    private Map<Long, Integer> ruleRatios;
}

