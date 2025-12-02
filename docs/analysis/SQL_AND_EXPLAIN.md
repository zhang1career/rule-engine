# SQL and EXPLAIN Statement Documentation

This document lists all SQL statements involved in API interfaces and their EXPLAIN analysis results.

## 1. Event Management Interfaces

### 1.1 GET /api/events/{eventId} - Query Event

**SQL**:
```sql
SELECT * FROM `event` WHERE `id` = ?;
```

**EXPLAIN**:
```sql
EXPLAIN SELECT * FROM `event` WHERE `id` = 10000001;
```

**Analysis Results**:
- `type`: const
- `key`: PRIMARY
- `rows`: 1
- `Extra`: Using index
- **Description**: Uses primary key query, optimal performance

---

### 1.2 GET /api/events - Query All Events

**SQL**:
```sql
SELECT * FROM `event`;
```

**EXPLAIN**:
```sql
EXPLAIN SELECT * FROM `event`;
```

**Analysis Results**:
- `type`: ALL
- `key`: NULL
- `rows`: Full table scan
- **Description**: Full table query, recommend adding pagination or limiting conditions

---

### 1.3 POST /api/events - Create Event

**SQL**:
```sql
-- 1. Check if event ID already exists
SELECT * FROM `event` WHERE `id` = ?;

-- 2. Insert event (if ID does not exist)
INSERT INTO `event` (`id`, `name`, `description`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?);
```

**EXPLAIN**:
```sql
-- Check if ID exists
EXPLAIN SELECT * FROM `event` WHERE `id` = 10000001;

-- Insert event
EXPLAIN INSERT INTO `event` (`id`, `name`, `description`, `ct`, `ut`) VALUES (10000001, 'Test Event', 'Test Description', 1234567890, 1234567890);
```

**Analysis Results**:
- Check ID: Uses primary key query, optimal performance
- Insert: INSERT operation, must specify ID (no longer uses auto-increment primary key)

---

### 1.4 PUT /api/events/{eventId} - Update Event

**SQL**:
```sql
-- 1. Query event
SELECT * FROM `event` WHERE `id` = ?;

-- 2. Update event
UPDATE `event` SET `name` = ?, `description` = ?, `ut` = ? WHERE `id` = ?;
```

**EXPLAIN**:
```sql
-- Query
EXPLAIN SELECT * FROM `event` WHERE `id` = 10000001;

-- Update
EXPLAIN UPDATE `event` SET `name` = 'Updated Name', `description` = 'Updated Description', `ut` = 1234567890 WHERE `id` = 10000001;
```

**Analysis Results**:
- Query: Uses primary key, optimal performance
- Update: Uses primary key, optimal performance

---

### 1.5 DELETE /api/events/{eventId} - Delete Event

**SQL**:
```sql
-- 1. Query event
SELECT * FROM `event` WHERE `id` = ?;

-- 2. Delete execution event relations
DELETE FROM `execution_event_rel` WHERE `event_id` = ?;

-- 3. Delete event
DELETE FROM `event` WHERE `id` = ?;
```

**EXPLAIN**:
```sql
-- Query event
EXPLAIN SELECT * FROM `event` WHERE `id` = 10000001;

-- Delete relations
EXPLAIN DELETE FROM `execution_event_rel` WHERE `event_id` = 10000001;

-- Delete event
EXPLAIN DELETE FROM `event` WHERE `id` = 10000001;
```

**Analysis Results**:
- Query event: Uses primary key
- Delete relations: Need to add `idx_event_id` index (if not exists)
- Delete event: Uses primary key

---

### 1.6 PUT /api/events/{eventId}/execution-items - Batch Set Execution Items

**SQL**:
```sql
-- 1. Query if event exists
SELECT * FROM `event` WHERE `id` = ?;

-- 2. Query existing relations
SELECT * FROM `execution_event_rel` WHERE `event_id` = ? ORDER BY `execution_order` ASC;

-- 3. Batch query if rules/rule groups exist (business logic validation)
SELECT * FROM `rule` WHERE `id` IN (?, ?, ...);
SELECT * FROM `rule_group` WHERE `id` IN (?, ?, ...);

-- 4. Query if relations exist (batch query)
SELECT * FROM `execution_event_rel` WHERE `event_id` = ?;

-- 5. Update or insert relations
UPDATE `execution_event_rel` SET `execution_order` = ?, `ut` = ? WHERE `event_id` = ? AND `item_type` = ? AND `item_id` = ?;
INSERT INTO `execution_event_rel` (`event_id`, `item_type`, `item_id`, `execution_order`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?, ?);
-- ... (multiple UPDATE/INSERT statements)

-- 6. Delete relations not in new list
DELETE FROM `execution_event_rel` WHERE `event_id` = ? AND `item_type` = ? AND `item_id` = ?;
-- ... (multiple DELETE statements)
```

**EXPLAIN**:
```sql
-- Query event
EXPLAIN SELECT * FROM `event` WHERE `id` = 10000001;

-- Query existing relations
EXPLAIN SELECT * FROM `execution_event_rel` WHERE `event_id` = 10000001 ORDER BY `execution_order` ASC;

-- Batch query rules
EXPLAIN SELECT * FROM `rule` WHERE `id` IN (10000001, 10000002);

-- Batch query rule groups
EXPLAIN SELECT * FROM `rule_group` WHERE `id` IN (10000001, 10000002);

-- Query all relations
EXPLAIN SELECT * FROM `execution_event_rel` WHERE `event_id` = 10000001;

-- Update relation
EXPLAIN UPDATE `execution_event_rel` SET `execution_order` = 1, `ut` = 1234567890 WHERE `event_id` = 10000001 AND `item_type` = 1 AND `item_id` = 10000001;

-- Insert relation
EXPLAIN INSERT INTO `execution_event_rel` (`event_id`, `item_type`, `item_id`, `execution_order`, `ct`, `ut`) VALUES (10000001, 1, 10000001, 1, 1234567890, 1234567890);

-- Delete relation
EXPLAIN DELETE FROM `execution_event_rel` WHERE `event_id` = 10000001 AND `item_type` = 1 AND `item_id` = 10000001;
```

**Analysis Results**:
- Query event: Uses primary key
- Query existing relations: Uses primary key `event_id` part, recommend adding `idx_event_id` index
- Batch query rules/rule groups: Uses primary key, IN query performs well
- Query all relations: Uses primary key `event_id` part
- Update/Insert/Delete: Uses composite primary key

---

### 1.7 GET /api/events/{eventId}/execution-items - Query Execution Items

**SQL**:
```sql
SELECT * FROM `execution_event_rel` WHERE `event_id` = ? ORDER BY `execution_order` ASC;
```

**EXPLAIN**:
```sql
EXPLAIN SELECT * FROM `execution_event_rel` WHERE `event_id` = 10000001 ORDER BY `execution_order` ASC;
```

**Analysis Results**:
- `type`: ref
- `key`: PRIMARY (uses `event_id` part of primary key)
- **Description**: Recommend adding `idx_event_id` index to improve performance

---

## 2. Rule Management Interfaces

### 2.1 GET /api/rules/{ruleId} - Query Rule

**SQL**:
```sql
-- 1. Query rule basic information
SELECT * FROM `rule` WHERE `id` = ?;

-- 2. Query rule content
SELECT * FROM `rule_content` WHERE `id` = ?;
```

**EXPLAIN**:
```sql
-- Query rule
EXPLAIN SELECT * FROM `rule` WHERE `id` = 10000001;

-- Query rule content
EXPLAIN SELECT * FROM `rule_content` WHERE `id` = 10000001;
```

**Analysis Results**:
- Query rule: Uses primary key, optimal performance
- Query rule content: Uses primary key, optimal performance

---

### 2.2 GET /api/rules - Query All Rules

**SQL**:
```sql
-- 1. Query all rules
SELECT * FROM `rule`;

-- 2. Batch query rule content (if there are many rules, can query in batches)
SELECT * FROM `rule_content` WHERE `id` IN (?, ?, ...);
```

**EXPLAIN**:
```sql
-- Query all rules
EXPLAIN SELECT * FROM `rule`;

-- Batch query rule content
EXPLAIN SELECT * FROM `rule_content` WHERE `id` IN (10000001, 10000002, 10000003);
```

**Analysis Results**:
- Query all rules: Full table scan, recommend adding pagination
- Batch query rule content: Uses primary key, IN query performs well

---

### 2.3 POST /api/rules - Create Rule

**SQL**:
```sql
-- 1. Insert rule basic information
INSERT INTO `rule` (`name`, `content_type`, `rule_status`, `rule_group_id`, `description`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?, ?, ?);

-- 2. Insert or update rule content
INSERT INTO `rule_content` (`id`, `content`, `ct`, `ut`) VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE `content` = ?, `ut` = ?;
```

**EXPLAIN**:
```sql
-- Insert rule
EXPLAIN INSERT INTO `rule` (`name`, `content_type`, `rule_status`, `rule_group_id`, `description`, `ct`, `ut`) VALUES ('Test Rule', 1, 1, 0, 'Test Description', 1234567890, 1234567890);

-- Insert rule content
EXPLAIN INSERT INTO `rule_content` (`id`, `content`, `ct`, `ut`) VALUES (10000001, 'rule content', 1234567890, 1234567890);
```

**Analysis Results**:
- Insert rule: Uses auto-increment primary key
- Insert rule content: Uses primary key

---

### 2.4 PUT /api/rules/{ruleId} - Update Rule

**SQL**:
```sql
-- 1. Query rule
SELECT * FROM `rule` WHERE `id` = ?;

-- 2. Update rule basic information
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = ?, `rule_group_id` = ?, `description` = ?, `ut` = ? WHERE `id` = ?;

-- 3. Insert or update rule content
INSERT INTO `rule_content` (`id`, `content`, `ct`, `ut`) VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE `content` = ?, `ut` = ?;
```

**EXPLAIN**:
```sql
-- Query rule
EXPLAIN SELECT * FROM `rule` WHERE `id` = 10000001;

-- Update rule
EXPLAIN UPDATE `rule` SET `name` = 'Updated Name', `content_type` = 1, `rule_status` = 1, `rule_group_id` = 0, `description` = 'Updated Description', `ut` = 1234567890 WHERE `id` = 10000001;

-- Insert or update rule content
EXPLAIN INSERT INTO `rule_content` (`id`, `content`, `ct`, `ut`) VALUES (10000001, 'updated content', 1234567890, 1234567890)
ON DUPLICATE KEY UPDATE `content` = 'updated content', `ut` = 1234567890;
```

**Analysis Results**:
- Query rule: Uses primary key
- Update rule: Uses primary key
- Insert or update rule content: Uses primary key

---

### 2.5 DELETE /api/rules/{ruleId} - Delete Rule

**SQL**:
```sql
-- 1. Query rule
SELECT * FROM `rule` WHERE `id` = ?;

-- 2. Delete rule content
DELETE FROM `rule_content` WHERE `id` = ?;

-- 3. Delete rule
DELETE FROM `rule` WHERE `id` = ?;

-- 4. Delete execution event relations
DELETE FROM `execution_event_rel` WHERE `item_type` = 1 AND `item_id` = ?;
```

**EXPLAIN**:
```sql
-- Query rule
EXPLAIN SELECT * FROM `rule` WHERE `id` = 10000001;

-- Delete rule content
EXPLAIN DELETE FROM `rule_content` WHERE `id` = 10000001;

-- Delete rule
EXPLAIN DELETE FROM `rule` WHERE `id` = 10000001;

-- Delete execution event relations
EXPLAIN DELETE FROM `execution_event_rel` WHERE `item_type` = 1 AND `item_id` = 10000001;
```

**Analysis Results**:
- Query rule: Uses primary key
- Delete rule content: Uses primary key
- Delete rule: Uses primary key
- Delete execution event relations: Uses `idx_item` index

---

## 3. Rule Group Management Interfaces

### 3.1 GET /api/rule-groups/{groupId} - Query Rule Group

**SQL**:
```sql
-- 1. Query rule group basic information
SELECT * FROM `rule_group` WHERE `id` = ?;

-- 2. Query rules associated with rule group
SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = ?;
```

**EXPLAIN**:
```sql
-- Query rule group
EXPLAIN SELECT * FROM `rule_group` WHERE `id` = 10000001;

-- Query rule group relations
EXPLAIN SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = 10000001;
```

**Analysis Results**:
- Query rule group: Uses primary key
- Query rule group relations: Need to add `idx_group_id` index (if not exists)

---

### 3.2 GET /api/rule-groups - Query All Rule Groups

**SQL**:
```sql
-- 1. Query all rule groups
SELECT * FROM `rule_group`;

-- 2. Batch query all rule group relations (query once)
SELECT * FROM `rule_group_rule_rel` WHERE `group_id` IN (?, ?, ...);
```

**EXPLAIN**:
```sql
-- Query all rule groups
EXPLAIN SELECT * FROM `rule_group`;

-- Batch query rule group relations
EXPLAIN SELECT * FROM `rule_group_rule_rel` WHERE `group_id` IN (10000001, 10000002, 10000003);
```

**Analysis Results**:
- Query all rule groups: Full table scan, recommend adding pagination
- Batch query rule group relations: Need to add `idx_group_id` index, IN query performs well

---

### 3.3 POST /api/rule-groups - Create Rule Group

**SQL**:
```sql
-- 1. Batch validate rule status (application layer logic)
SELECT * FROM `rule` WHERE `id` IN (?, ?, ...);

-- 2. Create rule group
INSERT INTO `rule_group` (`ct`, `ut`) VALUES (?, ?);

-- 3. Batch update rule status to AB_TEST and set rule group ID (batch update)
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = 4, `rule_group_id` = ?, `description` = ?, `ut` = ?, `ct` = ? WHERE `id` = ?;
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = 4, `rule_group_id` = ?, `description` = ?, `ut` = ?, `ct` = ? WHERE `id` = ?;
-- ... (multiple UPDATE statements, batch updated by MyBatis Plus)

-- 4. Batch add rules to rule group relation table
INSERT INTO `rule_group_rule_rel` (`group_id`, `rule_id`, `ab_test_ratio`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?);
INSERT INTO `rule_group_rule_rel` (`group_id`, `rule_id`, `ab_test_ratio`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?);
-- ... (multiple INSERT statements)

-- 5. Batch copy rule-event relations to rule group-event relations (business logic)
SELECT * FROM `execution_event_rel` WHERE `item_type` = 1 AND `item_id` IN (?, ?, ...);
INSERT INTO `execution_event_rel` (`event_id`, `item_type`, `item_id`, `execution_order`, `ct`, `ut`) VALUES (?, 2, ?, ?, ?, ?);
-- ... (multiple INSERT statements)
```

**EXPLAIN**:
```sql
-- Batch query rules
EXPLAIN SELECT * FROM `rule` WHERE `id` IN (10000001, 10000002);

-- Create rule group
EXPLAIN INSERT INTO `rule_group` (`ct`, `ut`) VALUES (1234567890, 1234567890);

-- Batch update rule status (example: update single rule)
EXPLAIN UPDATE `rule` SET `name` = 'Test Rule', `content_type` = 0, `rule_status` = 4, `rule_group_id` = 10000001, `description` = 'Test', `ut` = 1234567890, `ct` = 1234567890 WHERE `id` = 10000001;

-- Add rule relation
EXPLAIN INSERT INTO `rule_group_rule_rel` (`group_id`, `rule_id`, `ab_test_ratio`, `ct`, `ut`) VALUES (10000001, 10000001, 50, 1234567890, 1234567890);

-- Batch query rule-event relations
EXPLAIN SELECT * FROM `execution_event_rel` WHERE `item_type` = 1 AND `item_id` IN (10000001, 10000002);

-- Insert rule group-event relation
EXPLAIN INSERT INTO `execution_event_rel` (`event_id`, `item_type`, `item_id`, `execution_order`, `ct`, `ut`) VALUES (10000001, 2, 10000001, 1, 1234567890, 1234567890);
```

**Analysis Results**:
- Batch query rules: Uses primary key, IN query performs well
- Create rule group: Insert operation, uses auto-increment primary key
- Batch update rule status: Uses primary key, each UPDATE statement executed independently (MyBatis Plus batch update)
- Add rule relation: Uses composite primary key
- Batch query rule-event relations: Uses `idx_item` index
- Insert rule group-event relation: Uses composite primary key

---

### 3.4 PUT /api/rule-groups/{groupId} - Update Rule Group

**SQL**:
```sql
-- 1. Query if rule group exists
SELECT * FROM `rule_group` WHERE `id` = ?;

-- 2. Query existing rules in rule group
SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = ?;

-- 3. Batch validate new rule status (application layer logic)
SELECT * FROM `rule` WHERE `id` IN (?, ?, ...);

-- 4. Batch update rule status to AB_TEST (if not already AB_TEST)
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = 4, `rule_group_id` = ?, `description` = ?, `ut` = ?, `ct` = ? WHERE `id` = ?;
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = 4, `rule_group_id` = ?, `description` = ?, `ut` = ?, `ct` = ? WHERE `id` = ?;
-- ... (multiple UPDATE statements, batch updated by MyBatis Plus)

-- 5. Batch add or update rules to rule group relation table
INSERT INTO `rule_group_rule_rel` (`group_id`, `rule_id`, `ab_test_ratio`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?);
INSERT INTO `rule_group_rule_rel` (`group_id`, `rule_id`, `ab_test_ratio`, `ct`, `ut`) VALUES (?, ?, ?, ?, ?);
-- ... (multiple INSERT statements)

-- 6. Batch update ratios in rule group relation table (if exists)
UPDATE `rule_group_rule_rel` SET `ab_test_ratio` = ?, `ut` = ?, `ct` = ? WHERE `group_id` = ? AND `rule_id` = ?;
UPDATE `rule_group_rule_rel` SET `ab_test_ratio` = ?, `ut` = ?, `ct` = ? WHERE `group_id` = ? AND `rule_id` = ?;
-- ... (multiple UPDATE statements, batch updated by MyBatis Plus)

-- 7. Batch delete rules not in new rule list (set to OFFLINE and remove from group)
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = 1, `rule_group_id` = 0, `description` = ?, `ut` = ?, `ct` = ? WHERE `id` = ?;
UPDATE `rule` SET `name` = ?, `content_type` = ?, `rule_status` = 1, `rule_group_id` = 0, `description` = ?, `ut` = ?, `ct` = ? WHERE `id` = ?;
-- ... (multiple UPDATE statements, batch updated by MyBatis Plus)
DELETE FROM `rule_group_rule_rel` WHERE `group_id` = ? AND `rule_id` IN (?, ?, ...);

-- 8. Update rule group update time
UPDATE `rule_group` SET `ut` = ? WHERE `id` = ?;
```

**EXPLAIN**:
```sql
-- Query rule group
EXPLAIN SELECT * FROM `rule_group` WHERE `id` = 10000001;

-- Query rule group relations
EXPLAIN SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = 10000001;

-- Batch query rules
EXPLAIN SELECT * FROM `rule` WHERE `id` IN (10000001, 10000002);

-- Batch update rule status (example: update single rule)
EXPLAIN UPDATE `rule` SET `name` = 'Test Rule', `content_type` = 0, `rule_status` = 4, `rule_group_id` = 10000001, `description` = 'Test', `ut` = 1234567890, `ct` = 1234567890 WHERE `id` = 10000001;

-- Batch update rule group relation ratios (example: update single relation)
EXPLAIN UPDATE `rule_group_rule_rel` SET `ab_test_ratio` = 50, `ut` = 1234567890, `ct` = 1234567890 WHERE `group_id` = 10000001 AND `rule_id` = 10000001;

-- Batch delete rule relations
EXPLAIN DELETE FROM `rule_group_rule_rel` WHERE `group_id` = 10000001 AND `rule_id` IN (10000002, 10000003);

-- Update rule group
EXPLAIN UPDATE `rule_group` SET `ut` = 1234567890 WHERE `id` = 10000001;
```

**Analysis Results**:
- Query rule group: Uses primary key
- Query rule group relations: Need to add `idx_group_id` index
- Batch query rules: Uses primary key, IN query performs well
- Batch update rule status: Uses primary key, each UPDATE statement executed independently (MyBatis Plus batch update)
- Batch update rule group relation ratios: Uses composite primary key, each UPDATE statement executed independently (MyBatis Plus batch update)
- Batch delete rule relations: Uses composite primary key, IN query performs well
- Update rule group: Uses primary key

---

### 3.5 DELETE /api/rule-groups/{groupId} - Delete Rule Group

**SQL**:
```sql
-- 1. Query if rule group exists
SELECT * FROM `rule_group` WHERE `id` = ?;

-- 2. Query all rules in rule group
SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = ?;

-- 3. Set all rules in rule group to OFFLINE
UPDATE `rule` SET `rule_status` = 1, `rule_group_id` = 0, `ut` = ? WHERE `id` = ?;

-- 4. Delete rule group relations
DELETE FROM `rule_group_rule_rel` WHERE `group_id` = ?;

-- 5. Delete rule group
DELETE FROM `rule_group` WHERE `id` = ?;
```

**EXPLAIN**:
```sql
-- Query rule group
EXPLAIN SELECT * FROM `rule_group` WHERE `id` = 10000001;

-- Query rule group relations
EXPLAIN SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = 10000001;

-- Update rule status
EXPLAIN UPDATE `rule` SET `rule_status` = 1, `rule_group_id` = 0, `ut` = 1234567890 WHERE `id` = 10000001;

-- Delete rule group relations
EXPLAIN DELETE FROM `rule_group_rule_rel` WHERE `group_id` = 10000001;

-- Delete rule group
EXPLAIN DELETE FROM `rule_group` WHERE `id` = 10000001;
```

**Analysis Results**:
- Query rule group: Uses primary key
- Query rule group relations: Need to add `idx_group_id` index
- Update rule status: Uses primary key
- Delete rule group relations: Uses `group_id` index
- Delete rule group: Uses primary key

---

## 4. Rule Evaluation Interface

### 4.1 POST /api/eval - Execute Rule Evaluation

**SQL**:
```sql
-- 1. Query event execution items (batch query)
SELECT * FROM `execution_event_rel` WHERE `event_id` = ? ORDER BY `execution_order` ASC;

-- 2. Batch query rules or rule groups based on execution item type
-- If rules (batch query)
SELECT * FROM `rule` WHERE `id` IN (?, ?, ...);
SELECT * FROM `rule_content` WHERE `id` IN (?, ?, ...);

-- If rule groups (batch query)
SELECT * FROM `rule_group` WHERE `id` IN (?, ?, ...);
SELECT * FROM `rule_group_rule_rel` WHERE `group_id` IN (?, ?, ...);

-- 3. When selecting rule from rule group, batch query rule relations and ratios
SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = ? AND `rule_id` IN (?, ?, ...);

-- 4. Query selected rule (single query)
SELECT * FROM `rule` WHERE `id` = ?;
SELECT * FROM `rule_content` WHERE `id` = ?;
```

**EXPLAIN**:
```sql
-- Query event execution items
EXPLAIN SELECT * FROM `execution_event_rel` WHERE `event_id` = 10000001 ORDER BY `execution_order` ASC;

-- Batch query rules
EXPLAIN SELECT * FROM `rule` WHERE `id` IN (10000001, 10000002, 10000003);
EXPLAIN SELECT * FROM `rule_content` WHERE `id` IN (10000001, 10000002, 10000003);

-- Batch query rule groups
EXPLAIN SELECT * FROM `rule_group` WHERE `id` IN (10000001, 10000002);
EXPLAIN SELECT * FROM `rule_group_rule_rel` WHERE `group_id` IN (10000001, 10000002);

-- Batch query rule relations
EXPLAIN SELECT * FROM `rule_group_rule_rel` WHERE `group_id` = 10000001 AND `rule_id` IN (10000001, 10000002);

-- Query selected rule
EXPLAIN SELECT * FROM `rule` WHERE `id` = 10000001;
EXPLAIN SELECT * FROM `rule_content` WHERE `id` = 10000001;
```

**Analysis Results**:
- Query event execution items: Uses primary key `event_id` part, recommend adding `idx_event_id` index
- Batch query rules: Uses primary key, IN query performs well
- Batch query rule content: Uses primary key, IN query performs well
- Batch query rule groups: Uses primary key, IN query performs well
- Batch query rule group relations: Uses composite primary key, IN query performs well
- Query selected rule: Uses primary key, optimal performance

---

## 5. Dictionary Interface

### 5.1 GET /api/dicts - Query Enumeration Values

**SQL**:
```sql
-- This interface does not involve database queries, directly gets information from enumeration classes
-- No SQL statements
```

**EXPLAIN**:
```sql
-- No SQL statements
```

**Analysis Results**:
- This interface does not involve database operations, optimal performance

---

## Index Recommendations

### 1. execution_event_rel Table

Recommend adding the following indexes to improve query performance:

```sql
-- Index for querying by event_id
CREATE INDEX `idx_event_id` ON `execution_event_rel` (`event_id`);

-- Index for querying by item_type and item_id (idx_item already exists)
-- But recommend checking if it covers all query scenarios
```

### 2. rule_group_rule_rel Table

Recommend adding the following indexes:

```sql
-- Index for querying by group_id
CREATE INDEX `idx_group_id` ON `rule_group_rule_rel` (`group_id`);

-- Index for querying by rule_id (if reverse query is needed)
CREATE INDEX `idx_rule_id` ON `rule_group_rule_rel` (`rule_id`);
```

### 3. rule Table

Existing indexes:
- `idx_rule_group`: Used for querying rules associated with rule groups
- `idx_rule_status`: Used for filtering rules by status
- `idx_ct`: Used for sorting by creation time

### 4. event Table

Existing indexes:
- `idx_event_name`: Used for querying events by name

---

## Performance Optimization Recommendations

1. **Pagination**: For full table query interfaces like `GET /api/events` and `GET /api/rules`, recommend adding pagination functionality
2. **Index Optimization**: Add necessary indexes according to the above recommendations to improve query performance
3. **Query Optimization**: For queries that need to join multiple tables, consider using JOIN or subquery optimization
4. **Cache Strategy**: For frequently queried rules and rule groups, consider adding a cache layer

---

## Notes

1. All SQL statements use parameterized queries to prevent SQL injection
2. All delete operations use primary keys or indexed fields to ensure performance
3. Use of composite primary keys ensures data uniqueness and query performance
4. Enumeration fields use integer IDs for storage, improving query and comparison performance
