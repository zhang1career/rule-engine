package lab.zhang.rule.rule_engine.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * TypedValue - Typed value wrapper class
 * Used to encapsulate input parameters and output results in rule calculation
 * 
 * @author Rongjin Zhang
 */
@Data
public class TypedValue implements Serializable {
    
    private static final long serialVersionUID = 1L;


    public static TypedValue of(Object value, ValueTypeEnum type) {
        return new TypedValue(value, type);
    }

    public static TypedValue nullValue() {
        return new TypedValue(null, ValueTypeEnum.OBJECT);
    }

    public static TypedValue trueValue() {
        return new TypedValue(true, ValueTypeEnum.BOOLEAN);
    }

    public static TypedValue falseValue() {
        return new TypedValue(false, ValueTypeEnum.BOOLEAN);
    }

    /**
     * Actual value
     */
    private Object value;
    
    /**
     * Value type
     */
    private ValueTypeEnum type;

    
    public TypedValue() {
    }
    
    @JsonCreator
    public TypedValue(@JsonProperty("value") Object value, 
                     @JsonProperty("type") ValueTypeEnum type) {
        this.value = value;
        this.type = type;
    }


    /**
     * Get value automatically based on internal type information
     * This method intelligently converts the value based on the stored ValueType
     * and returns the most appropriate type
     * 
     * @return the value converted to the most appropriate type based on internal type information
     * 
     * @example
     * TypedValue tv = new TypedValue(100, ValueType.INTEGER);
     * Object result = tv.getValue();  // Returns Integer(100), not Object
     * Integer intValue = (Integer) result;  // Safe cast
     */
    public Object getValue() {
        if (value == null) {
            return null;
        }
        
        // Automatically convert based on internal type information
        if (type == null) {
            return value;  // If type is not set, return raw value
        }
        
        switch (type) {
            case STRING:
                return getStringValue();
            case INTEGER:
                return getIntegerValue();
            case LONG:
                return getLongValue();
            case DECIMAL:
                return getDecimalValue();
            case BOOLEAN:
                return getBooleanValue();
            case DATE:
            case OBJECT:
            default:
                return value;  // Return raw value for complex types
        }
    }
    
    /**
     * Get value and convert by type
     * These methods are used internally by getValue() for type conversion
     */
    private String getStringValue() {
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerValue() {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long getLongValue() {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private Double getDecimalValue() {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private Boolean getBooleanValue() {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}

