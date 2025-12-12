#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Controller to Swagger Generator
Generates Swagger/OpenAPI specification from Spring Boot Controller classes

This script scans Java Controller files and generates Swagger YAML documentation,
including support for @RequestHeader annotations.

Usage:
    python3 scripts/generate_swagger_from_controller.py \
        --controller_dir src/main/java/lab/zhang/rule/rule_engine/controller \
        --output docs/api/swagger.yaml

Features:
    - Detects HTTP method annotations (@GetMapping, @PostMapping, etc.)
    - Extracts path mappings from @RequestMapping and method-level annotations
    - Identifies @RequestHeader parameters and adds them to Swagger parameters
    - Identifies @PathVariable, @RequestParam, @RequestBody parameters
    - Generates OpenAPI 3.0.3 compatible YAML
"""

import argparse
import re
import yaml
from pathlib import Path
from typing import Dict, Any, List, Optional, Tuple
from collections import defaultdict


class ControllerToSwaggerGenerator:
    """Generate Swagger/OpenAPI spec from Spring Boot Controllers"""
    
    def __init__(self, controller_dir: str, base_package: str = "lab.zhang.rule.rule_engine"):
        """Initialize generator with controller directory"""
        self.controller_dir = Path(controller_dir)
        self.base_package = base_package
        self.swagger = self._init_swagger()
        self.endpoints = []
    
    def _init_swagger(self) -> Dict[str, Any]:
        """Initialize base Swagger structure"""
        return {
            "openapi": "3.0.3",
            "info": {
                "title": "Rule Engine API",
                "description": "RESTful API for Rule Engine System - Event, Rule, and Rule Group Management",
                "version": "1.0.0",
                "contact": {
                    "name": "Rule Engine Team",
                    "email": "support@rule-engine.com"
                }
            },
            "servers": [
                {
                    "url": "http://localhost:8080/api",
                    "description": "Development server"
                },
                {
                    "url": "https://api.rule-engine.com/api",
                    "description": "Production server"
                }
            ],
            "tags": [],
            "paths": {},
            "components": {
                "parameters": {},
                "responses": {
                    "BadRequest": {
                        "description": "Bad request - Invalid parameters",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": "#/components/schemas/ApiResponseVoid"
                                },
                                "example": {
                                    "code": 400,
                                    "msg": "Invalid request parameters",
                                    "data": None
                                }
                            }
                        }
                    },
                    "NotFound": {
                        "description": "Resource not found",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": "#/components/schemas/ApiResponseVoid"
                                },
                                "example": {
                                    "code": 404,
                                    "msg": "Resource not found",
                                    "data": None
                                }
                            }
                        }
                    },
                    "InternalServerError": {
                        "description": "Internal server error",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": "#/components/schemas/ApiResponseVoid"
                                },
                                "example": {
                                    "code": 500,
                                    "msg": "Internal server error",
                                    "data": None
                                }
                            }
                        }
                    }
                },
                "schemas": {
                    "ApiResponseVoid": {
                        "type": "object",
                        "properties": {
                            "code": {
                                "type": "integer",
                                "description": "Response code (0 for success, non-zero for error)",
                                "example": 0
                            },
                            "msg": {
                                "type": "string",
                                "description": "Response message",
                                "example": "success"
                            },
                            "data": {
                                "type": "object",
                                "nullable": True,
                                "description": "Response data (null for void responses)"
                            }
                        }
                    }
                }
            }
        }
    
    def _read_java_file(self, file_path: Path) -> str:
        """Read Java file content"""
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    
    def _extract_class_level_mapping(self, content: str) -> Tuple[str, Optional[str]]:
        """Extract @RequestMapping value from class level"""
        # Match @RequestMapping(value = "/path") or @RequestMapping("/path")
        pattern = r'@RequestMapping\s*\([^)]*value\s*=\s*["\']([^"\']+)["\']'
        match = re.search(pattern, content)
        if match:
            base_path = match.group(1)
        else:
            # Try without value= prefix
            pattern2 = r'@RequestMapping\s*\(["\']([^"\']+)["\']'
            match2 = re.search(pattern2, content)
            base_path = match2.group(1) if match2 else ""
        
        # Extract tag from class comment or class name
        tag_pattern = r'@RestController|@Controller'
        tag_match = re.search(tag_pattern, content)
        tag = None
        if tag_match:
            # Try to extract from class comment
            comment_pattern = r'/\*\*.*?\* ([^*]+)'
            comment_match = re.search(comment_pattern, content, re.DOTALL)
            if comment_match:
                tag = comment_match.group(1).strip().split()[0] if comment_match.group(1) else None
        
        return base_path, tag
    
    def _extract_method_info(self, content: str, method_start: int) -> Dict[str, Any]:
        """Extract method information including annotations and parameters"""
        # Find method signature - look backwards for annotations
        # Look back up to 200 characters to find annotations
        search_start = max(0, method_start - 200)
        method_block = content[search_start:method_start + 100]
        
        # Extract method signature
        method_end = content.find('{', method_start)
        if method_end == -1:
            return {}
        
        method_signature = content[method_start:method_end]
        
        # Extract HTTP method annotation - search in the method block (before and including method signature)
        http_method = None
        method_path = ""
        
        for annotation in ['@GetMapping', '@PostMapping', '@PutMapping', '@DeleteMapping', '@PatchMapping']:
            # Search in method block (includes annotations before method)
            # Pattern 1: @GetMapping(value = "/path")
            pattern = rf'{annotation}\s*\([^)]*value\s*=\s*["\']([^"\']+)["\']'
            match = re.search(pattern, method_block)
            if match:
                http_method = annotation.replace('@', '').replace('Mapping', '').lower()
                method_path = match.group(1)
                break
            
            # Pattern 2: @GetMapping("/path")
            pattern2 = rf'{annotation}\s*\(["\']([^"\']+)["\']'
            match2 = re.search(pattern2, method_block)
            if match2:
                http_method = annotation.replace('@', '').replace('Mapping', '').lower()
                method_path = match2.group(1)
                break
            
            # Pattern 3: @GetMapping() or @GetMapping (no parentheses)
            pattern3 = rf'{annotation}(?:\s*\(\))?'
            if re.search(pattern3, method_block):
                http_method = annotation.replace('@', '').replace('Mapping', '').lower()
                method_path = ""
                break
        
        if not http_method:
            return {}
        
        # Extract method name
        method_name_match = re.search(r'public\s+[\w<>,\s]+\s+(\w+)\s*\(', method_signature)
        if not method_name_match:
            method_name_match = re.search(r'(\w+)\s*\(', method_signature)
        method_name = method_name_match.group(1) if method_name_match else ""
        
        # Extract Javadoc comment - look backwards from method_start
        comment_pattern = r'/\*\*.*?\* ([^*]+)'
        # Search in the 500 characters before method_start
        search_start = max(0, method_start - 500)
        comment_match = re.search(comment_pattern, content[search_start:method_start], re.DOTALL)
        description = ""
        if comment_match:
            description = comment_match.group(1).strip()
            # Clean up description - remove * and extra whitespace
            description = re.sub(r'\*\s*', '', description)
            description = re.sub(r'\s+', ' ', description)
        
        # Extract parameters
        params = self._extract_parameters(method_signature)
        
        # Extract return type
        return_type_match = re.search(r'public\s+([\w<>,\s]+)\s+\w+\s*\(', method_signature)
        return_type = return_type_match.group(1).strip() if return_type_match else "void"
        
        return {
            "http_method": http_method,
            "path": method_path,
            "method_name": method_name,
            "description": description,
            "parameters": params,
            "return_type": return_type
        }
    
    def _extract_parameters(self, method_signature: str) -> List[Dict[str, Any]]:
        """Extract method parameters with annotations"""
        params = []
        
        # Extract parameter list
        param_pattern = r'\(([^)]*)\)'
        param_match = re.search(param_pattern, method_signature)
        if not param_match:
            return params
        
        param_str = param_match.group(1)
        if not param_str.strip():
            return params
        
        # Split parameters (handle generics and annotations)
        param_parts = self._split_parameters(param_str)
        
        for param_part in param_parts:
            param_info = self._parse_parameter(param_part.strip())
            if param_info:
                params.append(param_info)
        
        return params
    
    def _split_parameters(self, param_str: str) -> List[str]:
        """Split parameter string into individual parameters"""
        params = []
        current = ""
        depth = 0
        in_string = False
        string_char = None
        
        for char in param_str:
            if char in ['"', "'"] and (not current or current[-1] != '\\'):
                if not in_string:
                    in_string = True
                    string_char = char
                elif char == string_char:
                    in_string = False
                    string_char = None
            elif not in_string:
                if char == '<':
                    depth += 1
                elif char == '>':
                    depth -= 1
                elif char == ',' and depth == 0:
                    if current.strip():
                        params.append(current.strip())
                    current = ""
                    continue
            current += char
        
        if current.strip():
            params.append(current.strip())
        
        return params
    
    def _parse_parameter(self, param_str: str) -> Optional[Dict[str, Any]]:
        """Parse a single parameter string into parameter info"""
        param_info = {}
        
        # Check for @RequestHeader
        header_match = re.search(r'@RequestHeader\s*\([^)]*value\s*=\s*["\']([^"\']+)["\']', param_str)
        if header_match:
            header_name = header_match.group(1)
            param_info["type"] = "header"
            param_info["name"] = header_name
            param_info["in"] = "header"
            param_info["required"] = "required" not in param_str or "required = false" not in param_str
        else:
            # Try without value= prefix
            header_match2 = re.search(r'@RequestHeader\s*\(["\']([^"\']+)["\']', param_str)
            if header_match2:
                header_name = header_match2.group(1)
                param_info["type"] = "header"
                param_info["name"] = header_name
                param_info["in"] = "header"
                param_info["required"] = True
            else:
                # Check for @PathVariable
                path_match = re.search(r'@PathVariable\s*\([^)]*value\s*=\s*["\']([^"\']+)["\']', param_str)
                if path_match:
                    path_name = path_match.group(1)
                    param_info["type"] = "path"
                    param_info["name"] = path_name
                    param_info["in"] = "path"
                    param_info["required"] = True
                else:
                    # Try without value= prefix
                    path_match2 = re.search(r'@PathVariable\s*\(["\']([^"\']+)["\']', param_str)
                    if path_match2:
                        path_name = path_match2.group(1)
                        param_info["type"] = "path"
                        param_info["name"] = path_name
                        param_info["in"] = "path"
                        param_info["required"] = True
                    else:
                        # Check for @RequestParam
                        query_match = re.search(r'@RequestParam\s*\([^)]*value\s*=\s*["\']([^"\']+)["\']', param_str)
                        if query_match:
                            query_name = query_match.group(1)
                            param_info["type"] = "query"
                            param_info["name"] = query_name
                            param_info["in"] = "query"
                            param_info["required"] = "required" not in param_str or "required = false" not in param_str
                        else:
                            # Try without value= prefix
                            query_match2 = re.search(r'@RequestParam\s*\(["\']([^"\']+)["\']', param_str)
                            if query_match2:
                                query_name = query_match2.group(1)
                                param_info["type"] = "query"
                                param_info["name"] = query_name
                                param_info["in"] = "query"
                                param_info["required"] = True
                            else:
                                # Check for @RequestBody
                                if '@RequestBody' in param_str:
                                    param_info["type"] = "body"
                                    param_info["in"] = "body"
                                else:
                                    return None
        
        # Extract parameter type
        type_match = re.search(r'(\w+(?:<\w+>)?)\s+\w+\s*[,)]', param_str)
        if type_match:
            param_info["java_type"] = type_match.group(1)
        else:
            # Try to extract type before parameter name
            type_match2 = re.search(r'(\w+(?:<\w+>)?)\s+(\w+)\s*$', param_str)
            if type_match2:
                param_info["java_type"] = type_match2.group(1)
        
        # Extract parameter name
        name_match = re.search(r'(\w+)\s*[,)]', param_str)
        if name_match:
            param_info["param_name"] = name_match.group(1)
        
        # Extract validation annotations
        if '@NotNull' in param_str:
            param_info["required"] = True
        if '@Min' in param_str:
            min_match = re.search(r'@Min\s*\(\s*value\s*=\s*(\d+)', param_str)
            if min_match:
                param_info["minimum"] = int(min_match.group(1))
        
        return param_info
    
    def _java_type_to_swagger_type(self, java_type: str) -> Dict[str, Any]:
        """Convert Java type to Swagger schema type"""
        java_type = java_type.strip()
        
        if java_type == "String":
            return {"type": "string"}
        elif java_type in ["int", "Integer"]:
            return {"type": "integer", "format": "int32"}
        elif java_type in ["long", "Long"]:
            return {"type": "integer", "format": "int64"}
        elif java_type in ["double", "Double", "float", "Float"]:
            return {"type": "number", "format": "double"}
        elif java_type == "boolean" or java_type == "Boolean":
            return {"type": "boolean"}
        else:
            # For complex types, return object
            return {"type": "object"}
    
    def _process_controller_file(self, file_path: Path):
        """Process a single controller file"""
        content = self._read_java_file(file_path)
        
        # Extract class-level mapping
        base_path, tag = self._extract_class_level_mapping(content)
        
        # Find all public methods (including those with generics in return type)
        method_pattern = r'public\s+[\w<>,\s]+\s+\w+\s*\('
        method_matches = list(re.finditer(method_pattern, content))
        
        for match in method_matches:
            method_info = self._extract_method_info(content, match.start())
            if not method_info or not method_info.get("http_method"):
                continue
            
            # Build full path
            method_path = method_info["path"]
            if base_path:
                if method_path:
                    full_path = base_path.rstrip('/') + '/' + method_path.lstrip('/')
                else:
                    full_path = base_path
            else:
                full_path = method_path if method_path else "/"
            
            # Normalize path
            full_path = '/' + full_path.lstrip('/')
            if full_path == '/':
                full_path = base_path if base_path else "/"
            
            # Build endpoint info
            endpoint = {
                "path": full_path,
                "method": method_info["http_method"],
                "operation_id": method_info["method_name"],
                "summary": method_info["description"].split('.')[0] if method_info["description"] else method_info["method_name"],
                "description": method_info["description"],
                "parameters": [],
                "request_body": None,
                "tag": tag or "Default"
            }
            
            # Process parameters
            for param in method_info.get("parameters", []):
                if param["type"] == "header":
                    # Add header parameter
                    swagger_param = {
                        "name": param["name"],
                        "in": "header",
                        "description": f"Header parameter: {param['name']}",
                        "required": param.get("required", True),
                        "schema": self._java_type_to_swagger_type(param.get("java_type", "String"))
                    }
                    endpoint["parameters"].append(swagger_param)
                elif param["type"] == "path":
                    # Add path parameter
                    swagger_param = {
                        "name": param["name"],
                        "in": "path",
                        "required": True,
                        "description": f"Path parameter: {param['name']}",
                        "schema": self._java_type_to_swagger_type(param.get("java_type", "String"))
                    }
                    if "minimum" in param:
                        swagger_param["schema"]["minimum"] = param["minimum"]
                    endpoint["parameters"].append(swagger_param)
                elif param["type"] == "query":
                    # Add query parameter
                    swagger_param = {
                        "name": param["name"],
                        "in": "query",
                        "description": f"Query parameter: {param['name']}",
                        "required": param.get("required", False),
                        "schema": self._java_type_to_swagger_type(param.get("java_type", "String"))
                    }
                    endpoint["parameters"].append(swagger_param)
                elif param["type"] == "body":
                    # Add request body
                    java_type = param.get("java_type", "Object")
                    schema_name = java_type.replace('<', '').replace('>', '').replace(',', '')
                    endpoint["request_body"] = {
                        "required": True,
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": f"#/components/schemas/{schema_name}"
                                }
                            }
                        }
                    }
            
            self.endpoints.append(endpoint)
    
    def _generate_swagger_paths(self):
        """Generate Swagger paths from endpoints"""
        for endpoint in self.endpoints:
            path = endpoint["path"]
            method = endpoint["method"]
            
            if path not in self.swagger["paths"]:
                self.swagger["paths"][path] = {}
            
            operation = {
                "tags": [endpoint["tag"]],
                "summary": endpoint["summary"],
                "description": endpoint["description"],
                "operationId": endpoint["operation_id"]
            }
            
            if endpoint["parameters"]:
                operation["parameters"] = endpoint["parameters"]
            
            if endpoint["request_body"]:
                operation["requestBody"] = endpoint["request_body"]
            
            # Add default responses
            operation["responses"] = {
                "200": {
                    "description": "Success",
                    "content": {
                        "application/json": {
                            "schema": {
                                "$ref": "#/components/schemas/ApiResponseVoid"
                            }
                        }
                    }
                },
                "400": {
                    "$ref": "#/components/responses/BadRequest"
                },
                "500": {
                    "$ref": "#/components/responses/InternalServerError"
                }
            }
            
            self.swagger["paths"][path][method] = operation
    
    def generate(self) -> Dict[str, Any]:
        """Generate Swagger specification from controllers"""
        # Find all Java files in controller directory
        java_files = list(self.controller_dir.glob("**/*.java"))
        
        for java_file in java_files:
            if java_file.name.endswith("Controller.java"):
                print(f"Processing controller: {java_file}")
                self._process_controller_file(java_file)
        
        # Generate Swagger paths
        self._generate_swagger_paths()
        
        # Generate tags
        tags = set()
        for endpoint in self.endpoints:
            tags.add(endpoint["tag"])
        
        self.swagger["tags"] = [{"name": tag, "description": f"{tag} operations"} for tag in sorted(tags)]
        
        return self.swagger
    
    def save(self, output_path: str):
        """Save Swagger specification to file"""
        swagger = self.generate()
        
        output_file = Path(output_path)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            yaml.dump(swagger, f, default_flow_style=False, allow_unicode=True, sort_keys=False)
        
        print(f"Swagger specification saved to: {output_path}")
        print(f"Total endpoints processed: {len(self.endpoints)}")


def main():
    parser = argparse.ArgumentParser(
        description="Generate Swagger/OpenAPI specification from Spring Boot Controller classes",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    # Generate Swagger from controllers
    python3 scripts/generate_swagger_from_controller.py \\
        --controller_dir src/main/java/lab/zhang/rule/rule_engine/controller \\
        --output docs/api/swagger.yaml
    
    # With custom base package
    python3 scripts/generate_swagger_from_controller.py \\
        --controller_dir src/main/java/lab/zhang/rule/rule_engine/controller \\
        --output docs/api/swagger.yaml \\
        --base_package lab.zhang.rule.rule_engine
        """
    )
    
    parser.add_argument(
        "--controller_dir",
        type=str,
        required=True,
        help="Directory containing Controller Java files"
    )
    
    parser.add_argument(
        "--output",
        type=str,
        required=True,
        help="Output path for Swagger YAML file"
    )
    
    parser.add_argument(
        "--base_package",
        type=str,
        default="lab.zhang.rule.rule_engine",
        help="Base package name (default: lab.zhang.rule.rule_engine)"
    )
    
    args = parser.parse_args()
    
    generator = ControllerToSwaggerGenerator(
        controller_dir=args.controller_dir,
        base_package=args.base_package
    )
    
    generator.save(args.output)


if __name__ == "__main__":
    main()
