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
  `content_type` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule content type ID: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT',
  `rule_status` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule status ID: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL',
  `rule_group_id` BIGINT UNSIGNED DEFAULT 0 COMMENT 'Rule group ID, only valid when rule_status is AB_TEST',
  `description` VARCHAR(500) DEFAULT '' COMMENT 'Rule description',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`),
  KEY `idx_rule_group` (`rule_group_id`),
  KEY `idx_rule_status` (`rule_status`),
  KEY `idx_ct` (`ct`)
) AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='Rule table';
```

**Note**: Enumeration fields (`content_type`, `rule_status`) now store enumeration IDs (integers) instead of enumeration values (strings). See migration script for details.

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

## 4. Rule Group and Rule Association Table (rule_group_rule_rel)

Stores the association relationship between rule groups and rules.

```sql
CREATE TABLE `rule_group_rule_rel` (
  `group_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule group ID',
  `rule_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule ID',
  `ab_test_ratio` INT UNSIGNED DEFAULT 0 COMMENT 'A/B test ratio (0-100), only valid when rule status is AB_TEST',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`group_id`, `rule_id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Rule group and rule association table';
```

**Note**: The `ab_test_ratio` field stores the probability distribution for A/B testing. It is only meaningful when the rule is in AB_TEST status and belongs to a rule group.

## 5. Execution Event Relation Table (execution_event_rel)

Stores the relationship between rules (or rule groups) and events, as well as execution order.

```sql
CREATE TABLE `execution_event_rel` (
  `event_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Event ID',
  `item_type` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Item type ID: 0=RULE, 1=RULE_GROUP',
  `item_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Item ID (rule ID or rule group ID)',
  `execution_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Execution order (starting from 1)',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`event_id`, `item_type`, `item_id`),
  KEY `idx_item` (`item_type`, `item_id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Execution event relation table';
```

**Note**: The `item_type` field now stores enumeration ID (integer) instead of enumeration value (string). See migration script for details.

## 6. Event Table (event)

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

## Index Notes

### 1. rule table
- `idx_rule_group`: Used to query rules belonging to a rule group
- `idx_status`: Used to filter rules by status
- `idx_ct`: Used to sort by create time

### 2. rule_content table

### 3. rule_group table

### 4. rule_group_rule_rel table

### 5. execution_event_rel table
- `idx_item`: Used to query all events associated with an item

### 6. event table
- `idx_event_name`: Used to query events by name

## Important Notes

1. **Primary key auto-increment starting value**: Most table primary keys start from 10,000,000 (except `event` table which requires manual ID specification)
2. **Event table ID**: The `event` table's `id` field must be specified when creating, it is not auto-increment
3. **Time fields**: Use UNIX timestamp (seconds), not DATETIME
4. **Relation table naming**: Relation tables must be suffixed with `_rel`
5. **Default values**: All fields should have default values if possible
6. **Comments**: All comments should be written in English
