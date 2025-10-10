package com.willyes.clemenintegra.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Códigos estandarizados para responder errores funcionales de la API.
 */
public enum ApiErrorCode {

    BLOQUEO_RETENCION_NC(HttpStatus.CONFLICT),
    BLOQUEO_ESTADO_CUARENTENA(HttpStatus.CONFLICT),
    NC_ABIERTA(HttpStatus.CONFLICT),
    EVALUACIONES_FALTANTES(HttpStatus.UNPROCESSABLE_ENTITY),
    ROL_INSUFICIENTE(HttpStatus.FORBIDDEN),
    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    OPERACION_NO_PERMITIDA(HttpStatus.CONFLICT),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR),
    NEGOCIO_GENERICO(HttpStatus.BAD_REQUEST);

    private final HttpStatus httpStatus;

    ApiErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return name();
    }
}
