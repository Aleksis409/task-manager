package com.javarush.taskmanager.config;

import com.javarush.taskmanager.enums.TaskStatus;
import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.entity.Task;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.repository.TaskRepository;
import com.javarush.taskmanager.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.count() > 0) {
            log.info("Users already exist — skipping test data init");
            return;
        }

        log.info("Initializing test users and tasks...");

        // ---------- ADMIN ----------
        User admin = new User(
                "admin",
                passwordEncoder.encode("password"),
                UserRole.ROLE_ADMIN
        );

        userRepository.save(admin);

        // ---------- USERS ----------
        createUserWithTasks("user1", "password1");
        createUserWithTasks("user2", "password2");
        createUserWithTasks("user3", "password3");
        createUserWithTasks("user4", "password4");
        createUserWithTasks("user5", "password5");

        log.info("Test data initialized successfully");
    }

    private void createUserWithTasks(String username, String rawPassword) {

        User user = new User(
                username,
                passwordEncoder.encode(rawPassword),
                UserRole.ROLE_USER
        );

        userRepository.save(user);

        taskRepository.save(new Task(
                "Task 1 for " + username,
                "Description 1",
                TaskStatus.PENDING,
                LocalDate.now().plusDays(3),
                user
        ));

        taskRepository.save(new Task(
                "Task 2 for " + username,
                "Description 2",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(7),
                user
        ));

        taskRepository.save(new Task(
                "Task 3 for " + username,
                "Description 3",
                TaskStatus.COMPLETED,
                LocalDate.now().plusDays(1),
                user
        ));
    }
}

