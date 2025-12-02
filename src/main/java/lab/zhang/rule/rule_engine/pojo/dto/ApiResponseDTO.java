package lab.zhang.rule.rule_engine.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper
 * 
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {
    
    /**
     * Response code: 0 for success, non-zero for error
     */
    private Integer code;
    
    /**
     * Response message
     */
    private String msg;
    
    /**
     * Response data
     */
    private T data;
    
    /**
     * Create success response
     */
    public static <T> ApiResponseDTO<T> success(T data) {
        return ApiResponseDTO.<T>builder()
                .code(0)
                .msg("success")
                .data(data)
                .build();
    }
    
    /**
     * Create success response with message
     */
    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return ApiResponseDTO.<T>builder()
                .code(0)
                .msg(message)
                .data(data)
                .build();
    }
    
    /**
     * Create error response
     */
    public static <T> ApiResponseDTO<T> error(Integer code, String message) {
        return ApiResponseDTO.<T>builder()
                .code(code)
                .msg(message)
                .build();
    }
}

