package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Execution event relation entity for database (table x)
 * Represents the relationship between events, rules and rule groups
 * 
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("x")
public class ExecutionArrangementEntity extends BaseEntity {
    
    /**
     * Event ID (part of composite primary key, unsigned integer)
     */
    @TableId
    private Integer eventId;
    
    /**
     * Rule ID (part of composite primary key)
     */
    private Long ruleId;
    
    /**
     * Rule group ID (part of composite primary key)
     * 0 means standalone rule (not in any group)
     * Non-zero means rule belongs to a rule group with this ID
     */
    private Long groupId;
    
    /**
     * Execution order (starting from 0)
     */
    private Integer exeOrder;
    
    /**
     * A/B test ratio (0-100), traffic control for rules in group
     */
    private Integer abRatio;
}

