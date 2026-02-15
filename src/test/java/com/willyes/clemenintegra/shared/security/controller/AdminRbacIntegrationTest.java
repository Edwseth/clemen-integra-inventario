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
    void listarPermisosInvActivoTrueDevuelveSoloCodigosInv() throws Exception {
        crearPermisoSiNoExiste("INV_SMOKE_ACTIVE", "INV", "READ", true);
        crearPermisoSiNoExiste("INV_SMOKE_INACTIVE", "INV", "READ", false);
        crearPermisoSiNoExiste("QC_SMOKE_ACTIVE", "QC", "READ", true);

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "INV")
                        .with(authentication(superAdminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("INV_SMOKE_ACTIVE")))
                .andExpect(jsonPath("$[*].codigo", everyItem(startsWith("INV"))))
                .andExpect(jsonPath("$[?(@.codigo=='INV_SMOKE_INACTIVE')]").isEmpty())
                .andExpect(jsonPath("$[?(@.codigo=='QC_SMOKE_ACTIVE')]").isEmpty());
    }


    @Test
    void listarPermisosDocActivoTrueDevuelveSoloCodigosDoc() throws Exception {
        crearPermisoSiNoExiste("DOC_SMOKE_ACTIVE", "DOC", "READ", true);
        crearPermisoSiNoExiste("DOC_SMOKE_INACTIVE", "DOC", "WRITE", false);
        crearPermisoSiNoExiste("INV_SMOKE_DOC_FILTER", "INV", "READ", true);

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "DOC")
                        .param("activo", "true")
                        .with(authentication(superAdminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("DOC_SMOKE_ACTIVE")))
                .andExpect(jsonPath("$[*].codigo", everyItem(startsWith("DOC"))))
                .andExpect(jsonPath("$[?(@.codigo=='DOC_SMOKE_INACTIVE')]").isEmpty())
                .andExpect(jsonPath("$[?(@.codigo=='INV_SMOKE_DOC_FILTER')]").isEmpty());
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
                        .with(authentication(superAdminAuth()))
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

    private Authentication superAdminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "super-admin-test",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROL_SUPER_ADMIN"))
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
