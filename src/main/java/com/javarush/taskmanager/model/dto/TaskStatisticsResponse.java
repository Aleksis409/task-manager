package com.javarush.taskmanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskStatisticsResponse {

    /**
     * Total number of tasks owned by the user.
     */
    private long totalTasks;

    /**
     * Number of completed tasks owned by the user.
     */
    private long completedTasks;
}