package com.willyes.clemenintegra.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Códigos estandarizados para responder errores funcionales de la API.
 */
public enum ApiErrorCode {

    BLOQUEO_RETENCION_NC(HttpStatus.CONFLICT),
    BLOQUEO_NC_ACTIVA(HttpStatus.CONFLICT),
    BLOQUEO_CONDICION_USO(HttpStatus.CONFLICT),
    BLOQUEO_ESTADO_CUARENTENA(HttpStatus.CONFLICT),
    CALIDAD_LOTE_NO_LIBERADO(HttpStatus.UNPROCESSABLE_ENTITY),
    NC_ABIERTA(HttpStatus.CONFLICT),
    NC_NO_ENCONTRADA(HttpStatus.NOT_FOUND),
    NC_YA_CERRADA(HttpStatus.CONFLICT),
    NC_CAPA_REQUERIDA(HttpStatus.CONFLICT),
    EVALUACIONES_FALTANTES(HttpStatus.UNPROCESSABLE_ENTITY),
    DOCUMENTO_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    ROL_INSUFICIENTE(HttpStatus.FORBIDDEN),
    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    OPERACION_NO_PERMITIDA(HttpStatus.CONFLICT),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR),
    NEGOCIO_GENERICO(HttpStatus.BAD_REQUEST),
    LOTE_INVALIDO_CONTEO(HttpStatus.BAD_REQUEST),
    STOCK_INSUFICIENTE(HttpStatus.BAD_REQUEST),
    VIDA_UTIL_SOLO_PRODUCTO_TERMINADO(HttpStatus.BAD_REQUEST),
    VIDA_UTIL_SEMANAS_INVALIDAS(HttpStatus.BAD_REQUEST),
    VIDA_UTIL_NO_CONFIGURADA(HttpStatus.UNPROCESSABLE_ENTITY),
    MOVIMIENTO_DUPLICADO(HttpStatus.CONFLICT),
    UBICACION_NO_ENCONTRADA(HttpStatus.UNPROCESSABLE_ENTITY),
    UBICACION_NO_PERTENECE_ALMACEN(HttpStatus.UNPROCESSABLE_ENTITY),
    CONTEO_ESTADO_INVALIDO(HttpStatus.CONFLICT),
    CONTEO_YA_APLICADO(HttpStatus.CONFLICT),
    CONTEO_DETALLE_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY),
    CONTEO_SIN_DETALLES(HttpStatus.BAD_REQUEST),
    CONTEO_APLICACION_DUPLICADA(HttpStatus.CONFLICT),
    OC_TRANSICION_INVALIDA(HttpStatus.CONFLICT);

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
