package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Rule content entity for database
 * 
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("rule_content")
public class RuleContentEntity extends BaseEntity {
    
    /**
     * Rule ID (primary key, foreign key to rule table)
     */
    @TableId
    private Long id;
    
    /**
     * Rule content
     */
    private String content;
}

