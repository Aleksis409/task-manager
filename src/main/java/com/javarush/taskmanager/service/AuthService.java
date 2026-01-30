package com.javarush.taskmanager.service;

import com.javarush.taskmanager.model.dto.AuthRequest;
import com.javarush.taskmanager.model.dto.AuthResponse;
import com.javarush.taskmanager.model.dto.UserRegistrationRequest;
import com.javarush.taskmanager.model.dto.UserRegistrationResponse;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    AuthResponse refresh(String refreshToken);

    UserRegistrationResponse register(UserRegistrationRequest request);

    void logout(String refreshToken);
}
