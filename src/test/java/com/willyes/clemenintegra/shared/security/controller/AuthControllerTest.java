package com.willyes.clemenintegra.shared.security.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.UsuarioAuthoritiesService;
import com.willyes.clemenintegra.support.IntegrationTestH2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest extends IntegrationTestH2 {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioAuthoritiesService usuarioAuthoritiesService;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    private Usuario usuarioPlaneador;
    private Usuario usuarioContador;
    private Usuario usuarioJefeProduccion;

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

        usuarioContador = usuarioRepository.findByNombreUsuario("contador")
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nombreUsuario("contador")
                        .clave("secret")
                        .nombreCompleto("Contador Test")
                        .correo("contador@test.local")
                        .rol(RolUsuario.ROL_CONTADOR)
                        .activo(true)
                        .bloqueado(false)
                        .build()));

        usuarioJefeProduccion = usuarioRepository.findByNombreUsuario("jefeprod")
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nombreUsuario("jefeprod")
                        .clave("secret")
                        .nombreCompleto("Jefe Producción Test")
                        .correo("jefeprod@test.local")
                        .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                        .activo(true)
                        .bloqueado(false)
                        .build()));
    }

    @Test
    void meRetornaRolYPermisosDelPlaneador() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(
                usuarioPlaneador,
                usuarioAuthoritiesService.buildAuthorities(usuarioPlaneador)
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                principal.getAuthorities()
        );

        mockMvc.perform(get("/api/auth/me").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ROL_PLANEADOR"))
                .andExpect(jsonPath("$.permisos").isArray())
                .andExpect(jsonPath("$.permisos").value(hasItem("PROD_OP_CREATE")))
                .andExpect(jsonPath("$.permisos").value(hasItem("PROD_OP_READ")))
                .andExpect(jsonPath("$.permisos").value(hasItem("PROD_ETAPA_CHECKLIST_READ")))
                .andExpect(jsonPath("$.permisos").value(hasItem("PROD_BATCH_RECORD_READ")))
                .andExpect(jsonPath("$.permisos").value(hasItem("INV_READ")))
                .andExpect(jsonPath("$.permisos").value(hasItem("MENU_PROD")))
                .andExpect(jsonPath("$.permisos").value(hasItem("BOM_READ")))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_OP_EDIT"))))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_ETAPA_START"))))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_ETAPA_FINISH"))))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_ETAPA_CHECKLIST_WRITE"))))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_BATCH_RECORD_WRITE"))))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_OP_WORKFLOW_CANCEL"))))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_OP_WORKFLOW_FINALIZE"))));
    }


    @Test
    void meIncluyePermisoRegularizacionSoloParaContador() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(
                usuarioContador,
                usuarioAuthoritiesService.buildAuthorities(usuarioContador)
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                principal.getAuthorities()
        );

        mockMvc.perform(get("/api/auth/me").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ROL_CONTADOR"))
                .andExpect(jsonPath("$.permisos").value(hasItem("PROD_TRAZABILIDAD_REGULARIZACION")));
    }

    @Test
    void meNoIncluyePermisoRegularizacionParaNoContador() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(
                usuarioJefeProduccion,
                usuarioAuthoritiesService.buildAuthorities(usuarioJefeProduccion)
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                principal.getAuthorities()
        );

        mockMvc.perform(get("/api/auth/me").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ROL_JEFE_PRODUCCION"))
                .andExpect(jsonPath("$.permisos").value(not(hasItem("PROD_TRAZABILIDAD_REGULARIZACION"))));
    }

}
