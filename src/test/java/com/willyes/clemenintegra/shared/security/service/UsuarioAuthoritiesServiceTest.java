package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAuthoritiesServiceTest {

    @Mock
    private PermisoRepository permisoRepository;

    @InjectMocks
    private UsuarioAuthoritiesService usuarioAuthoritiesService;

    @Test
    void usaPermisosDeUsuariosRolesCuandoExisten() {
        Usuario usuario = Usuario.builder()
                .id(10L)
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .build();

        when(permisoRepository.findCodigosPermisosActivosByUsuarioId(10L))
                .thenReturn(List.of("INV_AJUSTES_WRITE", "INV_AJUSTES_WRITE", "INV_MOV_READ"));

        Collection<? extends GrantedAuthority> authorities = usuarioAuthoritiesService.buildAuthorities(usuario);
        List<String> values = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(values.contains("ROL_JEFE_ALMACENES"));
        assertTrue(values.contains("INV_AJUSTES_WRITE"));
        assertTrue(values.contains("INV_MOV_READ"));
        assertEquals(3, values.size());
        verify(permisoRepository, never()).findByRolesCodigoAndActivoTrue("ROL_JEFE_ALMACENES");
    }

    @Test
    void usaFallbackPorRolCuandoUsuariosRolesNoDevuelvePermisos() {
        Usuario usuario = Usuario.builder()
                .id(11L)
                .rol(RolUsuario.ROL_CONTADOR)
                .build();

        PermisoEntity permisoAjustes = PermisoEntity.builder().codigo("INV_AJUSTES_READ").build();
        PermisoEntity permisoDuplicado = PermisoEntity.builder().codigo("INV_AJUSTES_READ").build();
        PermisoEntity permisoConteos = PermisoEntity.builder().codigo("INV_CONTEOS_READ").build();

        when(permisoRepository.findCodigosPermisosActivosByUsuarioId(11L)).thenReturn(List.of());
        when(permisoRepository.findByRolesCodigoAndActivoTrue("ROL_CONTADOR"))
                .thenReturn(List.of(permisoAjustes, permisoDuplicado, permisoConteos));

        Collection<? extends GrantedAuthority> authorities = usuarioAuthoritiesService.buildAuthorities(usuario);
        List<String> values = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(values.contains("ROL_CONTADOR"));
        assertTrue(values.contains("INV_AJUSTES_READ"));
        assertTrue(values.contains("INV_CONTEOS_READ"));
        assertEquals(3, values.size());
    }
}
