# RESTful API Documentation

## Overview

This document describes the RESTful API interfaces of the rule engine system, including management interfaces for events, rules, and rule groups.

## Basic Information

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **Response Format**: Unified use of `ApiResponse` wrapper

### ApiResponse Format

```json
{
  "code": 0,
  "message": "Success",
  "data": {}
}
```

- `code`: 0 indicates success, non-zero indicates failure
- `message`: Response message
- `data`: Response data

## 1. Event Management Interfaces

### 1.1 Create Event

**Interface**: `POST /api/events`

**Request Body**:
```json
{
  "id": 10000001,
  "name": "User Registration Event",
  "description": "Event triggered when user registers"
}
```

**Description**:
- `id`: Event ID (required), must be specified and cannot be empty
- `name`: Event name (required)
- `description`: Event description (optional)
- If the specified ID already exists, will return 400 error

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10000001,
    "name": "User Registration Event",
    "description": "Event triggered when user registers"
  }
}
```

**Call Example**:
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10000001,
    "name": "User Registration Event",
    "description": "Event triggered when user registers"
  }'
```

### 1.2 Query Event

**Interface**: `GET /api/events/{eventId}`

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10000001,
    "name": "User Registration Event",
    "description": "Event triggered when user registers"
  }
}
```

**Note**: EventDTO does not contain executionItems field. To query event execution items, please use `GET /api/events/{eventId}/execution-items` interface.

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/events/10000001
```

### 1.3 Query All Events

**Interface**: `GET /api/events`

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 10000001,
      "name": "User Registration Event",
      "description": "Event triggered when user registers"
    },
    {
      "id": 10000002,
      "name": "User Login Event",
      "description": "Event triggered when user logs in"
    }
  ]
}
```

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/events
```

### 1.4 Update Event

**Interface**: `PUT /api/events/{eventId}`

**Request Body** (all fields are optional, only provided fields will be updated):
```json
{
  "name": "User Registration Event (Updated)",
  "description": "Updated description"
}
```

**Description**: 
- `name`: Event name (optional), will be updated if provided
- `description`: Event description (optional), will be updated if provided
- Only fields provided in the request body will be updated, unprovided fields remain unchanged

**Call Example**:
```bash
# Update name only
curl -X PUT http://localhost:8080/api/events/10000001 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "User Registration Event (Updated)"
  }'

# Update description only
curl -X PUT http://localhost:8080/api/events/10000001 \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated description"
  }'

# Update both name and description
curl -X PUT http://localhost:8080/api/events/10000001 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "User Registration Event (Updated)",
    "description": "Updated description"
  }'
```

### 1.5 Delete Event

**Interface**: `DELETE /api/events/{eventId}`

**Call Example**:
```bash
curl -X DELETE http://localhost:8080/api/events/10000001
```

### 1.6 Batch Set Event Execution Items

**Interface**: `PUT /api/events/{eventId}/execution-items`

**Description**: Batch set association relationships between events and rules/rule groups. The order of rules/rule groups in the list is the execution order (starting from 1).

**Behavior Description**:
- If the event already has association relationships with some rules/rule groups:
  - Original relationships not included in the input parameters will be deleted
  - New relationships that didn't exist will be created
  - Existing relationships that are also in the input parameters will be preserved (execution order will be updated)
- If an empty list is passed, all association relationships for the event will be deleted

**Request Body**:
```json
{
  "executionItems": [
    {
      "itemType": 0,
      "itemId": 10000001
    },
    {
      "itemType": 1,
      "itemId": 10000001
    },
    {
      "itemType": 0,
      "itemId": 10000002
    }
  ]
}
```

**Description**: 
- `itemType`: Execution item type ID, 0=RULE (rule), 1=RULE_GROUP (rule group)
- The first element in the list has execution order 1, the second has 2, and so on
- `executionOrder` field in the request will be ignored, determined only by list order
- If a rule or rule group does not exist, will return 400 error

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

**Call Example**:
```bash
curl -X PUT http://localhost:8080/api/events/10000001/execution-items \
  -H "Content-Type: application/json" \
  -d '{
    "executionItems": [
      {
        "itemType": 0,
        "itemId": 10000001
      },
      {
        "itemType": 1,
        "itemId": 10000001
      },
      {
        "itemType": 0,
        "itemId": 10000002
      }
    ]
  }'
```

### 1.7 Query Event Execution Items

**Interface**: `GET /api/events/{eventId}/execution-items`

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "itemType": 0,
      "itemId": 10000001,
      "executionOrder": 1
    },
    {
      "itemType": 1,
      "itemId": 10000001,
      "executionOrder": 2
    }
  ]
}
```

**Note**: `itemType` field value in response: 0=RULE (rule), 1=RULE_GROUP (rule group)

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/events/10000001/execution-items
```

**Note**: `executionOrder` field in response represents the actual execution order.

## 2. Rule Management Interfaces

### 2.1 Create Rule

**Interface**: `POST /api/rules`

**Request Body**:
```json
{
  "name": "Amount Check Rule",
  "contentType": 0,
  "description": "Check if amount is greater than 1000",
  "content": "amount > 1000"
}
```

**Description**: 
- `contentType`: Rule type ID, 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT (required)
- `ruleStatus`: **Do not provide**, newly created rules are automatically set to OFFLINE status (status ID = 0). If this field is included in the request body, it will be ignored.
- `ruleGroupId`: Do not provide, new rules do not belong to any rule group

**Response Example**:
```json
{
  "code": 0,
  "message": "Rule created successfully",
  "data": {
    "id": 10000001,
    "name": "Amount Check Rule",
    "contentType": 0,
    "ruleStatus": 0,
    "ruleGroupId": null,
    "description": "Check if amount is greater than 1000",
    "content": "amount > 1000"
  }
}
```

**Call Example**:
```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Amount Check Rule",
    "contentType": 0,
    "description": "Check if amount is greater than 1000",
    "content": "amount > 1000"
  }'
```

**Note**: 
- Newly created rules are automatically set to OFFLINE status (status ID = 0)
- If `ruleStatus` field is included in the request body, it will be ignored
- After creating a rule, you need to use the update interface (PUT /api/rules/{ruleId}) to modify the rule status

### 2.2 Query Rule

**Interface**: `GET /api/rules/{ruleId}`

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10000001,
    "name": "Amount Check Rule",
    "contentType": 0,
    "ruleStatus": 2,
    "ruleGroupId": null,
    "description": "Check if amount is greater than 1000",
    "content": "amount > 1000"
  }
}
```

**Description**: 
- `contentType`: Rule type ID, 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT
- `ruleStatus`: Rule status ID, 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/rules/10000001
```

### 2.3 Query All Rules

**Interface**: `GET /api/rules`

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/rules
```

### 2.4 Update Rule

**Interface**: `PUT /api/rules/{ruleId}`

**Request Body** (all fields are optional, only provided fields will be updated):
```json
{
  "name": "Amount Check Rule (Updated)",
  "ruleStatus": 4,
  "description": "Updated description",
  "content": "amount > 2000"
}
```

**Description**: 
- `name`: Rule name (optional), will be updated if provided
- `contentType`: Rule type ID (optional), will be updated if valid value is provided. Type IDs: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT
- `ruleStatus`: Rule status ID (optional), will be updated if provided. Status IDs: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL
- `description`: Rule description (optional), will be updated if provided
- `content`: Rule content (optional), will be updated if provided
- Only fields provided in the request body will be updated, unprovided fields remain unchanged

**Call Example**:
```bash
# Update name and status only
curl -X PUT http://localhost:8080/api/rules/10000001 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Amount Check Rule (Updated)",
    "ruleStatus": 4
  }'

# Update all optional fields (including type)
curl -X PUT http://localhost:8080/api/rules/10000001 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Amount Check Rule (Updated)",
    "contentType": 0,
    "ruleStatus": 4,
    "description": "Updated description",
    "content": "amount > 2000"
  }'
```

### 2.5 Delete Rule

**Interface**: `DELETE /api/rules/{ruleId}`

**Note**: Only rules with OFFLINE status can be deleted

**Call Example**:
```bash
curl -X DELETE http://localhost:8080/api/rules/10000001
```

## 3. Rule Group Management Interfaces

### 3.1 Query Rule Group

**Interface**: `GET /api/rule-groups/{groupId}`

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10000001,
    "rules": {
      "10000001": 50,
      "10000002": 50
    }
  }
}
```

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/rule-groups/10000001
```

### 3.2 Query All Rule Groups

**Interface**: `GET /api/rule-groups`

**Call Example**:
```bash
curl -X GET http://localhost:8080/api/rule-groups
```

### 3.3 Create Rule Group

**Interface**: `POST /api/rule-groups`

**Request Body**:
```json
{
  "rules": {
    "10000001": 50,
    "10000002": 50
  }
}
```

**Description**:
- `rules`: Map type, key is rule ID, value is rule traffic ratio (0-100)
- **Validation Rules**:
  1. Traffic ratio must be an integer between 0 and 100
  2. Sum of all traffic ratios cannot exceed 100
  3. Rules that already belong to other rule groups cannot be added to this rule group
  4. System will validate if rule status can be legally transitioned to AB_TEST:
     - Allowed status transitions: GRAY -> AB_TEST, FULL -> AB_TEST, AB_TEST -> AB_TEST (if already AB_TEST, ignore)
     - If rule status is invalid, will throw IllegalArgumentException
- If validation passes, rule status will be changed to AB_TEST and rule group will be created

**Call Example**:
```bash
curl -X POST http://localhost:8080/api/rule-groups \
  -H "Content-Type: application/json" \
  -d '{
    "rules": {
      "10000001": 50,
      "10000002": 50
    }
  }'
```

### 3.4 Update Rule Group

**Interface**: `PUT /api/rule-groups/{groupId}`

**Request Body**:
```json
{
  "rules": {
    "10000001": 70,
    "10000002": 30
  }
}
```

**Description**:
- `rules`: Map type, key is rule ID, value is rule traffic ratio (0-100)
- **Validation Rules**:
  1. Traffic ratio must be an integer between 0 and 100
  2. Sum of all traffic ratios cannot exceed 100
  3. Rules that already belong to other rule groups cannot be added to this rule group (except rules currently in this rule group)
  4. System will validate if rule status can be legally transitioned to AB_TEST (or is already AB_TEST):
     - Allowed status transitions: GRAY -> AB_TEST, FULL -> AB_TEST, AB_TEST -> AB_TEST (if already AB_TEST, ignore)
     - If rule status is invalid, will throw IllegalArgumentException
- If rule is already in AB_TEST status, ignore status transition for that rule
- If rule is valid, rule status will be changed to AB_TEST
- Rules originally in the rule group that are not in the input parameters will have their status changed to offline (OFFLINE) and be removed from the rule group

**Call Example**:
```bash
curl -X PUT http://localhost:8080/api/rule-groups/10000001 \
  -H "Content-Type: application/json" \
  -d '{
    "rules": {
      "10000001": 70,
      "10000002": 30
    }
  }'
```

### 3.5 Delete Rule Group

**Interface**: `DELETE /api/rule-groups/{groupId}`

**Description**:
- When deleting a rule group, the following operations will be performed:
  1. Set all rules in the rule group to offline status (OFFLINE)
  2. Remove these rules from the rule group
  3. Delete the rule group

**Call Example**:
```bash
curl -X DELETE http://localhost:8080/api/rule-groups/10000001
```

## 4. Rule Evaluation Interface

### 4.1 Execute Rule Evaluation

**Interface**: `POST /api/evalRequest`

**Description**: Execute rule evaluation based on event ID and user ID, return calculation result. This interface is a synchronous blocking HTTP interface.

**Request Body**:
```json
{
  "userId": 123456,
  "eventId": 10000001,
  "traceId": 999999,
  "arguments": {
    "amount": {
      "value": 1000.0,
      "type": "DECIMAL"
    },
    "age": {
      "value": 25,
      "type": "INTEGER"
    },
    "isVip": {
      "value": true,
      "type": "BOOLEAN"
    }
  }
}
```

**Request Parameter Description**:
- `userId` (Long, required): User ID
- `eventId` (Integer, required): Event ID
- `traceId` (Long, required): Business request trace ID, used for log tracing
- `arguments` (Map<String, TypedValue>, required): Parameter dictionary required for rule calculation
  - `TypedValue` contains two fields: `value` and `type`
  - Supported values for `type`: `STRING`, `INTEGER`, `LONG`, `DECIMAL`, `BOOLEAN`, `DATE`, `OBJECT`

**Response Example** (Success):
```json
{
  "result": {
    "value": true,
    "type": "BOOLEAN"
  },
  "success": true,
  "errmsg": null,
  "trace": {
    "steps": [
      {
        "itemType": 0,
        "itemId": 10000001,
        "result": {
          "value": true,
          "type": "BOOLEAN"
        }
      }
    ]
  }
}
```

**Response Example** (Failure):
```json
{
  "result": null,
  "success": false,
  "errmsg": "Rule execution failed: Event not found"
}
```

**Response Parameter Description**:
- `result` (TypedValue, optional): Calculation result, format same as values in `arguments` in request
- `success` (Boolean, required): Whether successful
- `errmsg` (String, optional): Error message, only exists when `success` is `false`
- `trace` (ExecutionTrace, optional): Execution trace information, contains list of executed steps

**Call Example**:
```bash
curl -X POST http://localhost:8080/api/eval \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456,
    "eventId": 10000001,
    "traceId": 999999,
    "arguments": {
      "amount": {
        "value": 1000.0,
        "type": "DECIMAL"
      },
      "age": {
        "value": 25,
        "type": "INTEGER"
      }
    }
  }'
```

**Execution Flow Description**:
1. Query execution items (rules and rule groups) associated with the event based on `eventId`
2. Execute in order according to execution sequence:
   - If it's a rule, directly execute rule calculation
   - If it's a rule group, select rule based on user hash and rule probability, then execute
3. Return calculation result of the last executed item
4. Asynchronously send calculation result to message queue (Kafka) for subsequent processing

**Notes**:
- This interface is synchronous blocking and will wait for rule calculation to complete before returning result
- If an exception occurs during rule calculation, error information will be returned, but HTTP exception will not be thrown (always returns 200 status code)
- Message queue send failure will not affect main flow, only logs will be recorded

## Error Response

When request fails, response format is as follows:

```json
{
  "code": 400,
  "msg": "Error message",
  "data": null
}
```

Common error codes:
- `400`: Request parameter error
- `404`: Resource not found
- `500`: Internal server error

## Status Code Description

### RuleStatusEnum (Rule Status ID)
- `0`: OFFLINE (Offline)
- `1`: TEST (Test)
- `2`: GRAY (Gray)
- `3`: AB_TEST (A/B Test)
- `4`: FULL (Full)

### ContentTypeEnum (Rule Type ID)
- `0`: EXPRESSION (Logical Expression)
- `1`: API_QUERY (API Query)
- `2`: SQL_QUERY (SQL Query)
- `3`: SCRIPT (Script Execution)

### ExecutionItemTypeEnum (Execution Item Type ID)
- `0`: RULE (Single Rule)
- `1`: RULE_GROUP (Rule Group)

### EnvironmentEnum (Environment Type ID)
- `0`: UNDEFINED (Undefined)
- `1`: TEST (Test Environment)
- `2`: PRODUCTION (Production Environment)
- `3`: GRAY (Gray Environment)

**Note**: According to project conventions, if parameter values passed through interfaces are enumerations, enumeration IDs (integers) should be passed instead of enumeration values (strings). You can query the mapping between enumeration IDs and enumeration values through `GET /api/dicts?keys=RuleStatusEnum,ContentTypeEnum,ExecutionItemTypeEnum,EnvironmentEnum` interface.

### ValueType (Value Type)
- `STRING`: String type
- `INTEGER`: Integer type
- `LONG`: Long integer type
- `DECIMAL`: Decimal type
- `BOOLEAN`: Boolean type
- `DATE`: Date type
- `OBJECT`: Object type

## 5. Enumeration Value Query Interface

### 5.1 Query Enumeration Values

**Interface**: `GET /api/dicts?keys={enumClassNames}`

**Description**: Query enumeration values in the project. Input enumeration class names (multiple separated by commas), returns enumeration class names as keys and enumeration value lists as values. Each object in the list contains `name` (enumeration value) and `value` (enumeration ID).

**Request Parameters**:
- `keys` (String, optional): Enumeration class names, multiple separated by commas. Supported enumeration classes: `RuleStatusEnum`, `ContentTypeEnum`, `ExecutionItemTypeEnum`, `EnvironmentEnum`

**Response Example**:
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "RuleStatusEnum": [
      {
        "name": "OFFLINE",
        "value": 0
      },
      {
        "name": "TEST",
        "value": 1
      },
      {
        "name": "GRAY",
        "value": 2
      },
      {
        "name": "AB_TEST",
        "value": 3
      },
      {
        "name": "FULL",
        "value": 4
      }
    ],
    "ContentTypeEnum": [
      {
        "name": "EXPRESSION",
        "value": 0
      },
      {
        "name": "API_QUERY",
        "value": 1
      },
      {
        "name": "SQL_QUERY",
        "value": 2
      },
      {
        "name": "SCRIPT",
        "value": 3
      }
    ]
  }
}
```

**Call Example**:
```bash
# Query single enumeration
curl -X GET "http://localhost:8080/api/dicts?keys=RuleStatusEnum"

# Query multiple enumerations
curl -X GET "http://localhost:8080/api/dicts?keys=RuleStatusEnum,ContentTypeEnum,ExecutionItemTypeEnum,EnvironmentEnum"

# Query all enumerations (no parameters, returns empty)
curl -X GET "http://localhost:8080/api/dicts"
```

**Notes**:
- If enumeration class name does not exist, it will be ignored (will not appear in return result)
- Enumeration ID mapping:
  - RuleStatusEnum: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL
  - ContentTypeEnum: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT
  - ExecutionItemTypeEnum: 0=RULE, 1=RULE_GROUP
  - EnvironmentEnum: 0=UNDEFINED, 1=TEST, 2=PRODUCTION, 3=GRAY
- Database stores enumeration IDs (integers), not enumeration values (strings)
