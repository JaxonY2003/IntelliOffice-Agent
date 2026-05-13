package com.jaxon.back_end.refresh.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

import com.jaxon.back_end.common.login.LoginUser;
import com.jaxon.back_end.refresh.entity.RefreshToken;
import com.jaxon.back_end.refresh.mapper.RefreshTokenMapper;

@Service
public class RefreshTokenService {
    
    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    public RefreshToken findByUserIdAndUserType(){
        LoginUser currentUser = getCurrentLoginUser();
        return refreshTokenMapper.findByUserIdAndUserType(currentUser.getUserId(), currentUser.getUserType());
    }

    public RefreshToken findByRefreshHash(String refreshToken){
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }

        return refreshTokenMapper.findByRefreshHash(hashRefreshToken(refreshToken));
    }

    public RefreshToken insertRefreshToken(RefreshToken refreshToken) {
        validateRefreshTokenEntity(refreshToken);

        int affectedRows = refreshTokenMapper.insertRefreshToken(refreshToken);
        if (affectedRows != 1 || refreshToken.getId() == null) {
            throw new IllegalStateException("Failed to insert refresh token");
        }
        return refreshToken;
    }

    public RefreshToken updateRefreshToken(RefreshToken refreshToken) {
        validateRefreshTokenEntity(refreshToken);
        if (refreshToken.getId() == null) {
            throw new IllegalArgumentException("refreshToken id must not be null");
        }

        int affectedRows = refreshTokenMapper.updateRefreshToken(refreshToken);
        if (affectedRows != 1) {
            throw new IllegalStateException("Failed to update refresh token");
        }
        return refreshToken;
    }

    public void revokeRefreshTokenByTokenHash(String refreshToken){
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }

        refreshTokenMapper.revokeRefreshTokenByTokenHash(hashRefreshToken(refreshToken));
    }

    public String createOrUpdateRefreshToken(LoginUser loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("loginUser must not be null");
        }

        String rawRefreshToken = generateRefreshToken();
        String tokenHash = hashRefreshToken(rawRefreshToken);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusNanos(refreshExpiration * 1_000_000L);

        RefreshToken existingToken =
                refreshTokenMapper.findByUserIdAndUserType(loginUser.getUserId(), loginUser.getUserType());

        if (existingToken == null) {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUserId(loginUser.getUserId());
            refreshToken.setUserType(loginUser.getUserType());
            refreshToken.setTokenHash(tokenHash);
            refreshToken.setExpiresAt(expiresAt);
            refreshToken.setRevoked(false);
            refreshToken.setRevokedAt(null);
            refreshToken.setLastUsedAt(now);
            insertRefreshToken(refreshToken);
        } else {
            existingToken.setTokenHash(tokenHash);
            existingToken.setExpiresAt(expiresAt);
            existingToken.setRevoked(false);
            existingToken.setRevokedAt(null);
            existingToken.setLastUsedAt(now);
            updateRefreshToken(existingToken);
        }

        return rawRefreshToken;
    }

    private LoginUser getCurrentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Current user is not authenticated");
        }
        if (!(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new AccessDeniedException("Current principal is not a valid login user");
        }
        return loginUser;
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String toHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private void validateRefreshTokenEntity(RefreshToken refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("refreshToken must not be null");
        }
        if (refreshToken.getUserId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (refreshToken.getUserType() == null || refreshToken.getUserType().isBlank()) {
            throw new IllegalArgumentException("userType must not be blank");
        }
        if (refreshToken.getTokenHash() == null || refreshToken.getTokenHash().isBlank()) {
            throw new IllegalArgumentException("tokenHash must not be blank");
        }
        if (refreshToken.getExpiresAt() == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
        if (refreshToken.getRevokedAt() != null && refreshToken.getRevokedAt().isAfter(LocalDateTime.now().plusSeconds(1))) {
            throw new IllegalArgumentException("revokedAt is invalid");
        }
    }
}
