package com.javarush.taskmanager.security.jwt;

import com.javarush.taskmanager.enums.UserRole;
import com.javarush.taskmanager.model.entity.User;
import com.javarush.taskmanager.security.SecurityUser;
import com.javarush.taskmanager.security.SecurityUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
        User user = new User("testuser", "password", UserRole.ROLE_USER);
        securityUser = new SecurityUser(user);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldContinueWhenNoAuthorizationHeader() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilterInternal_shouldContinueWhenHeaderNotBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic abc");
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilterInternal_shouldAuthenticateWhenTokenValid() throws ServletException, IOException {
        String token = "jwt-token";
        String username = "testuser";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(securityUser);
        when(jwtService.isTokenValid(token, securityUser)).thenReturn(true);
        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals(securityUser, auth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateWhenTokenInvalid() throws ServletException, IOException {
        String token = "jwt-token";
        String username = "testuser";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(securityUser);
        when(jwtService.isTokenValid(token, securityUser)).thenReturn(false);

        filter.doFilter(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueWhenJwtThrowsException() throws ServletException, IOException {
        String token = "jwt-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("bad token"));

        filter.doFilter(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueWhenUserServiceFails() throws ServletException, IOException {
        String token = "jwt-token";
        String username = "testuser";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException("User not found"));

        filter.doFilter(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSkipWhenAuthenticationAlreadyExists() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken existing =
                new UsernamePasswordAuthenticationToken("existing", null);

        SecurityContextHolder.getContext().setAuthentication(existing);
        String token = "jwt-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("user");

        filter.doFilter(request, response, filterChain);
        assertEquals(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(token);
        verifyNoMoreInteractions(userDetailsService);
    }
}
