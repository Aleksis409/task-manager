package com.javarush.taskmanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationErrorResponse {

    private String field;
    private String message;
}
