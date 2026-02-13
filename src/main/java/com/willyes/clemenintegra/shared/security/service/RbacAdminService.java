package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.security.dto.rbac.PermisoDTO;
import com.willyes.clemenintegra.shared.security.dto.rbac.RolDTO;
import java.util.List;

public interface RbacAdminService {

    List<RolDTO> listarRoles(Boolean incluirInactivos);

    List<PermisoDTO> listarPermisos(String modulo, Boolean activo);

    List<PermisoDTO> obtenerPermisosPorRol(Long rolId);

    List<PermisoDTO> actualizarPermisosRol(Long rolId, List<Long> permisoIds);
}
