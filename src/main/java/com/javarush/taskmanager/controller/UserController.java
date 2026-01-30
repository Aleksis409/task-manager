package com.javarush.taskmanager.controller;

import com.javarush.taskmanager.model.dto.ChangePasswordRequest;
import com.javarush.taskmanager.model.dto.UserProfileResponse;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.security.CurrentUserProvider;
import com.javarush.taskmanager.service.iml.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserServiceImpl userServiceImpl;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    public UserProfileResponse getProfile() {
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userServiceImpl.getById(userId);
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount() {
        Long userId = currentUserProvider.getCurrentUserId();
        userServiceImpl.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        Long userId = currentUserProvider.getCurrentUserId();
        userServiceImpl.changePassword(userId, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}