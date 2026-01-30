package com.javarush.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.exception.handler.GlobalExceptionHandler;
import com.javarush.taskmanager.model.dto.ChangeUserRoleRequest;
import com.javarush.taskmanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    private ChangeUserRoleRequest changeRoleRequest;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        changeRoleRequest = new ChangeUserRoleRequest();
        changeRoleRequest.setRole(UserRole.ROLE_ADMIN);
    }

    @Test
    void changeRole_ShouldChangeUserRoleSuccessfully() throws Exception {
        doNothing().when(userService).changeRole(userId, UserRole.ROLE_ADMIN);

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).changeRole(userId, UserRole.ROLE_ADMIN);
    }

    @Test
    void changeRole_ShouldValidateRequest() throws Exception {
        ChangeUserRoleRequest invalidRequest = new ChangeUserRoleRequest();

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    System.out.println("Validation error response: " + content);
                });

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleUserNotFound() throws Exception {
        doThrow(new RuntimeException("User not found"))
                .when(userService).changeRole(userId, UserRole.ROLE_ADMIN);

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).changeRole(userId, UserRole.ROLE_ADMIN);
    }

    @Test
    void changeRole_ShouldHandleInvalidUserId() throws Exception {
        String invalidUserId = "not-a-number";

        mockMvc.perform(put("/api/admin/users/{id}/role", invalidUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleNullRequestBody() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleAllRoleValues() throws Exception {
        for (UserRole role : UserRole.values()) {
            reset(userService);

            ChangeUserRoleRequest request = new ChangeUserRoleRequest();
            request.setRole(role);

            doNothing().when(userService).changeRole(userId, role);

            mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(userService, times(1)).changeRole(userId, role);
        }
    }

    @Test
    void changeRole_ShouldHandleSelfRoleChange() throws Exception {
        doNothing().when(userService).changeRole(userId, UserRole.ROLE_USER);

        ChangeUserRoleRequest request = new ChangeUserRoleRequest();
        request.setRole(UserRole.ROLE_USER);

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).changeRole(userId, UserRole.ROLE_USER);
    }

    @Test
    void changeRole_ShouldHandleNegativeUserId() throws Exception {
        Long negativeId = -1L;
        doThrow(new RuntimeException("Invalid user ID"))
                .when(userService).changeRole(negativeId, UserRole.ROLE_ADMIN);

        mockMvc.perform(put("/api/admin/users/{id}/role", negativeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).changeRole(negativeId, UserRole.ROLE_ADMIN);
    }

    @Test
    void changeRole_ShouldHandleZeroUserId() throws Exception {
        Long zeroId = 0L;
        doThrow(new RuntimeException("User not found"))
                .when(userService).changeRole(zeroId, UserRole.ROLE_ADMIN);

        mockMvc.perform(put("/api/admin/users/{id}/role", zeroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).changeRole(zeroId, UserRole.ROLE_ADMIN);
    }

    @Test
    void changeRole_ShouldHandleVeryLargeUserId() throws Exception {
        Long largeId = Long.MAX_VALUE;
        doThrow(new RuntimeException("User not found"))
                .when(userService).changeRole(largeId, UserRole.ROLE_ADMIN);

        mockMvc.perform(put("/api/admin/users/{id}/role", largeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).changeRole(largeId, UserRole.ROLE_ADMIN);
    }

    @Test
    void changeRole_ShouldHandleInvalidJson() throws Exception {
        String invalidJson = "{role: ADMIN}";

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleWrongJsonType() throws Exception {
        String wrongTypeJson = "{\"role\": 123}";

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongTypeJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleInvalidEnumValue() throws Exception {
        String invalidRoleJson = "{\"role\": \"SUPER_ADMIN\"}";

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRoleJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleMissingRoleField() throws Exception {
        String missingFieldJson = "{\"otherField\": \"value\"}";

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingFieldJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleEmptyRole() throws Exception {
        String emptyRoleJson = "{\"role\": \"\"}";

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyRoleJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void changeRole_ShouldHandleNullRole() throws Exception {
        String nullRoleJson = "{\"role\": null}";

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullRoleJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changeRole(any(), any());
    }
}