package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthenticationProvider; // ✅ usar provider

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();

        // 1) Deja pasar preflight CORS y endpoints públicos
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || uri.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header recibido: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            try {
                Authentication authRequest = new JwtAuthenticationToken(token);
                Authentication authResult  = jwtAuthenticationProvider.authenticate(authRequest);
                SecurityContextHolder.getContext().setAuthentication(authResult);
                log.debug("Usuario {} autenticado en {}", authResult.getName(), uri);
                log.info("Authorities asignadas: {}", authResult.getAuthorities());
            } catch (org.springframework.security.core.AuthenticationException ex) {
                log.warn("Auth failed [{}] en {} {}: {}",
                        ex.getClass().getSimpleName(), request.getMethod(), uri,
                        (ex.getMessage() != null ? ex.getMessage() : "sin mensaje"));
                SecurityContextHolder.clearContext();
                throw ex; // deja que ExceptionTranslationFilter genere el 401
            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                log.warn("JWT expirado en {} {}: {}", request.getMethod(), uri, ex.getMessage());
                SecurityContextHolder.clearContext();
                throw new org.springframework.security.authentication.BadCredentialsException("JWT expirado", ex);
            } catch (io.jsonwebtoken.security.SignatureException ex) {
                log.warn("Firma JWT inválida en {} {}: {}", request.getMethod(), uri, ex.getMessage());
                SecurityContextHolder.clearContext();
                throw new org.springframework.security.authentication.BadCredentialsException("Firma JWT inválida", ex);
            }
        } else {
            log.debug("Solicitud sin encabezado Authorization en {}", uri);
        }

        // ¡OJO!: no atrapar Exception genérica aquí
        filterChain.doFilter(request, response);
    }
}



