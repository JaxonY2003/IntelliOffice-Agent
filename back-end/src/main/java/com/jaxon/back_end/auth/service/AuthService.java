package com.jaxon.back_end.auth.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jaxon.back_end.auth.dto.LoginRequest;
import com.jaxon.back_end.auth.dto.LoginResponse;
import com.jaxon.back_end.security.CustomUserDetailsService;
import com.jaxon.back_end.security.JwtService;


@Service
public class AuthService {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public LoginResponse login(LoginRequest request){
        validateLoginRequest(request);

        String normalizedType = request.getType().trim().toUpperCase(Locale.ROOT);
        String normalizedUsername = request.getUsername().trim();
        String rawPassword = request.getPassword();

        UserDetails userDetails = customUserDetailsService.loadByTypeAndUsername(normalizedType, normalizedUsername);
        if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        String token = jwtService.generateToken(userDetails);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setType(normalizedType);
        response.setUsername(userDetails.getUsername());
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
}
