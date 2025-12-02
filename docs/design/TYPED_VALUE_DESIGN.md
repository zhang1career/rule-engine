# TypedValue Design Documentation

## Design Goals

Enable the `TypedValue` class to:
1. Store type information (implemented)
2. Automatically invoke corresponding conversion methods based on type information (implemented)
3. Allow callers to use values without knowing the original type or specifying expected type (implemented)

## Implementation

### Parameterless `getValue()` Method

```java
public Object getValue()
```

This method implements "fully automatic type conversion":
- **Input**: No parameters required
- **Processing**: The method internally selects the most appropriate conversion logic based on the stored `ValueType`
- **Output**: Returns a value of the corresponding type (although the return type is `Object`, the actual type is specific)

**How it works**:
```java
switch (type) {
    case INTEGER: return getIntegerValue();  // Returns Integer
    case STRING: return getStringValue();    // Returns String
    case LONG: return getLongValue();        // Returns Long
    case DECIMAL: return getDecimalValue();  // Returns Double
    case BOOLEAN: return getBooleanValue();  // Returns Boolean
    // ...
}
```

## Usage Comparison

### Previous Approach (Required Type Knowledge)

```java
TypedValue result = ruleExecutor.execute(rule, context);

// Caller needs to know the type of result
if (result.getType() == TypedValue.ValueType.INTEGER) {
    Integer value = result.getIntegerValue();
} else if (result.getType() == TypedValue.ValueType.STRING) {
    String value = result.getStringValue();
} else if (result.getType() == TypedValue.ValueType.BOOLEAN) {
    Boolean value = result.getBooleanValue();
}
// ... Need to handle all possible types
```

### Current Approach: Fully Automatic

```java
TypedValue result = ruleExecutor.execute(rule, context);

// Caller doesn't need to know the type or specify expected type
Object value = result.getValue();  // Automatically returns corresponding type based on internal type

// Use based on the actual type of the returned value
if (value instanceof Integer) {
    Integer intValue = (Integer) value;
} else if (value instanceof String) {
    String strValue = (String) value;
} else if (value instanceof Boolean) {
    Boolean boolValue = (Boolean) value;
}
```

## Advantages

### Parameterless `getValue()` Method Advantages:
1. **Fully Automatic**: Callers don't need to know the type or specify expected type
2. **Intelligent Conversion**: Automatically selects the most appropriate conversion based on internal type information
3. **Simplified Usage**: Simplest way to use, just call `getValue()`

## Technical Details

### Type Conversion Rules

The `getValue()` method automatically converts values based on the stored `ValueType`:

| ValueType | Returned Type | Conversion Method |
|-----------|--------------|-------------------|
| STRING    | String       | `getStringValue()` |
| INTEGER   | Integer      | `getIntegerValue()` |
| LONG      | Long         | `getLongValue()` |
| DECIMAL   | Double       | `getDecimalValue()` |
| BOOLEAN   | Boolean      | `getBooleanValue()` |
| DATE      | Object       | Raw value (no conversion) |
| OBJECT    | Object       | Raw value (no conversion) |

### Internal Conversion Methods

The following private methods are used internally for type conversion:

- `getStringValue()`: Converts value to String
- `getIntegerValue()`: Converts value to Integer
- `getLongValue()`: Converts value to Long
- `getDecimalValue()`: Converts value to Double
- `getBooleanValue()`: Converts value to Boolean

These methods handle type conversion intelligently:
- If the value is already of the target type, return it directly
- If the value is a Number, convert using appropriate Number methods
- If the value is a String, parse it to the target type
- If conversion fails, return `null` (no exception thrown)

### Error Handling

- If the value is `null`, returns `null`
- If type is not set (`type == null`), returns raw value
- Conversion methods handle exceptions internally and return `null` on failure
- No exceptions are thrown, ensuring safe calls

## Code Examples

### Example 1: Basic Usage

```java
// Create TypedValue instance
TypedValue tv = new TypedValue(100, TypedValue.ValueType.INTEGER);

// Fully automatic approach
Object result = tv.getValue();  // Automatically returns Integer(100)
if (result instanceof Integer) {
    Integer intValue = (Integer) result;  // 100
}
```

### Example 2: Usage in Rule Execution

```java
// Fully automatic approach
TypedValue result = ruleExecutor.execute(rule, context);
Object value = result.getValue();  // Automatically returns corresponding type based on internal type

// Use based on actual type
if (value instanceof Boolean) {
    Boolean shouldContinue = (Boolean) value;
    if (shouldContinue != null && shouldContinue) {
        // Continue execution
    }
}
```

### Example 3: Handling Different Data Types

```java
// String type
TypedValue name = new TypedValue("John", TypedValue.ValueType.STRING);
Object nameValue = name.getValue();  // Automatically returns String("John")
String nameStr = (String) nameValue;  // Safe cast

// Integer type
TypedValue age = new TypedValue(25, TypedValue.ValueType.INTEGER);
Object ageValue = age.getValue();  // Automatically returns Integer(25)
Integer ageInt = (Integer) ageValue;  // Safe cast

// Boolean type
TypedValue active = new TypedValue(true, TypedValue.ValueType.BOOLEAN);
Object activeValue = active.getValue();  // Automatically returns Boolean(true)
Boolean activeBool = (Boolean) activeValue;  // Safe cast
```

### Example 4: Usage in Expression Executor

```java
// Before: Needed to know the type
TypedValue result = ruleExecutor.execute(rule, context);
if (result.getType() == TypedValue.ValueType.BOOLEAN) {
    Boolean boolResult = result.getBooleanValue();
    // ...
} else if (result.getType() == TypedValue.ValueType.INTEGER) {
    Integer intResult = result.getIntegerValue();
    // ...
}

// Now: Fully automatic
TypedValue result = ruleExecutor.execute(rule, context);
Object value = result.getValue();  // Automatically returns corresponding type
if (value instanceof Boolean) {
    Boolean boolResult = (Boolean) value;
    // ...
} else if (value instanceof Integer) {
    Integer intResult = (Integer) value;
    // ...
}
```

## Notes

1. **Null Check**: The method returns `null` if the value is `null` or if type is not set. It's recommended to check the return value.
2. **Type Safety**: Although the return type is `Object`, the actual runtime type is specific. Use `instanceof` to check the type before casting.
3. **Performance**: Type conversion has minimal overhead, which is negligible for most scenarios.
4. **Backward Compatibility**: The internal conversion methods (`getStringValue()`, `getIntegerValue()`, etc.) are kept as private methods for internal use.
