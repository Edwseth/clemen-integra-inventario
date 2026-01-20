package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.exception.SesionExpiradaAuthenticationException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaAuthenticationException;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;

    @Value("${security.session.max-idle-minutes:20}")
    private long maxIdleMinutes;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();
        Claims claims;
        try {
            claims = jwtTokenService.extraerClaims(token);
            String username = claims.getSubject();
            Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            validarSesion(claims, usuario);

            CustomUserDetails principal = new CustomUserDetails(usuario);
            UsernamePasswordAuthenticationToken authenticated = new UsernamePasswordAuthenticationToken(
                    principal, token, principal.getAuthorities()
            );
            log.debug("JwtAuthenticationProvider: sesión válida para usuario {}, version {}", username, usuario.getSessionVersion());
            return authenticated;
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado para solicitud de {}", requestUsername(token));
            throw new SesionExpiradaAuthenticationException("Token expirado", e);
        } catch (SesionInactivaException e) {
            throw new SesionExpiradaAuthenticationException(e.getMessage(), e);
        } catch (SesionInvalidadaException e) {
            throw new SesionInvalidadaAuthenticationException(e.getMessage(), e);
        } catch (SignatureException e) {
            log.warn("Firma de token inválida");
            throw new BadCredentialsException("Firma del token inválida", e);
        } catch (UsernameNotFoundException e) {
            log.warn("Usuario no encontrado en token");
            throw new BadCredentialsException("Usuario no encontrado", e);
        } catch (AuthenticationException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Token inválido");
            throw new BadCredentialsException("Token inválido", e);
        }
    }

    private void validarSesion(Claims claims, Usuario usuario) {
        if (!usuario.isActivo() || usuario.isBloqueado()) {
            throw new BadCredentialsException("Usuario inactivo o bloqueado");
        }

        boolean tokenHasSessionVersion = claims.containsKey("sessionVersion");
        Long tokenSessionVersion = jwtTokenService.getSessionVersion(claims);
        Long usuarioSessionVersion = Optional.ofNullable(usuario.getSessionVersion()).orElse(0L);

        if (!tokenHasSessionVersion && usuarioSessionVersion == 0L) {
            log.debug("JwtAuthenticationProvider: token legado aceptado para usuario {} (dbVersion=0)", usuario.getNombreUsuario());
        } else {
            if (!usuarioSessionVersion.equals(tokenSessionVersion)) {
                log.debug("JwtAuthenticationProvider: sesión invalidada para usuario {} (tokenVersion={}, dbVersion={})",
                        usuario.getNombreUsuario(), tokenSessionVersion, usuarioSessionVersion);
                throw new SesionInvalidadaException(
                        String.format("La sesión fue invalidada (token=%d, bd=%d) para usuario %d",
                                tokenSessionVersion, usuarioSessionVersion, usuario.getId())
                );
            }
            log.debug("JwtAuthenticationProvider: versión de sesión válida para usuario {} (version={})",
                    usuario.getNombreUsuario(), usuarioSessionVersion);
        }

        LocalDateTime ultimaActividad = usuario.getUltimaActividad();
        LocalDateTime ahora = LocalDateTime.now();

        if (ultimaActividad == null) {
            log.debug("JwtAuthenticationProvider: primera actividad registrada para usuario {}", usuario.getNombreUsuario());
        } else {
            long minutosInactivo = Duration.between(ultimaActividad, ahora).toMinutes();
            if (minutosInactivo > maxIdleMinutes) {
                log.debug("JwtAuthenticationProvider: sesión inactiva para usuario {} (idleMinutes={}, maxIdle={})",
                        usuario.getNombreUsuario(), minutosInactivo, maxIdleMinutes);
                throw new SesionInactivaException("La sesión ha expirado por inactividad.");
            }
            log.debug("JwtAuthenticationProvider: sesión activa para usuario {} (idleMinutes={}, maxIdle={})",
                    usuario.getNombreUsuario(), minutosInactivo, maxIdleMinutes);
        }

        usuario.setUltimaActividad(ahora);
        usuarioRepository.saveAndFlush(usuario);
    }

    private String requestUsername(String token) {
        try {
            Claims claims = jwtTokenService.extraerClaims(token);
            return claims.getSubject();
        } catch (Exception ex) {
            return "desconocido";
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
