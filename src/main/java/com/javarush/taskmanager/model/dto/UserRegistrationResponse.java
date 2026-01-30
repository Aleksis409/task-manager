package com.javarush.taskmanager.model.dto;

import com.javarush.taskmanager.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRegistrationResponse {

    private Long id;
    private String username;
    private UserRole role;
}
