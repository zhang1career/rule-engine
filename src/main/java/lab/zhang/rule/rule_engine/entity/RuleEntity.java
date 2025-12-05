package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Rule entity for database
 * 
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("rule")
public class RuleEntity extends BaseEntity {
    
    /**
     * Rule ID (primary key)
     * Auto-increment starting from 10,000,000
     */
    @TableId(type = IdType.AUTO)
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
     * Rule content type ID (enumeration ID, stored in database)
     */
    @TableField("content_type")
    private Integer contentType;
    
    /**
     * Rule status ID (enumeration ID, stored in database)
     */
    @TableField("rule_status")
    private Integer ruleStatus;
    
    /**
     * Convert to ContentTypeEnum enum
     */
    public ContentTypeEnum getContentTypeEnum() {
        return ContentTypeEnum.fromId(contentType);
    }
    
    /**
     * Set ContentTypeEnum enum
     */
    public void setContentTypeEnum(ContentTypeEnum contentTypeEnum) {
        this.contentType = contentTypeEnum != null ? contentTypeEnum.getId() : null;
    }
    
    /**
     * Convert to RuleStatusEnum enum
     */
    public RuleStatusEnum getRuleStatusEnum() {
        return RuleStatusEnum.fromId(ruleStatus);
    }
    
    /**
     * Set RuleStatusEnum enum
     */
    public void setRuleStatusEnum(RuleStatusEnum ruleStatusEnum) {
        this.ruleStatus = ruleStatusEnum != null ? ruleStatusEnum.getId() : null;
    }
}

