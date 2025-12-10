#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Swagger to Load Test Config Converter
Converts Swagger/OpenAPI specification to load test configuration files

This script automatically processes all API endpoints in the Swagger file and generates
a separate configuration file for each endpoint.

Usage:
    python3 scripts/convert_swagger_to_config.py --swagger docs/api/swagger.yaml --output_dir scripts/mock

Parameters:
    --swagger: Path to Swagger/OpenAPI YAML file (required)
    --output_dir: Output directory for configuration files (required)
"""

import argparse
import re
import yaml
from pathlib import Path
from typing import Dict, Any, Optional, List, Tuple


class SwaggerToConfigConverter:
    """Convert Swagger/OpenAPI spec to load test configuration"""
    
    def __init__(self, swagger_path: str):
        """Initialize converter with Swagger file"""
        self.swagger_path = swagger_path
        self.swagger = self._load_swagger()
    
    def _load_swagger(self) -> Dict[str, Any]:
        """Load Swagger YAML file"""
        swagger_file = Path(self.swagger_path)
        if not swagger_file.exists():
            raise FileNotFoundError(f"Swagger file not found: {self.swagger_path}")
        
        try:
            with open(swagger_file, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f)
        except yaml.YAMLError as e:
            print(f"[WARN] YAML parsing error: {e}")
            print(f"[WARN] Attempting to load with yaml.load() instead...")
            try:
                with open(swagger_file, 'r', encoding='utf-8') as f:
                    return yaml.load(f, Loader=yaml.FullLoader)
            except Exception as e2:
                raise ValueError(f"Failed to parse Swagger file: {e2}")
    
    def _get_base_url(self) -> str:
        """Get base URL from Swagger servers (first server's URL)"""
        servers = self.swagger.get("servers", [])
        if servers and len(servers) > 0:
            base = servers[0].get("url", "")
            return base
        return "http://localhost:8080/api"
    
    def _get_schema(self, schema_name: str) -> Optional[Dict[str, Any]]:
        """Get schema definition from Swagger"""
        components = self.swagger.get("components", {})
        schemas = components.get("schemas", {})
        return schemas.get(schema_name)
    
    def _get_schema_from_ref(self, ref: str) -> Optional[Dict[str, Any]]:
        """Get schema from $ref reference"""
        if not ref.startswith("#/components/schemas/"):
            return None
        schema_name = ref.split("/")[-1]
        return self._get_schema(schema_name)
    
    def _snake_case(self, text: str) -> str:
        """Convert text to snake_case"""
        # Remove special characters, replace spaces and hyphens with underscores
        text = re.sub(r'[^\w\s-]', '', text)
        text = re.sub(r'[-\s]+', '_', text)
        # Convert to lowercase
        text = text.lower()
        # Remove leading/trailing underscores
        text = text.strip('_')
        return text
    
    def _get_request_schema(self, path_info: Dict[str, Any], method: str) -> Optional[Tuple[str, Dict[str, Any]]]:
        """Extract request schema from path info"""
        method_info = path_info.get(method.lower())
        if not method_info:
            return None
        
        request_body = method_info.get("requestBody")
        if not request_body:
            return None
        
        content = request_body.get("content", {})
        json_content = content.get("application/json", {})
        schema_ref = json_content.get("schema", {})
        
        # Handle $ref
        if "$ref" in schema_ref:
            schema_name = schema_ref["$ref"].split("/")[-1]
            schema = self._get_schema(schema_name)
            if schema:
                return (schema_name, schema)
        
        # Handle direct schema definition
        if "type" in schema_ref or "properties" in schema_ref:
            return ("InlineSchema", schema_ref)
        
        return None
    
    def _map_swagger_type_to_config_type(self, swagger_type: str, format_type: Optional[str] = None) -> str:
        """Map Swagger type to configuration type"""
        type_mapping = {
            "integer": "INTEGER" if format_type != "int64" else "LONG",
            "number": "DECIMAL",
            "string": "STRING",
            "boolean": "BOOLEAN",
            "object": "OBJECT",
            "array": "OBJECT"  # Arrays are treated as objects in our config
        }
        return type_mapping.get(swagger_type, "STRING")
    
    def _get_default_generation_method(self, field_type: str, field_name: str, schema_prop: Dict[str, Any]) -> Dict[str, Any]:
        """Generate default generation method based on field type and name
        
        For mock data generation, we prioritize random generation methods over fixed values
        to generate diverse test data. Example values from Swagger are ignored.
        """
        # Generate based on field name patterns
        field_lower = field_name.lower()
        
        if "id" in field_lower:
            if "user" in field_lower:
                return {
                    "method": "increment",
                    "start": 6000001,
                    "step": 1
                }
            elif "event" in field_lower:
                # Use random_int for eventId to generate diverse test data
                return {
                    "method": "random_int",
                    "min": 60000001,
                    "max": 60002000
                }
            elif "trace" in field_lower:
                return {
                    "method": "increment",
                    "start": 6000001,
                    "step": 1
                }
            else:
                return {
                    "method": "increment",
                    "start": 1,
                    "step": 1
                }
        
        # Generate based on type
        if field_type == "INTEGER" or field_type == "LONG":
            return {
                "method": "random_int",
                "min": 1,
                "max": 100
            }
        elif field_type == "DECIMAL":
            return {
                "method": "random_float",
                "min": 0.0,
                "max": 1000.0,
                "precision": 2
            }
        elif field_type == "BOOLEAN":
            return {
                "method": "random_choice",
                "values": [True, False]
            }
        elif field_type == "STRING":
            if "name" in field_lower:
                return {
                    "method": "template",
                    "template": f"{field_name}_{{userId}}"
                }
            elif "description" in field_lower or "content" in field_lower:
                return {
                    "method": "random_text",
                    "length": 100,
                    "charset": "alphanumeric"
                }
            else:
                return {
                    "method": "random_text",
                    "length": 50,
                    "charset": "alphanumeric"
                }
        
        # Default fallback
        return {
            "method": "fixed",
            "value": None
        }
    
    def _convert_schema_property(self, prop_name: str, prop_schema: Dict[str, Any], required_fields: List[str]) -> Dict[str, Any]:
        """Convert a schema property to config field"""
        # Handle $ref references
        if "$ref" in prop_schema:
            ref_path = prop_schema["$ref"].split("/")
            schema_name = ref_path[-1]
            ref_schema = self._get_schema(schema_name)
            if ref_schema:
                return self._convert_schema_property(prop_name, ref_schema, required_fields)
        
        # Get type
        prop_type = prop_schema.get("type", "string")
        format_type = prop_schema.get("format")
        config_type = self._map_swagger_type_to_config_type(prop_type, format_type)
        
        # Check if this is an object with nested structure
        has_nested_structure = False
        if prop_type == "object":
            has_nested_structure = (
                "additionalProperties" in prop_schema or 
                "properties" in prop_schema
            )
        
        # Build field config
        field_config = {
            "type": config_type,
            "required": prop_name in required_fields,
            "description": prop_schema.get("description", "")
        }
        
        # Only add generation method for non-nested objects
        # Nested objects (with additionalProperties or properties) should use fields instead
        if not has_nested_structure:
            field_config["generation"] = self._get_default_generation_method(config_type, prop_name, prop_schema)
        
        # Handle nested objects (like arguments with TypedValue)
        if prop_type == "object":
            if "additionalProperties" in prop_schema:
                # This is a map/dictionary (like arguments)
                additional_props = prop_schema["additionalProperties"]
                if "$ref" in additional_props:
                    # Reference to TypedValue or similar
                    ref_path = additional_props["$ref"].split("/")
                    ref_schema_name = ref_path[-1]
                    ref_schema = self._get_schema(ref_schema_name)
                    if ref_schema:
                        # For arguments, we need to define example fields
                        field_config["fields"] = self._generate_example_argument_fields(prop_schema)
                elif "type" in additional_props:
                    # Direct type definition
                    field_config["fields"] = {}
            elif "properties" in prop_schema:
                # Nested object with properties
                nested_props = prop_schema.get("properties", {})
                nested_required = prop_schema.get("required", [])
                field_config["fields"] = {}
                for nested_name, nested_schema in nested_props.items():
                    field_config["fields"][nested_name] = self._convert_schema_property(
                        nested_name, nested_schema, nested_required
                    )
        
        return field_config
    
    def _generate_example_argument_fields(self, arguments_schema: Dict[str, Any]) -> Dict[str, Any]:
        """Generate example argument fields for arguments object"""
        # Check if there's an example in the schema
        example = arguments_schema.get("example", {})
        if example:
            fields = {}
            for arg_name, arg_value in example.items():
                if isinstance(arg_value, dict):
                    # TypedValue structure
                    value = arg_value.get("value")
                    value_type = arg_value.get("type", "STRING")
                    fields[arg_name] = {
                        "type": value_type,
                        "required": False,
                        "description": f"{arg_name} parameter",
                        "generation": self._get_default_generation_method(value_type, arg_name, {"example": value})
                    }
            return fields
        
        # Default example fields
        return {
            "amount": {
                "type": "DECIMAL",
                "required": False,
                "description": "Transaction amount",
                "generation": {
                    "method": "random_float",
                    "min": 100.0,
                    "max": 10000.0,
                    "precision": 2
                }
            },
            "age": {
                "type": "INTEGER",
                "required": False,
                "description": "User age",
                "generation": {
                    "method": "random_int",
                    "min": 18,
                    "max": 80
                }
            },
            "isVip": {
                "type": "BOOLEAN",
                "required": False,
                "description": "Whether user is VIP",
                "generation": {
                    "method": "random_choice",
                    "values": [True, False]
                }
            },
            "name": {
                "type": "STRING",
                "required": False,
                "description": "User name",
                "generation": {
                    "method": "template",
                    "template": "User_{userId}"
                }
            }
        }
    
    def _convert_endpoint(self, path: str, method: str, path_info: Dict[str, Any], base_url: str) -> Optional[Dict[str, Any]]:
        """Convert a single endpoint to configuration"""
        method_info = path_info.get(method.lower())
        if not method_info:
            return None
        
        # Get request schema
        schema_result = self._get_request_schema(path_info, method)
        if not schema_result:
            # Skip endpoints without request body
            return None
        
        schema_name, schema = schema_result
        
        # Get summary for filename
        summary = method_info.get("summary", f"{method.upper()} {path}")
        
        # Build API config
        api_config = {
            "name": f"{summary} API",
            "method": method.upper(),
            "path": path,
            "base_url": base_url,
            "full_url": f"{base_url}{path}",
            "description": method_info.get("description", summary),
            "headers": {
                "Content-Type": "application/json"
            },
            "timeout": 30
        }
        
        # Build request schema
        properties = schema.get("properties", {})
        required_fields = schema.get("required", [])
        
        fields = {}
        for prop_name, prop_schema in properties.items():
            fields[prop_name] = self._convert_schema_property(prop_name, prop_schema, required_fields)
        
        request_schema = {
            "fields": fields
        }
        
        return {
            "api": api_config,
            "request_schema": request_schema,
            "filename": self._snake_case(summary) + ".mock"
        }
    
    def convert_all(self, output_dir: str):
        """Convert all endpoints in Swagger file to configuration files"""
        paths = self.swagger.get("paths", {})
        base_url = self._get_base_url()
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        http_methods = ["get", "post", "put", "delete", "patch", "head", "options"]
        converted_count = 0
        skipped_count = 0
        
        print(f"[INFO] Processing Swagger file: {self.swagger_path}")
        print(f"[INFO] Base URL (from servers[0].url): {base_url}")
        print(f"[INFO] Output directory: {output_dir}")
        print()
        
        for path, path_info in paths.items():
            for method in http_methods:
                if method.lower() in path_info:
                    config = self._convert_endpoint(path, method, path_info, base_url)
                    if config:
                        filename = config.pop("filename")
                        filepath = output_path / filename
                        
                        # Save configuration
                        with open(filepath, 'w', encoding='utf-8') as f:
                            yaml.dump(config, f, default_flow_style=False, allow_unicode=True, sort_keys=False, indent=2)
                        
                        print(f"[INFO] Generated: {filepath} (path: {path}, method: {method.upper()})")
                        converted_count += 1
                    else:
                        skipped_count += 1
        
        print()
        print(f"[INFO] Conversion completed!")
        print(f"[INFO] Generated {converted_count} configuration files")
        if skipped_count > 0:
            print(f"[INFO] Skipped {skipped_count} endpoints (no request body)")


def main():
    parser = argparse.ArgumentParser(
        description="Convert Swagger/OpenAPI to load test configuration files",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Convert all endpoints from Swagger file
  python3 scripts/convert_swagger_to_config.py --swagger docs/api/swagger.yaml --output_dir scripts/mock

Note:
  - Base URL is automatically extracted from servers[0].url in Swagger file
  - Each endpoint with a request body will generate a separate .mock file
  - File names are generated from the endpoint's summary field (snake_case)
        """
    )
    parser.add_argument("--swagger", type=str, required=True,
                       help="Path to Swagger/OpenAPI YAML file (required)")
    parser.add_argument("--output_dir", type=str, required=True,
                       help="Output directory for configuration files (required)")
    
    args = parser.parse_args()
    
    try:
        converter = SwaggerToConfigConverter(args.swagger)
        converter.convert_all(args.output_dir)
        
        print(f"\n[INFO] You can now use these configuration files:")
        print(f"  python3 scripts/insert_data.py --config scripts/mock/<filename>.mock --count 1000 --output scripts/out/mock_data.json")
        print(f"  python3 scripts/load_test.py --config scripts/mock/<filename>.mock --concurrent 10 --total 1000")

    except Exception as e:
        print(f"[ERROR] Conversion failed: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        exit(1)


if __name__ == "__main__":
    main()
