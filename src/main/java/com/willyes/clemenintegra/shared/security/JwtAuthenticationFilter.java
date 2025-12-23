package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<JwtTokenService> jwtTokenServiceProvider;
    private final ObjectProvider<CustomUserDetailsService> userDetailsServiceProvider;
    private final ObjectProvider<UsuarioRepository> usuarioRepositoryProvider;

    @Value("${security.session.max-idle-minutes:20}")
    private long maxIdleMinutes;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();

        // 1) Deja pasar preflight CORS y endpoints públicos
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/api/auth")
                || uri.startsWith("/api/public/")
                || "/auth/login".equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            try {
                JwtTokenService jwtTokenService = jwtTokenServiceProvider.getIfAvailable();
                CustomUserDetailsService userDetailsService = userDetailsServiceProvider.getIfAvailable();
                UsuarioRepository usuarioRepository = usuarioRepositoryProvider.getIfAvailable();

                if (jwtTokenService == null || userDetailsService == null || usuarioRepository == null) {
                    log.debug("Componentes de seguridad no disponibles, se omite autenticación para {}", uri);
                    filterChain.doFilter(request, response);
                    return;
                }

                Claims claims = jwtTokenService.extraerClaims(token);
                String username = claims.getSubject();
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                validarSesion(token, userDetails, jwtTokenService, usuarioRepository);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, token, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Usuario {} autenticado en {}", authentication.getName(), uri);
                log.debug("Authorities asignadas: {}", authentication.getAuthorities());
            } catch (SesionInvalidadaException | SesionInactivaException ex) {
                log.warn("Sesión rechazada [{}] en {} {}: {}",
                        ex.getClass().getSimpleName(), request.getMethod(), uri,
                        (ex.getMessage() != null ? ex.getMessage() : "sin mensaje"));
                SecurityContextHolder.clearContext();
                throw ex;
            } catch (ExpiredJwtException ex) {
                log.warn("JWT expirado en {} {}: {}", request.getMethod(), uri, ex.getMessage());
                SecurityContextHolder.clearContext();
                throw new BadCredentialsException("JWT expirado", ex);
            } catch (SignatureException ex) {
                log.warn("Firma JWT inválida en {} {}: {}", request.getMethod(), uri, ex.getMessage());
                SecurityContextHolder.clearContext();
                throw new BadCredentialsException("Firma JWT inválida", ex);
            } catch (JwtException ex) {
                log.warn("Token inválido en {} {}: {}", request.getMethod(), uri, ex.getMessage());
                SecurityContextHolder.clearContext();
                throw new BadCredentialsException("Token inválido", ex);
            }
        } else {
            log.debug("Solicitud sin encabezado Authorization en {}", uri);
        }

        // ¡OJO!: no atrapar Exception genérica aquí
        filterChain.doFilter(request, response);
    }

    private void validarSesion(String token,
                               UserDetails userDetails,
                               JwtTokenService jwtTokenService,
                               UsuarioRepository usuarioRepository) {
        if (!(userDetails instanceof CustomUserDetails cud)) {
            throw new BadCredentialsException("UserDetails inválido");
        }
        Usuario usuario = cud.getUsuario();

        Long tokenSessionVersion = jwtTokenService.getSessionVersion(token);
        Long usuarioSessionVersion = usuario.getSessionVersion() != null ? usuario.getSessionVersion() : 0L;

        if (!usuarioSessionVersion.equals(tokenSessionVersion)) {
            throw new SesionInvalidadaException("La sesión fue invalidada porque se inició una nueva sesión para este usuario.");
        }

        LocalDateTime ultimaActividad = usuario.getUltimaActividad();
        if (ultimaActividad == null) {
            throw new SesionInactivaException("La sesión ha expirado por inactividad.");
        }

        long minutosInactivo = Duration.between(ultimaActividad, LocalDateTime.now()).toMinutes();
        if (minutosInactivo > maxIdleMinutes) {
            throw new SesionInactivaException("La sesión ha expirado por inactividad.");
        }

        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}
