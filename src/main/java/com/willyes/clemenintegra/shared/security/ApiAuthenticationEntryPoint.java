package com.willyes.clemenintegra.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.shared.dto.ErrorResponseDTO;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.security.exception.SesionExpiradaAuthenticationException;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaAuthenticationException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String SESION_EXPIRADA_MESSAGE =
            "Tu sesión expiró por inactividad. Inicia sesión nuevamente.";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ApiErrorCode errorCode = resolveErrorCode(authException);
        String message = resolveMessage(authException, errorCode);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(errorCode.getCode())
                .message(message)
                .requestId(MDC.get(RequestIdFilter.MDC_KEY))
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private ApiErrorCode resolveErrorCode(AuthenticationException authException) {
        if (isSesionExpirada(authException)) {
            return ApiErrorCode.SESION_EXPIRADA;
        }
        if (isSesionInvalidada(authException)) {
            return ApiErrorCode.SESION_INVALIDA;
        }
        return ApiErrorCode.SESION_INVALIDA;
    }

    private String resolveMessage(AuthenticationException authException, ApiErrorCode errorCode) {
        if (ApiErrorCode.SESION_EXPIRADA.equals(errorCode)) {
            return SESION_EXPIRADA_MESSAGE;
        }
        return authException.getMessage() != null
                ? authException.getMessage()
                : "No autorizado";
    }

    private boolean isSesionExpirada(AuthenticationException authException) {
        return authException instanceof SesionExpiradaAuthenticationException
                || containsCause(authException, SesionInactivaException.class)
                || containsCause(authException, ExpiredJwtException.class);
    }

    private boolean isSesionInvalidada(AuthenticationException authException) {
        return authException instanceof SesionInvalidadaAuthenticationException
                || containsCause(authException, SesionInvalidadaException.class);
    }

    private boolean containsCause(Throwable source, Class<? extends Throwable> target) {
        Throwable current = source;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            if (target.isInstance(current.getCause())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
