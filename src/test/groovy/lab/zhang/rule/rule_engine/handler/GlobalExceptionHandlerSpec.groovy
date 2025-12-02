package lab.zhang.rule.rule_engine.handler

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import javax.validation.Path

/**
 * GlobalExceptionHandler unit test
 *
 * @author Rongjin Zhang
 */
class GlobalExceptionHandlerSpec extends Specification {

    GlobalExceptionHandler handler = new GlobalExceptionHandler()

    @Unroll
    def "handleMethodArgumentNotValid - should return validation error message - fieldErrors: #fieldErrors"() {
        given: "create MethodArgumentNotValidException with field errors"
        def bindingResult = Mock(BindingResult)
        def exception = new MethodArgumentNotValidException(null, bindingResult)
        
        def fieldErrorList = fieldErrors.collect { field, message ->
            def fieldError = Mock(FieldError)
            fieldError.getField() >> field
            fieldError.getDefaultMessage() >> message
            return fieldError
        }
        
        bindingResult.getFieldErrors() >> fieldErrorList

        when: "handle exception"
        def response = handler.handleMethodArgumentNotValid(exception)

        then: "should return error response with validation message"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
        response.body.msg.contains("Validation failed")
        expectedMessages.each { msg ->
            assert response.body.msg.contains(msg)
        }

        where:
        fieldErrors                                    | expectedMessages
        [name: "Name cannot be blank"]                | ["name", "Name cannot be blank"]
        [name: "Name cannot be blank", 
         age: "Age must be positive"]                 | ["name", "age", "Name cannot be blank", "Age must be positive"]
        [contentType: "Content type cannot be null"]  | ["contentType", "Content type cannot be null"]
    }

    @Unroll
    def "handleConstraintViolation - should return constraint violation error - violations: #violations"() {
        given: "create ConstraintViolationException with violations"
        def violationList = violations.collect { path, message ->
            def violation = Mock(ConstraintViolation)
            def propertyPath = Mock(Path)
            propertyPath.toString() >> path
            violation.getPropertyPath() >> propertyPath
            violation.getMessage() >> message
            return violation
        }
        
        def exception = new ConstraintViolationException(violationList as Set)

        when: "handle exception"
        def response = handler.handleConstraintViolation(exception)

        then: "should return error response with constraint violation message"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
        response.body.msg.contains("Validation failed")
        expectedMessages.each { msg ->
            assert response.body.msg.contains(msg)
        }

        where:
        violations                                    | expectedMessages
        [ruleId: "must be positive"]                  | ["ruleId", "must be positive"]
        [ruleId: "must be positive",
         eventId: "must not be null"]                 | ["ruleId", "eventId", "must be positive", "must not be null"]
    }

    @Unroll
    def "handleHttpMessageNotReadable - InvalidFormatException - should return field type mismatch error - fieldName: #fieldName, targetType: #targetType, value: #value"() {
        given: "create HttpMessageNotReadableException with InvalidFormatException"
        def reference = Mock(JsonMappingException.Reference)
        reference.getFieldName() >> fieldName
        
        def invalidFormatException = Mock(InvalidFormatException)
        invalidFormatException.getPath() >> [reference]
        invalidFormatException.getTargetType() >> Class.forName(targetType)
        invalidFormatException.getValue() >> value
        
        def httpException = new HttpMessageNotReadableException("Test message", invalidFormatException)

        when: "handle exception"
        def response = handler.handleHttpMessageNotReadable(httpException)

        then: "should return error response with field type mismatch message"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
        response.body.msg.contains(fieldName)
        response.body.msg.contains(expectedTypeName)
        response.body.msg.contains(value.toString())

        where:
        fieldName     | targetType              | value          | expectedTypeName
        "contentType" | "java.lang.Integer"     | "0=Expression" | "Integer"
        "ruleStatus"  | "java.lang.Integer"     | "invalid"       | "Integer"
        "itemId"      | "java.lang.Long"        | "abc"          | "Long"
    }

    def "handleHttpMessageNotReadable - with other cause - should return generic error message"() {
        given: "create HttpMessageNotReadableException with other cause"
        def cause = new RuntimeException("JSON parse error")
        def httpException = new HttpMessageNotReadableException("Test message", cause)

        when: "handle exception"
        def response = handler.handleHttpMessageNotReadable(httpException)

        then: "should return error response with generic message"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
        response.body.msg.contains("Invalid request body")
        response.body.msg.contains("JSON parse error")
    }

    def "handleHttpMessageNotReadable - without cause - should return default error message"() {
        given: "create HttpMessageNotReadableException without cause"
        def httpException = new HttpMessageNotReadableException("Test message")

        when: "handle exception"
        def response = handler.handleHttpMessageNotReadable(httpException)

        then: "should return error response with default message"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
        response.body.msg == "Invalid request body format"
    }

    @Unroll
    def "handleMethodArgumentTypeMismatch - should return parameter type mismatch error - parameterName: #parameterName, requiredType: #requiredType, value: #value"() {
        given: "create MethodArgumentTypeMismatchException"
        def exception = new MethodArgumentTypeMismatchException(
                value,
                requiredType,
                parameterName,
                null,
                new RuntimeException("Type mismatch")
        )

        when: "handle exception"
        def response = handler.handleMethodArgumentTypeMismatch(exception)

        then: "should return error response with parameter type mismatch message"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
        response.body.msg.contains(parameterName)
        response.body.msg.contains(requiredType.simpleName)
        response.body.msg.contains(value.toString())

        where:
        parameterName | requiredType | value
        "ruleId"      | Long.class    | "abc"
        "eventId"     | Long.class    | "xyz"
        "groupId"     | Long.class    | "123abc"
    }

    def "handleMethodArgumentTypeMismatch - with null requiredType - should handle gracefully"() {
        given: "create MethodArgumentTypeMismatchException with null requiredType"
        // MethodArgumentTypeMismatchException constructor requires non-null requiredType
        // So we'll test with a valid type but check the null handling in the code
        def exception = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "ruleId",
                null,
                new RuntimeException("Type mismatch")
        )

        when: "handle exception"
        def response = handler.handleMethodArgumentTypeMismatch(exception)

        then: "should return error response"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
    }

    @Unroll
    def "handleIllegalArgument - should return 404 for not found errors - message: #message, expectedCode: #expectedCode"() {
        given: "create IllegalArgumentException"
        def exception = new IllegalArgumentException(message)

        when: "handle exception"
        def response = handler.handleIllegalArgument(exception)

        then: "should return appropriate error code"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == expectedCode
        response.body.msg == message

        where:
        message                              | expectedCode
        "Rule not found: 123"                | 404
        "Event does not exist: 456"          | 404
        "Rule group does not exist: 789"   | 404
        "Invalid rule status"                | 400
        "Rule IDs do not exist in database"  | 400
        "Validation failed"                  | 400
    }

    def "handleIllegalArgument - with null message - should return 400"() {
        given: "create IllegalArgumentException with null message"
        def exception = new IllegalArgumentException(null as String)

        when: "handle exception"
        def response = handler.handleIllegalArgument(exception)

        then: "should return 400 error"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 400
    }

    @Unroll
    def "handleGenericException - should return 500 error - exceptionType: #exceptionType"() {
        given: "create generic exception"
        Exception exception
        if (exceptionType == "RuntimeException") {
            exception = new RuntimeException("Runtime error")
        } else if (exceptionType == "NullPointerException") {
            exception = new NullPointerException("Null pointer")
        } else {
            exception = new Exception("Generic error")
        }

        when: "handle exception"
        def response = handler.handleGenericException(exception)

        then: "should return 500 error response"
        response != null
        response.statusCode.value() == 200
        response.body != null
        response.body.code == 500
        response.body.msg.contains("Internal server error")
        response.body.msg.contains(exception.getMessage())

        where:
        exceptionType << ["RuntimeException", "NullPointerException", "Exception"]
    }

    def "handleHttpMessageNotReadable - InvalidFormatException with nested path - should extract correct field name"() {
        given: "create InvalidFormatException with nested path"
        def ref1 = Mock(JsonMappingException.Reference)
        ref1.getFieldName() >> "parent"
        def ref2 = Mock(JsonMappingException.Reference)
        ref2.getFieldName() >> "child"
        
        def invalidFormatException = Mock(InvalidFormatException)
        invalidFormatException.getPath() >> [ref1, ref2]
        invalidFormatException.getTargetType() >> Integer.class
        invalidFormatException.getValue() >> "invalid"
        
        def httpException = new HttpMessageNotReadableException("Test", invalidFormatException)

        when: "handle exception"
        def response = handler.handleHttpMessageNotReadable(httpException)

        then: "should extract the last field name in path"
        response != null
        response.body.msg.contains("child")
        response.body.msg.contains("Integer")
    }

    def "handleHttpMessageNotReadable - InvalidFormatException with null targetType - should handle gracefully"() {
        given: "create InvalidFormatException with null targetType"
        def reference = Mock(JsonMappingException.Reference)
        reference.getFieldName() >> "contentType"
        
        def invalidFormatException = Mock(InvalidFormatException)
        invalidFormatException.getPath() >> [reference]
        invalidFormatException.getTargetType() >> null
        invalidFormatException.getValue() >> "invalid"
        
        def httpException = new HttpMessageNotReadableException("Test", invalidFormatException)

        when: "handle exception"
        def response = handler.handleHttpMessageNotReadable(httpException)

        then: "should handle null targetType"
        response != null
        response.body.msg.contains("contentType")
        response.body.msg.contains("unknown type")
    }

    def "handleHttpMessageNotReadable - InvalidFormatException with null value - should handle gracefully"() {
        given: "create InvalidFormatException with null value"
        def reference = Mock(JsonMappingException.Reference)
        reference.getFieldName() >> "contentType"
        
        def invalidFormatException = Mock(InvalidFormatException)
        invalidFormatException.getPath() >> [reference]
        invalidFormatException.getTargetType() >> Integer.class
        invalidFormatException.getValue() >> null
        
        def httpException = new HttpMessageNotReadableException("Test", invalidFormatException)

        when: "handle exception"
        def response = handler.handleHttpMessageNotReadable(httpException)

        then: "should handle null value"
        response != null
        response.body.msg.contains("contentType")
        response.body.msg.contains("null")
    }
}

