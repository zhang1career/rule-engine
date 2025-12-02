package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event entity for database
 * 
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("event")
public class EventEntity extends BaseEntity {
    
    /**
     * Event ID (primary key, unsigned long integer)
     * Must be specified when creating, not auto-increment
     */
    @TableId(type = IdType.INPUT)
    private Long id;
    
    /**
     * Event name
     */
    private String name;
    
    /**
     * Event description
     */
    private String description;
}

