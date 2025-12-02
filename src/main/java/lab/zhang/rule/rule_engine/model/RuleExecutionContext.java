package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.common.TypedValue;
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
 * Contains user information, event information, input arguments,
 * and execution state (variables).
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
    private Long eventId;
    
    /**
     * Trace ID for request tracking.
     */
    private Long traceId;
    
    /**
     * Input parameter dictionary.
     * Defensively copied to prevent external modification.
     */
    private Map<String, TypedValue> arguments;
    
    /**
     * Variable storage during execution (for data transfer between rules).
     */
    private Map<String, TypedValue> variables;
    
    /**
     * Creates a new execution context with default values.
     */
    public RuleExecutionContext() {
        this.variables = new HashMap<>();
        this.arguments = new HashMap<>();
    }
    
    /**
     * Creates a new execution context with the specified parameters.
     * 
     * @param userId the user ID, may be null
     * @param eventId the event ID, may be null
     * @param traceId the trace ID, may be null
     * @param arguments the input arguments, may be null (will create empty map)
     * @throws IllegalArgumentException if arguments map contains null keys
     */
    public RuleExecutionContext(Long userId, Long eventId, Long traceId, 
                               Map<String, TypedValue> arguments) {
        this.userId = userId;
        this.eventId = eventId;
        this.traceId = traceId;
        this.arguments = arguments != null ? new HashMap<>(arguments) : new HashMap<>();
        this.variables = new HashMap<>();
        
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
     * Gets an argument from the arguments map.
     * 
     * @param key the argument key, must not be null
     * @return the argument value, or null if not found
     * @throws IllegalArgumentException if key is null
     */
    public TypedValue getArgument(String key) {
        Objects.requireNonNull(key, "Argument key cannot be null");
        return this.arguments != null ? this.arguments.get(key) : null;
    }
    
    /**
     * Sets a variable in the execution context.
     * 
     * @param key the variable key, must not be null
     * @param value the variable value, may be null
     * @throws IllegalArgumentException if key is null
     */
    public void setVariable(String key, TypedValue value) {
        Objects.requireNonNull(key, "Variable key cannot be null");
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, value);
    }
    
    /**
     * Gets a variable from the execution context.
     * 
     * @param key the variable key, must not be null
     * @return the variable value, or null if not found
     * @throws IllegalArgumentException if key is null
     */
    public TypedValue getVariable(String key) {
        Objects.requireNonNull(key, "Variable key cannot be null");
        return this.variables != null ? this.variables.get(key) : null;
    }
    
    /**
     * Gets an unmodifiable view of all variables.
     * 
     * @return an unmodifiable map of all variables
     */
    public Map<String, TypedValue> getAllVariables() {
        return this.variables != null ? Collections.unmodifiableMap(this.variables) : Collections.emptyMap();
    }
}

