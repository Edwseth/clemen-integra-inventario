package com.willyes.clemenintegra.shared.exception;

/**
 * Excepción de negocio genérica para representar condiciones
 * que no cumplen reglas específicas del dominio.
 */
public class CustomBusinessException extends RuntimeException {
    public CustomBusinessException(String message) {
        super(message);
    }

    public CustomBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
