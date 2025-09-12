package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.mapper.ProveedorMapper;
import com.willyes.clemenintegra.inventario.service.ProveedorService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProveedorController.class)
@Import(SecurityConfig.class)
class ProveedorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ProveedorRepository proveedorRepository;
    @MockBean private ProveedorMapper proveedorMapper;
    @MockBean private ProveedorService proveedorService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void compradorPuedeAccederProveedores() throws Exception {
        Page emptyPage = new PageImpl<>(Collections.emptyList());
        given(proveedorService.listar(any(Pageable.class))).willReturn(emptyPage);
        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk());
    }
}
