package com.jaxon.back_end.security;

import java.util.Locale;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jaxon.back_end.common.login.LoginUser;
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

    private final EmployeeMapper employeeMapper;
    private final ManagerMapper managerMapper;
    private final AdminMapper adminMapper;

    public LoginUser loadByTypeAndUsername(String type, String username) {
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

    public LoginUser loadByTypeAndUserId(String type, Long userId) {
        String normalizedType = normalizeType(type);
        if (userId == null) {
            throw new UsernameNotFoundException("User id must not be null");
        }

        return switch (normalizedType) {
            case "EMPLOYEE" -> {
                Employee employee = employeeMapper.selectById(userId);
                if (employee == null) {
                    throw userNotFoundById(normalizedType, userId);
                }
                yield buildEmployeeUserDetails(employee);
            }
            case "MANAGER" -> {
                Manager manager = managerMapper.selectById(userId);
                if (manager == null) {
                    throw userNotFoundById(normalizedType, userId);
                }
                yield buildManagerUserDetails(manager);
            }
            case "ADMIN" -> {
                Admin admin = adminMapper.selectById(userId);
                if (admin == null) {
                    throw userNotFoundById(normalizedType, userId);
                }
                yield buildAdminUserDetails(admin);
            }
            default -> throw new UsernameNotFoundException("Unsupported user type: " + type);
        };
    }

    // Compatibility helper for flows that pass "TYPE:username" as a combined principal.
    public LoginUser loadUserByUsername(String combinedPrincipal) {
        if (combinedPrincipal == null || combinedPrincipal.isBlank()) {
            throw new UsernameNotFoundException("Principal must not be blank");
        }

        String[] parts = combinedPrincipal.split(TYPE_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Principal must be in the format TYPE:username");
        }

        return loadByTypeAndUsername(parts[0], parts[1]);
    }

    private LoginUser buildEmployeeUserDetails(Employee employee) {
        return buildLoginUser(employee.getId(), employee.getUsername(), employee.getPassword(), "EMPLOYEE");
    }

    private LoginUser buildManagerUserDetails(Manager manager) {
        return buildLoginUser(manager.getId(), manager.getUsername(), manager.getPassword(), "MANAGER");
    }

    private LoginUser buildAdminUserDetails(Admin admin) {
        return buildLoginUser(admin.getId(), admin.getUsername(), admin.getPassword(), "ADMIN");
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

    private UsernameNotFoundException userNotFoundById(String type, Long userId) {
        return new UsernameNotFoundException("User not found for type " + type + " and id " + userId);
    }

    private LoginUser buildLoginUser(Long userId, String username, String password, String userType) {
        return LoginUser.builder()
                .userId(userId)
                .username(username)
                .password(password)
                .userType(userType)
                .authorities(AuthorityUtils.createAuthorityList("ROLE_" + userType))
                .build();
    }
}
