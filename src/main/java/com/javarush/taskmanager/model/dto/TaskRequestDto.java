package com.javarush.taskmanager.model.dto;

import com.javarush.taskmanager.enums.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDto {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private TaskStatus status;

    @FutureOrPresent
    private LocalDate deadline;
}
