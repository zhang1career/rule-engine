package lab.zhang.rule.rule_engine.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API
 * Handles validation exceptions and other common exceptions
 *
 * @author Rongjin Zhang
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation exceptions for @RequestBody @Valid
     *
     * @param ex MethodArgumentNotValidException
     * @return error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);

        return ResponseEntity.ok(ApiResponseDTO.error(400, "Validation failed: " + message));
    }

    /**
     * Handle constraint violation exceptions for method parameters
     *
     * @param ex ConstraintViolationException
     * @return error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        log.warn("Constraint violation: {}", message);

        return ResponseEntity.ok(ApiResponseDTO.error(400, "Validation failed: " + message));
    }

    /**
     * Handle HttpMessageNotReadableException
     * This exception occurs when JSON deserialization fails (e.g., type mismatch)
     *
     * @param ex HttpMessageNotReadableException
     * @return error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request body format";

        // Try to extract more specific error information from the exception
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatException) cause;
            String fieldName = invalidFormatException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .reduce((first, second) -> second)
                    .orElse("unknown");

            String targetType = invalidFormatException.getTargetType() != null
                    ? invalidFormatException.getTargetType().getSimpleName()
                    : "unknown type";

            Object value = invalidFormatException.getValue();
            String valueStr = value != null ? value.toString() : "null";

            message = String.format("Field '%s' expects a %s value, but received: %s",
                    fieldName, targetType, valueStr);
        } else if (cause != null) {
            message = "Invalid request body: " + cause.getMessage();
        }

        log.warn("Invalid request body: {}", message);

        return ResponseEntity.ok(ApiResponseDTO.error(400, message));
    }

    /**
     * Handle MethodArgumentTypeMismatchException
     * This exception occurs when path variable or request parameter type conversion fails
     * (e.g., passing a string "abc" to a Long parameter)
     *
     * @param ex MethodArgumentTypeMismatchException
     * @return error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getName();
        Class<?> requiredType = ex.getRequiredType();
        String requiredTypeName = requiredType != null ? requiredType.getSimpleName() : "unknown type";
        Object value = ex.getValue();
        String valueStr = value != null ? value.toString() : "null";
        
        String message = String.format("Parameter '%s' expects a %s value, but received: %s",
                parameterName, requiredTypeName, valueStr);
        
        log.warn("Type mismatch: {}", message);
        
        return ResponseEntity.ok(ApiResponseDTO.error(400, message));
    }

    /**
     * Handle IllegalArgumentException
     * This is commonly used for business logic validation errors
     * If the message contains "not found", returns 404, otherwise returns 400
     *
     * @param ex IllegalArgumentException
     * @return error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        log.warn("Illegal argument: {}", message);

        // Check if it's a "not found" error
        if (message != null && (message.toLowerCase().contains("not found") || 
                                message.toLowerCase().contains("does not exist"))) {
            return ResponseEntity.ok(ApiResponseDTO.error(404, message));
        }

        return ResponseEntity.ok(ApiResponseDTO.error(400, message));
    }

    /**
     * Handle all other exceptions
     *
     * @param ex Exception
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity.ok(ApiResponseDTO.error(500, "Internal server error: " + ex.getMessage()));
    }
}

