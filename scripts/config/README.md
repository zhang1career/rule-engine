# Load Test Configuration

This directory contains configuration files for load testing and mock data generation.

## Files

- `load_test_eval_config.yaml`: Configuration file defining API interface and data generation rules

## Configuration File Structure

The configuration file contains two main sections:

### 1. API Interface Definition (`api`)

Defines the API endpoint information:
- `name`: API name
- `method`: HTTP method (POST, GET, etc.)
- `path`: API path
- `base_url`: Base URL
- `full_url`: Complete API URL
- `headers`: HTTP headers
- `timeout`: Request timeout in seconds

### 2. Request Schema (`request_schema`)

Defines the request data structure and generation rules:

#### Root Level Fields

- `userId`: User ID field with generation rules
- `eventId`: Event ID field with generation rules
- `traceId`: Trace ID field with generation rules
- `arguments`: Nested object containing rule calculation parameters

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

## Usage

### Load Test Script

The load test script automatically reads from this configuration:

```bash
# Use default config
python3 scripts/load_test_eval.py --concurrent 10 --total 1000

# Use custom config
python3 scripts/load_test_eval.py --config scripts/config/custom_config.yaml --concurrent 10 --total 1000
```

### Mock Data Generator

Generate mock data based on configuration:

```bash
# Generate single mock data
python3 scripts/generate_mock_data.py

# Generate multiple mock data
python3 scripts/generate_mock_data.py --count 100

# Output to file
python3 scripts/generate_mock_data.py --count 100 --output mock_data.json

# Use custom config
python3 scripts/generate_mock_data.py --config scripts/config/custom_config.yaml --count 10
```

## Customization

To customize the test data generation:

1. Edit `load_test_eval_config.yaml`
2. Modify field generation rules as needed
3. Run load test or mock data generator with the updated config

The configuration supports flexible data generation rules, allowing you to:
- Change value ranges
- Switch between different generation methods
- Add or remove fields
- Customize nested structures

