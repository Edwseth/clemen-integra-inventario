package com.willyes.clemenintegra.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.shared.dto.ErrorResponseDTO;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.NivelAccesoAdmin;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.security.model.UsuarioPrincipal;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SuperAdminSoloLecturaWriteBlockFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> WHITELIST_PREFIXES = Set.of(
            "/api/auth",
            "/api/auth/",
            "/auth/login",
            "/actuator/health",
            "/actuator/info",
            "/api/health"
    );

    private final ObjectMapper objectMapper;

    public SuperAdminSoloLecturaWriteBlockFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (response.isCommitted()) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        if (!WRITE_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> RolUsuario.ROL_SUPER_ADMIN.name().equals(authority.getAuthority()));
        if (!isSuperAdmin) {
            filterChain.doFilter(request, response);
            return;
        }

        Usuario usuario = resolveUsuario(authentication);
        NivelAccesoAdmin nivelAccesoAdmin = usuario != null
                ? usuario.getNivelAccesoAdmin()
                : NivelAccesoAdmin.FULL;
        if (nivelAccesoAdmin == NivelAccesoAdmin.FULL) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiErrorCode errorCode = ApiErrorCode.SUPER_ADMIN_SOLO_LECTURA;
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(errorCode.getCode())
                .message("Super administrador en modo solo lectura.")
                .requestId(MDC.get(RequestIdFilter.MDC_KEY))
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isWhitelisted(String uri) {
        if (uri == null) {
            return false;
        }
        return WHITELIST_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private Usuario resolveUsuario(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UsuarioPrincipal usuarioPrincipal) {
            return usuarioPrincipal.getUsuario();
        }
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUsuario();
        }
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }
}
