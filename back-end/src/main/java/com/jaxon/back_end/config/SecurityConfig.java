package com.jaxon.back_end.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaxon.back_end.common.result.Result;
import com.jaxon.back_end.common.result.ResultCodeEnum;
import com.jaxon.back_end.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 前后端分离 + JWT， 一般关闭 CSRF
            .csrf(csrf -> csrf.disable())
            // 如果前后端端口不同，需要开启CORS
            .cors(Customizer.withDefaults())
            // JWT 项目不适用 Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 认证请求放行
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                // 如果你有 Swagger，也可以放行
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // admin 接口：只能 admin 访问
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // manager 接口: manager 和 admin 可以访问
                .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                // employee 接口: employee 可以访问
                .requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")
                // 除上述路径外，其他路径都需要认证
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            Object jwtError = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);

            ResultCodeEnum resultCodeEnum = jwtError instanceof ResultCodeEnum
                    ? (ResultCodeEnum) jwtError
                    : ResultCodeEnum.ADMIN_LOGIN_AUTH;

            writeJsonResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    Result.fail(resultCodeEnum.getCode(), resultCodeEnum.getMessage()));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeJsonResponse(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                Result.fail(
                        ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(),
                        ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getMessage()));
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    private void writeJsonResponse(HttpServletResponse response, int status, Result<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
    
}
