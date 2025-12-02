package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Execution event relation entity for database
 * Represents the relationship between events and execution items (rules or rule groups)
 * 
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("execution_event_rel")
public class ExecutionEventRelationEntity extends BaseEntity {
    
    /**
     * Event ID (part of composite primary key, unsigned long integer)
     */
    @TableId
    private Long eventId;
    
    /**
     * Item type ID: RULE (0) or RULE_GROUP (1) (part of composite primary key, enumeration ID stored in database)
     */
    private Integer itemType;
    
    /**
     * Item ID (rule ID or rule group ID) (part of composite primary key)
     */
    private Long itemId;
    
    /**
     * Execution order (starting from 1)
     */
    private Integer executionOrder;


    /**
     * Convert to ItemType enum
     */
    public ExecutionItemTypeEnum getItemTypeEnum() {
        return ExecutionItemTypeEnum.fromId(itemType);
    }
    
    /**
     * Set ItemType enum
     */
    public void setItemTypeEnum(ExecutionItemTypeEnum executionItemTypeEnum) {
        this.itemType = executionItemTypeEnum != null ? executionItemTypeEnum.getId() : null;
    }
}

