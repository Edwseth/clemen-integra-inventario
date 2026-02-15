package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResumenResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.SuperAdminSoloLecturaWriteBlockFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConteoCiclicoController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, ConteoCiclicoControllerSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class ConteoCiclicoControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private RequestIdFilter requestIdFilter;
    @MockBean
    private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
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

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestIdFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(superAdminSoloLecturaWriteBlockFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    void permiteListarConteosConPermisoCanonicoRead() throws Exception {
        ConteoCiclicoResumenResponseDTO response = ConteoCiclicoResumenResponseDTO.builder()
                .id(1L)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.listar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/inventario/conteos")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user("contador-permiso")
                                .authorities(() -> "INV_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void permiteCrearConteoConPermisoEscritura() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(10L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.crearConteo(any(ConteoCiclicoRequestDTO.class))).thenReturn(response);

        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user("contador-write")
                                .authorities(() -> "INV_CONTEOS_WRITE")))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaCrearConteoSoloConPermisoRead() throws Exception {
        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user("contador-read")
                                .authorities(() -> "INV_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteAplicarConteoConPermisoWorkflowFinish() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(77L)
                .estado(EstadoConteoCiclico.APLICADO)
                .build();
        when(conteoCiclicoService.aplicar(anyLong(), eq("k1"))).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .header("Idempotency-Key", "k1")
                        .with(SecurityMockMvcRequestPostProcessors.user("contador")
                                .authorities(() -> "INV_WORKFLOW_FINISH")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaAplicarConteoConRolJefeAlmacenes() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe")
                                .authorities(() -> "ROL_JEFE_ALMACENES")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaAplicarConteoConPermisoWriteSinRolContador() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .with(SecurityMockMvcRequestPostProcessors.user("perm-write")
                                .authorities(() -> "INV_CONTEOS_WRITE")))
                .andExpect(status().isForbidden());
    }

}
