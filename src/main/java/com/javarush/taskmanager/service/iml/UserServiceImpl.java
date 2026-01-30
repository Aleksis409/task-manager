package com.javarush.taskmanager.service.iml;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.exception.BusinessException;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.repository.RefreshTokenRepository;
import com.javarush.taskmanager.repository.TaskRepository;
import com.javarush.taskmanager.repository.UserRepository;
import com.javarush.taskmanager.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TaskRepository taskRepository;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           RefreshTokenRepository refreshTokenRepository,
                           TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public User register(String username, String password) {
        log.debug("Attempt to register user: username={}", username);

        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: username already exists [{}]", username);
            throw new BusinessException("Username already exists");
        }

        User user = new User(
                username,
                passwordEncoder.encode(password),
                UserRole.ROLE_USER
        );

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, username={}",
                savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        log.debug("Fetching user by username={}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found by username={}", username);
                    return new BusinessException("User not found with username: " + username);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        log.debug("Fetching user by id={}", id);

        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found by id={}", id);
                    return new BusinessException("User not found with id: " + id);
                });
    }

    @Override
    public void changePassword(Long userId, String newPassword) {
        log.info("Changing password for userId={}", userId);

        User user = getById(userId);
        user.changePassword(passwordEncoder.encode(newPassword));

        log.info("Password changed successfully for userId={}", userId);
    }

    @Override
    public void changeRole(Long userId, UserRole role) {
        log.info("Changing role for userId={} to {}", userId, role);

        User user = getById(userId);
        user.changeRole(role);

        log.info("Role changed successfully for userId={}", userId);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        refreshTokenRepository.deleteAllByUser(user);
        taskRepository.deleteAllByOwner(user);
        userRepository.delete(user);

        log.info("User deleted: {}", userId);
    }
}