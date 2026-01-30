package com.javarush.taskmanager.service;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.entity.User;

public interface UserService {

    User register(String username, String password);

    User    getByUsername(String username);

    User getById(Long id);

    void changePassword(Long userId, String newPassword);

    void changeRole(Long userId, UserRole role);

    void deleteUser(Long id);
}
