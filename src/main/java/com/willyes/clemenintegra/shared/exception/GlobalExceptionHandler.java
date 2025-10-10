package com.willyes.clemenintegra.shared.exception;

import com.willyes.clemenintegra.shared.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la aplicación.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        List<Map<String, String>> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage()))
                .toList();
        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA,
                "Solicitud inválida",
                detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, DateTimeParseException.class})
    public ResponseEntity<ErrorResponseDTO> handleInvalidDateFormat(Exception ex) {
        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA,
                "Formato de fecha inválido",
                "Use 'YYYY-MM-DDTHH:mm:ss', ej. '2025-09-10T00:00:00'.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA, message, null);
    }

    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(CustomBusinessException ex,
                                                           HttpServletRequest request) {
        ApiErrorCode code = ex.getCode() != null ? ex.getCode() : ApiErrorCode.NEGOCIO_GENERICO;
        return buildResponse(code, ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex,
                                                               HttpServletRequest request) {
        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        String message = "Valor inválido para el parámetro '" + ex.getName() + "'";
        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA, message, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        return buildResponse(ApiErrorCode.ROL_INSUFICIENTE,
                "Acceso denegado. Contacte a un administrador para solicitar el rol adecuado.",
                null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatus(ResponseStatusException ex,
                                                                 HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = ex.getReason() != null ? ex.getReason() : status.name();
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return buildResponse(status, code, message, null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntime(RuntimeException ex,
                                                          HttpServletRequest request) {
        return buildResponse(ApiErrorCode.ERROR_INTERNO,
                "Ocurrió un error inesperado. Intente nuevamente o contacte soporte.",
                null);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(ApiErrorCode code, String message, Object details) {
        ApiErrorCode effective = code != null ? code : ApiErrorCode.ERROR_INTERNO;
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(effective.getCode())
                .message(message)
                .details(details)
                .build();
        return ResponseEntity.status(effective.getHttpStatus()).body(body);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String code, String message, Object details) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(code != null ? code : status.name())
                .message(message)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
