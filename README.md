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

**Interface Path**: `POST /rule/evalRequest`

**Request Example**:
```json
{
  "userId": 123456789,
  "eventId": 1001,
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
- **Online (ONLINE)**: Fully executed in production environments
- **Gray (GRAY)**: Executable only in production and gray environment

#### Status Transition Constraints

Rule status transitions must follow these constraints:

```
OFFLINE → TEST
TEST → GRAY, OFFLINE
GRAY → OFFLINE, ONLINE
ONLINE → OFFLINE, AB_TEST
```

**Rule Deletion Constraints**:
- Only rules in Offline status (OFFLINE) can be deleted
- Attempting to delete rules in non-offline status will throw an exception

**Event Association Constraints for Status Transitions**:
- For rules transitioning to Test (TEST), Gray (GRAY), or Online (ONLINE) status, the rule must be associated with at least one event

### 4. Execution Flow

1. Get execution sequence based on `eventId`
2. Execute rules in the sequence in order
3. Determine execution based on rule status
4. Support early exit based on rule results
5. Support data transfer between rules


## Documentation

- [Product Requirements Document (PRD)](docs/prd/PRD.md)
- [API Interface Documentation](docs/api/API_DOCUMENTATION.md)
- [Database Schema Documentation](docs/schema/DATABASE_SCHEMA.md)
- [UML Activity Diagrams](docs/uml/)


## License

MIT License
