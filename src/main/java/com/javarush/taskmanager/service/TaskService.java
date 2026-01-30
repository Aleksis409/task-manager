package com.javarush.taskmanager.service;

import com.javarush.taskmanager.model.dto.TaskRequestDto;
import com.javarush.taskmanager.model.dto.TaskResponseDto;
import com.javarush.taskmanager.model.dto.TaskStatisticsResponse;

import java.util.List;

public interface TaskService {

    List<TaskResponseDto> getAllTasks();

    TaskResponseDto getTaskById(Long id);

    TaskResponseDto createTask(TaskRequestDto request);

    TaskResponseDto updateTask(Long id, TaskRequestDto request);

    void deleteTask(Long id);

    List<TaskResponseDto> filterTasks(String status, String fromDeadline, String toDeadline);

    TaskStatisticsResponse getStatistics();
}
