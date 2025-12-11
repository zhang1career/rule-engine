# Eval Interface Performance Optimization Guide

## Current Performance Bottlenecks

### 1. Synchronous Database Operations
- **Issue**: `saveLog()` performs synchronous database insert for every eval request
- **Impact**: High latency, blocking main execution flow
- **Location**: `EvalServiceImpl.saveLog()`

### 2. Excessive Logging
- **Issue**: Multiple `log.info()` calls in hot path
- **Impact**: I/O overhead, especially when log level is INFO
- **Locations**: 
  - `EvalController.eval()` - line 43
  - `EvalServiceImpl.eval()` - lines 46, 54
  - `RuleExecutionEngine.execute()` - lines 74, 132

### 3. JSON Serialization Overhead
- **Issue**: `ObjectMapper.writeValueAsString()` called synchronously for logging
- **Impact**: CPU and memory overhead
- **Location**: `EvalServiceImpl.saveLog()` - lines 97, 103

### 4. Database Query Optimization
- **Issue**: Multiple sequential database queries
  - `eventService.getExecutionItems()` - JOIN query
  - `ruleMapper.selectBatchIds()` - batch query
  - `ruleContentMapper.selectBatchIds()` - batch query
- **Impact**: Network round-trips, query execution time

### 5. Cache Key Generation
- **Issue**: Hash calculation for every cache lookup/put
- **Impact**: CPU overhead (though minimal)
- **Location**: `EvalCacheServiceLocalImpl.buildCacheKey()`

## Optimization Recommendations

### Priority 1: Critical Optimizations

#### 1.1 Asynchronous Log Writing
**Action**: Move log writing to async thread pool

**Benefits**:
- Reduce response time by 10-50ms per request
- Improve QPS by 20-40%

**Implementation**:
```java
@Async("logAsyncExecutor")
public void saveLogAsync(EvalRequest request, ExecutionTrace trace) {
    // Existing saveLog logic
}
```

**Configuration**:
- Create dedicated thread pool for async operations
- Use bounded queue to prevent memory issues
- Configure rejection policy

#### 1.2 Reduce Log Level in Hot Path
**Action**: Change `log.info()` to `log.debug()` or use conditional logging

**Benefits**:
- Reduce I/O overhead by 30-50%
- Improve CPU efficiency

**Locations to change**:
- `EvalController.eval()` - line 43: use `log.debug()`
- `EvalServiceImpl.eval()` - line 46, 54: use `log.debug()` or conditional
- `RuleExecutionEngine.execute()` - line 74: use `log.debug()`

**Implementation**:
```java
if (log.isDebugEnabled()) {
    log.debug("Eval request received: userId={}, eventId={}", ...);
}
```

#### 1.3 Batch Log Insertion
**Action**: Implement batch insert for eval logs

**Benefits**:
- Reduce database round-trips
- Improve write throughput by 5-10x

**Implementation**:
- Use `CompletableFuture` to batch log writes
- Flush batch every N requests or time interval
- Use `MyBatis Plus` batch insert API

### Priority 2: Important Optimizations

#### 2.1 Optimize Database Queries
**Action**: Add caching for execution arrangements

**Benefits**:
- Reduce database load
- Improve response time by 5-15ms

**Implementation**:
- Cache `eventService.getExecutionItems()` results
- Cache key: `eventId`
- TTL: 5-30 minutes (configurable)
- Invalidate on event/rule updates

#### 2.2 Connection Pool Optimization
**Action**: Tune database connection pool settings

**Current**: Default HikariCP settings (likely)

**Recommended**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

**Benefits**:
- Better connection reuse
- Reduced connection establishment overhead

#### 2.3 JSON Serialization Optimization
**Action**: Use pre-configured ObjectMapper with optimizations

**Benefits**:
- 10-20% faster serialization
- Lower memory allocation

**Implementation**:
```java
@Bean
public ObjectMapper optimizedObjectMapper() {
    return JsonMapper.builder()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
}
```

### Priority 3: Advanced Optimizations

#### 3.1 Cache Warm-up
**Action**: Pre-load frequently accessed rules and execution arrangements

**Benefits**:
- Reduce cold start latency
- Improve cache hit rate

**Implementation**:
- On application startup, load top N events' execution arrangements
- Pre-load frequently used rules

#### 3.2 Parallel Rule Execution (if applicable)
**Action**: Execute independent rules in parallel

**Benefits**:
- Reduce total execution time for multi-rule scenarios
- Improve throughput

**Note**: Only if rules are truly independent (no dependencies)

**Implementation**:
```java
List<CompletableFuture<TypedValue>> futures = executionItemList.stream()
    .map(item -> CompletableFuture.supplyAsync(() -> executeRule(...), executor))
    .collect(Collectors.toList());
```

#### 3.3 Response Compression
**Action**: Enable HTTP response compression

**Benefits**:
- Reduce network bandwidth
- Faster response transmission for large results

**Configuration**:
```yaml
server:
  compression:
    enabled: true
    mime-types: application/json
    min-response-size: 1024
```

#### 3.4 JVM Tuning
**Action**: Optimize JVM parameters for high-throughput scenarios

**Recommended**:
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:+ParallelRefProcEnabled
-Xms2g -Xmx4g
```

### Priority 4: Monitoring and Profiling

#### 4.1 Add Performance Metrics
**Action**: Add metrics collection for key operations

**Metrics to track**:
- Eval request latency (p50, p95, p99)
- Database query latency
- Cache hit rate
- Thread pool utilization
- JVM GC metrics

**Implementation**:
- Use Micrometer + Prometheus
- Or use Spring Boot Actuator

#### 4.2 Profiling
**Action**: Use APM tools to identify bottlenecks

**Tools**:
- JProfiler
- Async Profiler
- Spring Boot Actuator

## Implementation Priority

1. **Immediate** (Week 1):
   - Async log writing
   - Reduce log level in hot path
   - Connection pool tuning

2. **Short-term** (Week 2-3):
   - Batch log insertion
   - Execution arrangement caching
   - JSON serialization optimization

3. **Medium-term** (Month 1-2):
   - Cache warm-up
   - Performance metrics
   - JVM tuning

4. **Long-term** (Month 3+):
   - Parallel rule execution (if applicable)
   - Response compression
   - Advanced profiling

## Expected Performance Improvements

| Optimization | Expected QPS Improvement | Expected Latency Reduction |
|-------------|-------------------------|---------------------------|
| Async log writing | +20-40% | -10-50ms |
| Reduce log level | +10-20% | -2-5ms |
| Batch log insertion | +5-10% | -5-10ms |
| Execution arrangement cache | +15-25% | -5-15ms |
| Connection pool tuning | +5-15% | -2-5ms |
| JSON optimization | +5-10% | -1-3ms |
| **Total Expected** | **+60-120%** | **-25-88ms** |

## Configuration Changes Required

### application.yml additions:
```yaml
rule:
  eval:
    log:
      async:
        enabled: true
        thread-pool-size: 10
        queue-capacity: 1000
      batch:
        enabled: true
        batch-size: 100
        flush-interval-ms: 1000
    cache:
      execution-arrangement:
        enabled: true
        ttl: 300  # 5 minutes
        capacity: 1000

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
```

## Testing Recommendations

1. **Load Testing**: Use existing `scripts/load_test.py` to measure improvements
2. **Baseline**: Establish current QPS and latency metrics
3. **Incremental Testing**: Test each optimization independently
4. **Stress Testing**: Test under peak load conditions
5. **Monitoring**: Track metrics before and after each change

## Notes

- All optimizations should maintain backward compatibility
- Test thoroughly in staging environment before production
- Monitor for any regressions or side effects
- Consider business requirements (e.g., log reliability) when implementing async operations

