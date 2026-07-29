package com.alumni.academic_management_api.exception;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
