package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Rule execution context.
 *
 * <p>Used to pass and share data during rule execution.
 * Contains user information, event information, and input arguments
 * (including execution state variables).
 *
 * <p>This class provides defensive copying for mutable collections
 * to prevent external modification of internal state.
 *
 * @author Rongjin Zhang
 */
@Data
public class RuleExecutionContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * User ID.
     */
    private Long userId;
    
    /**
     * Event type ID.
     */
    private Integer eventId;
    
    /**
     * Input parameter dictionary.
     * Defensively copied to prevent external modification.
     */
    private Map<String, TypedValue> arguments;
    
    
    /**
     * Creates a new execution context with default values.
     */
    public RuleExecutionContext() {
        this.arguments = new HashMap<>();
    }
    
    /**
     * Creates a new execution context with the specified parameters.
     * 
     * @param userId the user ID, may be null
     * @param eventId the event ID, may be null
     * @param arguments the input arguments, may be null (will create empty map)
     * @throws IllegalArgumentException if arguments map contains null keys
     */
    public RuleExecutionContext(Long userId, Integer eventId, Map<String, TypedValue> arguments) {
        this.userId = userId;
        this.eventId = eventId;
        this.arguments = arguments != null ? new HashMap<>(arguments) : new HashMap<>();
        // Validate arguments map
        if (arguments != null) {
            for (String key : arguments.keySet()) {
                if (key == null) {
                    throw new IllegalArgumentException("Arguments map cannot contain null keys");
                }
            }
        }
    }
    
    /**
     * Gets a defensive copy of the arguments map.
     * 
     * @return an unmodifiable view of the arguments map
     */
    public Map<String, TypedValue> getArguments() {
        return arguments != null ? Collections.unmodifiableMap(arguments) : Collections.emptyMap();
    }
    
    /**
     * Sets the arguments map, creating a defensive copy.
     * 
     * @param arguments the arguments map, may be null (will create empty map)
     * @throws IllegalArgumentException if arguments map contains null keys
     */
    public void setArguments(Map<String, TypedValue> arguments) {
        if (arguments == null) {
            this.arguments = new HashMap<>();
        } else {
            // Validate and create defensive copy
            for (String key : arguments.keySet()) {
                if (key == null) {
                    throw new IllegalArgumentException("Arguments map cannot contain null keys");
                }
            }
            this.arguments = new HashMap<>(arguments);
        }
    }

    /**
     * Gets an argument from the arguments map.
     *
     * @param key the argument key, must not be null
     * @return the argument value, or null if not found
     * @throws IllegalArgumentException if key is null
     */
    public TypedValue getArgument(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument key cannot be null or blank");
        }
        return this.arguments != null ? this.arguments.get(key) : TypedValue.nullValue();
    }

    /**
     * Puts an argument into the arguments map.
     *
     * <p>This method provides a safe way to add or update arguments
     * without exposing the internal mutable map.
     *
     * @param key the argument key, must not be null
     * @param value the argument value, may be null
     * @throws IllegalArgumentException if key is null
     */
    public void putArgument(String key, TypedValue value) {
        Objects.requireNonNull(key, "Argument key cannot be null");
        if (this.arguments == null) {
            this.arguments = new HashMap<>();
        }
        this.arguments.put(key, value);
    }

    /**
     * Gets all variables including special properties.
     *
     * @return a map of all variables with special properties included
     */
    public Map<String, TypedValue> getVariables() {
        Map<String, TypedValue> variableMap = new HashMap<>(getArguments());
        variableMap.put("userId", new TypedValue(this.userId, ValueTypeEnum.LONG));
        variableMap.put("eventId", new TypedValue(this.eventId, ValueTypeEnum.INTEGER));
        return variableMap;
    }

    /**
     * Gets a variable by key.
     * If the key is one of the special properties (userId, eventId, traceId),
     * returns the value of that property. Otherwise, looks up in arguments.
     *
     * @param key the variable key, must not be null
     * @return the variable value, or null if not found
     * @throws IllegalArgumentException if key is null
     */
    public TypedValue getVariable(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument key cannot be null or blank");
        }

        switch (key) {
            case "userId":
                return new TypedValue(this.userId, ValueTypeEnum.LONG);
            case "eventId":
                return new TypedValue(this.eventId, ValueTypeEnum.INTEGER);
            default:
                return getArgument(key);
        }
    }

    /**
     * Sets a variable by key.
     * If the key is one of the special properties (userId, eventId, traceId),
     * sets the value of that property. Otherwise, stores in arguments.
     *
     * @param key the variable key, must not be null
     * @param value the variable value, may be null
     * @throws IllegalArgumentException if key is null
     */
    public void setVariable(String key, TypedValue value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument key cannot be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Argument value cannot be null");
        }

        switch (key) {
            case "userId":
                this.userId = (Long) value.getValue();
                break;
            case "eventId":
                this.eventId = (Integer) value.getValue();
                break;
            default:
                putArgument(key, value);
        }
    }
}
