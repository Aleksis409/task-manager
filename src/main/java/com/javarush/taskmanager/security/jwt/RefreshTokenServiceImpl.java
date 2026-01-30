package com.javarush.taskmanager.security.jwt;

import com.javarush.taskmanager.exception.InvalidTokenException;
import com.javarush.taskmanager.exception.TokenNotFoundException;
import com.javarush.taskmanager.model.entity.RefreshToken;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    public RefreshToken save(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(
                        Instant.now()
                                .plusMillis(refreshExpiration)
                )
                .revoked(false)
                .build();

        return repository.save(refreshToken);
    }

    @Override
    public RefreshToken validate(String token) {
        if (!jwtService.validateToken(token)) {
            throw new InvalidTokenException("Invalid token");
        }

        if (!jwtService.isRefreshToken(token)) {
            throw new InvalidTokenException("Not a refresh token");
        }

        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        return refreshToken;
    }

    @Override
    public void revoke(String token) {
        repository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    repository.save(rt);
                });
    }

    @Override
    public void revokeAll(User user) {
        repository.deleteAllByUser(user);
    }
}