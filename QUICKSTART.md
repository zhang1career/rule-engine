# Quick Start Guide

## Prerequisites

- JDK 1.8+
- Maven 3.6+

## Quick Start (No External Dependencies Required)

The project is configured to run without RabbitMQ and database (these are optional).

### 1. Build Project

```bash
cd /Users/mini/Projects/startups/rule-engine
mvn clean compile
```

### 2. Run Project

```bash
mvn spring-boot:run
```

Or package and run:

```bash
mvn clean package
java -jar target/rule-engine-0.7.0-SNAPSHOT.jar
```

### 3. Test Interface

After the project starts, some sample rules will be automatically initialized. You can test using the following command:

```bash
curl -X POST http://localhost:8080/rule/eval \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456789,
    "eventId": 1001,
    "traceId": 987654321,
    "arguments": {
      "amount": {"value": 2000.00, "type": "DECIMAL"},
      "age": {"value": 25, "type": "INTEGER"}
    }
  }'
```

**Expected Response**:
```json
{
  "result": {
    "value": 0.05,
    "type": "DECIMAL"
  },
  "success": true
}
```

## Configuration

### Environment Configuration

Edit `rule.engine.environment` in `src/main/resources/application.yml`:

- `TEST`: Test environment (default)
- `PRODUCTION`: Production environment
- `GRAY`: Gray environment

### RabbitMQ Configuration (Optional)

If you need to use RabbitMQ message queue functionality, uncomment the RabbitMQ configuration in `application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### Database Configuration (Optional)

If you need to use SQL query functionality, uncomment the database configuration in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rule_engine
    username: root
    password: root
```

## Rule Status Transitions

Rule status transitions must follow these constraints:

1. **Offline Status (OFFLINE)**: Can only transition to Test status (TEST)
2. **Test Status (TEST)**: Can transition to Gray status (GRAY) or Offline status (OFFLINE)
3. **Gray Status (GRAY)**: Can transition to Offline status (OFFLINE) or A/B Test status (AB_TEST)
4. **A/B Test Status (AB_TEST)**: Can transition to Offline status (OFFLINE) or Full status (FULL)
5. **Full Status (FULL)**: Can transition to Offline status (OFFLINE) or A/B Test status (AB_TEST)

**Rule Deletion Constraints**:
- Only rules in Offline status (OFFLINE) can be deleted

## Sample Rules

The following sample rules will be automatically created when the project starts:

1. **rule_001**: Amount check rule (expression)
   - Checks if amount is greater than 1000 and age is greater than or equal to 18
   - Status: Full

2. **rule_002**: Discount calculation rule (Groovy script)
   - Calculates discount based on amount (>5000: 10%, >2000: 5%)
   - Status: Full

3. **rule_003**: A/B test rule (expression)
   - Checks if amount is greater than 500
   - Status: A/B Test (50% ratio)

Execution sequences:
- eventId=1001: Execute rule_001 -> rule_002
- eventId=1002: Execute rule_001 -> rule_003

## Common Issues

### 1. Port Already in Use

Modify the `server.port` configuration in `application.yml`.

### 2. RabbitMQ Connection Failed

If RabbitMQ is not configured, the message sending functionality will automatically skip without affecting the main flow.

### 3. Database Connection Failed

If the database is not configured, SQL query functionality is unavailable, but other features work normally.

## Next Steps

- View [README.md](README.md) for detailed features
- View [PRD.md](docs/prd/PRD.md) for product requirements
- View [API_DOCUMENTATION.md](docs/api/API_DOCUMENTATION.md) for RESTful API interfaces
- View [DATABASE_SCHEMA.md](docs/schema/DATABASE_SCHEMA.md) for database schema
- Extend rule types and executors as needed
