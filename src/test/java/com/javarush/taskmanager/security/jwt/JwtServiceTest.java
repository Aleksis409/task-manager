package com.javarush.taskmanager.security.jwt;

import com.javarush.taskmanager.security.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private SecurityUser securityUser;

    private final String secretKey = "testSecretKey1234567890123456789012345678901234567890ABCDEF";
    private final long accessExpiration = 3600000;
    private final long refreshExpiration = 86400000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secretKey, accessExpiration, refreshExpiration);
    }

    @Test
    void generateAccessToken_ShouldReturnValidToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(securityUser);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateRefreshToken(securityUser);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateAccessToken_ShouldHaveCorrectSubject() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String expectedUsername = "testuser";
        String token = jwtService.generateAccessToken(securityUser);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(expectedUsername, extractedUsername);
    }

    @Test
    void generateRefreshToken_ShouldHaveCorrectSubject() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String expectedUsername = "testuser";
        String token = jwtService.generateRefreshToken(securityUser);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(expectedUsername, extractedUsername);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsernameForValidToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(securityUser);
        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void extractUsername_ShouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertThrows(MalformedJwtException.class, () ->
                jwtService.extractUsername(invalidToken));
    }

    @Test
    void extractUsername_ShouldThrowExceptionForTamperedToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String validToken = jwtService.generateAccessToken(securityUser);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";
        assertThrows(SignatureException.class, () ->
                jwtService.extractUsername(tamperedToken));
    }

    @Test
    void isTokenValid_ShouldReturnTrueForValidToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(securityUser);
        boolean isValid = jwtService.isTokenValid(token, securityUser);
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalseForExpiredToken() throws Exception {
        when(securityUser.getUsername()).thenReturn("testuser");
        JwtService shortLivedJwtService = new JwtService(
                secretKey,
                1,
                refreshExpiration
        );
        String token = shortLivedJwtService.generateAccessToken(securityUser);
        Thread.sleep(10);
        boolean isValid = shortLivedJwtService.isTokenValid(token, securityUser);
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalseForDifferentUser() {
        String token = jwtService.generateAccessToken(securityUser);
        SecurityUser differentUser = mock(SecurityUser.class);
        when(differentUser.getUsername()).thenReturn("differentuser");
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.token.here";
        boolean isValid = jwtService.isTokenValid(invalidToken, securityUser);
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalseForEmptyToken() {
        String emptyToken = "";
        boolean isValid = jwtService.isTokenValid(emptyToken, securityUser);
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalseForNullToken() {
        String nullToken = null;
        boolean isValid = jwtService.isTokenValid(nullToken, securityUser);
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalseForNullUser() {
        String token = jwtService.generateAccessToken(securityUser);
        boolean isValid = jwtService.isTokenValid(token, null);
        assertFalse(isValid);
    }

    @Test
    void accessToken_ShouldHaveCorrectExpiration() throws Exception {
        String token = jwtService.generateAccessToken(securityUser);
        Field secretKeyField = JwtService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        Key key = (Key) secretKeyField.get(jwtService);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();
        long actualTtl = expiration.getTime() - issuedAt.getTime();
        long tolerance = 1000;

        assertTrue(Math.abs(actualTtl - accessExpiration) <= tolerance,
                String.format("Expected TTL ~%d ms, but got %d ms",
                        accessExpiration, actualTtl));
    }

    @Test
    void refreshToken_ShouldHaveCorrectExpiration() throws Exception {
        String token = jwtService.generateRefreshToken(securityUser);
        Field secretKeyField = JwtService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        Key key = (Key) secretKeyField.get(jwtService);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();
        long actualTtl = expiration.getTime() - issuedAt.getTime();
        long tolerance = 1000;

        assertTrue(Math.abs(actualTtl - refreshExpiration) <= tolerance,
                String.format("Expected TTL ~%d ms, but got %d ms",
                        refreshExpiration, actualTtl));
    }

    @Test
    void tokens_ShouldHaveDifferentExpirationTimes() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String accessToken = jwtService.generateAccessToken(securityUser);
        String refreshToken = jwtService.generateRefreshToken(securityUser);
        try {
            Field secretKeyField = JwtService.class.getDeclaredField("secretKey");
            secretKeyField.setAccessible(true);
            Key key = (Key) secretKeyField.get(jwtService);

            Claims accessClaims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

            Claims refreshClaims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            long accessExp = accessClaims.getExpiration().getTime();
            long refreshExp = refreshClaims.getExpiration().getTime();

            assertTrue(refreshExp > accessExp,
                    "Refresh token should expire later than access token");
            long difference = refreshExp - accessExp;
            long expectedDifference = refreshExpiration - accessExpiration;
            long tolerance = 1000;

            assertTrue(Math.abs(difference - expectedDifference) < tolerance,
                    String.format("Expected difference ~%d ms, but got %d ms",
                            expectedDifference, difference));

        } catch (Exception e) {
            fail("Failed to parse token claims: " + e.getMessage());
        }
    }

    @Test
    void constructor_ShouldInitializeWithProvidedValues() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String testSecret = "anotherSecretKeyForTestingtest1234567890123456789012345678901234567890ABCDEF";
        long testAccessExp = 1800000;
        long testRefreshExp = 172800000;
        JwtService customJwtService = new JwtService(testSecret, testAccessExp, testRefreshExp);

        String token = customJwtService.generateAccessToken(securityUser);
        assertNotNull(token);
        assertTrue(customJwtService.isTokenValid(token, securityUser));
    }

    @Test
    void constructor_ShouldHandleEmptySecret() {
        String emptySecret = "";
        assertThrows(io.jsonwebtoken.security.WeakKeyException.class, () ->
                new JwtService(emptySecret, accessExpiration, refreshExpiration));
    }

    @Test
    void constructor_ShouldHandleVeryShortSecret() {
        String shortSecret = "short";
        assertThrows(io.jsonwebtoken.security.WeakKeyException.class, () ->
                new JwtService(shortSecret, accessExpiration, refreshExpiration));
    }

    @Test
    void buildToken_ShouldIncludeAllRequiredClaims() throws Exception {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(securityUser);
        Field secretKeyField = JwtService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        Key key = (Key) secretKeyField.get(jwtService);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertNotNull(claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        assertEquals("testuser", claims.getSubject());
        assertTrue(claims.getIssuedAt().before(new Date()));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void shouldHandleVeryLongUsername() {
        JwtService jwtService = new JwtService(secretKey, accessExpiration, refreshExpiration);
        SecurityUser user = mock(SecurityUser.class);
        String longUsername = "user".repeat(100);
        when(user.getUsername()).thenReturn(longUsername);
        String token = jwtService.generateAccessToken(user);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(longUsername, extractedUsername);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name",
            "user_name",
            "user-name",
            "user123",
            "User",
            "USER",
            "u"
    })
    void shouldHandleVariousUsernameFormats(String username) {
        JwtService jwtService = new JwtService(secretKey, accessExpiration, refreshExpiration);
        SecurityUser user = mock(SecurityUser.class);
        when(user.getUsername()).thenReturn(username);
        String token = jwtService.generateAccessToken(user);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void shouldHandleZeroExpiration() {
        JwtService jwtService = new JwtService(secretKey, 0, 0);
        SecurityUser user = mock(SecurityUser.class);
        when(user.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldHandleNegativeExpiration() {
        JwtService jwtService = new JwtService(secretKey, -1000, -1000);
        SecurityUser user = mock(SecurityUser.class);
        when(user.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertFalse(jwtService.isTokenValid(token, user));
    }

    @ParameterizedTest
    @ValueSource(longs = {
            2000L,                    // 1 sec
            60000L,                   // 1 min
            3600000L,                 // 1 h
            86400000L,                // 24 h
            31536000000L,             // 1 yar
    })
    void shouldHandleVariousExpirationValues(long expiration) {
        JwtService jwtService = new JwtService(secretKey, expiration, expiration);
        SecurityUser user = mock(SecurityUser.class);
        when(user.getUsername()).thenReturn("testuser");

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void extractUserId_ShouldReturnCorrectUserId() {
        when(securityUser.getUsername()).thenReturn("testuser");
        when(securityUser.getId()).thenReturn(42L);

        String token = jwtService.generateAccessToken(securityUser);
        Long userId = jwtService.extractUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    void extractJti_ShouldReturnJtiFromRefreshToken() {
        when(securityUser.getUsername()).thenReturn("testuser");

        String token = jwtService.generateRefreshToken(securityUser);
        String jti = jwtService.extractJti(token);
        assertNotNull(jti);
        assertFalse(jti.isBlank());
    }

    @Test
    void isRefreshToken_ShouldReturnTrueForRefreshToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateRefreshToken(securityUser);
        assertTrue(jwtService.isRefreshToken(token));
    }

    @Test
    void isRefreshToken_ShouldReturnFalseForAccessToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(securityUser);
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        when(securityUser.getUsername()).thenReturn("testuser");
        String token = jwtService.generateAccessToken(securityUser);
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalseForMalformedToken() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_ShouldReturnFalseForExpiredToken() throws Exception {
        when(securityUser.getUsername()).thenReturn("testuser");

        JwtService shortJwt = new JwtService(secretKey, 1, refreshExpiration);
        String token = shortJwt.generateAccessToken(securityUser);
        Thread.sleep(5);
        assertFalse(shortJwt.validateToken(token));
    }

}