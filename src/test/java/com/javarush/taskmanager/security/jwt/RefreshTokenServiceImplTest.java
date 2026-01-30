package com.javarush.taskmanager.security.jwt;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.entity.RefreshToken;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.javarush.taskmanager.exception.TokenNotFoundException;
import com.javarush.taskmanager.exception.InvalidTokenException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    private final long refreshExpiration = 86_400_000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                refreshTokenService,
                "refreshExpiration",
                refreshExpiration
        );

        testUser = new User("testuser", "password", UserRole.ROLE_USER);

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("test-refresh-token")
                .user(testUser)
                .expiresAt(Instant.now().plusMillis(refreshExpiration))
                .revoked(false)
                .build();
    }

    @Test
    void save_ShouldSaveRefreshTokenWithCorrectProperties() {
        String tokenValue = "new-refresh-token";
        when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken saved = refreshTokenService.save(testUser, tokenValue);

        assertNotNull(saved);
        assertEquals(tokenValue, saved.getToken());
        assertEquals(testUser, saved.getUser());
        assertFalse(saved.isRevoked());

        verify(refreshTokenRepository).save(any());
    }

    @Test
    void save_ShouldCalculateExpirationCorrectly() {
        Instant start = Instant.now();
        when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken saved = refreshTokenService.save(testUser, "token");
        long diff = Math.abs(saved.getExpiresAt().toEpochMilli() - start.plusMillis(refreshExpiration).toEpochMilli());
        assertTrue(diff < 1000);
    }

    @Test
    void save_ShouldHandleNullToken() {
        when(refreshTokenRepository.save(any())).thenReturn(testRefreshToken);
        RefreshToken saved = refreshTokenService.save(testUser, null);
        assertNotNull(saved);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void validate_ShouldReturnTokenWhenValid() {
        String token = "valid-token";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.isRefreshToken(token)).thenReturn(true);
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(testRefreshToken));

        RefreshToken result = refreshTokenService.validate(token);
        assertEquals(testRefreshToken, result);
    }

    @Test
    void validate_ShouldThrowWhenTokenNotFound() {
        String token = "missing";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.isRefreshToken(token)).thenReturn(true);

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());
        assertThrows(TokenNotFoundException.class, () -> refreshTokenService.validate(token));
    }

    @Test
    void validate_ShouldThrowWhenRevoked() {
        RefreshToken revoked =
                new RefreshToken(
                        1L,
                        "revoked",
                        testUser,
                        Instant.now().plusMillis(refreshExpiration),
                        true
                );

        when(jwtService.validateToken("revoked")).thenReturn(true);
        when(jwtService.isRefreshToken("revoked")).thenReturn(true);
        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(revoked));
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.validate("revoked"));
    }

    @Test
    void validate_ShouldThrowWhenExpired() {
        RefreshToken expired =
                new RefreshToken(
                        1L,
                        "expired",
                        testUser,
                        Instant.now().minusSeconds(10),
                        false
                );

        when(jwtService.validateToken("expired")).thenReturn(true);
        when(jwtService.isRefreshToken("expired")).thenReturn(true);
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.validate("expired"));
    }

    @Test
    void validate_ShouldThrowWhenJwtInvalid() {

        when(jwtService.validateToken("bad")).thenReturn(false);
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.validate("bad"));
        verify(refreshTokenRepository, never()).findByToken(any());
    }

    @Test
    void validate_ShouldThrowWhenNotRefreshToken() {
        when(jwtService.validateToken("access")).thenReturn(true);
        when(jwtService.isRefreshToken("access")).thenReturn(false);
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.validate("access"));
    }

    @Test
    void revoke_ShouldMarkTokenAsRevoked() {
        when(refreshTokenRepository.findByToken("x")).thenReturn(Optional.of(testRefreshToken));
        refreshTokenService.revoke("x");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertTrue(captor.getValue().isRevoked());
    }

    @Test
    void revoke_ShouldDoNothingWhenNotFound() {
        when(refreshTokenRepository.findByToken("x")).thenReturn(Optional.empty());
        refreshTokenService.revoke("x");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAll_ShouldDeleteAllForUser() {
        refreshTokenService.revokeAll(testUser);
        verify(refreshTokenRepository).deleteAllByUser(testUser);
    }
}