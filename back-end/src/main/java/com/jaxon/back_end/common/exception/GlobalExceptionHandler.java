package com.jaxon.back_end.common.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jaxon.back_end.common.result.Result;
import com.jaxon.back_end.common.result.ResultCodeEnum;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e){
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), ResultCodeEnum.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<?> handleUsernameNotFoundException(UsernameNotFoundException e){
        return Result.fail(ResultCodeEnum.ADMIN_ACCOUNT_NOT_EXIST_ERROR.getCode(), ResultCodeEnum.ADMIN_ACCOUNT_NOT_EXIST_ERROR.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Result<?> handleBadCredentialsException(BadCredentialsException e){
        return Result.fail(ResultCodeEnum.ADMIN_ACCOUNT_ERROR.getCode(), ResultCodeEnum.ADMIN_ACCOUNT_ERROR.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e){
        return Result.fail(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e){
        return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), ResultCodeEnum.SERVICE_ERROR.getMessage());
    }
}
