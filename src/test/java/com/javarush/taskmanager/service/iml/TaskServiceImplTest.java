package com.javarush.taskmanager.service.iml;

import com.javarush.taskmanager.enums.TaskStatus;
import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.dto.TaskStatisticsResponse;
import com.javarush.taskmanager.model.entity.Task;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.model.mapper.TaskMapper;
import com.javarush.taskmanager.repository.TaskRepository;
import com.javarush.taskmanager.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User testUser;
    private Task testTask;
    private TaskResponseDto testTaskDto;
    private final Long userId = 1L;
    private final Long taskId = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password", UserRole.ROLE_USER);
        testTask = new Task(
                "Test Task",
                "Test Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7),
                testUser
        );
        testTaskDto = new TaskResponseDto(
                taskId,
                "Test Task",
                "Test Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7)
        );
    }

    @Test
    void getAllTasks_ShouldReturnTasksForCurrentUser() {
        Task taskWithId = createTaskWithId(taskId);
        List<Task> tasks = List.of(taskWithId);

        TaskResponseDto taskDto = createTaskResponseDto(taskId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findAllByOwner(testUser)).thenReturn(tasks);
        when(taskMapper.toResponseDto(taskWithId)).thenReturn(taskDto);

        List<TaskResponseDto> result = taskService.getAllTasks();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto, result.get(0));

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskRepository, times(1)).findAllByOwner(testUser);
        verify(taskMapper, times(1)).toResponseDto(taskWithId);
    }

    @Test
    void getTaskById_ShouldReturnTaskWhenFound() {
        Task taskWithId = createTaskWithId(taskId);
        TaskResponseDto taskDto = createTaskResponseDto(taskId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(taskId, testUser)).thenReturn(Optional.of(taskWithId));
        when(taskMapper.toResponseDto(taskWithId)).thenReturn(taskDto);

        TaskResponseDto result = taskService.getTaskById(taskId);
        assertNotNull(result);
        assertEquals(taskDto, result);

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskRepository, times(1)).findByIdAndOwner(taskId, testUser);
        verify(taskMapper, times(1)).toResponseDto(taskWithId);
    }

    @Test
    void getTaskById_ShouldThrowExceptionWhenTaskNotFound() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(taskId, testUser)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskById(taskId));

        assertEquals("Task not found with id: " + taskId, exception.getMessage());

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskRepository, times(1)).findByIdAndOwner(taskId, testUser);
        verify(taskMapper, never()).toResponseDto(any());
    }

    @Test
    void createTask_ShouldCreateAndReturnTask() {
        TaskRequestDto requestDto = new TaskRequestDto(
                "New Task",
                "New Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(14)
        );

        Task newTask = new Task(
                "New Task",
                "New Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(14),
                testUser
        );

        Task savedTask = createTaskWithId(taskId);
        TaskResponseDto taskDto = createTaskResponseDto(taskId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskMapper.toEntity(requestDto, testUser)).thenReturn(newTask);
        when(taskRepository.save(newTask)).thenReturn(savedTask);
        when(taskMapper.toResponseDto(savedTask)).thenReturn(taskDto);

        TaskResponseDto result = taskService.createTask(requestDto);

        assertNotNull(result);
        assertEquals(taskDto, result);

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskMapper, times(1)).toEntity(requestDto, testUser);
        verify(taskRepository, times(1)).save(newTask);
        verify(taskMapper, times(1)).toResponseDto(savedTask);
    }

    @Test
    void updateTask_ShouldUpdateAndReturnTask() {
        TaskRequestDto updateDto = new TaskRequestDto(
                "Updated Task",
                "Updated Description",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(10)
        );

        Task existingTask = createTaskWithId(taskId);
        TaskResponseDto updatedDto = new TaskResponseDto(
                taskId,
                "Updated Task",
                "Updated Description",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(10)
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(taskId, testUser)).thenReturn(Optional.of(existingTask));
        doNothing().when(taskMapper).updateEntity(existingTask, updateDto);
        when(taskMapper.toResponseDto(existingTask)).thenReturn(updatedDto);

        TaskResponseDto result = taskService.updateTask(taskId, updateDto);

        assertNotNull(result);
        assertEquals("Updated Task", result.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskRepository, times(1)).findByIdAndOwner(taskId, testUser);
        verify(taskMapper, times(1)).updateEntity(existingTask, updateDto);
        verify(taskMapper, times(1)).toResponseDto(existingTask);
    }

    @Test
    void deleteTask_ShouldDeleteTaskWhenFound() {
        Task existingTask = createTaskWithId(taskId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findByIdAndOwner(taskId, testUser)).thenReturn(Optional.of(existingTask));
        doNothing().when(taskRepository).delete(existingTask);

        taskService.deleteTask(taskId);
        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskRepository, times(1)).findByIdAndOwner(taskId, testUser);
        verify(taskRepository, times(1)).delete(existingTask);
    }

    @Test
    void filterTasks_ShouldReturnFilteredTasks() {
        String status = "PENDING";
        String fromDeadline = "2024-01-01";
        String toDeadline = "2024-12-31";

        Task filteredTask = createTaskWithId(taskId);
        List<Task> filteredTasks = List.of(filteredTask);
        TaskResponseDto taskDto = createTaskResponseDto(taskId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findAll(ArgumentMatchers.<Specification<Task>>any())).thenReturn(filteredTasks);
        when(taskMapper.toResponseDto(filteredTask)).thenReturn(taskDto);

        List<TaskResponseDto> result = taskService.filterTasks(status, fromDeadline, toDeadline);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto, result.get(0));
        verify(taskRepository).findAll(ArgumentMatchers.<Specification<Task>>any());
    }

    @Test
    void getStatistics_ShouldReturnStatisticsForCurrentUser() {
        long totalTasks = 10L;
        long completedTasks = 4L;

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.countByOwner(testUser)).thenReturn(totalTasks);
        when(taskRepository.countByOwnerAndStatus(testUser, TaskStatus.COMPLETED)).thenReturn(completedTasks);

        TaskStatisticsResponse result = taskService.getStatistics();

        assertNotNull(result);
        assertEquals(totalTasks, result.getTotalTasks());
        assertEquals(completedTasks, result.getCompletedTasks());

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskRepository, times(1)).countByOwner(testUser);
        verify(taskRepository, times(1)).countByOwnerAndStatus(testUser, TaskStatus.COMPLETED);
    }

    @Test
    void filterTasks_ShouldHandleNullParameters() {

        Task filteredTask = createTaskWithId(taskId);
        List<Task> filteredTasks = List.of(filteredTask);
        TaskResponseDto taskDto = createTaskResponseDto(taskId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskRepository.findAll(ArgumentMatchers.<Specification<Task>>any())).thenReturn(filteredTasks);

        when(taskMapper.toResponseDto(filteredTask)).thenReturn(taskDto);

        List<TaskResponseDto> result = taskService.filterTasks(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository).findAll(ArgumentMatchers.<Specification<Task>>any());
    }


    @Test
    void createTask_ShouldHandleNullDeadline() {
        TaskRequestDto requestDto = new TaskRequestDto(
                "Task without deadline",
                "Description",
                TaskStatus.PENDING,
                null
        );

        Task newTask = new Task(
                "Task without deadline",
                "Description",
                TaskStatus.PENDING,
                null,
                testUser
        );

        Task savedTask = new Task(
                "Task without deadline",
                "Description",
                TaskStatus.PENDING,
                null,
                testUser
        );

        TaskResponseDto taskDto = new TaskResponseDto(
                taskId,
                "Task without deadline",
                "Description",
                TaskStatus.PENDING,
                null
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(testUser);
        when(taskMapper.toEntity(requestDto, testUser)).thenReturn(newTask);
        when(taskRepository.save(newTask)).thenReturn(savedTask);
        when(taskMapper.toResponseDto(savedTask)).thenReturn(taskDto);

        TaskResponseDto result = taskService.createTask(requestDto);

        assertNotNull(result);
        assertNull(result.getDeadline());

        verify(currentUserProvider, times(1)).getCurrentUserId();
        verify(userService, times(1)).getById(userId);
        verify(taskMapper, times(1)).toEntity(requestDto, testUser);
        verify(taskRepository, times(1)).save(newTask);
        verify(taskMapper, times(1)).toResponseDto(savedTask);
    }

    private Task createTaskWithId(Long id) {
        Task task = new Task(
                "Test Task",
                "Test Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7),
                testUser
        );

        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, id);
            idField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set task ID", e);
        }
        return task;
    }

    private TaskResponseDto createTaskResponseDto(Long id) {
        return new TaskResponseDto(
                id,
                "Test Task",
                "Test Description",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(7)
        );
    }
}