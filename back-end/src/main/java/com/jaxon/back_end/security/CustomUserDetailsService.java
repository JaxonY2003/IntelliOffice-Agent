package com.jaxon.back_end.security;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jaxon.back_end.identity.entity.Admin;
import com.jaxon.back_end.identity.entity.Employee;
import com.jaxon.back_end.identity.entity.Manager;
import com.jaxon.back_end.identity.mapper.AdminMapper;
import com.jaxon.back_end.identity.mapper.EmployeeMapper;
import com.jaxon.back_end.identity.mapper.ManagerMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private static final String TYPE_SEPARATOR = ":";

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private ManagerMapper managerMapper;
    @Autowired
    private AdminMapper adminMapper;

    public UserDetails loadByTypeAndUsername(String type, String username) {
        String normalizedType = normalizeType(type);
        String normalizedUsername = normalizeUsername(username);

        return switch (normalizedType) {
            case "EMPLOYEE" -> employeeMapper.findByUsername(normalizedUsername)
                    .map(this::buildEmployeeUserDetails)
                    .orElseThrow(() -> userNotFound(normalizedType, normalizedUsername));
            case "MANAGER" -> managerMapper.findByUsername(normalizedUsername)
                    .map(this::buildManagerUserDetails)
                    .orElseThrow(() -> userNotFound(normalizedType, normalizedUsername));
            case "ADMIN" -> adminMapper.findByUsername(normalizedUsername)
                    .map(this::buildAdminUserDetails)
                    .orElseThrow(() -> userNotFound(normalizedType, normalizedUsername));
            default -> throw new UsernameNotFoundException("Unsupported user type: " + type);
        };
    }

    // Compatibility helper for flows that pass "TYPE:username" as a combined principal.
    public UserDetails loadUserByUsername(String combinedPrincipal) {
        if (combinedPrincipal == null || combinedPrincipal.isBlank()) {
            throw new UsernameNotFoundException("Principal must not be blank");
        }

        String[] parts = combinedPrincipal.split(TYPE_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Principal must be in the format TYPE:username");
        }

        return loadByTypeAndUsername(parts[0], parts[1]);
    }

    private UserDetails buildEmployeeUserDetails(Employee employee) {
        return User.withUsername(employee.getUsername())
                .password(employee.getPassword())
                .roles("EMPLOYEE")
                .build();
    }

    private UserDetails buildManagerUserDetails(Manager manager) {
        return User.withUsername(manager.getUsername())
                .password(manager.getPassword())
                .roles("MANAGER")
                .build();
    }

    private UserDetails buildAdminUserDetails(Admin admin) {
        return User.withUsername(admin.getUsername())
                .password(admin.getPassword())
                .roles("ADMIN")
                .build();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new UsernameNotFoundException("User type must not be blank");
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Username must not be blank");
        }
        return username.trim();
    }

    private UsernameNotFoundException userNotFound(String type, String username) {
        return new UsernameNotFoundException("User not found for type " + type + ": " + username);
    }
}
