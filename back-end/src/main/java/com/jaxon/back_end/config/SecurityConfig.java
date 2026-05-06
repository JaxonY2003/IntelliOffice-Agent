package com.jaxon.back_end.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.jaxon.back_end.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    
}
