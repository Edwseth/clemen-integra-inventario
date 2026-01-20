package com.willyes.clemenintegra.shared.security.exception;

import org.springframework.security.core.AuthenticationException;

public class SesionExpiradaAuthenticationException extends AuthenticationException {

    public SesionExpiradaAuthenticationException(String message) {
        super(message);
    }

    public SesionExpiradaAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
