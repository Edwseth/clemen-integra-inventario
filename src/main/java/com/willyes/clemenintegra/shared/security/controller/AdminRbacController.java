package com.willyes.clemenintegra.shared.security.controller;

import com.willyes.clemenintegra.shared.security.dto.rbac.PermisoDTO;
import com.willyes.clemenintegra.shared.security.dto.rbac.RolDTO;
import com.willyes.clemenintegra.shared.security.dto.rbac.UpdateRolPermisosRequest;
import com.willyes.clemenintegra.shared.security.service.RbacAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
public class AdminRbacController {

    private final RbacAdminService rbacAdminService;

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ADMIN_RBAC_READ','ADMIN_RBAC_WRITE')")
    public List<RolDTO> listarRoles(@RequestParam(required = false, defaultValue = "false") Boolean incluirInactivos) {
        return rbacAdminService.listarRoles(incluirInactivos);
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasAnyAuthority('ADMIN_RBAC_READ','ADMIN_RBAC_WRITE')")
    public List<PermisoDTO> listarPermisos(@RequestParam String modulo,
                                           @RequestParam(required = false, defaultValue = "true") Boolean activo) {
        return rbacAdminService.listarPermisos(modulo, activo);
    }

    @GetMapping("/roles/{rolId}/permisos")
    @PreAuthorize("hasAnyAuthority('ADMIN_RBAC_READ','ADMIN_RBAC_WRITE')")
    public List<PermisoDTO> obtenerPermisosPorRol(@PathVariable Long rolId) {
        return rbacAdminService.obtenerPermisosPorRol(rolId);
    }

    @PutMapping("/roles/{rolId}/permisos")
    @PreAuthorize("hasAuthority('ADMIN_RBAC_WRITE')")
    public List<PermisoDTO> actualizarPermisosRol(@PathVariable Long rolId,
                                                  @Valid @RequestBody UpdateRolPermisosRequest request) {
        return rbacAdminService.actualizarPermisosRol(rolId, request.permisoIds(), request.modulo());
    }
}
