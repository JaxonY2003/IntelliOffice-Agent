package com.jaxon.back_end.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.back_end.auth.dto.LoginRequest;
import com.jaxon.back_end.auth.dto.LoginResponse;
import com.jaxon.back_end.auth.service.AuthService;
import com.jaxon.back_end.common.result.Result;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request){
        return Result.ok(authService.login(request));
    }
}
