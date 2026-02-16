package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAuthoritiesServiceTest {

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private UsuarioAuthoritiesService usuarioAuthoritiesService;

    @Test
    void noMezclaPermisosCuandoUsuariosRolesApuntaARolDistinto() {
        Usuario usuario = Usuario.builder()
                .id(10L)
                .nombreUsuario("usuario.alimentos")
                .rol(RolUsuario.ROL_LIDER_ALIMENTOS)
                .build();


        when(rolRepository.findCodigosByUsuarioId(10L)).thenReturn(List.of("ROL_JEFE_PRODUCCION"));
        when(permisoRepository.findByRolesCodigoAndActivoTrue("ROL_LIDER_ALIMENTOS"))
                .thenReturn(List.of());

        Collection<? extends GrantedAuthority> authorities = usuarioAuthoritiesService.buildAuthorities(usuario);
        List<String> values = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(values.contains("ROL_LIDER_ALIMENTOS"));
        assertFalse(values.contains("INV_PRODUCT_READ"));
        assertEquals(1, values.size());
        verify(permisoRepository).findByRolesCodigoAndActivoTrue("ROL_LIDER_ALIMENTOS");
    }

    @Test
    void usaPermisosDelRolCuandoUsuariosRolesCoincide() {
        Usuario usuario = Usuario.builder()
                .id(11L)
                .nombreUsuario("usuario.jefe")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .build();

        PermisoEntity permiso = PermisoEntity.builder().codigo("INV_PRODUCT_READ").build();

        when(rolRepository.findCodigosByUsuarioId(11L)).thenReturn(List.of("ROL_JEFE_PRODUCCION"));
        when(permisoRepository.findByRolesCodigoAndActivoTrue("ROL_JEFE_PRODUCCION"))
                .thenReturn(List.of(permiso));

        Collection<? extends GrantedAuthority> authorities = usuarioAuthoritiesService.buildAuthorities(usuario);
        List<String> values = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(values.contains("ROL_JEFE_PRODUCCION"));
        assertTrue(values.contains("INV_PRODUCT_READ"));
        assertEquals(2, values.size());
    }

    @Test
    void usaFallbackPorUsuarioRolCuandoUsuariosRolesEstaVacio() {
        Usuario usuario = Usuario.builder()
                .id(12L)
                .nombreUsuario("usuario.contador")
                .rol(RolUsuario.ROL_CONTADOR)
                .build();

        PermisoEntity permisoAjustes = PermisoEntity.builder().codigo("INV_AJUSTES_READ").build();
        PermisoEntity permisoDuplicado = PermisoEntity.builder().codigo("INV_AJUSTES_READ").build();
        PermisoEntity permisoConteos = PermisoEntity.builder().codigo("INV_CONTEOS_READ").build();

        when(rolRepository.findCodigosByUsuarioId(12L)).thenReturn(List.of());
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
