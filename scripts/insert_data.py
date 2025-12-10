#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Mock Data Generator and API Request Script
Generates mock data based on configuration file and sends requests to API endpoint

This script:
1. Reads configuration from --config file
2. Generates mock data based on request_schema
3. Sends HTTP requests to api.full_url with generated data
4. Optionally saves JSON data to file if --output is specified

The JSON data is used as request payload. It's only saved to file if --output is specified.

Usage:
    # Generate and send single request
    python3 scripts/mock_data.py --config scripts/mock/some.mock
    
    # Generate and send multiple requests
    python3 scripts/mock_data.py --config scripts/mock/some.mock --count 100
    
    # Save JSON data to file (still sends requests)
    python3 scripts/mock_data.py --config scripts/mock/some.mock --count 10 --output scripts/out/mock_data.json
    
    # Only generate JSON without sending requests (for testing)
    python3 scripts/mock_data.py --config scripts/mock/some.mock --count 10 --no-request --output scripts/out/mock_data.json
"""

import argparse
import json
import sys
import yaml
import requests
import time
from typing import Dict, Any, List, Optional, Tuple
from pathlib import Path

# Import shared data generator
sys.path.insert(0, str(Path(__file__).parent))
from mock_data_generator import MockDataGeneratorBase


class MockDataGenerator(MockDataGeneratorBase):
    """Generate mock data based on configuration"""
    
    def _load_config(self) -> Dict[str, Any]:
        """Load configuration from YAML file"""
        config_file = Path(self.config_path)
        if not config_file.exists():
            raise FileNotFoundError(f"Configuration file not found: {self.config_path}")
        
        with open(config_file, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)
    
    def generate_single(self, reset_counters: bool = False) -> Dict[str, Any]:
        """Generate a single mock data object"""
        if reset_counters:
            self.counters = {}
        
        return self._generate_from_schema()
    
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
    
    def send_request(self, data: Dict[str, Any]) -> Tuple[bool, Optional[Dict[str, Any]], Optional[str]]:
        """Send HTTP request to API endpoint with generated data"""
        api_info = self.get_api_info()
        # Use resolved URL with path variables replaced
        url = self.get_resolved_url()
        if not url:
            # Fallback to original full_url if path variable resolution fails
            url = api_info.get("full_url")
        method = api_info.get("method", "POST").upper()
        headers = api_info.get("headers", {})
        timeout = api_info.get("timeout", 30)
        
        if not url:
            raise ValueError("API full_url not found in configuration")
        
        try:
            if method == "POST":
                response = requests.post(url, json=data, headers=headers, timeout=timeout)
            elif method == "PUT":
                response = requests.put(url, json=data, headers=headers, timeout=timeout)
            elif method == "PATCH":
                response = requests.patch(url, json=data, headers=headers, timeout=timeout)
            elif method == "GET":
                response = requests.get(url, params=data, headers=headers, timeout=timeout)
            else:
                raise ValueError(f"Unsupported HTTP method: {method}")
            
            success = response.status_code in [200, 201, 202, 204]
            response_data = None
            error_msg = None
            
            if response.text:
                try:
                    response_data = response.json()
                except json.JSONDecodeError:
                    response_data = {"text": response.text}
            
            if not success:
                error_msg = f"HTTP {response.status_code}: {response.text[:200]}"
            
            return success, response_data, error_msg
            
        except requests.exceptions.Timeout:
            return False, None, "Request timeout"
        except requests.exceptions.ConnectionError as e:
            return False, None, f"Connection error: {e}"
        except requests.exceptions.RequestException as e:
            return False, None, f"Request error: {e}"
        except Exception as e:
            return False, None, f"Unexpected error: {e}"


def main():
    parser = argparse.ArgumentParser(
        description="Generate mock data and send requests to API endpoint",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
This script:
1. Reads configuration from --config file
2. Generates mock data based on request_schema
3. Sends HTTP requests to api.full_url with generated data
4. Optionally saves JSON data to file if --output is specified

The JSON data is used as request payload. It's only saved to file if --output is specified.
        """
    )
    parser.add_argument("--config", default="scripts/mock/load_test_config.mock",
                       help="Path to configuration file (default: scripts/mock/load_test_config.mock)")
    parser.add_argument("--count", type=int, default=1,
                       help="Number of requests to send (default: 1)")
    parser.add_argument("--output", type=str, default=None,
                       help="Output file path to save JSON data (optional, default: not saved)")
    parser.add_argument("--pretty", action="store_true",
                       help="Pretty print JSON output when saving to file")
    parser.add_argument("--no-request", action="store_true",
                       help="Only generate JSON data without sending requests (for testing)")
    
    args = parser.parse_args()
    
    try:
        generator = MockDataGenerator(args.config)
        api_info = generator.get_api_info()
        # Show resolved URL (with path variables replaced) if available
        url = generator.get_resolved_url() or api_info.get("full_url", "N/A")
        method = api_info.get("method", "POST")
        
        print(f"[INFO] Configuration: {args.config}")
        print(f"[INFO] API URL: {url}")
        print(f"[INFO] Method: {method}")
        print(f"[INFO] Count: {args.count}")
        print()
        
        success_count = 0
        failure_count = 0
        all_data = []
        
        for i in range(args.count):
            # Generate mock data
            data = generator.generate_single(reset_counters=(i == 0))
            all_data.append(data)
            
            # Send request unless --no-request is specified
            if not args.no_request:
                success, response_data, error_msg = generator.send_request(data)
                
                if success:
                    success_count += 1
                    print(f"[{i+1}/{args.count}] ✓ Request successful")
                    if response_data:
                        print(f"      Response: {json.dumps(response_data, ensure_ascii=False)[:100]}...")
                else:
                    failure_count += 1
                    print(f"[{i+1}/{args.count}] ✗ Request failed: {error_msg}")
            else:
                print(f"[{i+1}/{args.count}] Generated data (not sent)")
        
        # Save to file if --output is specified
        if args.output:
            if args.count == 1:
                output_data = all_data[0]
            else:
                output_data = all_data
            
            json_str = json.dumps(output_data, indent=2 if args.pretty else None, ensure_ascii=False)
            output_path = Path(args.output)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            with open(output_path, 'w', encoding='utf-8') as f:
                f.write(json_str)
            print(f"\n[INFO] JSON data saved to {args.output}")
        
        # Print summary
        if not args.no_request:
            print(f"\n[INFO] Summary:")
            print(f"      Total: {args.count}")
            print(f"      Success: {success_count}")
            print(f"      Failed: {failure_count}")
            print(f"      Success Rate: {(success_count/args.count*100):.2f}%")
    
    except Exception as e:
        print(f"[ERROR] Failed: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        exit(1)


if __name__ == "__main__":
    main()

