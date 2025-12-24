package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
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
        try {
            Claims claims = jwtTokenService.extraerClaims(token);
            String username = claims.getSubject();
            Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            validarSesion(token, usuario);

            CustomUserDetails principal = new CustomUserDetails(usuario);
            return new UsernamePasswordAuthenticationToken(
                    principal, token, principal.getAuthorities()
            );
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado para solicitud de {}", requestUsername(token));
            throw new BadCredentialsException("Token expirado", e);
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

    private void validarSesion(String token, Usuario usuario) {
        if (!usuario.isActivo() || usuario.isBloqueado()) {
            throw new BadCredentialsException("Usuario inactivo o bloqueado");
        }

        Long tokenSessionVersion = jwtTokenService.getSessionVersion(token);
        Long usuarioSessionVersion = Optional.ofNullable(usuario.getSessionVersion()).orElse(0L);

        if (!usuarioSessionVersion.equals(tokenSessionVersion)) {
            throw new SesionInvalidadaException(
                    String.format("La sesión fue invalidada (token=%d, bd=%d) para usuario %d",
                            tokenSessionVersion, usuarioSessionVersion, usuario.getId())
            );
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
