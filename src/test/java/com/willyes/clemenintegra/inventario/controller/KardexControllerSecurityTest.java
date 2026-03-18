package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;
import com.willyes.clemenintegra.inventario.service.KardexService;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KardexController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class KardexControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KardexService kardexService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void configureFilters() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(usuarioInactivoFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestTimingFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    void permiteAccesoConRolJefeProduccion() throws Exception {
        when(kardexService.obtenerKardex(any(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/inventario/kardex")
                        .param("productoId", "1")
                        .param("ordenProduccionId", "7")
                        .param("etapaProduccionId", "11")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe-produccion")
                                .authorities(() -> "INV_KARDEX_READ"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<KardexFiltro> captor = ArgumentCaptor.forClass(KardexFiltro.class);
        verify(kardexService).obtenerKardex(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().getOrdenProduccionId()).isEqualTo(7L);
        assertThat(captor.getValue().getEtapaProduccionId()).isEqualTo(11L);
    }

    @Test
    void permiteAccesoConRolSuperAdmin() throws Exception {
        when(kardexService.obtenerKardex(any(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(KardexItemDTO.builder().build())));

        mockMvc.perform(get("/api/inventario/kardex")
                        .param("productoId", "9")
                        .with(SecurityMockMvcRequestPostProcessors.user("superadmin")
                                .authorities(() -> "INV_KARDEX_READ")))
                .andExpect(status().isOk());

        verify(kardexService).obtenerKardex(any(), any(Pageable.class));
    }

    @Test
    void rechazaRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/inventario/kardex")
                        .param("productoId", "3")
                        .with(SecurityMockMvcRequestPostProcessors.user("analista")
                                .authorities(() -> "QC_READ")))
                .andExpect(status().isForbidden());
    }
}
