package com.willyes.clemenintegra.shared.security.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador que envuelve la entidad Usuario y la expone como UserDetails para Spring Security.
 */
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioPrincipal(Usuario usuario) {
        this(usuario, List.of(new SimpleGrantedAuthority(usuario.getRol().name())));
    }

    public UsuarioPrincipal(Usuario usuario, Collection<? extends GrantedAuthority> authorities) {
        this.usuario = usuario;
        this.authorities = authorities;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getClave();
    }

    @Override
    public String getUsername() {
        return usuario.getNombreUsuario();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // podrías agregar lógica si manejas fecha de expiración
    }

    @Override
    public boolean isAccountNonLocked() {
        return !usuario.isBloqueado();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // podrías agregar lógica si expiras contraseñas
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}
