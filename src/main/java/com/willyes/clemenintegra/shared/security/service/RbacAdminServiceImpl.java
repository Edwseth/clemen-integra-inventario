package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.model.rbac.RolEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolPermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import com.willyes.clemenintegra.shared.security.dto.rbac.PermisoDTO;
import com.willyes.clemenintegra.shared.security.dto.rbac.RolDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RbacAdminServiceImpl implements RbacAdminService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> listarRoles(Boolean incluirInactivos) {
        List<RolEntity> roles = Boolean.TRUE.equals(incluirInactivos)
                ? rolRepository.findAllByOrderByCodigoAsc()
                : rolRepository.findByActivoTrueOrderByCodigoAsc();

        return roles.stream().map(this::toRolDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermisoDTO> listarPermisos(String modulo, Boolean activo) {
        boolean hasModulo = modulo != null && !modulo.isBlank();
        List<PermisoEntity> permisos;

        if (hasModulo && activo != null) {
            permisos = permisoRepository.findByModuloIgnoreCaseAndActivoOrderByModuloAscCodigoAsc(modulo.trim(), activo);
        } else if (hasModulo) {
            permisos = permisoRepository.findByModuloIgnoreCaseOrderByModuloAscCodigoAsc(modulo.trim());
        } else if (activo != null) {
            permisos = permisoRepository.findByActivoOrderByModuloAscCodigoAsc(activo);
        } else {
            permisos = permisoRepository.findAllByOrderByModuloAscCodigoAsc();
        }

        return permisos.stream().map(this::toPermisoDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermisoDTO> obtenerPermisosPorRol(Long rolId) {
        validarRolExiste(rolId);
        return permisoRepository.findByRolesIdOrderByCodigoAsc(rolId).stream()
                .map(this::toPermisoDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<PermisoDTO> actualizarPermisosRol(Long rolId, List<Long> permisoIds) {
        validarRolExiste(rolId);

        List<Long> idsNormalizados = permisoIds == null ? List.of() : permisoIds;
        Set<Long> idsUnicos = new LinkedHashSet<>(idsNormalizados);

        List<PermisoEntity> permisos = idsUnicos.isEmpty()
                ? List.of()
                : permisoRepository.findAllById(idsUnicos);

        if (permisos.size() != idsUnicos.size()) {
            Set<Long> idsEncontrados = permisos.stream().map(PermisoEntity::getId).collect(java.util.stream.Collectors.toSet());
            List<Long> faltantes = idsUnicos.stream().filter(id -> !idsEncontrados.contains(id)).toList();
            throw new IllegalArgumentException("No existen permisos para ids: " + faltantes);
        }

        rolPermisoRepository.deleteByRolId(rolId);

        if (!idsUnicos.isEmpty()) {
            rolPermisoRepository.insertBatch(rolId, new ArrayList<>(idsUnicos));
        }

        return permisoRepository.findByRolesIdOrderByCodigoAsc(rolId).stream()
                .map(this::toPermisoDTO)
                .toList();
    }

    private void validarRolExiste(Long rolId) {
        if (rolId == null || !rolRepository.existsById(rolId)) {
            throw new EntityNotFoundException("Rol no encontrado: " + rolId);
        }
    }

    private RolDTO toRolDTO(RolEntity rol) {
        return new RolDTO(rol.getId(), rol.getCodigo(), rol.getNombre(), rol.isActivo());
    }

    private PermisoDTO toPermisoDTO(PermisoEntity permiso) {
        return new PermisoDTO(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getModulo(),
                permiso.getAccion(),
                permiso.getDescripcion(),
                permiso.isActivo()
        );
    }
}
