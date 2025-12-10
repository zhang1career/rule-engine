# Eval API Load Test Script Usage Guide

## Requirements

- Python 3.6+
- requests library

## Install Dependencies

```bash
pip install requests
```

Or use requirements.txt:

```bash
pip install -r scripts/requirements.txt
```

## Usage

### Basic Usage

```bash
# Default parameters: 10 concurrent, 1000 requests
python3 scripts/load_test.py

# Specify URL and concurrency
python3 scripts/load_test.py --url http://localhost:8080/api/eval --concurrent 20 --total 5000

# Load test based on duration (run for 60 seconds)
python3 scripts/load_test.py --concurrent 50 --duration 60 --total 0
```

### Parameter Description

- `--url`: API address (default: http://localhost:8080/api/eval)
- `--concurrent`: Concurrency (default: 10)
- `--total`: Total requests (default: 1000, set to 0 for unlimited, use with duration)
- `--duration`: Duration in seconds (default: 0, means use total parameter)
- `--event-id`: Event ID (default: 1001)
- `--user-id-start`: Starting user ID (default: 1000000)
- `--interval`: Request interval in milliseconds (default: 0)

### Usage Examples

#### 1. Quick Load Test (100 concurrent, 10000 requests)

```bash
python3 scripts/load_test.py --concurrent 100 --total 10000
```

#### 2. Continuous Load Test (50 concurrent, run for 5 minutes)

```bash
python3 scripts/load_test.py --concurrent 50 --duration 300 --total 0
```

#### 3. Low Concurrency Load Test (test stability)

```bash
python3 scripts/load_test.py --concurrent 5 --total 1000 --interval 100
```

#### 4. Specify Event ID and User ID Range

```bash
python3 scripts/load_test.py --event-id 1002 --user-id-start 2000000 --concurrent 20 --total 2000
```

## Load Test Report Description

The script outputs a detailed load test report, including:

1. **Request Statistics**
   - Total requests
   - Success/failure count
   - Success rate
   - QPS (requests per second)

2. **Response Time Statistics**
   - Average response time
   - Min/max response time
   - P50/P90/P95/P99 percentiles

3. **Error Statistics**
   - Error types and counts

## Notes

1. Ensure target server is started and accessible
2. Adjust concurrency based on server performance to avoid overwhelming the server
3. Recommend starting with low concurrency and gradually increasing
4. Before load testing, ensure corresponding eventId data exists in the database
5. For long-duration load tests, recommend monitoring server resource usage

## Performance Recommendations

- **Development Environment**: Concurrency 10-20, total requests 1000-5000
- **Test Environment**: Concurrency 50-100, total requests 10000-50000
- **Production Environment Load Test**: Requires caution, recommend during off-peak hours, concurrency not exceeding 50
