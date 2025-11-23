package lab.zhang.rule.rule_engine.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * TypedValue - Typed value wrapper class
 * Used to encapsulate input parameters and output results in rule calculation
 * 
 * @author rule-engine
 */
@Data
public class TypedValue implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Value type enumeration
     */
    public enum ValueType {
        STRING,
        INTEGER,
        LONG,
        DECIMAL,
        BOOLEAN,
        DATE,
        OBJECT
    }
    
    /**
     * Actual value
     */
    private Object value;
    
    /**
     * Value type
     */
    private ValueType type;
    
    public TypedValue() {
    }
    
    @JsonCreator
    public TypedValue(@JsonProperty("value") Object value, 
                     @JsonProperty("type") ValueType type) {
        this.value = value;
        this.type = type;
    }

    /**
     * Get value and convert by type
     */
    public String getStringValue() {
        return value != null ? value.toString() : null;
    }
    
    public Integer getIntegerValue() {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
    
    public Long getLongValue() {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
    
    public Double getDecimalValue() {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }
    
    public Boolean getBooleanValue() {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}

