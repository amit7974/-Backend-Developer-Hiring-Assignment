package com.example.productapi.service;

import com.example.productapi.dto.request.LoginRequest;
import com.example.productapi.dto.request.RefreshTokenRequest;
import com.example.productapi.dto.response.AuthResponse;
import com.example.productapi.entity.RefreshToken;
import com.example.productapi.entity.User;
import com.example.productapi.exception.TokenRefreshException;
import com.example.productapi.repository.RefreshTokenRepository;
import com.example.productapi.repository.UserRepository;
import com.example.productapi.security.JwtUtils;
import com.example.productapi.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);

        user = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .password("encodedPassword")
                .enabled(true)
                .roles(Collections.emptySet())
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin")
                .password("encodedPassword")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("login - should return AuthResponse with tokens")
    void login_shouldReturnAuthResponse() {
        LoginRequest request = LoginRequest.builder()
                .username("admin").password("Admin@123").build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("mock-jwt");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAll()).thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-jwt");
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("refreshToken - should return new tokens when valid")
    void refreshToken_shouldReturnNewTokens() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("old-refresh-token")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("new-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-jwt");
        assertThat(response.getRefreshToken()).isNotEqualTo("old-refresh-token");
    }

    @Test
    @DisplayName("refreshToken - should throw when token expired")
    void refreshToken_shouldThrow_whenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .user(user)
                .token("expired-token")
                .expiryDate(Instant.now().minusSeconds(100))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));
        doNothing().when(refreshTokenRepository).delete(expiredToken);

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(TokenRefreshException.class);
    }

    @Test
    @DisplayName("logout - should delete refresh token for user")
    void logout_shouldDeleteRefreshToken() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        doNothing().when(refreshTokenRepository).deleteByUser(user);

        assertThatCode(() -> authService.logout("admin")).doesNotThrowAnyException();
        verify(refreshTokenRepository).deleteByUser(user);
    }
}
