package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisResumenDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import com.willyes.clemenintegra.calidad.service.PlantillasAnalisisService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlantillasAnalisisController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(com.willyes.clemenintegra.shared.security.SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class PlantillasAnalisisControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlantillasAnalisisService plantillasAnalisisService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.repository.UsuarioRepository usuarioRepository;

    @BeforeEach
    void configureFilters() throws ServletException, java.io.IOException {
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
    }

    @Test
    void permiteLecturaConQcRead() throws Exception {
        when(plantillasAnalisisService.listarPorProductoYTipo(1L, TipoAnalisisPlantilla.FISICO))
                .thenReturn(List.of(PlantillaAnalisisResumenDTO.builder().id(1L).build()));

        mockMvc.perform(get("/api/calidad/plantillas-analisis/producto/1")
                        .param("tipoAnalisis", "FISICO")
                        .with(SecurityMockMvcRequestPostProcessors.user("reader").authorities(() -> "QC_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaWriteSinQcWrite() throws Exception {
        when(plantillasAnalisisService.marcarVigente(1L)).thenReturn(PlantillaAnalisisDetalleDTO.builder().id(1L).build());

        mockMvc.perform(patch("/api/calidad/plantillas-analisis/1/vigente")
                        .with(SecurityMockMvcRequestPostProcessors.user("reader").authorities(() -> "QC_READ")))
                .andExpect(status().isForbidden());
    }
}
