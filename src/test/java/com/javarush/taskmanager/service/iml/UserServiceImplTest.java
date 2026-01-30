package com.javarush.taskmanager.service.iml;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.exception.BusinessException;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.repository.RefreshTokenRepository;
import com.javarush.taskmanager.repository.TaskRepository;
import com.javarush.taskmanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private final Long userId = 1L;
    private final String username = "testuser";
    private final String password = "password123";
    private final String encodedPassword = "encodedPassword123";

    @BeforeEach
    void setUp() {
        testUser = new User(username, encodedPassword, UserRole.ROLE_USER);

        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, userId);
            idField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID", e);
        }
    }

    @Test
    void register_ShouldSuccessfullyRegisterNewUser() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User registeredUser = userService.register(username, password);

        assertNotNull(registeredUser);
        assertEquals(userId, registeredUser.getId());
        assertEquals(username, registeredUser.getUsername());
        assertEquals(encodedPassword, registeredUser.getPassword());
        assertEquals(UserRole.ROLE_USER, registeredUser.getRole());

        verify(userRepository, times(1)).existsByUsername(username);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowExceptionWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername(username)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.register(username, password));

        assertEquals("Username already exists", exception.getMessage());

        verify(userRepository, times(1)).existsByUsername(username);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldEncodePassword() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            return userToSave;
        });

        User registeredUser = userService.register(username, password);
        assertEquals(encodedPassword, registeredUser.getPassword());
        verify(passwordEncoder, times(1)).encode(password);
    }

    @Test
    void getByUsername_ShouldReturnUserWhenFound() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        User foundUser = userService.getByUsername(username);
        assertNotNull(foundUser);
        assertEquals(username, foundUser.getUsername());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void getByUsername_ShouldThrowExceptionWhenUserNotFound() {
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getByUsername(nonExistentUsername));

        assertEquals("User not found with username: " + nonExistentUsername, exception.getMessage());
        verify(userRepository, times(1)).findByUsername(nonExistentUsername);
    }

    @Test
    void getById_ShouldReturnUserWhenFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        User foundUser = userService.getById(userId);
        assertNotNull(foundUser);
        assertEquals(userId, foundUser.getId());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getById_ShouldThrowExceptionWhenUserNotFound() {
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getById(nonExistentId));

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void changePassword_ShouldChangePasswordForExistingUser() {
        String newPassword = "newPassword123";
        String newEncodedPassword = "newEncodedPassword123";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);

        String originalPassword = testUser.getPassword();

        userService.changePassword(userId, newPassword);

        assertNotEquals(originalPassword, testUser.getPassword());
        assertEquals(newEncodedPassword, testUser.getPassword());

        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).encode(newPassword);
    }

    @Test
    void changePassword_ShouldThrowExceptionWhenUserNotFound() {
        Long nonExistentId = 999L;
        String newPassword = "newPassword123";

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.changePassword(nonExistentId, newPassword));

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepository, times(1)).findById(nonExistentId);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changeRole_ShouldChangeRoleForExistingUser() {
        UserRole newRole = UserRole.ROLE_ADMIN;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        UserRole originalRole = testUser.getRole();

        userService.changeRole(userId, newRole);
        assertNotEquals(originalRole, testUser.getRole());
        assertEquals(newRole, testUser.getRole());

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void changeRole_ShouldThrowExceptionWhenUserNotFound() {
        Long nonExistentId = 999L;
        UserRole newRole = UserRole.ROLE_ADMIN;

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.changeRole(nonExistentId, newRole));

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void changeRole_ShouldHandleAllRoleValues() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        for (UserRole role : UserRole.values()) {
            testUser.changeRole(UserRole.ROLE_USER);
            userService.changeRole(userId, role);
            assertEquals(role, testUser.getRole());
        }
        verify(userRepository, times(UserRole.values().length)).findById(userId);
    }

    @Test
    void getByUsername_ShouldHandleCaseSensitivity() {
        String uppercaseUsername = "TESTUSER";
        String lowercaseUsername = "testuser";

        User uppercaseUser = new User(uppercaseUsername, encodedPassword, UserRole.ROLE_USER);

        when(userRepository.findByUsername(uppercaseUsername)).thenReturn(Optional.of(uppercaseUser));
        when(userRepository.findByUsername(lowercaseUsername)).thenReturn(Optional.of(testUser));

        User foundUppercase = userService.getByUsername(uppercaseUsername);
        User foundLowercase = userService.getByUsername(lowercaseUsername);

        assertEquals(uppercaseUsername, foundUppercase.getUsername());
        assertEquals(lowercaseUsername, foundLowercase.getUsername());

        verify(userRepository, times(1)).findByUsername(uppercaseUsername);
        verify(userRepository, times(1)).findByUsername(lowercaseUsername);
    }

    @Test
    void changePassword_ShouldHandleSamePassword() {
        String samePassword = password;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(samePassword)).thenReturn("newEncodingOfSamePassword");
        String originalPassword = testUser.getPassword();

        userService.changePassword(userId, samePassword);
        assertNotEquals(originalPassword, testUser.getPassword());
        verify(passwordEncoder, times(1)).encode(samePassword);
    }

    @Test
    void changeRole_ShouldHandleSameRole() {
        UserRole sameRole = UserRole.ROLE_USER;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        userService.changeRole(userId, sameRole);
        assertEquals(sameRole, testUser.getRole());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void deleteUser_ShouldDeleteUserAndDependencies() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

        userService.deleteUser(1L);
        InOrder inOrder = inOrder(refreshTokenRepository, taskRepository, userRepository);
        inOrder.verify(refreshTokenRepository).deleteAllByUser(testUser);
        inOrder.verify(taskRepository).deleteAllByOwner(testUser);
        inOrder.verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> userService.deleteUser(1L));
        assertEquals("User not found", ex.getMessage());
        verifyNoInteractions(refreshTokenRepository, taskRepository);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void deleteUser_ShouldLogDeletion() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
        assertDoesNotThrow(() -> userService.deleteUser(1L));
    }
}