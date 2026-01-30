package com.javarush.taskmanager.controller;

import com.javarush.taskmanager.model.dto.ChangeUserRoleRequest;
import com.javarush.taskmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long id,
            @RequestBody @Valid ChangeUserRoleRequest request) {

        log.info("Admin changing role: userId={}, newRole={}",
                id, request.getRole());

        userService.changeRole(id, request.getRole());
        return ResponseEntity.noContent().build();
    }
}