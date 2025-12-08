# Rule Engine System Product Requirements Document

## 1 Project Overview

### 1.1 Project Background
The rule engine system is a rule calculation service based on SpringBoot, used to execute corresponding rule sequences based on event types and user parameters, and return calculation results.

### 1.2 Technology Stack
- **Framework**: SpringBoot
- **JDK Version**: JDK 8
- **Build Tool**: Maven 3.6
- **Script Engine**: Groovy

## 2 Functional Requirements

### 2.1 Core Interface

#### 2.1.1 Interface Definition
- **Interface Path**: `/rule/eval`
- **Interface Name**: `eval`
- **Call Methods**: 
  - HTTP (initial implementation)
  - RPC (future extension)
- **Call Modes**:
  - Synchronous blocking (initial implementation)
  - Synchronous non-blocking (future extension)
  - Asynchronous non-blocking (future extension)

#### 2.1.2 Request Parameters

| Parameter Name | Type | Required | Description |
|---------------|------|----------|-------------|
| userId | Long | Yes | User ID, long integer |
| eventId | Integer | Yes | Event type ID, used to distinguish different event types, integer |
| traceId | Long | Yes | Business request trace ID, long integer |
| arguments | Map<String, TypedValue> | Yes | Parameter dictionary for rule calculation, parameter name as key, value is TypedValue type |

**TypedValue Class Specification**:
- Provides `getValue()` method to get parameter value
- Supports encapsulation of multiple data types

#### 2.1.3 Response Result

- **Return Type**: `TypedValue`
- **HTTP Response**: JSON serialization of TypedValue instance
- **TypedValue Class Specification**:
  - Provides `getValue()` method to get calculation result

### 2.2 Rule Execution Engine

#### 2.2.1 Execution Flow
1. Get corresponding execution item list (ExecutionItem list, containing rules and rule groups) based on `eventId`
2. Execute each execution item in order according to execution sequence:
   - If it's a single rule, execute the rule directly
   - If it's a rule group, select one rule from the group to execute based on probability
3. Rule execution supports:
   - Early exit from sequential execution based on internal rule logic
   - Loop execution
   - Mutual calls between rules

#### 2.2.2 Rule Types
Rules support the following operation types:
- **Logical Expression**: Parse and execute logical expressions
- **API Query**: Call external HTTP/RPC interfaces to retrieve data
- **SQL Query**: Execute database query operations
- **Script Execution**: Execute Groovy scripts

#### 2.2.3 Rule Management
- **Rule Identifier**: Each rule has a unique `ruleId`
- **Execution Sequence**: Each `eventId` corresponds to an execution sequence
- **Sequence Composition**: Execution sequence contains multiple rules, execution order is set and managed by the sequence
- **Rule Reuse**: One rule can be used in multiple execution sequences

### 2.3 Rule Status Control

#### 2.3.1 Status Types
Rule statuses are divided into the following five types:

| Status | Description | Execution Condition |
|--------|-------------|---------------------|
| Offline | Rule unavailable | Not allowed to be added to any execution sequence corresponding to eventId |
| Test | Test environment rule | Only allows eventId triggered by test environment requests to execute |
| A/B Test | A/B test rule | Allows eventId triggered by production and gray environment requests to execute, belongs to rule group, executed based on probability selection |
| Full | Production environment rule | Allows eventId triggered by production and gray environment requests to execute |
| Gray | Gray environment rule | Only allows eventId triggered by gray environment requests to execute |

#### 2.3.2 Status Transition Constraints
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
- Attempting to delete rules in non-offline status will throw `IllegalArgumentException` exception

#### 2.3.4 Event Association Constraints for Status Transitions
For rules transitioning to Test (TEST), Gray (GRAY), or Online (ONLINE) status, the following constraints must be satisfied:
- The rule must be associated with at least one event (eventId)
- The rule must not have formed a rule group (i.e., there must be records in the execution arrangement table where `group_id = 0` and `rule_id = current rule ID`)

If these constraints are not met, the status transition will throw an `IllegalStateException` exception.

#### 2.3.3 A/B Test Rule Group Mechanism
- **Rule Group Concept**: Rules with A/B test status belong to a rule group, one or more rules in the group are assigned call probability according to `abTestRatio`
- **Execution Logic**: When executing each rule group, at most one rule in the group is executed. If the sum of execution probabilities of all rules in the rule group is less than 100, it means that other calls beyond the sum of call probabilities will directly skip this rule group
- **Execution Order**: Rule groups and other rules are at the same level, rule groups participate in execution order arrangement
- **EventId Relationship Management**:
  - One eventId is associated with a list composed of rules and rule groups (ExecutionItem list)
  - **Business Logic 1**: When a rule transitions from other status to A/B test status, the system automatically creates a rule group and copies the association relationship between the rule and all eventIds (including correspondence and execution order) to the association relationship between the rule group and eventIds
  - **Business Logic 2**: When a rule transitions from A/B test status to other status, the system automatically removes the rule from its rule group
  - **Business Logic 3**: When a rule group contains no rules, the system automatically deletes the rule group and its association relationships with all eventIds
- **User Hash Mechanism**: 
  - Before executing any rule, the system performs murmur-hash calculation on `userId`, stores the hash result (string) in `arguments` of `RuleExecutionContext`, with key as `userHash`
  - This hash value is used for subsequent rule group selection to ensure that the same user can get consistent rule selection results in different requests
- **Rule Selection Algorithm**:
  - First check cache: If the combination of `userId`, `eventId` and `groupId` has already selected a rule, directly return the cached rule ID
  - If cache miss, perform probability selection:
    1. Get `userHash` value from `arguments`
    2. Convert `userHash` (string) to an integer value between 1 and 100
    3. Use this integer value as seed to generate pseudo-random number (range 1-100)
    4. Compare pseudo-random number with cumulative `abTestRatio` of rules in the rule group to select corresponding rule
    5. Cache selection result, key is `rule:gw:abt:{userId}:{eventId}:{groupId}`, value is selected `ruleId`
- **Cache Mechanism**:
  - Cache is used to ensure that the same user selects the same rule for each rule group under the same event type
  - Since one event may be associated with multiple rule groups, cache needs to record selection results for each rule group
  - Cache key format: `rule:gw:abt:{userId}:{eventId}:{groupId}` (stored in Redis)
  - Cache value: Selected `ruleId` (Long type)
  - Cache expiration time: 24 hours
  - If cached rule becomes invalid (rule status changes, rule removed from group, etc.), cache automatically becomes invalid and reselects
- **Rule Group Management**:
  - When a rule transitions from other status to A/B test status, need to create a rule group
  - When a rule transitions from A/B test status to offline, test or full status, need to remove the rule from its rule group
  - When a rule transitions from A/B test status to full status, need to change all other rules in the rule group to offline status
  - If a rule group contains no rules, need to delete the rule group
  - Rule Group Rule Copy: Within a rule group, can copy existing rules to create new rules with the same event associations and group memberships
- **Constraints**:
  - Only rules in A/B test status can belong to rule groups
  - One rule can only belong to one rule group
- **Environment and Rule Status Matching Rules**:
  - Rules executable in test environment: Test status
  - Rules executable in production environment: A/B test status, Full status
  - Rules executable in gray environment: Gray status, A/B test status, Full status

### 2.4 Message Queue Integration

#### 2.4.1 Message Sending
- **Trigger Timing**: After rule calculation completes
- **Message Content**: Contains input parameters and calculation results
- **Message Queue**: RabbitMQ (external system, called through interface)
- **Sending Method**: Asynchronous sending, does not affect main flow

## 3 Non-functional Requirements

### 3.1 Extensibility Design
- **Interface Extension**: Reserved RPC interface extension points
- **Call Mode Extension**: Reserved synchronous non-blocking, asynchronous non-blocking extension points
- **Rule Type Extension**: Supports adding new rule types

### 3.2 Performance Requirements
- Support high concurrency request processing
- Rule execution efficiency optimization

### 3.3 Maintainability
- Clear code structure, modular design
- Support rule configuration management
- Comprehensive logging

## 4 Technical Implementation Points

### 4.1 Architecture Design
- **Layered Architecture**: Controller layer, Service layer, Rule engine layer, Data access layer
- **Interface Abstraction**: Define unified rule interface, support multiple rule type implementations
- **Execution Engine**: Implement rule sequence execution engine, support flow control

### 4.2 Data Model
- **Rule Model**: Rule (ruleId, ruleType, ruleContent, status, abTestRatio, ruleGroupId, etc.)
- **Execution Sequence Model**: ExecutionSequence (eventId, ruleIds, etc.)
- **Rule Group Model**: RuleGroup (groupId, ruleIds, etc.)
- **Execution Item Model**: ExecutionItem (can be a single rule or rule group)
- **EventId Relationship**:
  - RuleService maintains mapping between eventId and rule ID list (ExecutionSequence)
  - RuleGroupService maintains association relationship between eventId and rule groups
  - When rule status changes or rule group changes, automatically synchronize and update relationships

### 4.3 Configuration Management
- Rule configuration storage (recommended to use database or configuration center)
- Execution sequence configuration management
- A/B test rule group configuration and probability allocation

## 5 Development Plan

### 5.1 Phase 1 (MVP)
- [x] Project basic architecture setup
- [ ] HTTP synchronous blocking interface implementation
- [ ] Basic rule execution engine
- [ ] Rule status control (offline, test, full)
- [ ] RabbitMQ message sending

### 5.2 Phase 2
- [x] A/B test rule group functionality implementation
- [ ] Rule type extension (logical expression, API query, SQL query, Groovy script)
- [ ] Rule execution flow control (loops, rule calls)

### 5.3 Phase 3
- [ ] RPC interface implementation
- [ ] Synchronous non-blocking call method
- [ ] Asynchronous non-blocking call method

## 6 Interface Examples

### 6.1 HTTP Request Example
```json
POST /rule/eval
{
  "userId": 123456789,
  "eventId": 1001,
  "traceId": 987654321,
  "arguments": {
    "amount": {
      "value": 1000.00,
      "contentType": "DECIMAL"
    },
    "age": {
      "value": 25,
      "contentType": "INTEGER"
    }
  }
}
```

### 6.2 HTTP Response Example
```json
{
  "value": true,
  "contentType": "BOOLEAN"
}
```
