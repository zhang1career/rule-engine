package lab.zhang.rule.rule_engine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Example controller demonstrating @RequestHeader detection
 *
 * @author Rongjin Zhang
 */
@RestController
@RequestMapping("/api/example")
public class ExampleController {

    /**
     * Get user with authorization header
     * GET /api/example/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDTO> getUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = true) String authToken,
            @RequestHeader("X-API-Key") String apiKey) {
        // Method implementation
        return ResponseEntity.ok(new UserDTO());
    }

    /**
     * Create data with custom headers
     * POST /api/example/data
     */
    @PostMapping("/data")
    public ResponseEntity<DataDTO> createData(
            @RequestHeader(value = "Authorization", required = true) String auth,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            @RequestBody DataQO data) {
        // Method implementation
        return ResponseEntity.ok(new DataDTO());
    }
}
