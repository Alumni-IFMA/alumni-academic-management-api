package com.alumni.academic_management_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class TokenGoneException extends RuntimeException {

    public TokenGoneException(String message) {
        super(message);
    }
}
