package com.javarush.taskmanager.repository;

import com.javarush.taskmanager.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     * Used for authentication and registration checks.
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a user with the given username already exists.
     */
    boolean existsByUsername(String username);
}