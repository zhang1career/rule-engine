# Controller to Swagger Generator

This script generates Swagger/OpenAPI specification from Spring Boot Controller classes, with support for detecting `@RequestHeader` annotations.

## Features

- **Automatic Detection**: Scans all Controller classes in the specified directory
- **HTTP Method Detection**: Identifies `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- **Path Extraction**: Extracts paths from `@RequestMapping` (class-level) and method-level annotations
- **Header Support**: **Detects `@RequestHeader` annotations and includes them in Swagger parameters**
- **Parameter Detection**: Identifies `@PathVariable`, `@RequestParam`, `@RequestBody`
- **OpenAPI 3.0.3**: Generates compliant OpenAPI 3.0.3 YAML

## Usage

```bash
# Generate Swagger from controllers
python3 scripts/generate_swagger_from_controller.py \
    --controller_dir src/main/java/lab/zhang/rule/rule_engine/controller \
    --output docs/api/swagger.yaml
```

## Parameters

- `--controller_dir`: Directory containing Controller Java files (required)
- `--output`: Output path for Swagger YAML file (required)
- `--base_package`: Base package name (default: `lab.zhang.rule.rule_engine`)

## How It Works

1. **Scans Controller Files**: Finds all `*Controller.java` files in the specified directory
2. **Extracts Class-Level Mapping**: Reads `@RequestMapping` from class level to get base path
3. **Processes Methods**: For each public method:
   - Detects HTTP method annotation (`@GetMapping`, `@PostMapping`, etc.)
   - Extracts path from method annotation
   - Parses method parameters
4. **Detects Headers**: **Identifies `@RequestHeader` annotations and extracts:**
   - Header name (from `value` attribute or parameter name)
   - Required flag (from `required` attribute)
   - Parameter type (from Java type)
5. **Generates Swagger**: Creates OpenAPI 3.0.3 YAML with:
   - Paths and operations
   - Parameters (including headers in `parameters` section with `in: header`)
   - Request bodies
   - Default responses

## Header Detection Examples

The script can detect headers in the following formats:

```java
// With explicit value
@GetMapping("/users")
public ResponseEntity<UserDTO> getUser(
    @RequestHeader(value = "Authorization", required = true) String authToken) {
    // ...
}

// Without value (uses parameter name)
@GetMapping("/users")
public ResponseEntity<UserDTO> getUser(
    @RequestHeader("X-API-Key") String apiKey) {
    // ...
}

// Multiple headers
@PostMapping("/data")
public ResponseEntity<DataDTO> createData(
    @RequestHeader("Authorization") String auth,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
    // ...
}
```

## Generated Swagger Output

Headers detected from `@RequestHeader` annotations will appear in the Swagger YAML as:

```yaml
paths:
  /users:
    get:
      parameters:
        - name: Authorization
          in: header
          required: true
          description: Header parameter: Authorization
          schema:
            type: string
        - name: X-API-Key
          in: header
          required: true
          description: Header parameter: X-API-Key
          schema:
            type: string
```

## Limitations

- Currently generates basic schemas for complex types (returns `object` type)
- Does not extract detailed schema information from DTO classes
- Does not parse Javadoc for parameter descriptions (uses default descriptions)
- Does not detect response types automatically (uses default `ApiResponseVoid`)

## Integration with Existing Workflow

This script can be used to:
1. **Generate initial Swagger**: Create Swagger YAML from existing controllers
2. **Update Swagger**: Regenerate when controllers change
3. **Verify Headers**: Ensure all `@RequestHeader` annotations are documented

## Example Workflow

```bash
# 1. Generate Swagger from controllers
python3 scripts/generate_swagger_from_controller.py \
    --controller_dir src/main/java/lab/zhang/rule/rule_engine/controller \
    --output docs/api/swagger.yaml

# 2. Review generated Swagger (especially headers section)
cat docs/api/swagger.yaml | grep -A 5 "in: header"

# 3. Use generated Swagger with other tools
python3 scripts/convert_swagger_to_mock.py \
    --swagger docs/api/swagger.yaml \
    --output_dir scripts/mock
```

## Notes

- The script preserves existing Swagger structure (components, schemas, responses)
- Headers are automatically detected and added to the `parameters` section
- All detected headers are marked with `in: header` in the generated Swagger
- Header names are extracted from the `@RequestHeader` annotation's `value` attribute or parameter name
