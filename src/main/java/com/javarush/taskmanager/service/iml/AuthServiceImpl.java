package com.javarush.taskmanager.service.iml;

import com.javarush.taskmanager.exception.AuthenticationException;
import com.javarush.taskmanager.exception.InvalidTokenException;
import com.javarush.taskmanager.model.dto.AuthRequest;
import com.javarush.taskmanager.model.dto.AuthResponse;
import com.javarush.taskmanager.model.dto.UserRegistrationRequest;
import com.javarush.taskmanager.model.dto.UserRegistrationResponse;
import com.javarush.taskmanager.model.entity.RefreshToken;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.security.SecurityUser;
import com.javarush.taskmanager.security.jwt.JwtService;
import com.javarush.taskmanager.security.jwt.RefreshTokenService;
import com.javarush.taskmanager.service.AuthService;
import com.javarush.taskmanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(
            UserService userService,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            RefreshTokenService refreshTokenService
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        log.info("User registration started: username={}", request.getUsername());

        User user = userService.register(
                request.getUsername(),
                request.getPassword()
        );

        log.info("User registration completed: id={}, username={}",
                user.getId(), user.getUsername());

        return new UserRegistrationResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            log.info("User logged in: {}", securityUser.getUsername());

            User user = userService.getById(securityUser.getId());
            String accessToken = jwtService.generateAccessToken(securityUser);
            String refreshToken = jwtService.generateRefreshToken(securityUser);

            refreshTokenService.save(user, refreshToken);
            return new AuthResponse(accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid username or password");
        }
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        // Проверяем, что это refresh токен
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid token type");
        }

        RefreshToken tokenEntity = refreshTokenService.validate(refreshToken);
        SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(
                tokenEntity.getUser().getUsername()
        );

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);

        refreshTokenService.revoke(refreshToken);
        refreshTokenService.save(tokenEntity.getUser(), newRefresh);
        return new AuthResponse(newAccess, newRefresh);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}