package com.javarush.taskmanager.security.jwt;

import com.javarush.taskmanager.model.entity.RefreshToken;
import com.javarush.taskmanager.model.entity.User;

public interface RefreshTokenService {

    RefreshToken save(User user, String token);

    RefreshToken validate(String token);

    void revoke(String token);

    void revokeAll(User user);
}
