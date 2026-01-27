package com.willyes.clemenintegra.shared.security.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.UsuarioAuthoritiesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioAuthoritiesService usuarioAuthoritiesService;

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
                .andExpect(jsonPath("$.permisos").value(org.hamcrest.Matchers.hasItem("PROD_OP_CREATE")));
    }
}
