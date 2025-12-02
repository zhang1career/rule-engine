#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Eval API Load Test Script
Used for load testing the /api/eval interface

Usage:
    python3 scripts/load_test_eval.py --url http://localhost:8080/api/eval --concurrent 10 --total 1000

Parameters:
    --url: API address (default: http://localhost:8080/api/eval)
    --concurrent: Concurrency (default: 10)
    --total: Total requests (default: 1000)
    --duration: Duration in seconds (default: 0, means use total parameter)
    --event-id: Event ID (default: 1001)
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
from typing import Dict, List, Tuple
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


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
    
    def get_statistics(self) -> Dict:
        if not self.response_times:
            return {
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
            }
        
        sorted_times = sorted(self.response_times)
        return {
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
        }
    
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


def generate_test_data(user_id: int, event_id: int, trace_id: int) -> Dict:
    """Generate test data"""
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


def send_request(session: requests.Session, url: str, data: Dict) -> Tuple[bool, float, str]:
    """Send a single request"""
    start_time = time.time()
    try:
        response = session.post(
            url,
            json=data,
            headers={"Content-Type": "application/json"},
            timeout=30
        )
        response_time = (time.time() - start_time) * 1000  # Convert to milliseconds
        
        if response.status_code == 200:
            result = response.json()
            if result.get("success", False):
                return True, response_time, None
            else:
                return False, response_time, result.get("errmsg", "Unknown error")
        else:
            return False, response_time, f"HTTP {response.status_code}: {response.text}"
    
    except requests.exceptions.Timeout:
        response_time = (time.time() - start_time) * 1000
        return False, response_time, "Request timeout"
    except Exception as e:
        response_time = (time.time() - start_time) * 1000
        return False, response_time, str(e)


def run_load_test(
    url: str,
    concurrent: int,
    total: int,
    duration: int,
    event_id: int,
    user_id_start: int,
    interval_ms: int
):
    """Execute load test"""
    print(f"\n{'='*60}")
    print(f"Eval API Load Test")
    print(f"{'='*60}")
    print(f"URL: {url}")
    print(f"Concurrent: {concurrent}")
    print(f"Total Requests: {total if total > 0 else 'Unlimited'}")
    print(f"Duration: {duration}s" if duration > 0 else "")
    print(f"Event ID: {event_id}")
    print(f"User ID Start: {user_id_start}")
    print(f"Interval: {interval_ms}ms")
    print(f"{'='*60}\n")
    
    result = LoadTestResult()
    result.start_time = datetime.now()
    
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
    
    def worker():
        """Worker thread function"""
        nonlocal user_id_counter, trace_id_counter
        local_user_id = user_id_counter
        user_id_counter += 1
        local_trace_id = trace_id_counter
        trace_id_counter += 1
        
        data = generate_test_data(local_user_id, event_id, local_trace_id)
        success, response_time, error = send_request(session, url, data)
        result.add_result(success, response_time, error)
        
        if interval_ms > 0:
            time.sleep(interval_ms / 1000.0)
    
    # Execute load test
    if duration > 0:
        # Duration-based load test
        end_time = time.time() + duration
        with ThreadPoolExecutor(max_workers=concurrent) as executor:
            futures = []
            while time.time() < end_time:
                # Maintain concurrency
                while len(futures) < concurrent and time.time() < end_time:
                    future = executor.submit(worker)
                    futures.append(future)
                
                # Clean up completed tasks
                completed = [f for f in futures if f.done()]
                for f in completed:
                    futures.remove(f)
                    try:
                        f.result()
                    except Exception as e:
                        result.add_result(False, 0, str(e))
                
                time.sleep(0.01)  # Avoid high CPU usage
            
            # Wait for all tasks to complete
            for future in as_completed(futures):
                try:
                    future.result()
                except Exception as e:
                    result.add_result(False, 0, str(e))
    else:
        # Total request count-based load test
        with ThreadPoolExecutor(max_workers=concurrent) as executor:
            futures = []
            for i in range(total):
                future = executor.submit(worker)
                futures.append(future)
            
            # Wait for all tasks to complete and show progress
            completed = 0
            for future in as_completed(futures):
                completed += 1
                try:
                    future.result()
                except Exception as e:
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
        print(f"Error Summary")
        print(f"{'='*60}")
        error_counts = {}
        for error in result.error_messages:
            error_counts[error] = error_counts.get(error, 0) + 1
        
        for error, count in sorted(error_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
            print(f"{error}: {count}")
    
    print(f"\n{'='*60}\n")


def main():
    parser = argparse.ArgumentParser(description="Eval API Load Test")
    parser.add_argument("--url", default="http://localhost:8080/api/eval",
                       help="API URL (default: http://localhost:8080/api/eval)")
    parser.add_argument("--concurrent", type=int, default=10,
                       help="Concurrent requests (default: 10)")
    parser.add_argument("--total", type=int, default=1000,
                       help="Total requests (default: 1000, 0 means unlimited)")
    parser.add_argument("--duration", type=int, default=0,
                       help="Duration in seconds (default: 0, use total if set)")
    parser.add_argument("--event-id", type=int, default=1001,
                       help="Event ID (default: 1001)")
    parser.add_argument("--user-id-start", type=int, default=1000000,
                       help="Start user ID (default: 1000000)")
    parser.add_argument("--interval", type=int, default=0,
                       help="Request interval in milliseconds (default: 0)")
    
    args = parser.parse_args()
    
    result = run_load_test(
        url=args.url,
        concurrent=args.concurrent,
        total=args.total,
        duration=args.duration,
        event_id=args.event_id,
        user_id_start=args.user_id_start,
        interval_ms=args.interval
    )
    
    print_report(result)


if __name__ == "__main__":
    main()
