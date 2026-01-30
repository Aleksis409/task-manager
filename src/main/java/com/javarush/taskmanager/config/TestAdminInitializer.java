package com.javarush.taskmanager.config;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class TestAdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        log.info("Initializing test database...");

        cleanDatabase();
        createTestAdmin();

        log.info("Test database initialized successfully");
    }

    private void cleanDatabase() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SET session_replication_role = 'replica'");

            statement.execute("DELETE FROM tasks");
            statement.execute("DELETE FROM refresh_tokens");
            statement.execute("DELETE FROM users");

            statement.execute("SET session_replication_role = 'origin'");

            statement.execute("ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1");
            statement.execute("ALTER SEQUENCE IF EXISTS tasks_id_seq RESTART WITH 1");
            statement.execute("ALTER SEQUENCE IF EXISTS refresh_tokens_id_seq RESTART WITH 1");

            log.info("Database cleaned successfully");

        } catch (Exception e) {
            log.error("Error cleaning database: {}", e.getMessage());
        }
    }

    private void createTestAdmin() {
        if (userRepository.existsByUsername("admin")) {
            log.debug("TEST ADMIN already exists");
            return;
        }

        try {
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("password"),
                    UserRole.ROLE_ADMIN
            );

            User savedAdmin = userRepository.save(admin);
            log.info("TEST ADMIN created with id: {}", savedAdmin.getId());
        } catch (Exception e) {
            log.error("Failed to create test admin: {}", e.getMessage());
        }
    }
}

