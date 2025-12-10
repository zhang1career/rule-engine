#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Mock Data Generator Script
Generates mock data based on load test configuration file

Usage:
    # Generate single mock data
    python3 scripts/mock_data.py
    
    # Generate multiple mock data
    python3 scripts/mock_data.py --count 100
    
    # Output to file
    python3 scripts/mock_data.py --count 100 --output mock_data.json
    
    # Use custom config file
    python3 scripts/mock_data.py --config scripts/mock/custom_config.mock
"""

import argparse
import json
import random
import yaml
from typing import Dict, Any, List, Optional
from pathlib import Path


class MockDataGenerator:
    """Generate mock data based on configuration"""
    
    def __init__(self, config_path: str = "scripts/mock/load_test_config.mock"):
        """Initialize generator with configuration file"""
        self.config_path = config_path
        self.config = self._load_config()
        self.counters = {}
    
    def _load_config(self) -> Dict[str, Any]:
        """Load configuration from YAML file"""
        config_file = Path(self.config_path)
        if not config_file.exists():
            raise FileNotFoundError(f"Configuration file not found: {self.config_path}")
        
        with open(config_file, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)
    
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
    
    def generate_single(self, reset_counters: bool = False) -> Dict[str, Any]:
        """Generate a single mock data object"""
        if reset_counters:
            self.counters = {}
        
        schema = self.config.get("request_schema", {})
        fields_config = schema.get("fields", {})
        
        result = {}
        context = {}
        
        # Generate root level fields first
        for field_name, field_config in fields_config.items():
            if field_name == "arguments":
                # Handle nested arguments field
                arguments_config = field_config.get("fields", {})
                result[field_name] = self._generate_nested_fields(arguments_config, context)
            else:
                # Generate regular field
                value = self._generate_field_value(field_config, context)
                result[field_name] = value
                # Add to context for template substitution
                context[field_name] = value
        
        return result
    
    def generate_batch(self, count: int, reset_counters: bool = True) -> List[Dict[str, Any]]:
        """Generate multiple mock data objects"""
        if reset_counters:
            self.counters = {}
        
        results = []
        for i in range(count):
            result = self.generate_single(reset_counters=(i == 0))
            results.append(result)
        
        return results
    
    def get_api_info(self) -> Dict[str, Any]:
        """Get API interface information"""
        return self.config.get("api", {})


def main():
    parser = argparse.ArgumentParser(description="Generate mock data for load testing")
    parser.add_argument("--config", default="scripts/mock/load_test_config.mock",
                       help="Path to configuration file (default: scripts/mock/load_test_config.mock)")
    parser.add_argument("--count", type=int, default=1,
                       help="Number of mock data to generate (default: 1)")
    parser.add_argument("--output", type=str, default=None,
                       help="Output file path (default: print to stdout)")
    parser.add_argument("--pretty", action="store_true",
                       help="Pretty print JSON output")
    
    args = parser.parse_args()
    
    try:
        generator = MockDataGenerator(args.config)
        
        if args.count == 1:
            data = generator.generate_single()
            output_data = data
        else:
            data = generator.generate_batch(args.count)
            output_data = data
        
        json_str = json.dumps(output_data, indent=2 if args.pretty else None, ensure_ascii=False)
        
        if args.output:
            output_path = Path(args.output)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            with open(output_path, 'w', encoding='utf-8') as f:
                f.write(json_str)
            print(f"[INFO] Generated {args.count} mock data and saved to {args.output}")
        else:
            print(json_str)
    
    except Exception as e:
        print(f"[ERROR] Failed to generate mock data: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        exit(1)


if __name__ == "__main__":
    main()

