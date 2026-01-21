package com.willyes.clemenintegra.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.NivelAccesoAdmin;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuperAdminSoloLecturaWriteBlockFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SuperAdminSoloLecturaWriteBlockFilter filter =
                new SuperAdminSoloLecturaWriteBlockFilter(new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductosTestController())
                .addFilter(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permitePostCuandoSuperAdminEsFull() throws Exception {
        setAuthentication(NivelAccesoAdmin.FULL);

        var result = mockMvc.perform(post("/api/productos"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(ApiErrorCode.SUPER_ADMIN_SOLO_LECTURA.getCode());
    }

    @Test
    void bloqueaPostCuandoSuperAdminEsSoloConsulta() throws Exception {
        setAuthentication(NivelAccesoAdmin.SOLO_CONSULTA);

        mockMvc.perform(post("/api/productos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SUPER_ADMIN_SOLO_LECTURA.getCode()));
    }

    @Test
    void permiteGetCuandoSuperAdminEsSoloConsulta() throws Exception {
        setAuthentication(NivelAccesoAdmin.SOLO_CONSULTA);

        var result = mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(ApiErrorCode.SUPER_ADMIN_SOLO_LECTURA.getCode());
    }

    private void setAuthentication(NivelAccesoAdmin nivelAccesoAdmin) {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("admin")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .nivelAccesoAdmin(nivelAccesoAdmin)
                .build();

        CustomUserDetails principal = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @RestController
    @RequestMapping("/api/productos")
    static class ProductosTestController {

        @PostMapping
        ResponseEntity<String> crear() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping
        ResponseEntity<String> listar() {
            return ResponseEntity.ok("ok");
        }
    }
}
