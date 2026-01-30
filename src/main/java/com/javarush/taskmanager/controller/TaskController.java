package com.javarush.taskmanager.controller;

import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.dto.TaskStatisticsResponse;
import com.javarush.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Slf4j
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * GET /api/tasks
     * <p>
     * Retrieves all tasks belonging to the currently authenticated user.
     *
     * @return list of user's tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        log.info("HTTP GET /api/tasks");
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    /**
     * GET /api/tasks/{id}
     * <p>
     * Retrieves a specific task by its ID.
     * Access is allowed only if the task belongs to the current user.
     *
     * @param id task identifier
     * @return task details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long id) {
        log.info("HTTP GET /api/tasks/{}", id);
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * POST /api/tasks
     * <p>
     * Creates a new task for the currently authenticated user.
     *
     * @param request task creation data
     * @return created task
     */
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(
            @RequestBody @Valid TaskRequestDto request) {

        log.info("HTTP POST /api/tasks title={}", request.getTitle());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskService.createTask(request));
    }

    /**
     * PUT /api/tasks/{id}
     * <p>
     * Updates an existing task.
     * Only the owner of the task is allowed to perform this operation.
     *
     * @param id      task identifier
     * @param request updated task data
     * @return updated task
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(
            @PathVariable Long id,
            @RequestBody @Valid TaskRequestDto request) {

        log.info("HTTP PUT /api/tasks/{}", id);
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    /**
     * DELETE /api/tasks/{id}
     * <p>
     * Deletes a task by its ID.
     * Only the owner of the task is allowed to perform this operation.
     *
     * @param id task identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        log.info("HTTP DELETE /api/tasks/{}", id);
        taskService.deleteTask(id);
    }

    /**
     * GET /api/tasks/filter
     * <p>
     * Filters tasks by status and/or deadline range.
     * All filters are optional.
     *
     * @param status       task status
     * @param fromDeadline start of deadline range
     * @param toDeadline   end of deadline range
     * @return filtered list of tasks
     */
    @GetMapping("/filter")
    public ResponseEntity<List<TaskResponseDto>> filterTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDeadline,
            @RequestParam(required = false) String toDeadline) {

        log.info("HTTP GET /api/tasks/filter status={}, from={}, to={}", status, fromDeadline, toDeadline);
        return ResponseEntity.ok(taskService.filterTasks(status, fromDeadline, toDeadline));
    }

    /**
     * GET /api/tasks/stats
     * <p>
     * Returns task statistics for the currently authenticated user.
     *
     * @return task statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<TaskStatisticsResponse> getTaskStatistics() {
        log.info("HTTP GET /api/tasks/stats");
        return ResponseEntity.ok(taskService.getStatistics());
    }
}

