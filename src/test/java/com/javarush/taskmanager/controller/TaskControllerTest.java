package com.javarush.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.taskmanager.enums.TaskStatus;
import com.javarush.taskmanager.exception.handler.GlobalExceptionHandler;
import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.dto.TaskStatisticsResponse;
import com.javarush.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private ObjectMapper objectMapper;
    private TaskResponseDto taskResponseDto;
    private TaskRequestDto taskRequestDto;
    private final Long taskId = 1L;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        taskResponseDto = new TaskResponseDto(
                taskId,
                "Test Task",
                "Test Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7)
        );

        taskRequestDto = new TaskRequestDto(
                "Test Task",
                "Test Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7)
        );
    }

    @Test
    void getAllTasks_ShouldReturnListOfTasks() throws Exception {
        List<TaskResponseDto> tasks = List.of(taskResponseDto);
        when(taskService.getAllTasks()).thenReturn(tasks);

        mockMvc.perform(get("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(taskId.intValue())))
                .andExpect(jsonPath("$[0].title", is("Test Task")))
                .andExpect(jsonPath("$[0].description", is("Test Description")))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    void getTaskById_ShouldReturnTask() throws Exception {
        when(taskService.getTaskById(taskId)).thenReturn(taskResponseDto);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Test Task")));

        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    void getTaskById_ShouldHandleTaskNotFound() throws Exception {
        when(taskService.getTaskById(taskId))
                .thenThrow(new RuntimeException("Task not found"));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("Internal server error: Task not found")));

        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    void createTask_ShouldCreateAndReturnTask() throws Exception {
        when(taskService.createTask(any(TaskRequestDto.class))).thenReturn(taskResponseDto);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Test Task")));

        verify(taskService, times(1)).createTask(any(TaskRequestDto.class));
    }

    @Test
    void createTask_ShouldValidateRequest() throws Exception {
        TaskRequestDto invalidRequest = new TaskRequestDto(
                "",
                "Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7)
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field", is("title")))
                .andExpect(jsonPath("$[0].message", notNullValue()));

        verify(taskService, never()).createTask(any());
    }

    @Test
    void updateTask_ShouldUpdateAndReturnTask() throws Exception {
        TaskResponseDto updatedDto = new TaskResponseDto(
                taskId,
                "Updated Task",
                "Updated Description",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(5)
        );

        when(taskService.updateTask(eq(taskId), any(TaskRequestDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Task")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        verify(taskService, times(1)).updateTask(eq(taskId), any(TaskRequestDto.class));
    }

    @Test
    void updateTask_ShouldHandleTaskNotFound() throws Exception {
        when(taskService.updateTask(eq(taskId), any(TaskRequestDto.class)))
                .thenThrow(new RuntimeException("Task not found"));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("Internal server error: Task not found")));

        verify(taskService, times(1)).updateTask(eq(taskId), any(TaskRequestDto.class));
    }

    @Test
    void deleteTask_ShouldDeleteTask() throws Exception {
        doNothing().when(taskService).deleteTask(taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(taskId);
    }

    @Test
    void deleteTask_ShouldHandleTaskNotFound() throws Exception {
        doThrow(new RuntimeException("Task not found")).when(taskService).deleteTask(taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("Internal server error: Task not found")));

        verify(taskService, times(1)).deleteTask(taskId);
    }

    @Test
    void filterTasks_ShouldReturnFilteredTasks() throws Exception {
        List<TaskResponseDto> filteredTasks = List.of(taskResponseDto);
        String status = "PENDING";
        String fromDeadline = "2024-01-01";
        String toDeadline = "2024-12-31";

        when(taskService.filterTasks(status, fromDeadline, toDeadline)).thenReturn(filteredTasks);

        mockMvc.perform(get("/api/tasks/filter")
                        .param("status", status)
                        .param("fromDeadline", fromDeadline)
                        .param("toDeadline", toDeadline)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is(status)));

        verify(taskService, times(1)).filterTasks(status, fromDeadline, toDeadline);
    }

    @Test
    void filterTasks_ShouldHandleInvalidStatus() throws Exception {
        String invalidStatus = "INVALID_STATUS";

        when(taskService.filterTasks(invalidStatus, null, null))
                .thenThrow(new IllegalArgumentException("Invalid status"));

        mockMvc.perform(get("/api/tasks/filter")
                        .param("status", invalidStatus)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("Internal server error: Invalid status")));

        verify(taskService, times(1)).filterTasks(invalidStatus, null, null);
    }

    @Test
    void getTaskStatistics_ShouldReturnStatistics() throws Exception {
        TaskStatisticsResponse statistics = new TaskStatisticsResponse(10L, 4L);
        when(taskService.getStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/api/tasks/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks", is(10)))
                .andExpect(jsonPath("$.completedTasks", is(4)));

        verify(taskService, times(1)).getStatistics();
    }

    @Test
    void getTaskStatistics_ShouldHandleZeroTasks() throws Exception {
        TaskStatisticsResponse statistics = new TaskStatisticsResponse(0L, 0L);
        when(taskService.getStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/api/tasks/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks", is(0)))
                .andExpect(jsonPath("$.completedTasks", is(0)));

        verify(taskService, times(1)).getStatistics();
    }

    @Test
    void updateTask_ShouldHandleIllegalStateException() throws Exception {
        when(taskService.updateTask(eq(taskId), any(TaskRequestDto.class)))
                .thenThrow(new IllegalStateException("Task cannot be updated"));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Task cannot be updated")));

        verify(taskService, times(1)).updateTask(eq(taskId), any(TaskRequestDto.class));
    }

    @Test
    void createTask_ShouldHandleIllegalStateException() throws Exception {
        when(taskService.createTask(any(TaskRequestDto.class)))
                .thenThrow(new IllegalStateException("Invalid task data"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid task data")));

        verify(taskService, times(1)).createTask(any(TaskRequestDto.class));
    }

    @Test
    void getTaskById_ShouldHandleInvalidIdFormat() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", "not-a-number")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Parameter 'id' must be of type")));

        verify(taskService, never()).getTaskById(any());
    }

    @Test
    void deleteTask_ShouldHandleInvalidIdFormat() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Parameter 'id' must be of type")));

        verify(taskService, never()).deleteTask(any());
    }

    @Test
    void updateTask_ShouldHandleInvalidIdFormat() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", "not-a-number")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Parameter 'id' must be of type")));

        verify(taskService, never()).updateTask(any(), any());
    }

    @Test
    void createTask_ShouldHandleInvalidEnumValue() throws Exception {
        String invalidStatusJson = """
        {
            "title": "Test Task",
            "description": "Test Description",
            "status": "INVALID_STATUS",
            "deadline": "2024-12-31"
        }
        """;

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStatusJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid value")));

        verify(taskService, never()).createTask(any());
    }
}