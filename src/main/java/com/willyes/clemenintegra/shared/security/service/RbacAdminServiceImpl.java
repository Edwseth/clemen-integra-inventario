package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.model.rbac.RolEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import com.willyes.clemenintegra.shared.security.dto.rbac.PermisoDTO;
import com.willyes.clemenintegra.shared.security.dto.rbac.RolDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RbacAdminServiceImpl implements RbacAdminService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

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
        if (modulo == null || modulo.isBlank()) {
            throw new IllegalArgumentException("El parámetro modulo es obligatorio");
        }

        String moduloNormalizado = modulo.trim().toUpperCase(Locale.ROOT);
        boolean activoFiltro = activo == null || activo;

        return permisoRepository.findByModuloIgnoreCaseAndActivoOrderByCodigoAsc(moduloNormalizado, activoFiltro).stream()
                .map(this::toPermisoDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listarModulosPermisosActivos() {
        return permisoRepository.findDistinctModulosActivosOrderByModuloAsc();
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
    public List<PermisoDTO> actualizarPermisosRol(Long rolId, List<Long> permisoIds, String modulo) {
        RolEntity rol = obtenerRol(rolId);
        String moduloNormalizado = normalizarModulo(modulo);

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

        List<PermisoModuloInvalido> permisosOtroModulo = permisos.stream()
                .filter(permiso -> permiso.getModulo() == null || !moduloNormalizado.equalsIgnoreCase(permiso.getModulo()))
                .map(permiso -> new PermisoModuloInvalido(permiso.getId(), permiso.getCodigo(), permiso.getModulo()))
                .toList();

        if (!permisosOtroModulo.isEmpty()) {
            log.warn("RBAC_VALIDATION permisos_fuera_de_modulo rolId={} moduloSolicitado={} invalidos={} ",
                    rolId, moduloNormalizado, permisosOtroModulo);
            throw new IllegalArgumentException("Todos los permisos deben pertenecer al módulo "
                    + moduloNormalizado + ". Inválidos: " + permisosOtroModulo.stream()
                    .map(PermisoModuloInvalido::idYCodigo)
                    .toList());
        }

        Set<PermisoEntity> permisosOtrosModulos = rol.getPermisos().stream()
                .filter(permiso -> permiso.getModulo() == null || !moduloNormalizado.equalsIgnoreCase(permiso.getModulo()))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        Set<PermisoEntity> permisosFinales = new HashSet<>(permisosOtrosModulos);
        permisosFinales.addAll(permisos);

        rol.setPermisos(permisosFinales);
        rolRepository.save(rol);

        auditarCambioPermisosRol(rolId, moduloNormalizado, idsUnicos);

        return rol.getPermisos().stream()
                .sorted(Comparator.comparing(PermisoEntity::getCodigo, String.CASE_INSENSITIVE_ORDER))
                .map(this::toPermisoDTO)
                .toList();
    }

    private void auditarCambioPermisosRol(Long rolId, String modulo, Set<Long> permisosIds) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null ? authentication.getName() : "sistema";
        log.info("RBAC_AUDIT actor={} action=UPDATE_ROLE_PERMISSIONS rolId={} modulo={} permisosCount={} permisosIds={}",
                actor, rolId, modulo, permisosIds.size(), permisosIds);
    }

    private String normalizarModulo(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            throw new IllegalArgumentException("El módulo es obligatorio para actualizar permisos de rol");
        }
        return modulo.trim().toUpperCase(Locale.ROOT);
    }

    private RolEntity obtenerRol(Long rolId) {
        if (rolId == null) {
            throw new EntityNotFoundException("Rol no encontrado: null");
        }
        return rolRepository.findById(rolId)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + rolId));
    }

    private void validarRolExiste(Long rolId) {
        obtenerRol(rolId);
    }

    private RolDTO toRolDTO(RolEntity rol) {
        return new RolDTO(rol.getId(), rol.getCodigo(), rol.getNombre(), rol.isActivo());
    }

    private PermisoDTO toPermisoDTO(PermisoEntity permiso) {
        return new PermisoDTO(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getDescripcion(),
                permiso.getModulo(),
                permiso.isActivo(),
                permiso.getAccion()
        );
    }

    private record PermisoModuloInvalido(Long id, String codigo, String moduloReal) {
        String idYCodigo() {
            return Objects.toString(id, "null") + ":" + Objects.toString(codigo, "SIN_CODIGO");
        }
    }
}
