package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Execution event relation entity class.
 * 
 * <p>Represents the relationship between events and execution items (rules or rule groups),
 * including execution order and other association information.
 * 
 * <p>This class follows the builder pattern for object creation.
 * Use {@link ExecutionEventRelationBuilder} to create instances.
 * 
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEventRelation implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Event type ID.
     */
    private Integer eventId;
    
    /**
     * Execution items (rules or rule groups) arranged in execution order.
     * Each item contains the item type (RULE or RULE_GROUP) and item ID.
     */
    @Builder.Default
    private List<ExecutionItemInfo> items = new ArrayList<>();
    
    /**
     * Execution sequence name (optional).
     */
    private String sequenceName;
    
    /**
     * Execution sequence description (optional).
     */
    private String description;
    
    /**
     * Execution item information.
     * Contains item type and item ID.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionItemInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * Item type: RULE or RULE_GROUP.
         */
        private ExecutionItemTypeEnum itemType;
        
        /**
         * Item ID (rule ID or rule group ID).
         */
        private Long itemId;
        
        /**
         * Execution order (starting from 1).
         */
        private Integer executionOrder;
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExecutionItemInfo that = (ExecutionItemInfo) o;
            return itemType == that.itemType && Objects.equals(itemId, that.itemId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(itemType, itemId);
        }
    }
    
    /**
     * Gets rule IDs from execution items (for backward compatibility).
     * 
     * @return list of rule IDs
     */
    public List<Long> getRuleIds() {
        List<Long> ruleIds = new ArrayList<>();
        if (items != null) {
            for (ExecutionItemInfo item : items) {
                if (item.getItemType() == ExecutionItemTypeEnum.RULE) {
                    ruleIds.add(item.getItemId());
                }
            }
        }
        return ruleIds;
    }
    
    /**
     * Sets rule IDs (for backward compatibility).
     * Converts rule IDs to ExecutionItemInfo list.
     * 
     * @param ruleIds list of rule IDs
     */
    public void setRuleIds(List<Long> ruleIds) {
        if (ruleIds == null) {
            this.items = new ArrayList<>();
            return;
        }
        this.items = new ArrayList<>();
        for (int i = 0; i < ruleIds.size(); i++) {
            ExecutionItemInfo item = new ExecutionItemInfo(
                ExecutionItemTypeEnum.RULE,
                ruleIds.get(i),
                i + 1
            );
            this.items.add(item);
        }
    }
}

