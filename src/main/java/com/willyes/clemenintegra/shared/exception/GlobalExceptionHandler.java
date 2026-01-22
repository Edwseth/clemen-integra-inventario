package com.willyes.clemenintegra.shared.exception;

import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.shared.dto.ErrorResponseDTO;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Manejador global de excepciones para toda la aplicación.
 */
@RestControllerAdvice
@Slf4j
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
    public ResponseEntity<ErrorResponseDTO> handleUnreadableMessage(Exception ex, HttpServletRequest request) {
        Throwable root = ex instanceof HttpMessageNotReadableException hmre
                ? hmre.getMostSpecificCause()
                : ex;
        boolean esRetencionRequest = request != null
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/api/calidad/retenciones");

        if (root instanceof DateTimeParseException) {
            if (esRetencionRequest) {
                return buildResponse(ApiErrorCode.RETENCION_FECHA_INVALIDA,
                        "Formato de fecha inválido",
                        "Use 'YYYY-MM-DDTHH:mm:ss' (ISO-8601). Si el campo no aplica, envíelo nulo u omítalo.");
            }
            return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Formato de fecha inválido",
                    "Use 'YYYY-MM-DDTHH:mm:ss' (ISO-8601). Si el campo no aplica, envíelo nulo u omítalo.");
        }

        if (root instanceof InvalidFormatException ife) {
            if (esRetencionRequest && MotivoRetencion.class.equals(ife.getTargetType())) {
                return buildResponse(ApiErrorCode.RETENCION_MOTIVO_INVALIDO,
                        "Motivo de retención inválido",
                        Map.of("field", "motivo",
                                "acceptedValues", Arrays.toString(MotivoRetencion.values())));
            }
            if (esRetencionRequest && LocalDateTime.class.equals(ife.getTargetType())) {
                return buildResponse(ApiErrorCode.RETENCION_FECHA_INVALIDA,
                        "Formato de fecha inválido",
                        "Use 'YYYY-MM-DDTHH:mm:ss' (ISO-8601). Si el campo no aplica, envíelo nulo u omítalo.");
            }
            String path = ife.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .reduce("", (acc, curr) -> acc.isEmpty() ? curr : acc + "." + curr);

            Map<String, Object> details = Map.of(
                    "field", path,
                    "rejectedValue", ife.getValue()
            );

            return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Valor inválido para el campo '" + path + "'",
                    details);
        }

        return buildResponse(ApiErrorCode.SOLICITUD_INVALIDA,
                "Solicitud inválida",
                root != null ? root.getMessage() : ex.getMessage());
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
        HttpStatus status = code != null && code.getHttpStatus() != null
                ? code.getHttpStatus()
                : HttpStatus.UNPROCESSABLE_ENTITY;

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(code != null ? code.getCode() : ApiErrorCode.NEGOCIO_GENERICO.getCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(EntityNotFoundException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado",
                null);
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

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<ErrorResponseDTO> handlePdfGeneration(PdfGenerationException ex,
                                                                HttpServletRequest request) {
        return buildResponse(ApiErrorCode.ERROR_INTERNO, ex.getMessage(), null);
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT,
                "CONFLICTO_INTEGRIDAD",
                "Conflicto de integridad en la operación solicitada.",
                null);
    }

    @ExceptionHandler(SesionInactivaException.class)
    public ResponseEntity<ErrorResponseDTO> handleSesionInactiva(SesionInactivaException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(ApiErrorCode.SESION_EXPIRADA,
                "Tu sesión expiró por inactividad. Inicia sesión nuevamente.",
                null);
    }

    @ExceptionHandler(SesionInvalidadaException.class)
    public ResponseEntity<ErrorResponseDTO> handleSesionInvalidada(SesionInvalidadaException ex,
                                                                   HttpServletRequest request) {
        return buildResponse(ApiErrorCode.SESION_INVALIDA, ex.getMessage(), null);
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
        logInternalError(ex, request);
        return buildResponse(ApiErrorCode.ERROR_INTERNO,
                "Ocurrió un error inesperado. Intente nuevamente o contacte soporte.",
                null);
    }

    private void logInternalError(Exception ex, HttpServletRequest request) {
        Throwable root = getRootCause(ex);
        String requestId = MDC.get("requestId");
        String method = request != null ? request.getMethod() : null;
        String uri = request != null ? request.getRequestURI() : null;
        String query = request != null ? request.getQueryString() : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null ? auth.getName() : null;
        String roles = auth != null
                ? auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","))
                : null;

        log.error("Error inesperado requestId={} method={} uri={} query={} usuario={} roles={} rootType={} rootMessage={}",
                requestId,
                method,
                uri,
                query,
                usuario,
                roles,
                root != null ? root.getClass().getSimpleName() : null,
                root != null ? root.getMessage() : null,
                ex);
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable current = ex;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(ApiErrorCode code, String message, Object details) {
        ApiErrorCode effective = code != null ? code : ApiErrorCode.ERROR_INTERNO;
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(effective.getCode())
                .message(message)
                .details(details)
                .requestId(MDC.get("requestId"))
                .build();
        return ResponseEntity.status(effective.getHttpStatus()).body(body);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String code, String message, Object details) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(code != null ? code : status.name())
                .message(message)
                .details(details)
                .requestId(MDC.get("requestId"))
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
