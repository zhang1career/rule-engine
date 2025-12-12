# Database Schema Design

Database table structure designed according to project conventions (.cursorrules).

## Convention Notes

1. **Primary key field**: Use `id` uniformly, unsigned integer or long integer, auto-increment starting from 10,000,000
2. **Time fields**:
   - `ct`: Create time, unsigned integer, storing UNIX timestamp in seconds
   - `ut`: Update time, unsigned integer, storing UNIX timestamp in seconds
3. **Index naming**:
   - Key index: prefix `idx_`
   - Unique index: prefix `uni_`
4. **Relation table naming**: Relation tables should be suffixed with `_rel`
5. **All fields should have default values if possible**
6. **Comments should be written in English**

## 1. Rule Table (rule)

Stores basic rule information (excluding ruleContent) and rule status.

```sql
CREATE TABLE `rule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Rule ID, primary key',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Rule name',
  `description` VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'Rule description',
  `content_type` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule content type ID: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT',
  `content_args` VARCHAR(1000) DEFAULT '' COMMENT 'Rule content arguments, comma-separated list of parameter names',
  `rule_status` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule status ID: 0=OFFLINE, 1=TEST, 2=GRAY, 3=ONLINE',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`),
  KEY `idx_rule_status` (`rule_status`),
  KEY `idx_ct` (`ct`)
) AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='Rule table';
```

**Note**: 
- Enumeration fields (`content_type`, `rule_status`) now store enumeration IDs (integers) instead of enumeration values (strings). See migration script for details.
- Rule status: OFFLINE (0), TEST (1), GRAY (2), ONLINE (3). A/B test is no longer a rule status, but a traffic control mechanism applied to ONLINE rules through rule groups.

## 2. Rule Content Table (rule_content)

Stores rule content, one-to-one with rule table.

```sql
CREATE TABLE `rule_content` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT 'Rule ID, primary key, references rule.id',
  `content` TEXT NOT NULL COMMENT 'Rule content',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Rule content table';
```

## 3. Rule Group Table (rule_group)

Stores rule group information.

```sql
CREATE TABLE `rule_group` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Rule group ID, primary key',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='Rule group table';
```

## 4. Execution Event Relation Table (x)

Stores the relationship between events, rules and rule groups, as well as execution order and A/B test ratios.

**Design Principles**:
- A rule can be standalone (group_id = 0) or belong to a rule group (group_id != 0).
- Execution order starts from 0 and increments for each rule in the same event.
- A/B test ratio (ab_ratio) is only used when group_id != 0.
- When a rule belongs to a rule group, the group_id field stores the rule group ID.

```sql
CREATE TABLE `x` (
  `event_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Event ID',
  `rule_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule ID',
  `group_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule group ID: 0 means standalone rule, non-zero means rule belongs to group',
  `exe_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Execution order (starting from 0)',
  `ab_ratio` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'A/B test ratio (0-100), traffic control for rules in group',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`event_id`, `rule_id`),
  KEY `idx_rule` (`rule_id`, `group_id`),
  KEY `idx_group` (`group_id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Event-rule/group relation table';
```

**Note**: 
- The `group_id` field: 0 means the rule is standalone (not in any group), non-zero means the rule belongs to a rule group with this ID.
- The `exe_order` field stores execution order starting from 0.
- The `ab_ratio` field stores the traffic control ratio (0-100) for rules in groups. Default is 0 (bypass all requests).
- When a rule transitions to ONLINE status, the group_id is updated from 0 to the new rule group ID.
- When a rule transitions to OFFLINE status, all records for this rule are deleted.

## 5. Event Table (event)

Stores event information.

```sql
CREATE TABLE `event` (
  `id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Event ID, primary key, must be specified when creating',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Event name',
  `description` VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'Event description',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`),
  KEY `idx_event_name` (`name`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Event table';
```

**Note**: The `id` field must be specified when creating an event. It is not auto-increment.

## 6. Eval Log Table (eval_log)

Stores rule evaluation execution logs, including trace information, arguments, and execution steps.

```sql
CREATE TABLE `eval_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Eval log ID, primary key',
  `trace_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Business request trace ID',
  `event_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Event ID',
  `user_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'User ID',
  `arguments` TEXT COMMENT 'Parameter dictionary for rule calculation, JSON encoded string',
  `steps` TEXT COMMENT 'List of execution steps, JSON encoded string',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uni_trace_id` (`trace_id`)
) ENGINE=MyISAM AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='Eval log table';
```

**Note**: 
- The `arguments` field stores the parameter dictionary as a JSON encoded string.
- The `steps` field stores the list of execution steps as a JSON encoded string.
- The `trace_id` field has a unique index to ensure each trace ID is recorded only once.

## Index Notes

### 1. rule table
- `idx_rule_status`: Used to filter rules by status
- `idx_ct`: Used to sort by create time

**Note**: Rules belonging to a rule group are queried through the `x` table using `group_id` field.

### 2. rule_content table

### 3. rule_group table

### 4. x table
- `idx_group`: Used to query all rules in a rule group

### 5. event table
- `idx_event_name`: Used to query events by name

### 6. eval_log table
- `uni_trace_id`: Unique index on trace_id to ensure each trace ID is recorded only once

## Important Notes

1. **Primary key auto-increment starting value**: Most table primary keys start from 10,000,000 (except `event` table which requires manual ID specification)
2. **Event table ID**: The `event` table's `id` field must be specified when creating, it is not auto-increment
3. **Time fields**: Use UNIX timestamp (seconds), not DATETIME
4. **Relation table naming**: Relation tables must be suffixed with `_rel`
5. **Default values**: All fields should have default values if possible
6. **Comments**: All comments should be written in English

## Design Principles

### Rule Status and Traffic Control

1. **Rule Status**: 
   - OFFLINE (0): Rule is not associated with any event. Rules in OFFLINE status cannot be executed.
   - TEST (1): Rule can be executed in test environment. Must be associated with at least one event.
   - GRAY (2): Rule can be executed in gray environment. Must be associated with at least one event.
   - ONLINE (3): Rule can be executed in production environment. Must be associated with at least one event.

2. **Traffic Control (A/B Test)**:
   - A/B test is no longer a rule status, but a traffic control mechanism.
   - Traffic control is applied to ONLINE rules through rule groups.
   - Traffic control information (ab_ratio) is stored in the `x` table.
   - Default ab_ratio is 0 (bypass all requests).

3. **Rule-Event Association**:
   - A rule in OFFLINE status is not associated with any event.
   - When a rule transitions to TEST/GRAY/ONLINE status, it must be associated with at least one event.
   - When a rule transitions to OFFLINE status, all event associations are deleted.

### Rule Group Design

1. **Rule Group Creation**:
   - When a rule transitions to ONLINE status, a rule group is created for each event associated with the rule.
   - A rule group must be associated with exactly one event.
   - A rule group can contain a rule at most once.

2. **Rule Group Management**:
   - When a rule in a rule group transitions to OFFLINE status:
     - The record in `x` table is updated: group_id is set from groupId to 0 (standalone rule).
     - If the rule group becomes empty, the rule group is deleted.
   - When a rule group becomes empty, it is automatically deleted.

3. **Traffic Control**:
   - Traffic control ratios are managed through the `x` table (ab_ratio field).
   - The sum of all ratios in a rule group should not exceed 100.
   - Rules can be copied within a rule group, with the copied rule having ab_ratio = 0 by default.

4. **Execution Order**:
   - Execution order (exe_order) starts from 0 and increments for each rule in the same event.
   - Rules are executed in the order of exe_order from smallest to largest.
   - When setting execution items for an event, the order in the list determines the exe_order (0, 1, 2, ...).
