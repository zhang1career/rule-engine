# Rule Engine System

[![CI](https://github.com/zhang1career/rule-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/zhang1career/rule-engine/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/zhang1career/rule-engine/branch/main/graph/badge.svg)](https://codecov.io/gh/zhang1career/rule-engine)

A rule calculation service based on SpringBoot, supporting multiple rule types and flexible rule execution flows.

## Technology Stack

- **JDK**: 1.8
- **Framework**: SpringBoot 2.7.18
- **Build Tool**: Maven 3.6
- **Script Engine**: Groovy 3.0.17
- **Expression Engine**: MVEL 2.4.14

## Core Features

- Rule execution engine: Supports multiple rule types (expression, script, API query, SQL query)
- Rule status management: Supports five statuses: offline, test, A/B test, full, gray
- **Status transition constraints**: Strict status transition rules to ensure the legality of rule status changes
- **Rule deletion constraints**: Only rules in offline status can be deleted
- A/B testing support: Rule group mechanism for probabilistic rule selection
- Execution sequence management: Supports configuring different rule execution sequences for different events
- Intelligent relationship management:
  - Automatically create/delete rule groups when rule status changes
  - Automatically synchronize event relationships between rules and rule groups
  - Automatically clean up empty rule groups and their relationships
- **RESTful API**: Provides complete event, rule, and rule group management interfaces
  - Event management: CRUD operations, associate rules/rule groups, modify execution order
  - Rule management: CRUD operations
  - Rule group management: Query, modify rule execution probability

### 1. Rule Evaluation Interface

**Interface Path**: `POST /rule/eval`

**Request Example**:
```json
{
  "userId": 123456789,
  "eventId": 1001,
  "traceId": 987654321,
  "arguments": {
    "amount": {
      "value": 1000.00,
      "type": "DECIMAL"
    },
    "age": {
      "value": 25,
      "type": "INTEGER"
    }
  }
}
```

**Response Example**:
```json
{
  "result": {
    "value": true,
    "type": "BOOLEAN"
  },
  "success": true
}
```

### 2. Rule Types

- **Logical Expression (EXPRESSION)**: Uses MVEL expression engine to execute logical expressions
- **API Query (API_QUERY)**: Calls external HTTP interfaces to retrieve data
- **SQL Query (SQL_QUERY)**: Executes database query operations
- **Script Execution (SCRIPT)**: Executes Groovy scripts

### 3. Rule Statuses

- **Offline (OFFLINE)**: Rule unavailable, execution not allowed
- **Test (TEST)**: Executable only in test environment
- **A/B Test (AB_TEST)**: Executed proportionally in production and gray environments
- **Full (FULL)**: Fully executed in production and gray environments
- **Gray (GRAY)**: Executable only in gray environment

#### Status Transition Constraints

Rule status transitions must follow these constraints:

1. **Offline Status (OFFLINE)**: Can only transition to Test status (TEST)
2. **Test Status (TEST)**: Can transition to Gray status (GRAY) or Offline status (OFFLINE)
3. **Gray Status (GRAY)**: Can transition to Offline status (OFFLINE) or A/B Test status (AB_TEST)
4. **A/B Test Status (AB_TEST)**: Can transition to Offline status (OFFLINE) or Full status (FULL)
5. **Full Status (FULL)**: Can transition to Offline status (OFFLINE) or A/B Test status (AB_TEST)

**Status Transition Diagram**:
```
OFFLINE → TEST
TEST → GRAY, OFFLINE
GRAY → OFFLINE, AB_TEST
AB_TEST → OFFLINE, FULL
FULL → OFFLINE, AB_TEST
```

**Rule Deletion Constraints**:
- Only rules in Offline status (OFFLINE) can be deleted
- Attempting to delete rules in non-offline status will throw an exception

### 4. Execution Flow

1. Get execution sequence based on `eventId`
2. Execute rules in the sequence in order
3. Determine execution based on rule status
4. Support early exit based on rule results
5. Support data transfer between rules
6. Support loops and recursive calls (controlled by execution depth)

## Quick Start

### 1. Environment Requirements

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+ (optional, only needed when using SQL query functionality)
- RabbitMQ (optional, only needed when message queue functionality is required)

### 2. Configuration

Edit `src/main/resources/application.yml`:

```yaml
# Rule engine environment configuration
rule:
  engine:
    environment: TEST  # or PRODUCTION or GRAY

# RabbitMQ configuration (optional)
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Database configuration (optional)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rule_engine
    username: root
    password: root
```

### 3. Build and Run

```bash
# Build project
mvn clean compile

# Run project
mvn spring-boot:run

# Or package and run
mvn clean package
java -jar target/rule-engine-0.7.0-SNAPSHOT.jar
```

### 4. Test Interface

```bash
curl -X POST http://localhost:8080/rule/eval \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456789,
    "eventId": 1001,
    "traceId": 987654321,
    "arguments": {
      "amount": {"value": 1000.00, "type": "DECIMAL"},
      "age": {"value": 25, "type": "INTEGER"}
    }
  }'
```

## Extension Guide

### 1. Adding New Rule Types

1. Add new type to `RuleType` enumeration
2. Implement `RuleExecutor` interface
3. Register executor in `RuleEngineConfig`

### 2. Supporting RPC Calls

RPC interface extension points are reserved and can be extended as follows:

- Create `RpcRuleController` to implement RPC interface
- Support RPC call methods in `EvalService`

### 3. Supporting Asynchronous Non-blocking Calls

- Use Spring WebFlux to implement reactive programming
- Add asynchronous interfaces at Controller layer

### 4. Data Persistence

Currently uses database storage (MySQL + MyBatis Plus), supporting:

- Persistent storage of rules, rule groups, and events
- Persistent storage of execution sequences
- Persistent storage of rule content

## Documentation

- [Product Requirements Document (PRD)](docs/prd/PRD.md)
- [API Interface Documentation](docs/api/API_DOCUMENTATION.md)
- [Database Schema Documentation](docs/schema/DATABASE_SCHEMA.md)
- [UML Activity Diagrams](docs/uml/)

## Notes

1. **Rule Configuration**: Uses MySQL database for persistent storage
2. **A/B Testing**: Implemented using rule group mechanism, data persisted in database
3. **Execution Depth Limit**: Default maximum execution depth is 100 to prevent infinite recursion
4. **Message Queue**: RabbitMQ is optional configuration, message sending will be skipped if not configured
5. **Database**: Database is required for storing rules, events, rule groups, etc.
6. **Redis Cache**: Rule selection cache uses Redis storage, cache key format is `rule:gw:abt:{userId}:{eventId}:{groupId}`, used to record selection results for each rule group, cache expiration time is 24 hours

## Development Plan

- [x] Basic architecture setup
- [x] HTTP synchronous blocking interface
- [x] Basic rule execution engine
- [x] Rule status control
- [ ] Kafka message sending
- [x] Rule configuration persistence
- [ ] Asynchronous non-blocking calls
- [ ] Rule execution monitoring and logging
- [ ] Rule content caching
- [ ] Security validation on script/api/sql/groovy content
- [ ] Load test
- [ ] docker configurable

## License

MIT License
