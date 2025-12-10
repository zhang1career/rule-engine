# Mock Data Generation Guide

This guide explains how to use the mock data generation system for load testing.

## Quick Start

### 1. Generate Mock Data

Generate a single mock data object:

```bash
python3 scripts/mock_data.py
```

Output:
```json
{
  "userId": 1000000,
  "eventId": 1001,
  "traceId": 1000000,
  "arguments": {
    "amount": {
      "value": 5234.56,
      "type": "DECIMAL"
    },
    "age": {
      "value": 35,
      "type": "INTEGER"
    },
    "isVip": {
      "value": true,
      "type": "BOOLEAN"
    },
    "name": {
      "value": "User_1000000",
      "type": "STRING"
    }
  }
}
```

### 2. Generate Multiple Mock Data

Generate 100 mock data objects and save to file:

```bash
python3 scripts/mock_data.py --count 100 --output mock_data.json --pretty
```

### 3. Use in Load Testing

The load test script automatically uses the configuration:

```bash
# Use default config
python3 scripts/load_test.py --concurrent 10 --total 1000

# Use custom config
python3 scripts/load_test.py --config scripts/mock/custom_config.mock --concurrent 20 --total 5000
```

## Configuration File Structure

The configuration file (`scripts/mock/*.mock`) defines:

### API Interface

```yaml
api:
  name: "EvalRequest API"
  method: "POST"
  path: "/api/eval"
  base_url: "http://localhost:8080"
  full_url: "http://localhost:8080/api/eval"
```

### Data Generation Rules

Each field can have different generation methods:

#### Fixed Value
```yaml
eventId:
  generation:
    method: "fixed"
    value: 1001
```

#### Incrementing Counter
```yaml
userId:
  generation:
    method: "increment"
    start: 1000000
    step: 1
```

#### Random Integer
```yaml
age:
  generation:
    method: "random_int"
    min: 18
    max: 80
```

#### Random Float
```yaml
amount:
  generation:
    method: "random_float"
    min: 100.0
    max: 10000.0
    precision: 2
```

#### Random Choice
```yaml
isVip:
  generation:
    method: "random_choice"
    values: [true, false]
```

#### Template String
```yaml
name:
  generation:
    method: "template"
    template: "User_{userId}"
```

## Customization Examples

### Example 1: Change Amount Range

Edit `scripts/mock/execute_rule_evaluation.mock`:

```yaml
amount:
  generation:
    method: "random_float"
    min: 50.0      # Changed from 100.0
    max: 5000.0   # Changed from 10000.0
    precision: 2
```

### Example 2: Add New Field

Add a new field to arguments:

```yaml
arguments:
  fields:
    # ... existing fields ...
    city:
      type: "STRING"
      generation:
        method: "random_choice"
        values: ["Beijing", "Shanghai", "Guangzhou", "Shenzhen"]
```

### Example 3: Use Sequential Event IDs

```yaml
eventId:
  generation:
    method: "list"
    values: [1001, 1002, 1003, 1004, 1005]
```

## Advanced Usage

### Generate Mock Data for Different Scenarios

Create multiple configuration files for different test scenarios:

```bash
# Create config for high-value transactions
cp scripts/mock/execute_rule_evaluation.mock scripts/mock/high_value_config.mock
# Edit high_value_config.mock to set amount min=5000, max=50000

# Generate mock data
python3 scripts/mock_data.py --config scripts/mock/high_value_config.mock --count 50 --output high_value_mock.json

# Run load test
python3 scripts/load_test.py --config scripts/mock/high_value_config.mock --concurrent 20 --total 1000
```

### Integration with CI/CD

Generate mock data as part of test setup:

```bash
#!/bin/bash
# test_setup.sh

# Generate test data
python3 scripts/mock_data.py --count 1000 --output test_data.json

# Run load test
python3 scripts/load_test.py --concurrent 50 --total 10000
```

## Benefits

1. **Centralized Configuration**: All data generation rules in one place
2. **Flexible Generation**: Multiple generation methods for different needs
3. **Reusable**: Same configuration for mock data generation and load testing
4. **Maintainable**: Easy to update data structures and ranges
5. **Extensible**: Easy to add new fields and generation methods

## Troubleshooting

### Configuration File Not Found

If you see:
```
[WARN] Failed to load config file: FileNotFoundError
```

Make sure the configuration file exists at the specified path, or use `--config` to specify the correct path.

### Invalid Generation Method

If you see:
```
ValueError: Unsupported generation method: xxx
```

Check the configuration file and ensure the generation method is one of the supported methods (see `scripts/mock/README.md`).

## Next Steps

- Review `scripts/mock/*.mock` files for current configuration
- Customize data generation rules for your specific needs
- Generate mock data for testing and development
- Use in load testing to validate API performance

