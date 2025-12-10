# Swagger to Config Converter

This script converts Swagger/OpenAPI specification files to load test configuration files.

## Usage

```bash
# Convert all endpoints from Swagger file
python3 scripts/convert_swagger_to_mock.py \
  --swagger docs/api/swagger.yaml \
  --output_dir scripts/mock
```

## Parameters

- `--swagger`: Path to Swagger/OpenAPI YAML file (required)
- `--output_dir`: Output directory for configuration files (required)

## How It Works

The converter automatically:

1. **Reads Swagger file**: Loads the OpenAPI/Swagger specification
2. **Extracts base URL**: Gets base URL from `servers[0].url` in Swagger file
   - If the URL ends with `/api`, it will be removed
   - Example: `http://localhost:8080/api` → `http://localhost:8080`
3. **Processes all endpoints**: Iterates through all paths in the Swagger file
4. **Extracts endpoint information**:
   - **Path**: From `paths/{path_value}`
   - **Method**: From `paths/{path_value}/{http_method}` (get, post, put, delete, etc.)
   - **Schema**: From `paths/{path_value}/{http_method}/requestBody/content/application/json/schema/$ref`
   - **Summary**: From `paths/{path_value}/{http_method}/summary` (used for filename)
5. **Generates configuration files**:
   - One `.mock` file per endpoint that has a request body
   - Filename: `{summary_snake_case}.mock`
   - Example: `Execute rule evaluation` → `execute_rule_evaluation.mock`
6. **Maps types**: Converts Swagger types to configuration types:
   - `integer` → `INTEGER` or `LONG` (based on format)
   - `number` → `DECIMAL`
   - `string` → `STRING`
   - `boolean` → `BOOLEAN`
   - `object` → `OBJECT`
7. **Generates default rules**: Creates default generation methods based on:
   - Field names (e.g., `userId` → increment, `eventId` → fixed)
   - Field types (e.g., INTEGER → random_int, DECIMAL → random_float)
   - Example values from Swagger

## Generated Configuration Structure

The converter generates configuration files with:

```yaml
api:
  name: "Execute rule evaluation API"
  method: "POST"
  path: "/evalRequest"
  base_url: "http://localhost:8080"  # From servers[0].url
  full_url: "http://localhost:8080/api/evalRequest"
  description: "..."
  headers:
    Content-Type: "application/json"
  timeout: 30

request_schema:
  fields:
    userId:
      type: "LONG"
      required: true
      description: "User ID (required, must be positive)"
      generation:
        method: "increment"
        start: 1000000
        step: 1
    # ... more fields
```

## Base URL Extraction

The base URL is automatically extracted from the Swagger file's `servers` section:

- **Source**: `servers[0].url` (first server URL in the list)
- **Processing**: If the URL ends with `/api`, it is removed
- **Example**:
  ```yaml
  servers:
    - url: http://localhost:8080/api
      description: Development server
  ```
  Results in: `base_url: "http://localhost:8080"`

## File Naming Convention

Configuration files are named based on the endpoint's `summary` field:

- **Source**: `paths/{path}/{method}/summary`
- **Conversion**: Converted to snake_case
- **Suffix**: `.mock`
- **Examples**:
  - `Execute rule evaluation` → `execute_rule_evaluation.mock`
  - `Query enumeration values` → `query_enumeration_values.mock`
  - `Create event` → `create_event.mock`

## Customization After Conversion

After conversion, you should review and customize:

1. **Generation methods**: Adjust generation rules based on your needs
2. **Value ranges**: Modify min/max values for random generation
3. **Fixed values**: Change fixed values if needed
4. **Additional fields**: Add fields that might not be in Swagger but are needed for testing

## Example Workflow

1. **Convert from Swagger**:
   ```bash
   python3 scripts/convert_swagger_to_mock.py \
     --swagger docs/api/swagger.yaml \
     --output_dir scripts/mock
   ```

2. **Review generated files**:
   ```bash
   ls scripts/mock/*.mock
   ```

3. **Customize configuration files** as needed

4. **Use in load testing**:
   ```bash
   python3 scripts/load_test.py \
     --config scripts/mock/execute_rule_evaluation.mock \
     --concurrent 10 \
     --total 1000
   ```

## Limitations

- The converter generates **default** generation rules. You should review and customize them.
- Only endpoints with `requestBody` are processed (GET endpoints without request body are skipped)
- Complex nested structures might need manual adjustment
- Some Swagger features (like `oneOf`, `anyOf`) are not fully supported
- The converter uses heuristics to determine generation methods based on field names and types

## Troubleshooting

### YAML Parsing Errors

If you encounter YAML parsing errors:

1. Check if the Swagger file is valid YAML
2. Try using a different YAML parser or fix the Swagger file
3. The script will attempt to use `yaml.load()` as a fallback

### No Files Generated

If no configuration files are generated:

1. Check if the Swagger file has endpoints with `requestBody`
2. GET endpoints without request body are skipped
3. Verify the Swagger file structure is correct

### Missing Fields

If some fields are missing in the generated config:

1. Check if the fields are in the Swagger schema
2. Fields marked as `nullable: true` might be skipped
3. You may need to manually add missing fields

### Base URL Issues

If the base URL is incorrect:

1. Check the `servers` section in Swagger file
2. The converter uses `servers[0].url` (first server)
3. If `/api` suffix is present, it will be automatically removed
4. You can manually edit the generated `.mock` files to fix the base URL
