package com.jaxon.back_end.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jaxon.back_end.auth.dto.LoginRequest;
import com.jaxon.back_end.auth.dto.LoginResponse;
import com.jaxon.back_end.auth.dto.LogoutRequest;
import com.jaxon.back_end.auth.dto.RefreshTokenRequest;
import com.jaxon.back_end.auth.dto.RefreshTokenResponse;
import com.jaxon.back_end.common.login.LoginUser;
import com.jaxon.back_end.refresh.entity.RefreshToken;
import com.jaxon.back_end.refresh.service.RefreshTokenService;
import com.jaxon.back_end.security.CustomUserDetailsService;
import com.jaxon.back_end.security.JwtService;


@Service
public class AuthService {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public LoginResponse login(LoginRequest request){
        validateLoginRequest(request);

        String normalizedType = request.getType().trim().toUpperCase(Locale.ROOT);
        String normalizedUsername = request.getUsername().trim();
        String rawPassword = request.getPassword();

        LoginUser loginUser = customUserDetailsService.loadByTypeAndUsername(normalizedType, normalizedUsername);
        if (!passwordEncoder.matches(rawPassword, loginUser.getPassword())) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        String token = jwtService.generateToken(loginUser);
        String refreshToken = refreshTokenService.createOrUpdateRefreshToken(loginUser);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setType(normalizedType);
        response.setUsername(loginUser.getUsername());
        return response;
    }

    public void logout(LogoutRequest request){
        refreshTokenService.revokeRefreshTokenByTokenHash(request.getRefreshToken());
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request){
        validateRefreshTokenRequest(request);

        RefreshToken refreshToken = refreshTokenService.findByRefreshHash(request.getRefreshToken());
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token is invalid");
        }
        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        LoginUser loginUser = customUserDetailsService.loadByTypeAndUserId(
                refreshToken.getUserType(),
                refreshToken.getUserId());

        String newAccessToken = jwtService.generateToken(loginUser);
        String newRefreshToken = refreshTokenService.createOrUpdateRefreshToken(loginUser);

        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        return response;
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request must not be null");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("User type must not be blank");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
    }

    private void validateRefreshTokenRequest(RefreshTokenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Refresh token request must not be null");
        }
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
    }
}
