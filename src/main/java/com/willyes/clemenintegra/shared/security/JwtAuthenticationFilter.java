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
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header recibido: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Authentication authRequest = new JwtAuthenticationToken(token);
                Authentication authResult = jwtAuthenticationProvider.authenticate(authRequest);

                SecurityContextHolder.getContext().setAuthentication(authResult);
                log.debug("Usuario {} autenticado en {}", authResult.getName(), requestURI);

                // ✅ Aquí sí es seguro poner el log
                log.info("Authorities asignadas: {}", authResult.getAuthorities());
            } catch (AuthenticationException ex) {
                log.warn("Autenticación JWT fallida en {}: {}", request.getRequestURI(), ex.getMessage());
                log.debug("Solicitud no autorizada en {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
                return;
            } catch (Exception ex) {
                log.error("Error procesando JWT en {}: {}", requestURI, ex.getMessage(), ex);
                log.debug("Solicitud no autorizada en {}", requestURI);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error de autenticación");
                return;
            }
        } else {
            log.debug("Solicitud sin encabezado Authorization en {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

}


