package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.enums.RuleStatus;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lombok.Data;

import java.io.Serializable;

/**
 * Rule entity class
 * 
 * @author rule-engine
 */
@Data
public class Rule implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Rule ID (unique identifier)
     */
    private Long ruleId;
    
    /**
     * Rule name
     */
    private String ruleName;
    
    /**
     * Rule type
     */
    private RuleType ruleType;
    
    /**
     * Rule content (format varies by type)
     */
    private String ruleContent;
    
    /**
     * Rule status
     */
    private RuleStatus status;
    
    /**
     * A/B test ratio (0-100, only valid when status is AB_TEST)
     */
    private Integer abTestRatio;
    
    /**
     * Rule description
     */
    private String description;
}

