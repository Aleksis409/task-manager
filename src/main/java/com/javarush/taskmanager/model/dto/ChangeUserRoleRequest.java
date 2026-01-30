package com.javarush.taskmanager.model.dto;

import com.javarush.taskmanager.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeUserRoleRequest {

    @NotNull
    private UserRole role;
}
