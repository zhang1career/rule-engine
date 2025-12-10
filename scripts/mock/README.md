# Mock Configuration Files

This directory contains mock data generation configuration files for load testing.

## Files

- `*.mock`: Configuration files defining API interface and data generation rules
  - File names are generated from Swagger endpoint summaries (snake_case)
  - Each file corresponds to one API endpoint

## Configuration File Structure

The configuration file contains two main sections:

### 1. API Interface Definition (`api`)

Defines the API endpoint information:
- `name`: API name
- `method`: HTTP method (POST, GET, etc.)
- `path`: API path
- `base_url`: Base URL (extracted from Swagger servers[0].url)
- `full_url`: Complete API URL
- `headers`: HTTP headers
- `timeout`: Request timeout in seconds

### 2. Request Schema (`request_schema`)

Defines the request data structure and generation rules:

#### Root Level Fields

- Field definitions with type, required status, and description
- Each field has generation rules

#### Field Generation Methods

Each field can use one of the following generation methods:

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
    values: [1001, 1002, 1003]
  ```
