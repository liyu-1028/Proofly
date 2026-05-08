package com.lyllink.proofly.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public BusinessException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message, HttpStatus.CONFLICT);
    }
}
