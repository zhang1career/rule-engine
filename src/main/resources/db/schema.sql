-- Database schema for rule engine
-- According to project conventions (.cursorrules)
-- 
-- Field naming conventions:
-- - Primary key: `id` (unsigned integer/long, auto-increment from 10,000,000)
-- - Create time: `ct` (unsigned integer, UNIX timestamp in seconds)
-- - Update time: `ut` (unsigned integer, UNIX timestamp in seconds)
-- - Index naming: `idx_` for key index, `uni_` for unique index
-- - All fields should have default values if possible
-- - Comments should be written in English

-- 1. Rule table
CREATE TABLE IF NOT EXISTS `rule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Rule ID, primary key',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Rule name',
  `content_type` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule content type ID: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT',
  `rule_status` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule status ID: 0=OFFLINE, 1=TEST, 2=GRAY, 3=ONLINE',
  `description` VARCHAR(500) DEFAULT '' COMMENT 'Rule description',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`),
  KEY `idx_rule_status` (`rule_status`),
  KEY `idx_ct` (`ct`)
) AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='Rule table';

-- 2. Rule content table
CREATE TABLE IF NOT EXISTS `rule_content` (
  `id` BIGINT UNSIGNED NOT NULL COMMENT 'Rule ID, primary key, references rule.id',
  `content` TEXT NOT NULL COMMENT 'Rule content',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Rule content table';

-- 3. Rule group table
CREATE TABLE IF NOT EXISTS `rule_group` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Rule group ID, primary key',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8mb4 COMMENT='Rule group table';

-- 4. Execution event relation table (x)
CREATE TABLE IF NOT EXISTS `x` (
  `event_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Event ID',
  `rule_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule ID',
  `group_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Rule group ID: 0 means standalone rule, non-zero means rule belongs to group',
  `exe_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Execution order (starting from 0)',
  `ab_ratio` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'A/B test ratio (0-100), traffic control for rules in group',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`event_id`, `rule_id`),
  KEY `idx_group` (`group_id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Event-rule/group relation table';

-- 6. Event table
CREATE TABLE IF NOT EXISTS `event` (
  `id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Event ID, primary key, must be specified when creating',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Event name',
  `description` VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'Event description',
  `ct` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Create time, UNIX timestamp in seconds',
  `ut` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Update time, UNIX timestamp in seconds',
  PRIMARY KEY (`id`),
  KEY `idx_event_name` (`name`)
) DEFAULT CHARSET=utf8mb4 COMMENT='Event table';

-- 7. Eval log table
CREATE TABLE IF NOT EXISTS `eval_log` (
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
