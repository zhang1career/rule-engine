# SQL and EXPLAIN Statement Documentation

This document lists all SQL statements involved in the critical interfaces and their EXPLAIN analysis results.

## POST /api/eval - Execute Rule Evaluation

**SQL**:
```sql
-- 1. Query execution arrangements by event ID with rule status filter
SELECT x.event_id, x.rule_id, x.group_id, x.exe_order, x.ab_ratio, x.ct, x.ut
FROM x
INNER JOIN rule ON x.rule_id = rule.id
WHERE x.event_id = ?
AND rule.rule_status IN (?, ?, ...)
ORDER BY x.exe_order ASC;

-- 2. Batch query rules
SELECT id, name, content_type, rule_status, description, content_args, ct, ut 
FROM rule 
WHERE id IN (?, ?, ...);

-- 3. Batch query rule contents
SELECT id, content, ct, ut 
FROM rule_content 
WHERE id IN (?, ?, ...);

-- 4. Insert evaluation log
INSERT INTO eval_log (trace_id, event_id, user_id, arguments, steps, ct, ut) 
VALUES (?, ?, ?, ?, ?, ?, ?);
```

**EXPLAIN**:
```sql
-- Query execution arrangements with rule status filter
EXPLAIN SELECT x.event_id, x.rule_id, x.group_id, x.exe_order, x.ab_ratio, x.ct, x.ut
FROM x
INNER JOIN rule ON x.rule_id = rule.id
WHERE x.event_id = 10000001
AND rule.rule_status IN (2, 3, 4)
ORDER BY x.exe_order ASC;

-- Batch query rules
EXPLAIN SELECT id, name, content_type, rule_status, description, content_args, ct, ut 
FROM rule 
WHERE id IN (10000001, 10000002, 10000003);

-- Batch query rule contents
EXPLAIN SELECT id, content, ct, ut 
FROM rule_content 
WHERE id IN (10000001, 10000002, 10000003);

-- Insert evaluation log
EXPLAIN INSERT INTO eval_log (trace_id, event_id, user_id, arguments, steps, ct, ut) 
VALUES (999999, 10000001, 123456, '{"amount":{"value":1000.0,"type":"DECIMAL"}}', '[{"itemType":0,"itemId":10000001,"result":{"value":true,"type":"BOOLEAN"}}]', 1234567890, 1234567890);
```

**Analysis Results**:
- Query execution arrangements: Uses composite primary key `(event_id, rule_id)` with `event_id` part, and JOIN with `rule` table using `rule.id` primary key. The `rule.rule_status` filter uses `idx_rule_status` index. Recommend adding `idx_event_id` index on `x` table for better performance.
- Batch query rules: Uses primary key `id`, IN query performs well.
- Batch query rule contents: Uses primary key `id`, IN query performs well.
- Insert evaluation log: Uses unique index `uni_trace_id` on `trace_id` to ensure uniqueness.

---

## Index Recommendations

### 1. x Table

Existing indexes:
- Primary key: `(event_id, rule_id)`
- `idx_rule`: `(rule_id, group_id)`
- `idx_group`: `(group_id)`

Recommend adding:
```sql
-- Index for querying by event_id (improves JOIN query performance)
CREATE INDEX `idx_event_id` ON `x` (`event_id`);
```

### 2. rule Table

Existing indexes:
- Primary key: `id`
- `idx_rule_status`: Used to filter rules by status (used in JOIN query)
- `idx_ct`: Used to sort by create time

### 3. rule_content Table

Existing indexes:
- Primary key: `id`

### 4. eval_log Table

Existing indexes:
- Primary key: `id`
- `uni_trace_id`: Unique index on `trace_id` to ensure each trace ID is recorded only once

---

## Performance Optimization Recommendations

1. **Index Optimization**: Add `idx_event_id` index on `x` table to improve JOIN query performance when filtering by `event_id`.
2. **Query Optimization**: The JOIN query with `rule_status` filter is optimized using `idx_rule_status` index on `rule` table.
3. **Cache Strategy**: Rule content caching is implemented to reduce database queries for frequently accessed rules.
4. **Batch Operations**: Batch queries are used for loading multiple rules and rule contents to reduce database round trips.

---

## Notes

1. All SQL statements use parameterized queries to prevent SQL injection.
2. The execution arrangement query uses INNER JOIN with `rule` table to filter by `rule_status`, ensuring only rules in allowed statuses are returned.
3. Rule selection from rule groups is performed in memory based on `ab_ratio` and `userHashInt`, no additional SQL queries are needed.
4. MyBatis Plus `selectBatchIds` method is used for batch queries, which generates efficient IN queries.
5. The `eval_log` insert operation uses unique index on `trace_id` to prevent duplicate log entries.