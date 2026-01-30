package com.javarush.taskmanager.service.iml;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.exception.AuthenticationException;
import com.javarush.taskmanager.exception.InvalidTokenException;
import com.javarush.taskmanager.model.dto.AuthRequest;
import com.javarush.taskmanager.model.dto.UserRegistrationRequest;
import com.javarush.taskmanager.model.entity.RefreshToken;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.security.SecurityUser;
import com.javarush.taskmanager.security.jwt.JwtService;
import com.javarush.taskmanager.security.jwt.RefreshTokenService;
import com.javarush.taskmanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.javarush.taskmanager.model.dto.AuthResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private SecurityUser securityUser;

    private final Long userId = 1L;
    private final String username = "testuser";
    private final String password = "password123";

    private final String accessToken = "access.token";
    private final String refreshToken = "refresh.token";

    @BeforeEach
    void setUp() {
        testUser = new User(username, "encodedPassword", UserRole.ROLE_USER);
        setId(testUser, userId);

        securityUser = new SecurityUser(testUser);
    }

    @Test
    void register_success() {
        when(userService.register(username, password)).thenReturn(testUser);
        UserRegistrationRequest request = new UserRegistrationRequest(username, password);
        var response = authService.register(request);
        assertEquals(userId, response.getId());
        assertEquals(username, response.getUsername());
        verify(userService).register(username, password);
    }

    @Test
    void login_success() {
        AuthRequest request = new AuthRequest(username, password);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(userService.getById(userId)).thenReturn(testUser);
        when(jwtService.generateAccessToken(securityUser)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(securityUser)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);
        assertEquals(accessToken, response.getToken());
        assertEquals(refreshToken, response.getRefreshToken());
        verify(refreshTokenService).save(testUser, refreshToken);
    }

    @Test
    void login_invalidPassword() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        AuthRequest request = new AuthRequest(username, "bad");
        assertThrows(AuthenticationException.class, () -> authService.login(request));
    }

    @Test
    void refresh_success() {
        String oldToken = "old";
        RefreshToken tokenEntity = mock(RefreshToken.class);
        when(jwtService.isRefreshToken(oldToken)).thenReturn(true);
        when(refreshTokenService.validate(oldToken)).thenReturn(tokenEntity);
        when(tokenEntity.getUser()).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(securityUser);
        when(jwtService.generateAccessToken(securityUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(securityUser)).thenReturn("new-refresh");

        var response = authService.refresh(oldToken);
        assertEquals("new-access", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(refreshTokenService).revoke(oldToken);
        verify(refreshTokenService).save(testUser, "new-refresh");
    }

    @Test
    void refresh_notRefreshToken() {
        when(jwtService.isRefreshToken("x")).thenReturn(false);
        assertThrows(InvalidTokenException.class, () -> authService.refresh("x"));
        verify(refreshTokenService, never()).validate(any());
    }

    @Test
    void refresh_userNotFound() {
        when(jwtService.isRefreshToken("x")).thenReturn(true);
        RefreshToken token = mock(RefreshToken.class);
        when(refreshTokenService.validate("x")).thenReturn(token);
        when(token.getUser()).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> authService.refresh("x"));
    }

    @Test
    void logout_success() {
        doNothing().when(refreshTokenService).revoke("t");
        authService.logout("t");
        verify(refreshTokenService).revoke("t");
    }

    private void setId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id via reflection", e);
        }
    }
}
