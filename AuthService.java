package com.example.productapi.service;

import com.example.productapi.dto.request.LoginRequest;
import com.example.productapi.dto.request.RefreshTokenRequest;
import com.example.productapi.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String username);
}
