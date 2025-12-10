#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Eval API Load Test Script
Used for load testing specified interface

The script reads API interface definition and data generation rules from configuration file.
Configuration file is REQUIRED and must be specified via --config parameter.

Usage:
    # Basic usage with configuration file
    python3 scripts/load_test.py --config path/to/custom_config.yaml --concurrent 10 --total 1000

    # Override specific parameters
    python3 scripts/load_test.py --config path/to/custom_config.yaml --url http://localhost:8080/api/some/other --concurrent 20 --total 5000

Parameters:
    --config: Path to configuration file (REQUIRED)
    --url: API address (default: from config file)
    --concurrent: Concurrency (default: 10)
    --total: Total requests (default: 1000, 0 means unlimited)
    --duration: Duration in seconds (default: 0, means use total parameter)
    --event-id: Event ID (default: from config file or 1001)
    --user-id-start: Starting user ID (default: 1000000)
    --interval: Request interval in milliseconds (default: 0)
"""

import argparse
import json
import random
import time
import statistics
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from typing import Dict, List, Tuple, Optional, Any
from pathlib import Path
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import yaml

# Import shared data generator
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from mock_data_generator import MockDataGeneratorBase


class LoadTestResult:
    """Load test result statistics"""
    
    def __init__(self):
        self.total_requests = 0
        self.success_count = 0
        self.failure_count = 0
        self.response_times: List[float] = []
        self.error_messages: List[str] = []
        self.start_time = None
        self.end_time = None
    
    def add_result(self, success: bool, response_time: float, error_msg: str = None):
        self.total_requests += 1
        if success:
            self.success_count += 1
            self.response_times.append(response_time)
        else:
            self.failure_count += 1
            if error_msg:
                self.error_messages.append(error_msg)

        # Periodic summary logging
        if self.total_requests % 100 == 0:
            unknown_count = sum(1 for error in self.error_messages if error and error.startswith("Unknown error"))
            null_result_count = sum(1 for error in self.error_messages if error and "Null evaluation result" in error)
            print(f"[INFO] Progress: {self.total_requests} requests, Success: {self.success_count}, Failures: {self.failure_count}, Unknown errors: {unknown_count}, Null results: {null_result_count}")
    
    def get_statistics(self) -> Dict:
        # Count different types of errors
        unknown_error_count = sum(1 for error in self.error_messages if error and error.startswith("Unknown error"))
        null_result_count = sum(1 for error in self.error_messages if error and "Null evaluation result" in error)
        invalid_format_count = sum(1 for error in self.error_messages if error and "Invalid response format" in error)

        if not self.response_times:
            stats = {
                "total": self.total_requests,
                "success": self.success_count,
                "failure": self.failure_count,
                "success_rate": 0.0,
                "avg_response_time": 0.0,
                "min_response_time": 0.0,
                "max_response_time": 0.0,
                "p50": 0.0,
                "p90": 0.0,
                "p95": 0.0,
                "p99": 0.0,
                "unknown_errors": unknown_error_count,
                "null_results": null_result_count,
                "invalid_formats": invalid_format_count,
            }
            print(f"[DEBUG] No response times available. Stats: {stats}")
            return stats

        sorted_times = sorted(self.response_times)
        stats = {
            "total": self.total_requests,
            "success": self.success_count,
            "failure": self.failure_count,
            "success_rate": (self.success_count / self.total_requests) * 100,
            "avg_response_time": statistics.mean(self.response_times),
            "min_response_time": min(self.response_times),
            "max_response_time": max(self.response_times),
            "p50": self._percentile(sorted_times, 50),
            "p90": self._percentile(sorted_times, 90),
            "p95": self._percentile(sorted_times, 95),
            "p99": self._percentile(sorted_times, 99),
            "unknown_errors": unknown_error_count,
            "null_results": null_result_count,
            "invalid_formats": invalid_format_count,
        }
        print(f"[DEBUG] Generated statistics: success={stats['success']}, failure={stats['failure']}, unknown_errors={unknown_error_count}, null_results={null_result_count}")
        return stats
    
    @staticmethod
    def _percentile(sorted_list: List[float], percentile: int) -> float:
        """Calculate percentile"""
        if not sorted_list:
            return 0.0
        index = int(len(sorted_list) * percentile / 100)
        return sorted_list[min(index, len(sorted_list) - 1)]
    
    def get_duration(self) -> float:
        """Get total duration in seconds"""
        if self.start_time and self.end_time:
            return (self.end_time - self.start_time).total_seconds()
        return 0.0
    
    def get_qps(self) -> float:
        """Calculate QPS (requests per second)"""
        duration = self.get_duration()
        if duration > 0:
            return self.total_requests / duration
        return 0.0


class TestDataGenerator(MockDataGeneratorBase):
    """Generate test data based on configuration file"""
    
    def generate(self, user_id: Optional[int] = None, event_id: Optional[int] = None, trace_id: Optional[int] = None) -> Dict:
        """Generate test data, optionally override specific fields"""
        if self.config is None:
            # Fallback to default generation if config not available
            return self._generate_default(user_id or 1000000, event_id or 1001, trace_id or 1000000)
        
        # Build context with provided values
        context = {}
        if user_id is not None:
            context["userId"] = user_id
        if event_id is not None:
            context["eventId"] = event_id
        if trace_id is not None:
            context["traceId"] = trace_id
        
        # Use shared generation logic
        return self._generate_from_schema(context)
    
    def _generate_default(self, user_id: int, event_id: int, trace_id: int) -> Dict:
        """Default generation method (fallback)"""
        return {
            "userId": user_id,
            "eventId": event_id,
            "traceId": trace_id,
            "arguments": {
                "amount": {
                    "value": round(random.uniform(100.0, 10000.0), 2),
                    "type": "DECIMAL"
                },
                "age": {
                    "value": random.randint(18, 80),
                    "type": "INTEGER"
                },
                "isVip": {
                    "value": random.choice([True, False]),
                    "type": "BOOLEAN"
                },
                "name": {
                    "value": f"User_{user_id}",
                    "type": "STRING"
                }
            }
        }


# Global generator instance
_data_generator: Optional[TestDataGenerator] = None


def get_data_generator(config_path: str) -> TestDataGenerator:
    """Get or create data generator instance"""
    global _data_generator
    if _data_generator is None or _data_generator.config_path != config_path:
        _data_generator = TestDataGenerator(config_path)
    return _data_generator


def generate_test_data(user_id: int, event_id: int, trace_id: int, config_path: str) -> Dict:
    """Generate test data using configuration file"""
    generator = get_data_generator(config_path)
    return generator.generate(user_id, event_id, trace_id)


def send_request(session: requests.Session, url: str, data: Dict) -> Tuple[bool, float, str]:
    """Send a single request"""
    user_id = data.get("userId", "unknown")
    event_id = data.get("eventId", "unknown")
    trace_id = data.get("traceId", "unknown")

    start_time = time.time()
    try:
        print(f"[DEBUG] Sending request: userId={user_id}, eventId={event_id}, traceId={trace_id}")

        response = session.post(
            url,
            json=data,
            headers={"Content-Type": "application/json"},
            timeout=30
        )
        response_time = (time.time() - start_time) * 1000  # Convert to milliseconds

        print(f"[DEBUG] Response received: status={response.status_code}, time={response_time:.2f}ms, userId={user_id}")

        if response.status_code == 200:
            try:
                result = response.json()
                print(f"[DEBUG] Response JSON: {result}")

                # Check for null result response: {'result': {'value': None, 'type': 'OBJECT'}, 'briefSteps': {}}
                if 'result' in result and isinstance(result['result'], dict):
                    result_value = result['result'].get('value')
                    result_type = result['result'].get('type')

                    if result_value is None and result_type == 'OBJECT':
                        print(f"[WARN] Null result received for userId={user_id}")
                        print(f"[WARN] This indicates the evaluation returned no definitive result (neither true nor false)")
                        print(f"[WARN] Full response: {result}")
                        return False, response_time, f"Null evaluation result (value=None, type={result_type})"

                # Check for standard success/error response
                if "success" in result:
                    if result.get("success", False):
                        print(f"[DEBUG] Request successful for userId={user_id}")
                        return True, response_time, None
                    else:
                        # Detailed debugging for business logic failures
                        success_value = result.get("success")
                        errmsg_value = result.get("errmsg")
                        print(f"[WARN] Business logic failure for userId={user_id}")
                        print(f"[WARN] success field value: {success_value} (type: {type(success_value)})")
                        print(f"[WARN] errmsg field value: {errmsg_value} (type: {type(errmsg_value)})")
                        print(f"[WARN] Full response: {result}")

                        if errmsg_value is None:
                            print(f"[WARN] errmsg field is missing or None, will use 'Unknown error'")
                            error_msg = f"Unknown error (success={success_value})"
                        else:
                            error_msg = str(errmsg_value)

                        print(f"[WARN] Final error message: '{error_msg}', userId={user_id}")
                        return False, response_time, error_msg
                else:
                    # Response doesn't have success field, check if it's a null result
                    if 'result' in result:
                        print(f"[WARN] Response has 'result' field but no 'success' field for userId={user_id}")
                        print(f"[WARN] This might be an unexpected response format: {result}")
                        return False, response_time, f"Unexpected response format (has result but no success field)"
                    else:
                        print(f"[WARN] Response has neither 'success' nor 'result' field for userId={user_id}")
                        print(f"[WARN] Full response: {result}")
                        return False, response_time, f"Invalid response format (missing success/result fields)"

            except json.JSONDecodeError as e:
                print(f"[ERROR] Failed to parse JSON response: {e}, response text: {response.text[:200]}..., userId={user_id}")
                return False, response_time, f"JSON decode error: {e}"
        else:
            print(f"[DEBUG] HTTP error: {response.status_code}, userId={user_id}, response: {response.text[:200]}...")
            return False, response_time, f"HTTP {response.status_code}: {response.text}"

    except requests.exceptions.Timeout:
        response_time = (time.time() - start_time) * 1000
        print(f"[ERROR] Request timeout for userId={user_id}, time={response_time:.2f}ms")
        return False, response_time, "Request timeout"
    except requests.exceptions.ConnectionError as e:
        response_time = (time.time() - start_time) * 1000
        print(f"[ERROR] Connection error for userId={user_id}: {e}")
        return False, response_time, f"Connection error: {e}"
    except requests.exceptions.RequestException as e:
        response_time = (time.time() - start_time) * 1000
        print(f"[ERROR] Request exception for userId={user_id}: {e}")
        return False, response_time, f"Request error: {e}"
    except Exception as e:
        response_time = (time.time() - start_time) * 1000
        print(f"[ERROR] Unexpected error for userId={user_id}: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        return False, response_time, f"Unexpected error: {e}"


def load_api_config(config_path: str) -> Dict[str, Any]:
    """Load API configuration from config file"""
    config_file = Path(config_path)
    
    if not config_file.exists():
        return {}
    
    try:
        with open(config_file, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
            return config.get("api", {})
    except Exception as e:
        print(f"[WARN] Failed to load API config from {config_path}: {e}")
        return {}


def run_load_test(
    url: str,
    concurrent: int,
    total: int,
    duration: int,
    event_id: int,
    user_id_start: int,
    interval_ms: int,
    config_path: str
):
    """Execute load test"""
    # Load API config to get default URL if not provided
    api_config = load_api_config(config_path)
    if not url and api_config.get("full_url"):
        url = api_config["full_url"]
    
    # Try to get resolved URL (with path variables) for display
    generator = get_data_generator(config_path)
    display_url = generator.get_resolved_url() or url
    
    print(f"\n{'='*60}")
    print(f"Eval API Load Test")
    print(f"{'='*60}")
    print(f"URL: {display_url}")
    print(f"Concurrent: {concurrent}")
    print(f"Total Requests: {total if total > 0 else 'Unlimited'}")
    print(f"Duration: {duration}s" if duration > 0 else "")
    print(f"Event ID: {event_id}")
    print(f"User ID Start: {user_id_start}")
    print(f"Interval: {interval_ms}ms")
    print(f"{'='*60}\n")

    print(f"[INFO] Initializing load test...")
    result = LoadTestResult()
    result.start_time = datetime.now()
    print(f"[INFO] Load test started at {result.start_time}")
    
    # Create session with retry mechanism
    session = requests.Session()
    retry_strategy = Retry(
        total=3,
        backoff_factor=0.1,
        status_forcelist=[429, 500, 502, 503, 504],
    )
    adapter = HTTPAdapter(max_retries=retry_strategy)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    
    user_id_counter = user_id_start
    trace_id_counter = 1000000
    
    # Get generator instance for path variable resolution
    generator = get_data_generator(config_path)
    
    def worker():
        """Worker thread function"""
        try:
            nonlocal user_id_counter, trace_id_counter
            local_user_id = user_id_counter
            user_id_counter += 1
            local_trace_id = trace_id_counter
            trace_id_counter += 1

            print(f"[DEBUG] Worker started for userId={local_user_id}")

            data = generate_test_data(local_user_id, event_id, local_trace_id, config_path)
            print(f"[DEBUG] Generated test data for userId={local_user_id}")

            # Resolve URL with path variables replaced
            # Path variables are already generated in data, so we can use them for context
            resolved_url = generator.get_resolved_url(context=data) or url

            success, response_time, error = send_request(session, resolved_url, data)
            result.add_result(success, response_time, error)

            print(f"[DEBUG] Worker completed for userId={local_user_id}, success={success}")

            if interval_ms > 0:
                print(f"[DEBUG] Sleeping for {interval_ms}ms")
                time.sleep(interval_ms / 1000.0)
        except Exception as e:
            print(f"[ERROR] Worker exception for userId={local_user_id}: {type(e).__name__}: {e}")
            import traceback
            traceback.print_exc()
            result.add_result(False, 0, f"Worker error: {e}")
    
    # Execute load test
    if duration > 0:
        print(f"[INFO] Starting duration-based load test for {duration} seconds")
        # Duration-based load test
        end_time = time.time() + duration
        with ThreadPoolExecutor(max_workers=concurrent) as executor:
            futures = []
            while time.time() < end_time:
                # Maintain concurrency
                while len(futures) < concurrent and time.time() < end_time:
                    future = executor.submit(worker)
                    futures.append(future)
                    print(f"[DEBUG] Submitted task, active futures: {len(futures)}")

                # Clean up completed tasks
                completed = [f for f in futures if f.done()]
                for f in completed:
                    futures.remove(f)
                    try:
                        f.result()
                        print(f"[DEBUG] Task completed successfully, remaining futures: {len(futures)}")
                    except Exception as e:
                        print(f"[ERROR] Task failed: {e}")
                        result.add_result(False, 0, str(e))

                time.sleep(0.01)  # Avoid high CPU usage

            print(f"[INFO] Duration reached, waiting for remaining {len(futures)} tasks to complete")
            # Wait for all tasks to complete
            for future in as_completed(futures):
                try:
                    future.result()
                    print(f"[DEBUG] Final task completed")
                except Exception as e:
                    print(f"[ERROR] Final task failed: {e}")
                    result.add_result(False, 0, str(e))
    else:
        print(f"[INFO] Starting total request count-based load test for {total} requests")
        # Total request count-based load test
        with ThreadPoolExecutor(max_workers=concurrent) as executor:
            futures = []
            for i in range(total):
                future = executor.submit(worker)
                futures.append(future)
                if (i + 1) % 100 == 0:
                    print(f"[DEBUG] Submitted {i + 1}/{total} tasks")

            print(f"[INFO] All {total} tasks submitted, waiting for completion")

            # Wait for all tasks to complete and show progress
            completed = 0
            for future in as_completed(futures):
                completed += 1
                try:
                    future.result()
                    if completed % 100 == 0:
                        print(f"[DEBUG] Completed {completed}/{total} tasks")
                except Exception as e:
                    print(f"[ERROR] Task {completed} failed: {e}")
                    result.add_result(False, 0, str(e))

                if completed % 100 == 0:
                    print(f"Progress: {completed}/{total} requests completed", end='\r')
    
    result.end_time = datetime.now()
    session.close()
    
    return result


def print_report(result: LoadTestResult):
    """Print load test report"""
    stats = result.get_statistics()
    duration = result.get_duration()
    qps = result.get_qps()
    
    print(f"\n{'='*60}")
    print(f"Load Test Report")
    print(f"{'='*60}")
    print(f"Start Time: {result.start_time.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"End Time: {result.end_time.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Duration: {duration:.2f}s")
    print(f"\n{'='*60}")
    print(f"Request Statistics")
    print(f"{'='*60}")
    print(f"Total Requests: {stats['total']}")
    print(f"Success: {stats['success']}")
    print(f"Failure: {stats['failure']}")
    print(f"Success Rate: {stats['success_rate']:.2f}%")
    print(f"Unknown Errors: {stats.get('unknown_errors', 0)}")
    print(f"Null Results: {stats.get('null_results', 0)}")
    print(f"Invalid Formats: {stats.get('invalid_formats', 0)}")
    print(f"QPS: {qps:.2f} requests/second")
    print(f"\n{'='*60}")
    print(f"Response Time Statistics (ms)")
    print(f"{'='*60}")
    print(f"Average: {stats['avg_response_time']:.2f}ms")
    print(f"Min: {stats['min_response_time']:.2f}ms")
    print(f"Max: {stats['max_response_time']:.2f}ms")
    print(f"P50: {stats['p50']:.2f}ms")
    print(f"P90: {stats['p90']:.2f}ms")
    print(f"P95: {stats['p95']:.2f}ms")
    print(f"P99: {stats['p99']:.2f}ms")
    
    if result.error_messages:
        print(f"\n{'='*60}")
        print(f"Error Summary (Total errors: {len(result.error_messages)})")
        print(f"{'='*60}")
        error_counts = {}
        for error in result.error_messages:
            error_counts[error] = error_counts.get(error, 0) + 1

        print(f"[INFO] Found {len(error_counts)} unique error types")
        for error, count in sorted(error_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
            print(f"{error}: {count}")
            # Show additional details for special error types
            if error.startswith("Unknown error") and count > 0:
                print(f"  -> API returned success=false but no error message (appears {count} times)")
            elif "Null evaluation result" in error and count > 0:
                print(f"  -> Rule evaluation returned null (neither true nor false) (appears {count} times)")
            elif "Invalid response format" in error and count > 0:
                print(f"  -> API response missing expected fields (appears {count} times)")
    
    print(f"\n{'='*60}\n")


def main():
    print("[INFO] Starting Eval API Load Test Script")

    parser = argparse.ArgumentParser(description="Eval API Load Test")
    parser.add_argument("--url", default=None,
                       help="API URL (default: from config file or http://localhost:8080/api/eval)")
    parser.add_argument("--concurrent", type=int, default=10,
                       help="Concurrent requests (default: 10)")
    parser.add_argument("--total", type=int, default=1000,
                       help="Total requests (default: 1000, 0 means unlimited)")
    parser.add_argument("--duration", type=int, default=0,
                       help="Duration in seconds (default: 0, use total if set)")
    parser.add_argument("--event-id", type=int, default=None,
                       help="Event ID (default: from config file or 1001)")
    parser.add_argument("--user-id-start", type=int, default=1000000,
                       help="Start user ID (default: 1000000)")
    parser.add_argument("--interval", type=int, default=0,
                       help="Request interval in milliseconds (default: 0)")
    parser.add_argument("--config", type=str, required=True,
                       help="Path to configuration file (required)")

    try:
        args = parser.parse_args()
        print(f"[INFO] Parsed arguments: {args}")
        
        # Load config to get default values
        api_config = load_api_config(args.config)
        if not args.url:
            args.url = api_config.get("full_url", "http://localhost:8080/api/eval")
        if args.event_id is None:
            # Try to get from config, otherwise use default
            schema = None
            try:
                config_file = Path(args.config)
                if config_file.exists():
                    with open(config_file, 'r', encoding='utf-8') as f:
                        full_config = yaml.safe_load(f)
                        schema = full_config.get("request_schema", {})
                        event_id_config = schema.get("fields", {}).get("eventId", {})
                        gen_config = event_id_config.get("generation", {})
                        if gen_config.get("method") == "fixed":
                            args.event_id = gen_config.get("value", 1001)
                        else:
                            args.event_id = 1001
                    if args.event_id is None:
                        args.event_id = 1001
                else:
                    args.event_id = 1001
            except Exception:
                args.event_id = 1001

        result = run_load_test(
            url=args.url,
            concurrent=args.concurrent,
            total=args.total,
            duration=args.duration,
            event_id=args.event_id,
            user_id_start=args.user_id_start,
            interval_ms=args.interval,
            config_path=args.config
        )

        print("[INFO] Load test completed, generating report")
        print_report(result)
        print("[INFO] Script execution completed successfully")

    except Exception as e:
        print(f"[ERROR] Script execution failed: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[INFO] Script interrupted by user")
    except Exception as e:
        print(f"[FATAL] Unexpected error in main execution: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
