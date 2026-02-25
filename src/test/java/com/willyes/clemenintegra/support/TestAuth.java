package com.willyes.clemenintegra.support;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuth {

    private static final String[] SUPER_ADMIN_AUTHORITIES = {
            "ROLE_SUPER_ADMIN", "ROL_SUPER_ADMIN",
            "INV_READ", "INV_WRITE", "INV_EXPORT", "INV_WORKFLOW", "INV_WORKFLOW_START", "INV_WORKFLOW_FINISH", "INV_DECIDE",
            "INV_CONTEOS_READ", "INV_CONTEOS_WRITE", "INV_CONTEOS_APPLY", "INV_CONTEOS_CLOSE", "INV_CONTEOS_START",
            "INV_AJUSTES_READ", "INV_AJUSTES_WRITE", "INV_MOVIMIENTOS_READ", "INV_LOTES_READ", "INV_KARDEX_READ",
            "INV_SOLICITUDES_READ", "INV_SOLICITUDES_WRITE", "INV_ALERTAS_READ",
            "QC_READ", "QC_WRITE", "QC_EXPORT", "QC_WORKFLOW", "QC_WORKFLOW_FINISH", "QC_DECIDE", "QC_ALERTAS_READ",
            "PROD_READ", "PROD_WRITE", "PROD_WORKFLOW", "PROD_WORKFLOW_FINISH", "PROD_DECIDE",
            "PO_READ", "PO_WRITE", "PO_EXPORT", "PO_WORKFLOW", "PO_WORKFLOW_FINISH", "PO_DECIDE",
            "DOC_READ", "DOC_WRITE", "DOC_DELETE", "ADMIN_RBAC_READ", "ADMIN_RBAC_WRITE",
            "BOM_READ", "BOM_WRITE", "BOM_FORMULA_READ", "BOM_FORMULA_WRITE"
    };

    private static final String[] CONTADOR_AUTHORITIES = {
            "INV_READ", "INV_CONTEOS_READ", "INV_CONTEOS_WRITE", "INV_CONTEOS_APPLY", "INV_CONTEOS_CLOSE", "INV_CONTEOS_START"
    };

    private static final String[] JEFE_CALIDAD_AUTHORITIES = {
            "QC_READ", "QC_WRITE", "QC_WORKFLOW", "QC_WORKFLOW_FINISH", "QC_DECIDE"
    };

    private static final String[] PLANEADOR_AUTHORITIES = {
            "PO_READ", "PO_WRITE", "PO_MRP_READ", "PO_MRP_WRITE", "PO_PLAN_SEMANAL_READ", "PO_PLAN_SEMANAL_WRITE"
    };

    private TestAuth() {
    }


    public static RequestPostProcessor jwtWithAuthorities(String... auths) {
        return auth("jwt-test", auths);
    }

    public static UserRequestPostProcessor userWithAuthorities(String... auths) {
        return SecurityMockMvcRequestPostProcessors.user("user-test").authorities(toGrantedAuthorities(auths));
    }

    public static RequestPostProcessor auth(String username, String... authorities) {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario(username)
                .clave("N/A")
                .nombreCompleto(username)
                .correo(username + "@test.local")
                .rol(null)
                .activo(true)
                .bloqueado(false)
                .build();
        CustomUserDetails principal = new CustomUserDetails(usuario, toGrantedAuthorities(authorities));
        return SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal,
                        "N/A",
                        toGrantedAuthorities(authorities)
                )
        );
    }

    public static UserRequestPostProcessor authWithRoles(String username, String... roles) {
        List<GrantedAuthority> grantedAuthorities = toGrantedAuthorities(roles);
        return SecurityMockMvcRequestPostProcessors.user(username).authorities(grantedAuthorities);
    }

    public static RequestPostProcessor authWithPermissionsAndRoles(String username, String[] authorities, String[] roles) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(Arrays.asList(authorities));
        merged.addAll(Arrays.asList(roles));
        return auth(username, merged.toArray(String[]::new));
    }

    public static RequestPostProcessor authMixed(String username, String[] authorities, String[] roles) {
        return authWithPermissionsAndRoles(username, authorities, roles);
    }

    public static RequestPostProcessor asSuperAdmin() {
        return auth("super-admin", SUPER_ADMIN_AUTHORITIES);
    }

    public static RequestPostProcessor asContador() {
        return auth("contador", CONTADOR_AUTHORITIES);
    }

    public static RequestPostProcessor asJefeCalidad() {
        return auth("jefe-calidad", JEFE_CALIDAD_AUTHORITIES);
    }

    public static RequestPostProcessor asPlaneador() {
        return auth("planeador", PLANEADOR_AUTHORITIES);
    }

    private static List<GrantedAuthority> toGrantedAuthorities(String... values) {
        return Arrays.stream(values)
                .map(v -> (GrantedAuthority) new SimpleGrantedAuthority(v))
                .toList();
    }
}
