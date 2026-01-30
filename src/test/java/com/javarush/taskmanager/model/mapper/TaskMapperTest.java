package com.javarush.taskmanager.model.mapper;

import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.entity.Task;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTest {

    private TaskMapper taskMapper;
    private User owner;
    private final Long taskId = 10L;

    @BeforeEach
    void setUp() {
        taskMapper = new TaskMapper();
        owner = new User(
                "testuser",
                "encodedPass",
                UserRole.ROLE_USER
        );

        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(owner, 1L);

            var taskIdField = Task.class.getDeclaredField("id");
            taskIdField.setAccessible(true);
            taskIdField.set(null, null);
        } catch (Exception ignored) {
        }
    }

    @Test
    void toEntity_shouldMapAllFields() {
        LocalDate deadline = LocalDate.now().plusDays(2);

        TaskRequestDto dto = new TaskRequestDto(
                "Title",
                "Description",
                TaskStatus.IN_PROGRESS,
                deadline
        );

        Task task = taskMapper.toEntity(dto, owner);

        assertNotNull(task);
        assertEquals("Title", task.getTitle());
        assertEquals("Description", task.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(deadline, task.getDeadline());
        assertEquals(owner, task.getOwner());
    }

    @Test
    void updateEntity_shouldUpdateTaskFields() {
        LocalDate oldDeadline = LocalDate.now().plusDays(1);
        LocalDate newDeadline = LocalDate.now().plusDays(5);

        Task task = new Task(
                "Old title",
                "Old description",
                TaskStatus.PENDING,
                oldDeadline,
                owner
        );

        TaskRequestDto dto = new TaskRequestDto(
                "New title",
                "New description",
                TaskStatus.COMPLETED,
                newDeadline
        );

        taskMapper.updateEntity(task, dto);

        assertEquals("New title", task.getTitle());
        assertEquals("New description", task.getDescription());
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(newDeadline, task.getDeadline());
    }

    @Test
    void toResponseDto_shouldMapEntityToDto() {
        LocalDate deadline = LocalDate.now().plusDays(3);

        Task task = new Task(
                "Task title",
                "Task description",
                TaskStatus.IN_PROGRESS,
                deadline,
                owner
        );

        try {
            var field = Task.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TaskResponseDto dto = taskMapper.toResponseDto(task);

        assertNotNull(dto);
        assertEquals(taskId, dto.getId());
        assertEquals("Task title", dto.getTitle());
        assertEquals("Task description", dto.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, dto.getStatus());
        assertEquals(deadline, dto.getDeadline());
    }

    @Test
    void updateEntity_shouldNotChangeOwner() {
        Task task = new Task(
                "Title",
                "Desc",
                TaskStatus.PENDING,
                LocalDate.now(),
                owner
        );

        User originalOwner = task.getOwner();

        TaskRequestDto dto = new TaskRequestDto(
                "Updated",
                "Updated",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(1)
        );

        taskMapper.updateEntity(task, dto);
        assertEquals(originalOwner, task.getOwner());
    }
}
