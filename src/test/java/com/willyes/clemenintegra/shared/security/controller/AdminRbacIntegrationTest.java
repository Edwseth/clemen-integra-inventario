package com.willyes.clemenintegra.shared.security.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.UsuarioAuthoritiesService;
import com.willyes.clemenintegra.support.IntegrationTestH2;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRbacIntegrationTest extends IntegrationTestH2 {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioAuthoritiesService usuarioAuthoritiesService;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    private Usuario usuarioPlaneador;

    @BeforeEach
    void setUp() {
        usuarioPlaneador = usuarioRepository.findByNombreUsuario("planeador")
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nombreUsuario("planeador")
                        .clave("secret")
                        .nombreCompleto("Planeador Test")
                        .correo("planeador@test.local")
                        .rol(RolUsuario.ROL_PLANEADOR)
                        .activo(true)
                        .bloqueado(false)
                        .build()));
    }


    @Test
    void listarRolesConPermisoLecturaResponde200YSinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/admin/rbac/roles")
                        .with(authentication(rbacReadAuth())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/rbac/roles")
                        .with(authentication(noRbacAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarPermisosConPermisoEscrituraResponde200YSinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "INV")
                        .with(authentication(rbacWriteAuth())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "INV")
                        .with(authentication(noRbacAuth())))
                .andExpect(status().isForbidden());
    }
    @Test
    void listarPermisosInvActivoTrueDevuelveSoloCodigosInv() throws Exception {
        crearPermisoSiNoExiste("INV_SMOKE_ACTIVE", "INV", "READ", true);
        crearPermisoSiNoExiste("INV_SMOKE_INACTIVE", "INV", "READ", false);
        crearPermisoSiNoExiste("QC_SMOKE_ACTIVE", "QC", "READ", true);

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "INV")
                        .with(authentication(rbacWriteAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("INV_SMOKE_ACTIVE")))
                .andExpect(jsonPath("$[*].codigo", everyItem(startsWith("INV"))))
                .andExpect(jsonPath("$[?(@.codigo=='INV_SMOKE_INACTIVE')]").isEmpty())
                .andExpect(jsonPath("$[?(@.codigo=='QC_SMOKE_ACTIVE')]").isEmpty());
    }



    @Test
    void listarPermisosMenuDevuelvePermisosMenu() throws Exception {
        crearPermisoSiNoExiste("MENU_PROD", "MENU", "READ", true);
        crearPermisoSiNoExiste("MENU_INV", "MENU", "READ", true);
        crearPermisoSiNoExiste("INV_MENU_FILTER_CHECK", "INV", "READ", true);

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "MENU")
                        .with(authentication(rbacWriteAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("MENU_PROD")))
                .andExpect(jsonPath("$[*].codigo", everyItem(startsWith("MENU"))))
                .andExpect(jsonPath("$[?(@.codigo=='INV_MENU_FILTER_CHECK')]").isEmpty());
    }

    @Test
    void listarModulosIncluyeMenu() throws Exception {
        crearPermisoSiNoExiste("MENU_DOC", "MENU", "READ", true);

        mockMvc.perform(get("/api/admin/rbac/modulos")
                        .with(authentication(rbacReadAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("MENU")));
    }

    @Test
    void listarPermisosDocActivoTrueDevuelveSoloCodigosDoc() throws Exception {
        crearPermisoSiNoExiste("DOC_SMOKE_ACTIVE", "DOC", "READ", true);
        crearPermisoSiNoExiste("DOC_SMOKE_INACTIVE", "DOC", "WRITE", false);
        crearPermisoSiNoExiste("INV_SMOKE_DOC_FILTER", "INV", "READ", true);

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "DOC")
                        .param("activo", "true")
                        .with(authentication(rbacWriteAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_SMOKE_ACTIVE")))
                .andExpect(jsonPath("$[*].codigo", everyItem(startsWith("DOC"))))
                .andExpect(jsonPath("$[?(@.codigo=='DOC_SMOKE_INACTIVE')]").isEmpty())
                .andExpect(jsonPath("$[?(@.codigo=='INV_SMOKE_DOC_FILTER')]").isEmpty());
    }


    @Test
    void asignarPermisosRolSinWriteResponde403() throws Exception {
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String payload = objectMapper.writeValueAsString(Map.of(
                "modulo", "INV",
                "permisoIds", List.of()
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacReadAuth()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void asignarPermisosRolAceptaAliasModule() throws Exception {
        PermisoEntity permiso = crearPermisoSiNoExiste("INV_RBAC_ALIAS", "INV", "WRITE", true);
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String payload = objectMapper.writeValueAsString(Map.of(
                "module", "INV",
                "permisoIds", List.of(permiso.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("INV_RBAC_ALIAS"));
    }

    @Test
    void asignarPermisosRolModuloVacioResponde400ConSolicitudInvalida() throws Exception {
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String payload = objectMapper.writeValueAsString(Map.of(
                "modulo", "",
                "permisoIds", List.of()
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.details[0].field").value("modulo"));
    }

    @Test
    void asignarPermisosRolSinModuloResponde400ConSolicitudInvalida() throws Exception {
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String payload = objectMapper.writeValueAsString(Map.of(
                "permisoIds", List.of()
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.details[0].field").value("modulo"));
    }



    @Test
    void actualizarPermisosPorModuloMantienePermisosDeOtrosModulos() throws Exception {
        PermisoEntity docRead = crearPermisoSiNoExiste("DOC_RBAC_KEEP_READ", "DOC", "READ", true);
        PermisoEntity docWrite = crearPermisoSiNoExiste("DOC_RBAC_KEEP_WRITE", "DOC", "WRITE", true);
        crearPermisoSiNoExiste("INV_RBAC_REPLACE_OLD", "INV", "WRITE", true);
        PermisoEntity invRead = crearPermisoSiNoExiste("INV_RBAC_REPLACE_NEW", "INV", "READ", true);
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String setupPayload = objectMapper.writeValueAsString(Map.of(
                "modulo", "DOC",
                "permisoIds", List.of(docRead.getId(), docWrite.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(setupPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_RBAC_KEEP_READ")))
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_RBAC_KEEP_WRITE")));

        String updateInvPayload = objectMapper.writeValueAsString(Map.of(
                "modulo", "INV",
                "permisoIds", List.of(invRead.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(updateInvPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_RBAC_KEEP_READ")))
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_RBAC_KEEP_WRITE")))
                .andExpect(jsonPath("$[*].codigo", hasItem("INV_RBAC_REPLACE_NEW")))
                .andExpect(jsonPath("$[?(@.codigo=='INV_RBAC_REPLACE_OLD')]").isEmpty());
    }

    @Test
    void actualizarPermisosPorModuloConListaVaciaEliminaSoloEseModulo() throws Exception {
        PermisoEntity docRead = crearPermisoSiNoExiste("DOC_RBAC_KEEP_ONLY_DOC", "DOC", "READ", true);
        PermisoEntity invRead = crearPermisoSiNoExiste("INV_RBAC_REMOVE_ONLY_INV", "INV", "READ", true);
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String setupDocPayload = objectMapper.writeValueAsString(Map.of(
                "modulo", "DOC",
                "permisoIds", List.of(docRead.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(setupDocPayload))
                .andExpect(status().isOk());

        String setupInvPayload = objectMapper.writeValueAsString(Map.of(
                "modulo", "INV",
                "permisoIds", List.of(invRead.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(setupInvPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_RBAC_KEEP_ONLY_DOC")))
                .andExpect(jsonPath("$[*].codigo", hasItem("INV_RBAC_REMOVE_ONLY_INV")));

        String removeInvPayload = objectMapper.writeValueAsString(Map.of(
                "modulo", "INV",
                "permisoIds", List.of()
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(removeInvPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_RBAC_KEEP_ONLY_DOC")))
                .andExpect(jsonPath("$[?(@.codigo=='INV_RBAC_REMOVE_ONLY_INV')]").isEmpty());
    }

    @Test
    void asignarPermisosRolDeOtroModuloIncluyeIdYCodigoEnMensaje() throws Exception {
        PermisoEntity permisoInv = crearPermisoSiNoExiste("INV_RBAC_OTRO_MODULO", "INV", "READ", true);
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String payload = objectMapper.writeValueAsString(Map.of(
                "modulo", "DOC",
                "permisoIds", List.of(permisoInv.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.message").value("Todos los permisos deben pertenecer al módulo DOC. Inválidos: [" + permisoInv.getId() + ":INV_RBAC_OTRO_MODULO]"));
    }

    @Test
    void asignarPermisosRolSeReflejaEnAuthMe() throws Exception {
        PermisoEntity permiso = crearPermisoSiNoExiste("INV_RBAC_ASSIGN", "INV", "WRITE", true);
        Long rolPlaneadorId = rolRepository.findByCodigoAndActivoTrue("ROL_PLANEADOR")
                .orElseThrow()
                .getId();

        String payload = objectMapper.writeValueAsString(Map.of(
                "modulo", "INV",
                "permisoIds", List.of(permiso.getId())
        ));

        mockMvc.perform(put("/api/admin/rbac/roles/{rolId}/permisos", rolPlaneadorId)
                        .with(authentication(rbacWriteAuth()))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("INV_RBAC_ASSIGN"));

        CustomUserDetails principal = new CustomUserDetails(
                usuarioPlaneador,
                usuarioAuthoritiesService.buildAuthorities(usuarioPlaneador)
        );
        Authentication authPlaneador = new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());

        mockMvc.perform(get("/api/auth/me").with(authentication(authPlaneador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisos", hasItem("INV_RBAC_ASSIGN")));
    }

    private Authentication rbacWriteAuth() {
        return new UsernamePasswordAuthenticationToken(
                "rbac-admin-write-test",
                "N/A",
                List.of(new SimpleGrantedAuthority("ADMIN_RBAC_WRITE"))
        );
    }

    private Authentication rbacReadAuth() {
        return new UsernamePasswordAuthenticationToken(
                "rbac-admin-read-test",
                "N/A",
                List.of(new SimpleGrantedAuthority("ADMIN_RBAC_READ"))
        );
    }

    private Authentication noRbacAuth() {
        return new UsernamePasswordAuthenticationToken(
                "no-rbac-test",
                "N/A",
                List.of(new SimpleGrantedAuthority("INV_READ"))
        );
    }

    private PermisoEntity crearPermisoSiNoExiste(String codigo, String modulo, String accion, boolean activo) {
        return permisoRepository.findAll().stream()
                .filter(p -> codigo.equals(p.getCodigo()))
                .findFirst()
                .orElseGet(() -> permisoRepository.save(PermisoEntity.builder()
                        .codigo(codigo)
                        .modulo(modulo)
                        .accion(accion)
                        .descripcion("smoke")
                        .activo(activo)
                        .build()));
    }
}
