package com.javarush.taskmanager.controller;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.security.CurrentUserProvider;
import com.javarush.taskmanager.security.SecurityConfig;
import com.javarush.taskmanager.security.jwt.JwtAuthenticationFilter;
import com.javarush.taskmanager.security.jwt.JwtService;
import com.javarush.taskmanager.service.iml.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, SecurityConfig.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserServiceImpl userService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private User testUser;

    @BeforeEach
    void setUp() {

        testUser = new User(
                "testuser",
                "encoded",
                UserRole.ROLE_USER
        );

        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------- GET /me ---------------------

    @Test
    void getProfile_shouldReturnProfile() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    // ------------------- DELETE /me ---------------------

    @Test
    void deleteAccount_shouldReturn204() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    // ------------------- PUT /me/password ---------------------

    @Test
    void changePassword_shouldReturn204() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        String json = """
                {
                    "newPassword": "newPass123"
                }
                """;

        mockMvc.perform(
                        put("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNoContent());

        verify(userService).changePassword(1L, "newPass123");
    }

    @Test
    void changePassword_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(
                        put("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }
}