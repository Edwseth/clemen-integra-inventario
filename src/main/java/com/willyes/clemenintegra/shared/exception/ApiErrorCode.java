package com.willyes.clemenintegra.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Códigos estandarizados para responder errores funcionales de la API.
 */
public enum ApiErrorCode {

    BLOQUEO_RETENCION_NC(HttpStatus.CONFLICT),
    BLOQUEO_ESTADO_CUARENTENA(HttpStatus.CONFLICT),
    CALIDAD_LOTE_NO_LIBERADO(HttpStatus.CONFLICT),
    NC_ABIERTA(HttpStatus.CONFLICT),
    NC_NO_ENCONTRADA(HttpStatus.NOT_FOUND),
    NC_YA_CERRADA(HttpStatus.CONFLICT),
    EVALUACIONES_FALTANTES(HttpStatus.UNPROCESSABLE_ENTITY),
    ROL_INSUFICIENTE(HttpStatus.FORBIDDEN),
    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    OPERACION_NO_PERMITIDA(HttpStatus.CONFLICT),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR),
    NEGOCIO_GENERICO(HttpStatus.BAD_REQUEST),
    STOCK_INSUFICIENTE(HttpStatus.BAD_REQUEST),
    VIDA_UTIL_SOLO_PRODUCTO_TERMINADO(HttpStatus.BAD_REQUEST),
    VIDA_UTIL_SEMANAS_INVALIDAS(HttpStatus.BAD_REQUEST),
    VIDA_UTIL_NO_CONFIGURADA(HttpStatus.UNPROCESSABLE_ENTITY);

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
