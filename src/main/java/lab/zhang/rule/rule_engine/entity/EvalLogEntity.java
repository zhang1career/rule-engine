package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Eval log entity for database
 * 
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("eval_log")
public class EvalLogEntity extends BaseEntity {
    
    /**
     * Eval log ID (primary key)
     * Auto-increment starting from 10,000,000
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * Business request trace ID
     */
    @TableField("trace_id")
    private Long traceId;

    /**
     * Event ID
     */
    @TableField("event_id")
    private Long eventId;

    /**
     * User ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Parameter dictionary for rule calculation, JSON encoded string
     */
    private String arguments;

    /**
     * List of execution steps, JSON encoded string
     */
    private String steps;
}

