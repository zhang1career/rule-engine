package lab.zhang.rule.rule_engine.controller;

import lab.zhang.rule.rule_engine.entity.ExecutionEventRelationEntity;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lab.zhang.rule.rule_engine.model.Event;
import lab.zhang.rule.rule_engine.pojo.dto.ApiResponseDTO;
import lab.zhang.rule.rule_engine.pojo.dto.EventDTO;
import lab.zhang.rule.rule_engine.pojo.dto.ExecutionItemDTO;
import lab.zhang.rule.rule_engine.pojo.qo.BatchSetExecutionItemsQO;
import lab.zhang.rule.rule_engine.pojo.qo.EventQO;
import lab.zhang.rule.rule_engine.pojo.qo.ExecutionItemQO;
import lab.zhang.rule.rule_engine.service.EventService;
import lab.zhang.rule.rule_engine.struct_mapper.EventStructMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Event REST API controller
 *
 * @author Rongjin Zhang
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@Validated
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventStructMapper eventStructMapper;


    /**
     * Get all events
     * GET /api/events
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<EventDTO>>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        List<EventDTO> dtos = events.stream()
                .map(eventStructMapper::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDTO.success(dtos));
    }

    /**
     * Get event by ID
     * GET /api/events/{eventId}
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponseDTO<EventDTO>> getEvent(
            @PathVariable @NotNull @Min(value = 1, message = "Event ID must be a positive integer") Long eventId) {
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found for ID: " + eventId);
        }

        EventDTO dto = eventStructMapper.entityToDTO(event);
        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    /**
     * Create event
     * POST /api/events
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<EventDTO>> createEvent(
            @RequestBody @Validated(EventQO.Create.class) EventQO eventQO) {
        Event event = eventService.createEvent(eventQO.getId(), eventQO.getName(), eventQO.getDescription());
        EventDTO dto = eventStructMapper.entityToDTO(event);
        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    /**
     * Update event
     * PUT /api/events/{eventId}
     * <p>
     * Note:
     * - name is optional (only provided field will be updated)
     * - description is optional (only provided field will be updated)
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponseDTO<EventDTO>> updateEvent(
            @PathVariable @NotNull Long eventId,
            @RequestBody @Validated(EventQO.Update.class) EventQO eventQO) {
        Event event = eventService.updateEvent(eventId, eventQO.getName(), eventQO.getDescription());
        EventDTO dto = eventStructMapper.entityToDTO(event);
        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    /**
     * Delete event
     * DELETE /api/events/{eventId}
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteEvent(
            @PathVariable @NotNull Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Batch set execution items (rules and rule groups) for an event
     * The order of items in the list represents the execution order
     * PUT /api/events/{eventId}/execution-items
     */
    @PutMapping("/{eventId}/execution-items")
    public ResponseEntity<ApiResponseDTO<Void>> batchSetExecutionItems(
            @PathVariable @NotNull Long eventId,
            @RequestBody @Valid BatchSetExecutionItemsQO request) {
        // Build ExecutionItemDTO list from request (order represents execution order)
        List<ExecutionItemQO> executionItems = request.getExecutionItems();
        if (executionItems == null || executionItems.isEmpty()) {
            // Empty list means remove all execution items
            executionItems = Collections.emptyList();
        }

        eventService.batchSetExecutionItems(eventId, executionItems);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    /**
     * Get execution items for event
     * GET /api/events/{eventId}/execution-items
     */
    @GetMapping("/{eventId}/execution-items")
    public ResponseEntity<ApiResponseDTO<List<ExecutionItemDTO>>> getExecutionItems(
            @PathVariable @NotNull Long eventId) {
        List<ExecutionEventRelationEntity> relations =
                eventService.getExecutionEventRelations(eventId);
        List<ExecutionItemDTO> dtos = relations.stream()
                .map(relation -> {
                    ExecutionItemTypeEnum executionItemTypeEnum = relation.getItemTypeEnum();
                    return ExecutionItemDTO.builder()
                            .itemType(executionItemTypeEnum != null ? executionItemTypeEnum.getId() : null)
                            .itemId(relation.getItemId())
                            .executionOrder(relation.getExecutionOrder())
                            .build();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDTO.success(dtos));
    }

}

