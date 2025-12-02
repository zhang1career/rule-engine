# Test Documentation

This project uses the Spock framework for writing unit tests. Spock is a Groovy-based testing framework that provides more concise and expressive test syntax.

## Test Framework

- **Spock Framework**: 2.3-groovy-3.0
- **Groovy**: 3.0.17

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=TypedValueSpec
```

### Run Specific Test Method

```bash
mvn test -Dtest=TypedValueSpec#testTypedValueBasicCreationAndGetValue
```

## Test Coverage

### 1. Core Class Tests

- **TypedValueSpec**: Tests TypedValue class type conversion and value retrieval
- **RuleExecutionContextSpec**: Tests rule execution context functionality

### 2. Service Layer Tests

- **RuleServiceImplSpec**: Tests rule service CRUD operations
  - Save and retrieve rules
  - Save execution sequences
  - Rule status filtering
  - Exception handling

- **RuleGroupServiceImplSpec**: Tests rule group service (A/B testing functionality)
  - A/B testing decision logic
  - Record and query A/B test records
  - Parameter validation

- **EvalServiceImplSpec**: Tests rule evaluation service
  - Normal execution flow
  - Message queue exception handling
  - Execution context construction

### 3. Rule Executor Tests

- **ExpressionRuleExecutorSpec**: Tests expression rule executor
  - Boolean expression execution
  - Arithmetic expression execution
  - System variables and context variable usage
  - Exception handling

- **ScriptRuleExecutorSpec**: Tests Groovy script executor
  - Simple script execution
  - Complex script execution
  - Variable access
  - Exception handling

### 4. Rule Engine Tests

- **RuleExecutionEngineSpec**: Tests rule execution engine
  - Rule sequence execution
  - Rule status control
  - Environment judgment
  - A/B test rule execution
  - Early exit logic
  - Exception handling

### 5. Controller Tests

- **RuleControllerSpec**: Tests HTTP interfaces
  - Normal request processing
  - Exception handling
  - Parameter passing validation

## Spock Test Syntax

### Given-When-Then Structure

```groovy
def "test example"() {
    given: "prepare test data"
    def value = 100
    
    when: "execute operation"
    def result = value * 2
    
    then: "verify result"
    result == 200
}
```

### Mock Objects

```groovy
def service = Mock(Service)

when:
service.doSomething()

then:
1 * service.doSomething()  // Verify method is called once
```

### Parameterized Tests

```groovy
@Unroll
def "test type conversion - value: #value, type: #type"() {
    expect:
    convert(value) == expected
    
    where:
    value | type | expected
    100   | INT  | 100
    99.9  | DEC  | 99.9
}
```

### Exception Tests

```groovy
when:
service.doSomething()

then:
thrown(RuntimeException)  // Verify exception is thrown
```

## Test Coverage

Run test coverage report:

```bash
mvn clean test jacoco:report
```

Coverage report will be generated at `target/site/jacoco/index.html`

## Notes

1. **Groovy Version**: Ensure Groovy version is compatible with Spock (currently using 3.0.17)
2. **Test Isolation**: Each test method is independent and does not affect each other
3. **Mock Objects**: Using Spock's Mock functionality can easily simulate dependencies
4. **Parameterized Tests**: Using `@Unroll` annotation can generate multiple test cases

## Extending Tests

### Adding New Test Classes

1. Create corresponding test class in `src/test/groovy` directory
2. Class name ends with `Spec` (Spock convention)
3. Extend `Specification` class
4. Write test cases using Spock test syntax

### Example

```groovy
package lab.zhang.rule.rule_engine.service.impl

import spock.lang.Specification

class MyServiceSpec extends Specification {

    def service = new MyService()

    def "test method"() {
        given:
        // Prepare data

        when:
        // Execute operation

        then:
        // Verify result
    }
}
```

## Continuous Integration

Tests can be automatically run in CI/CD processes:

```yaml
# Example: GitHub Actions
- name: Run Tests
  run: mvn test
```
