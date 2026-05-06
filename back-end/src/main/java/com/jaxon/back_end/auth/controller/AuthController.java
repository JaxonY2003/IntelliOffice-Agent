package com.jaxon.back_end.auth.controller;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.back_end.auth.dto.LoginRequest;
import com.jaxon.back_end.auth.dto.LoginResponse;
import com.jaxon.back_end.auth.service.AuthService;
import com.jaxon.back_end.common.result.Result;
import com.jaxon.back_end.common.result.ResultCodeEnum;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request){
        try {
            return Result.ok(authService.login(request));
        } catch (IllegalArgumentException e) {
            return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), e.getMessage());
        } catch (UsernameNotFoundException | BadCredentialsException e) {
            return Result.fail(ResultCodeEnum.ADMIN_ACCOUNT_ERROR.getCode(), ResultCodeEnum.ADMIN_ACCOUNT_ERROR.getMessage());
        }
    }
}
