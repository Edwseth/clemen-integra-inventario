package com.willyes.clemenintegra.shared.security.exception;

import org.springframework.security.core.AuthenticationException;

public class SesionInvalidadaAuthenticationException extends AuthenticationException {

    public SesionInvalidadaAuthenticationException(String message) {
        super(message);
    }

    public SesionInvalidadaAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
