package com.example.productapi.service.impl;

import com.example.productapi.dto.request.LoginRequest;
import com.example.productapi.dto.request.RefreshTokenRequest;
import com.example.productapi.dto.response.AuthResponse;
import com.example.productapi.entity.RefreshToken;
import com.example.productapi.entity.User;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.exception.TokenRefreshException;
import com.example.productapi.repository.RefreshTokenRepository;
import com.example.productapi.repository.UserRepository;
import com.example.productapi.security.JwtUtils;
import com.example.productapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtUtils jwtUtils;
        private final UserDetailsService userDetailsService;
        private final RefreshTokenRepository refreshTokenRepository;
        private final UserRepository userRepository;

        @Value("${app.jwt.refresh-expiration-ms}")
        private Long refreshExpirationMs;

        @Override
        @Transactional
        public AuthResponse login(LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String accessToken = jwtUtils.generateToken(userDetails);
                RefreshToken refreshToken = createOrUpdateRefreshToken(request.getUsername());

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .tokenType("Bearer")
                                .username(userDetails.getUsername())
                                .build();
        }

        @Override
        @Transactional
        public AuthResponse refreshToken(RefreshTokenRequest request) {
                RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                                .orElseThrow(() -> new TokenRefreshException(request.getRefreshToken(),
                                                "Refresh token not found"));

                if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(refreshToken);
                        throw new TokenRefreshException(request.getRefreshToken(), "Refresh token has expired");
                }

                // Rotate: issue a new refresh token
                String newRawToken = UUID.randomUUID().toString();
                refreshToken.setToken(newRawToken);
                refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
                refreshTokenRepository.save(refreshToken);

                UserDetails userDetails = userDetailsService.loadUserByUsername(refreshToken.getUser().getUsername());
                String newAccessToken = jwtUtils.generateToken(userDetails);

                return AuthResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRawToken)
                                .tokenType("Bearer")
                                .username(userDetails.getUsername())
                                .build();
        }

        @Override
        @Transactional
        public void logout(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
                refreshTokenRepository.deleteByUser(user);
                log.info("User {} logged out successfully", username);
        }

        private RefreshToken createOrUpdateRefreshToken(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

                // Use the OneToOne relationship: the user field is the FK key
                RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                                .orElse(RefreshToken.builder().user(user).build());

                refreshToken.setToken(UUID.randomUUID().toString());
                refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
                return refreshTokenRepository.save(refreshToken);
        }
}
