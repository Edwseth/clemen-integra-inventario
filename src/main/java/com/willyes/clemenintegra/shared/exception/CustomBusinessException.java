package com.willyes.clemenintegra.shared.exception;

/**
 * Excepción de negocio genérica para representar condiciones
 * que no cumplen reglas específicas del dominio.
 */
public class CustomBusinessException extends RuntimeException {

    private final ApiErrorCode code;
    private final transient Object details;

    public CustomBusinessException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public CustomBusinessException(ApiErrorCode code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public CustomBusinessException(String message) {
        super(message);
        this.code = ApiErrorCode.NEGOCIO_GENERICO;
        this.details = null;
    }

    public CustomBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ApiErrorCode.NEGOCIO_GENERICO;
        this.details = null;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }
}
