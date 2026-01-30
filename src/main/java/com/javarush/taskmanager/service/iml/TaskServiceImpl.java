package com.javarush.taskmanager.service.iml;

import com.javarush.taskmanager.enums.TaskStatus;
import com.javarush.taskmanager.exception.BusinessException;
import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.dto.TaskStatisticsResponse;
import com.javarush.taskmanager.model.entity.Task;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.model.mapper.TaskMapper;
import com.javarush.taskmanager.repository.TaskRepository;
import com.javarush.taskmanager.repository.TaskSpecs;
import com.javarush.taskmanager.security.CurrentUserProvider;
import com.javarush.taskmanager.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final CurrentUserProvider currentUserProvider;
    private final UserServiceImpl userServiceImpl;

    public TaskServiceImpl(TaskRepository taskRepository,
                           TaskMapper taskMapper,
                           CurrentUserProvider currentUserProvider,
                           UserServiceImpl userServiceImpl) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.currentUserProvider = currentUserProvider;
        this.userServiceImpl = userServiceImpl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> getAllTasks() {
        User currentUser = getCurrentUserEntity();

        log.info("Fetching all tasks for userId={}", currentUser.getId());

        return taskRepository.findAllByOwner(currentUser)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDto getTaskById(Long id) {
        log.info("Fetching task by id={}", id);

        Task task = getTaskOrThrow(id);
        return taskMapper.toResponseDto(task);
    }

    @Override
    public TaskResponseDto createTask(TaskRequestDto request) {
        User currentUser = getCurrentUserEntity();

        log.info("Creating task for userId={}, title={}",
                currentUser.getId(), request.getTitle());

        Task task = taskMapper.toEntity(request, currentUser);
        Task savedTask = taskRepository.save(task);

        log.info("Task created: id={}, userId={}",
                savedTask.getId(), currentUser.getId());

        return taskMapper.toResponseDto(savedTask);
    }

    @Override
    public TaskResponseDto updateTask(Long id, TaskRequestDto request) {
        log.info("Updating task id={}", id);

        Task task = getTaskOrThrow(id);
        taskMapper.updateEntity(task, request);

        log.info("Task updated successfully: id={}", id);

        return taskMapper.toResponseDto(task);
    }

    @Override
    public void deleteTask(Long id) {
        log.info("Deleting task id={}", id);

        Task task = getTaskOrThrow(id);
        taskRepository.delete(task);

        log.info("Task deleted successfully: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> filterTasks(String status,
                                             String fromDeadline,
                                             String toDeadline) {

        User currentUser = getCurrentUserEntity();

        Specification<Task> spec = TaskSpecs.byOwner(currentUser);

        if (status != null) {
            spec = spec.and(TaskSpecs.hasStatus(TaskStatus.valueOf(status))
            );
        }

        if (fromDeadline != null) {
            spec = spec.and(TaskSpecs.deadlineFrom(LocalDate.parse(fromDeadline))
            );
        }

        if (toDeadline != null) {
            spec = spec.and(TaskSpecs.deadlineTo(LocalDate.parse(toDeadline))
            );
        }

        return taskRepository.findAll(spec)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatisticsResponse getStatistics() {
        User currentUser = getCurrentUserEntity();

        log.info("Fetching task statistics for userId={}", currentUser.getId());

        long total = taskRepository.countByOwner(currentUser);
        long completed = taskRepository.countByOwnerAndStatus(currentUser, TaskStatus.COMPLETED);

        return new TaskStatisticsResponse(total, completed);
    }

    private Task getTaskOrThrow(Long id) {
        User currentUser = getCurrentUserEntity();

        return taskRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> {
                    log.warn("Task not found: id={}, userId={}",
                            id, currentUser.getId());
                    return new BusinessException("Task not found with id: " + id);
                });
    }

    private User getCurrentUserEntity() {
        Long userId = currentUserProvider.getCurrentUserId();
        return userServiceImpl.getById(userId);
    }
}

