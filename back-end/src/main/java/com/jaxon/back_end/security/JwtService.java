package com.jaxon.back_end.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jaxon.back_end.common.login.LoginUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;



@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // 生成token
    public String generateToken(UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();

        claims.put("authorities", extractAuthorities(userDetails));
        claims.put("type", extractType(userDetails));
        claims.put("userId", extractUserId(userDetails));

        return Jwts.builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSignInKey())
            .compact();
    }

    // 从 token 中取出用户名
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    // 判断 token 是否有效
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        String type = extractType(token);

        return username.equals(userDetails.getUsername())
                && hasAuthorityForType(userDetails, type)
                && !isTokenExpired(token);
    }

    // 判断 token 是否过期
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // 解析 token 中的所有信息
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSignInKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }    
    // 获取签名密钥
    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(hashSecret(secretKey));
    }

    private List<String> extractAuthorities(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private String extractType(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("UserDetails has no role-based authority"));
    }

    private boolean hasAuthorityForType(UserDetails userDetails, String type) {
        if (type == null || type.isBlank()) {
            return false;
        }

        String expectedAuthority = "ROLE_" + type;
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expectedAuthority::equals);
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        throw new IllegalArgumentException("UserDetails is not an instance of LoginUser");
    }

    private byte[] hashSecret(String rawSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to initialize JWT signing key", e);
        }
    }
}
