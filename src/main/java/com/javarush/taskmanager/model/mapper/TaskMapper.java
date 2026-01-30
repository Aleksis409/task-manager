package com.javarush.taskmanager.model.mapper;

import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.entity.Task;
import com.javarush.taskmanager.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequestDto dto, User owner) {
        return new Task(
                dto.getTitle(),
                dto.getDescription(),
                dto.getStatus(),
                dto.getDeadline(),
                owner
        );
    }

    public void updateEntity(Task task, TaskRequestDto dto) {
        task.updateDetails(
                dto.getTitle(),
                dto.getDescription(),
                dto.getDeadline()
        );
        task.changeStatus(dto.getStatus());
    }

    public TaskResponseDto toResponseDto(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDeadline()
        );
    }
}
