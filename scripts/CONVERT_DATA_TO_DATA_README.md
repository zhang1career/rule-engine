# Mock Configuration Files

This directory contains mock data generation configuration files for load testing.

## 1. Files

- `*.mock`: Configuration files defining API interface and data generation rules
  - File names are generated from Swagger endpoint summaries (snake_case)
  - Each file corresponds to one API endpoint

## 2. Configuration File Content

The configuration file contains two main sections:

### 2.1. API Interface Definition (`api`)

Defines the API endpoint information:
- `name`: API name
- `method`: HTTP method (POST, GET, etc.)
- `path`: API path
- `base_url`: Base URL (extracted from Swagger servers[0].url)
- `full_url`: Complete API URL
- `headers`: HTTP headers
- `timeout`: Request timeout in seconds


### 2.2. Generation Methods

Each value to be generated can use one of the following generation methods:

- **`fixed`**: Use a fixed value
  ```yaml
  generation:
    method: "fixed"
    value: 1001
  ```

- **`increment`**: Increment from start value with step
  ```yaml
  generation:
    method: "increment"
    start: 1000000
    step: 1
  ```

- **`random_int`**: Random integer between min and max
  ```yaml
  generation:
    method: "random_int"
    min: 18
    max: 80
  ```

- **`random_float`**: Random float between min and max with precision
  ```yaml
  generation:
    method: "random_float"
    min: 100.0
    max: 10000.0
    precision: 2
  ```

- **`random_choice`**: Random selection from a list of values
  ```yaml
  generation:
    method: "random_choice"
    values: [true, false]
  ```

- **`template`**: String template with variable substitution
  ```yaml
  generation:
    method: "template"
    template: "User_{userId}"
  ```

- **`list`**: Sequential selection from a list (cycles through)
  ```yaml
  generation:
    method: "list"
    min_length: 3
    max_length: 15
    element:
      type: Long
      generation:
        method: random_int
        min: 10000000
        max: 10009999
  ```

- **`array`**: Generate an array of elements
  ```yaml
  generation:
    method: "array"
    values: [1001, 1002, 1003]
  ```

### 2.3. Generated Values

Supported Field Types

- `INTEGER`: Integer values
- `LONG`: Long integer values (64-bit)
- `DECIMAL`: Decimal/float values
- `BOOLEAN`: Boolean values (true/false)
- `STRING`: String values
- `OBJECT`: Object/struct values
- `ARRAY`: Array/list values


## 3. Request Schema (`request_schema`)

The following is an example:

```yaml
request_schema:
  fields:
    userId:
      type: LONG
      required: true
      description: "User ID (required, must be positive)"
      generation:
        method: "increment"
        start: 1000000
        step: 1
```

## 4. Path Variable (`path_variables`)

The following is an example: 

```yaml
api:
  path_variables:
    groupId:
      type: LONG
      required: true
      generation:
        method: increment
        start: 10000000
        step: 1
```

## 5. Headers (`headers_generation`)

The following is an example:

```yaml
api:
  headers_generation:
    Authorization:
      type: STRING
      required: true
      description: "Header parameter: Authorization"
      generation:
        method: random_text
        length: 32
        charset: alphanumeric
        prefix: "Bearer "
    X-API-Key:
      type: STRING
      required: true
      description: "Header parameter: X-API-Key"
      generation:
        method: random_text
        length: 32
        charset: alphanumeric
```

### 5.1. Header Generation Rules

Headers are generated based on their names and types:
- **Authorization/Token headers**: Generated with `Bearer ` prefix and random token
- **Other headers**: Generated based on their type (STRING, INTEGER, etc.)

### 5.2. Header Generation Methods

Supported generation methods:
- `random_text`: Generate random text (supports `prefix` for Bearer tokens)
- `random_int`: Generate random integers
- `increment`: Generate incrementing numbers
- `random_choice`: Choose from a list of values
- `template`: Generate from template with variable substitution

### 5.3. Notes

- Headers are generated for each request independently
- Default headers (like `Content-Type`) are always included
- Generated headers are merged with default headers
- If a header value is provided in context, it will be used instead of generating a new one
- Header values are converted to strings before being sent in requests


## 6. Advanced Features

### 6.1. Context and Variable Substitution

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

### 6.2. Nested Objects

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


## 7. Best Practices

1. **Use appropriate generation methods**: Choose methods that generate realistic test data
2. **Set reasonable ranges**: Avoid extreme values that might cause validation errors
3. **Use templates for related fields**: Reference other fields using templates for consistency
4. **Configure array lengths**: Set appropriate min/max lengths for arrays based on your needs
5. **Test your configuration**: Generate a few samples to verify the output format


### 7.1. Example: Simple Field Generation

```yaml
age:
  type: "INTEGER"
  generation:
    method: "random_int"
    min: 18
    max: 80
```

### 7.2. Example: Array of Strings

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

### 7.3. Example: Array of Complex Objects

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
