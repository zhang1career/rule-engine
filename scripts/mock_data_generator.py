#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Mock Data Generator Base Module
Shared data generation logic for mock_data.py and load_test.py

This module provides the core functionality for generating mock data based on
configuration files, which is used by both mock_data.py and load_test.py.
"""

import random
import yaml
from typing import Dict, Any, List, Optional
from pathlib import Path


class MockDataGeneratorBase:
    """Base class for mock data generation based on configuration"""
    
    def __init__(self, config_path: str):
        """Initialize generator with configuration file"""
        self.config_path = config_path
        self.config = self._load_config()
        self.counters = {}
    
    def _load_config(self) -> Optional[Dict[str, Any]]:
        """Load configuration from YAML file
        
        Subclasses can override this to customize error handling.
        """
        config_file = Path(self.config_path)
        if not config_file.exists():
            return None
        
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f)
        except Exception as e:
            print(f"[WARN] Failed to load config file {self.config_path}: {e}")
            return None
    
    def _get_counter(self, field_name: str, start: int = 0, step: int = 1) -> int:
        """Get and increment counter for increment method"""
        if field_name not in self.counters:
            self.counters[field_name] = start
        else:
            self.counters[field_name] += step
        return self.counters[field_name]
    
    def _generate_field_value(self, field_config: Dict[str, Any], context: Dict[str, Any] = None) -> Any:
        """Generate value for a single field based on its configuration"""
        if context is None:
            context = {}
        
        gen_config = field_config.get("generation", {})
        method = gen_config.get("method", "fixed")
        
        if method == "fixed":
            return gen_config.get("value")
        
        elif method == "increment":
            start = gen_config.get("start", 0)
            step = gen_config.get("step", 1)
            field_name = field_config.get("name", "unknown")
            return self._get_counter(field_name, start, step)
        
        elif method == "random_int":
            min_val = gen_config.get("min", 0)
            max_val = gen_config.get("max", 100)
            return random.randint(min_val, max_val)
        
        elif method == "random_float":
            min_val = gen_config.get("min", 0.0)
            max_val = gen_config.get("max", 100.0)
            precision = gen_config.get("precision", 2)
            value = random.uniform(min_val, max_val)
            return round(value, precision)
        
        elif method == "random_choice":
            values = gen_config.get("values", [])
            if not values:
                return None
            return random.choice(values)
        
        elif method == "template":
            template = gen_config.get("template", "")
            # Replace variables in template with context values
            result = template
            for key, value in context.items():
                result = result.replace(f"{{{key}}}", str(value))
            return result
        
        elif method == "list":
            values = gen_config.get("values", [])
            if not values:
                return None
            field_name = field_config.get("name", "unknown")
            index = self.counters.get(field_name, 0) % len(values)
            self.counters[field_name] = index + 1
            return values[index]
        
        elif method == "random_text":
            length = gen_config.get("length", 10)
            charset = gen_config.get("charset", "alphanumeric")
            
            if charset == "alphanumeric":
                chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            elif charset == "alphabetic":
                chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            elif charset == "numeric":
                chars = "0123456789"
            elif charset == "ascii":
                chars = "".join(chr(i) for i in range(32, 127))
            elif isinstance(charset, str):
                chars = charset
            else:
                chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            
            return ''.join(random.choice(chars) for _ in range(length))
        
        elif method == "array":
            min_length = gen_config.get("min_length", 1)
            max_length = gen_config.get("max_length", 5)
            array_length = random.randint(min_length, max_length)
            
            # Get element configuration from field_config
            element_config = field_config.get("element", {})
            if not element_config:
                # Fallback: generate empty array or array with default values
                return []
            
            # Generate array elements
            result = []
            for _ in range(array_length):
                if element_config.get("type") == "OBJECT" and "fields" in element_config:
                    # Array of objects
                    element_value = self._generate_nested_fields(element_config.get("fields", {}), context)
                elif element_config.get("type") == "OBJECT" and "properties" in element_config:
                    # Array of objects with properties (from Swagger conversion)
                    nested_props = element_config.get("properties", {})
                    nested_required = element_config.get("required", [])
                    element_value = {}
                    for nested_name, nested_schema in nested_props.items():
                        element_value[nested_name] = self._generate_field_value(
                            self._convert_element_schema_to_field_config(nested_name, nested_schema, nested_required),
                            context
                        )
                else:
                    # Array of primitives
                    element_value = self._generate_field_value(element_config, context)
                result.append(element_value)
            
            return result
        
        else:
            raise ValueError(f"Unsupported generation method: {method}")
    
    def _generate_typed_value(self, field_config: Dict[str, Any], value: Any) -> Dict[str, Any]:
        """Generate TypedValue structure for arguments fields"""
        field_type = field_config.get("type", "STRING")
        
        # Map internal types to API types
        type_mapping = {
            "INTEGER": "INTEGER",
            "LONG": "LONG",
            "DECIMAL": "DECIMAL",
            "BOOLEAN": "BOOLEAN",
            "STRING": "STRING",
            "OBJECT": "OBJECT"
        }
        
        api_type = type_mapping.get(field_type, "STRING")
        
        return {
            "value": value,
            "type": api_type
        }
    
    def _convert_element_schema_to_field_config(self, prop_name: str, prop_schema: Dict[str, Any], required_fields: List[str]) -> Dict[str, Any]:
        """Convert element schema to field config (helper for array of objects)"""
        prop_type = prop_schema.get("type", "string")
        format_type = prop_schema.get("format")
        
        # Simple type mapping
        type_mapping = {
            "integer": "INTEGER" if format_type != "int64" else "LONG",
            "number": "DECIMAL",
            "string": "STRING",
            "boolean": "BOOLEAN",
            "object": "OBJECT"
        }
        config_type = type_mapping.get(prop_type, "STRING")
        
        field_config = {
            "type": config_type,
            "required": prop_name in required_fields
        }
        
        # Add default generation method
        if prop_type != "object":
            if config_type == "INTEGER" or config_type == "LONG":
                field_config["generation"] = {"method": "random_int", "min": 1, "max": 100}
            elif config_type == "DECIMAL":
                field_config["generation"] = {"method": "random_float", "min": 0.0, "max": 100.0, "precision": 2}
            elif config_type == "BOOLEAN":
                field_config["generation"] = {"method": "random_choice", "values": [True, False]}
            elif config_type == "STRING":
                field_config["generation"] = {"method": "random_text", "length": 10, "charset": "alphanumeric"}
        
        return field_config
    
    def _generate_nested_fields(self, fields_config: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        """Generate nested fields (like arguments)"""
        if context is None:
            context = {}
        
        result = {}
        for field_name, field_config in fields_config.items():
            value = self._generate_field_value(field_config, context)
            
            # Check if this field should be wrapped in TypedValue structure
            # (for arguments fields)
            if field_config.get("type") in ["INTEGER", "LONG", "DECIMAL", "BOOLEAN", "STRING", "OBJECT"]:
                result[field_name] = self._generate_typed_value(field_config, value)
            else:
                result[field_name] = value
        
        return result
    
    def _generate_from_schema(self, context: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Generate data from request_schema
        
        This is the core generation logic shared by both classes.
        Subclasses can call this method and add their own logic around it.
        
        Args:
            context: Optional context dictionary with pre-set field values
            
        Returns:
            Generated data dictionary
        """
        if self.config is None:
            return {}
        
        schema = self.config.get("request_schema", {})
        fields_config = schema.get("fields", {})
        
        if context is None:
            context = {}
        
        result = {}
        
        # Generate root level fields
        for field_name, field_config in fields_config.items():
            if field_name == "arguments":
                # Handle nested arguments field
                arguments_config = field_config.get("fields", {})
                result[field_name] = self._generate_nested_fields(arguments_config, context)
            elif field_config.get("type") == "ARRAY":
                # Handle array fields
                if field_name in context:
                    value = context[field_name]
                else:
                    value = self._generate_field_value(field_config, context)
                result[field_name] = value
                # Add to context for template substitution
                context[field_name] = value
            else:
                # Use provided value if available, otherwise generate
                if field_name in context:
                    value = context[field_name]
                else:
                    value = self._generate_field_value(field_config, context)
                result[field_name] = value
                # Add to context for template substitution
                context[field_name] = value
        
        return result

