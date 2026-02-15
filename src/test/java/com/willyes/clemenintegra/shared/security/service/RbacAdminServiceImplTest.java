package com.willyes.clemenintegra.shared.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolPermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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
    @Mock
    private RolPermisoRepository rolPermisoRepository;

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
    void actualizarPermisosRolReemplazaSetAnterior() {
        Long rolId = 10L;
        when(rolRepository.existsById(rolId)).thenReturn(true);

        PermisoEntity p1 = permiso(1L, "INV_READ", "INV");
        PermisoEntity p2 = permiso(2L, "INV_WRITE", "INV");

        when(permisoRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(p1, p2));
        when(permisoRepository.findByRolesIdOrderByCodigoAsc(rolId)).thenReturn(List.of(p1, p2));

        var resultado = service.actualizarPermisosRol(rolId, List.of(1L, 2L, 2L), "INV");

        verify(rolPermisoRepository).deleteByRolId(rolId);
        verify(rolPermisoRepository).insertBatch(rolId, List.of(1L, 2L));
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).codigo()).isEqualTo("INV_READ");
    }

    @Test
    void actualizarPermisosRolValidaModuloSeleccionado() {
        Long rolId = 10L;
        when(rolRepository.existsById(rolId)).thenReturn(true);

        PermisoEntity p1 = permiso(1L, "INV_READ", "INV");
        PermisoEntity p2 = permiso(2L, "QC_READ", "QC");

        when(permisoRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(p1, p2));

        assertThatThrownBy(() -> service.actualizarPermisosRol(rolId, List.of(1L, 2L), "INV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("módulo INV");

        verify(rolPermisoRepository, never()).deleteByRolId(rolId);
    }

    @Test
    void actualizarConListaVaciaBorraTodo() {
        Long rolId = 11L;
        when(rolRepository.existsById(rolId)).thenReturn(true);
        when(permisoRepository.findByRolesIdOrderByCodigoAsc(rolId)).thenReturn(List.of());

        var resultado = service.actualizarPermisosRol(rolId, List.of(), "INV");

        verify(rolPermisoRepository).deleteByRolId(rolId);
        verify(rolPermisoRepository, never()).insertBatch(org.mockito.ArgumentMatchers.anyLong(), anyList());
        assertThat(resultado).isEmpty();
    }

    @Test
    void rolInexistenteLanzaNotFound() {
        when(rolRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.actualizarPermisosRol(99L, List.of(1L), "INV"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Rol no encontrado");
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
