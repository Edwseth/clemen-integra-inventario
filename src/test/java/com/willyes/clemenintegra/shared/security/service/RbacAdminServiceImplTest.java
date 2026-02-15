package com.willyes.clemenintegra.shared.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.model.rbac.RolEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacAdminServiceImplTest {

    @Mock
    private RolRepository rolRepository;
    @Mock
    private PermisoRepository permisoRepository;

    @InjectMocks
    private RbacAdminServiceImpl service;

    @Test
    void listarPermisosExigeModuloYActivoTruePorDefecto() {
        PermisoEntity p1 = permiso(1L, "INV_A", "INV");
        PermisoEntity p2 = permiso(2L, "INV_B", "INV");
        when(permisoRepository.findByModuloIgnoreCaseAndActivoOrderByCodigoAsc("INV", true)).thenReturn(List.of(p1, p2));

        var resultado = service.listarPermisos("inv", null);

        verify(permisoRepository).findByModuloIgnoreCaseAndActivoOrderByCodigoAsc("INV", true);
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).codigo()).isEqualTo("INV_A");
        assertThat(resultado.get(0).tipo()).isEqualTo("READ");
    }

    @Test
    void listarPermisosSinModuloLanzaError() {
        assertThatThrownBy(() -> service.listarPermisos(" ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modulo");
    }

    @Test
    void actualizarPermisosRolReemplazaSoloElModuloSolicitado() {
        Long rolId = 10L;
        RolEntity rol = rolConPermisos(
                permiso(100L, "DOC_READ", "DOC"),
                permiso(101L, "DOC_WRITE", "DOC"),
                permiso(200L, "INV_OLD", "INV")
        );

        PermisoEntity invRead = permiso(1L, "INV_READ", "INV");

        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));
        when(permisoRepository.findAllById(Set.of(1L))).thenReturn(List.of(invRead));

        var resultado = service.actualizarPermisosRol(rolId, List.of(1L), "INV");

        verify(rolRepository).save(rol);
        assertThat(codigos(rol.getPermisos())).containsExactlyInAnyOrder("DOC_READ", "DOC_WRITE", "INV_READ");
        assertThat(resultado).extracting("codigo").containsExactly("DOC_READ", "DOC_WRITE", "INV_READ");
    }

    @Test
    void actualizarPermisosRolConListaVaciaQuitaSoloPermisosDelModulo() {
        Long rolId = 11L;
        RolEntity rol = rolConPermisos(
                permiso(100L, "DOC_READ", "DOC"),
                permiso(101L, "DOC_WRITE", "DOC"),
                permiso(200L, "INV_OLD", "INV")
        );

        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));

        var resultado = service.actualizarPermisosRol(rolId, List.of(), "INV");

        verify(rolRepository).save(rol);
        verify(permisoRepository, never()).findAllById(org.mockito.ArgumentMatchers.anySet());
        assertThat(codigos(rol.getPermisos())).containsExactlyInAnyOrder("DOC_READ", "DOC_WRITE");
        assertThat(resultado).extracting("codigo").containsExactly("DOC_READ", "DOC_WRITE");
    }

    @Test
    void actualizarPermisosRolValidaModuloSeleccionado() {
        Long rolId = 10L;
        RolEntity rol = rolConPermisos(permiso(100L, "DOC_READ", "DOC"));

        PermisoEntity p1 = permiso(1L, "INV_READ", "INV");
        PermisoEntity p2 = permiso(2L, "QC_READ", "QC");

        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));
        when(permisoRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(p1, p2));

        assertThatThrownBy(() -> service.actualizarPermisosRol(rolId, List.of(1L, 2L), "INV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("módulo INV");

        verify(rolRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rolInexistenteLanzaNotFound() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarPermisosRol(99L, List.of(1L), "INV"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Rol no encontrado");
    }

    private Set<String> codigos(Set<PermisoEntity> permisos) {
        return permisos.stream().map(PermisoEntity::getCodigo).collect(java.util.stream.Collectors.toSet());
    }

    private RolEntity rolConPermisos(PermisoEntity... permisos) {
        RolEntity rol = new RolEntity();
        rol.setPermisos(new HashSet<>(List.of(permisos)));
        return rol;
    }

    private PermisoEntity permiso(Long id, String codigo, String modulo) {
        PermisoEntity p = new PermisoEntity();
        p.setId(id);
        p.setCodigo(codigo);
        p.setModulo(modulo);
        p.setAccion("READ");
        p.setActivo(true);
        return p;
    }
}
