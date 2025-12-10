# Mock Data Generator Documentation

This document describes the mock data generation system used by `mock_data.py` and `load_test.py`.

## Overview

The mock data generator supports various field types and generation methods to create diverse test data based on configuration files.

## Supported Field Types

- `INTEGER`: Integer values
- `LONG`: Long integer values (64-bit)
- `DECIMAL`: Decimal/float values
- `BOOLEAN`: Boolean values (true/false)
- `STRING`: String values
- `OBJECT`: Object/struct values
- `ARRAY`: Array/list values

## Generation Methods

### 1. Fixed Value

Generates a fixed value.

```yaml
generation:
  method: "fixed"
  value: "constant_value"
```

### 2. Increment

Generates incrementing values starting from a base value.

```yaml
generation:
  method: "increment"
  start: 1000000
  step: 1
```

### 3. Random Integer

Generates random integer values within a range.

```yaml
generation:
  method: "random_int"
  min: 1
  max: 100
```

### 4. Random Float

Generates random decimal values within a range.

```yaml
generation:
  method: "random_float"
  min: 0.0
  max: 1000.0
  precision: 2
```

### 5. Random Choice

Randomly selects a value from a predefined list.

```yaml
generation:
  method: "random_choice"
  values:
    - "value1"
    - "value2"
    - "value3"
```

### 6. Template

Generates values using a template with variable substitution.

```yaml
generation:
  method: "template"
  template: "name_{userId}"
```

Variables in templates are replaced with values from the context (previously generated fields).

### 7. List (Cyclic)

Cycles through a list of values.

```yaml
generation:
  method: "list"
  values:
    - "value1"
    - "value2"
    - "value3"
```

### 8. Random Text

Generates random text strings.

```yaml
generation:
  method: "random_text"
  length: 50
  charset: "alphanumeric"
```

**Parameters:**
- `length`: Length of the generated text (default: 10)
- `charset`: Character set to use:
  - `"alphanumeric"`: Letters and numbers (default)
  - `"alphabetic"`: Letters only
  - `"numeric"`: Numbers only
  - `"ascii"`: ASCII printable characters (32-126)
  - Custom string: Use the provided string as character set

### 9. Array

Generates arrays with random length and elements.

```yaml
generation:
  method: "array"
  min_length: 1
  max_length: 5
```

**Array Configuration:**

For arrays, you need to specify the `element` configuration:

```yaml
field_name:
  type: "ARRAY"
  element:
    type: "STRING"
    generation:
      method: "random_text"
      length: 10
  generation:
    method: "array"
    min_length: 1
    max_length: 5
```

**Array of Primitives:**

```yaml
tags:
  type: "ARRAY"
  element:
    type: "INTEGER"
    generation:
      method: "random_int"
      min: 1
      max: 100
  generation:
    method: "array"
    min_length: 2
    max_length: 10
```

**Array of Objects:**

```yaml
items:
  type: "ARRAY"
  element:
    type: "OBJECT"
    fields:
      id:
        type: "INTEGER"
        generation:
          method: "increment"
          start: 1
      name:
        type: "STRING"
        generation:
          method: "random_text"
          length: 20
  generation:
    method: "array"
    min_length: 1
    max_length: 5
```

## Configuration File Structure

A complete configuration file example:

```yaml
api:
  name: "Example API"
  method: "POST"
  path: "/example"
  base_url: "http://localhost:8080/api"
  full_url: "http://localhost:8080/api/example"
  description: "Example API endpoint"
  headers:
    Content-Type: "application/json"
  timeout: 30

request_schema:
  fields:
    userId:
      type: "LONG"
      required: true
      description: "User ID"
      generation:
        method: "increment"
        start: 6000001
        step: 1
    
    eventId:
      type: "LONG"
      required: true
      description: "Event ID"
      generation:
        method: "random_int"
        min: 60000001
        max: 60002000
    
    name:
      type: "STRING"
      required: false
      description: "Name"
      generation:
        method: "template"
        template: "name_{userId}"
    
    tags:
      type: "ARRAY"
      required: false
      description: "List of tags"
      element:
        type: "STRING"
        generation:
          method: "random_text"
          length: 10
          charset: "alphanumeric"
      generation:
        method: "array"
        min_length: 1
        max_length: 5
    
    items:
      type: "ARRAY"
      required: false
      description: "List of items"
      element:
        type: "OBJECT"
        fields:
          id:
            type: "INTEGER"
            generation:
              method: "increment"
              start: 1
          value:
            type: "STRING"
            generation:
              method: "random_text"
              length: 20
      generation:
        method: "array"
        min_length: 2
        max_length: 10
```

## Context and Variable Substitution

The generator maintains a context dictionary that stores previously generated field values. This allows template methods to reference other fields:

```yaml
userId:
  type: "LONG"
  generation:
    method: "increment"
    start: 1000000

userName:
  type: "STRING"
  generation:
    method: "template"
    template: "User_{userId}"  # Will use the generated userId value
```

## Nested Objects

For nested objects (like `arguments` with TypedValue structure):

```yaml
arguments:
  type: "OBJECT"
  fields:
    amount:
      type: "DECIMAL"
      generation:
        method: "random_float"
        min: 100.0
        max: 10000.0
        precision: 2
```

The generator automatically wraps primitive values in TypedValue structure when needed.

## Best Practices

1. **Use appropriate generation methods**: Choose methods that generate realistic test data
2. **Set reasonable ranges**: Avoid extreme values that might cause validation errors
3. **Use templates for related fields**: Reference other fields using templates for consistency
4. **Configure array lengths**: Set appropriate min/max lengths for arrays based on your needs
5. **Test your configuration**: Generate a few samples to verify the output format

## Examples

### Example 1: Simple Field Generation

```yaml
age:
  type: "INTEGER"
  generation:
    method: "random_int"
    min: 18
    max: 80
```

### Example 2: Array of Strings

```yaml
categories:
  type: "ARRAY"
  element:
    type: "STRING"
    generation:
      method: "random_choice"
      values:
        - "electronics"
        - "clothing"
        - "books"
  generation:
    method: "array"
    min_length: 1
    max_length: 3
```

### Example 3: Array of Complex Objects

```yaml
products:
  type: "ARRAY"
  element:
    type: "OBJECT"
    fields:
      productId:
        type: "LONG"
        generation:
          method: "increment"
          start: 1000001
      name:
        type: "STRING"
        generation:
          method: "random_text"
          length: 30
      price:
        type: "DECIMAL"
        generation:
          method: "random_float"
          min: 10.0
          max: 1000.0
          precision: 2
  generation:
    method: "array"
    min_length: 1
    max_length: 10
```

