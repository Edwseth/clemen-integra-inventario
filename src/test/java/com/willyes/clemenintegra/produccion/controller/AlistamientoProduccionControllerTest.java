package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.AlistamientoOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.SolicitudAlistamientoResumenDTO;
import com.willyes.clemenintegra.produccion.service.ProduccionAlistamientoService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlistamientoProduccionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
@EnableMethodSecurity
class AlistamientoProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProduccionAlistamientoService alistamientoService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "PROD_READ")
    void obtenerAlistamientoRetornaDto() throws Exception {
        AlistamientoOrdenProduccionDTO dto = AlistamientoOrdenProduccionDTO.builder()
                .ordenId(1L)
                .codigoOrden("OP-123")
                .estadoOrden("EN_PROCESO")
                .estadoAlistamiento("EN_PROCESO")
                .solicitudes(List.of(
                        SolicitudAlistamientoResumenDTO.builder()
                                .solicitudId(5L)
                                .estado("PENDIENTE")
                                .fechaCreacion(LocalDateTime.of(2024, 1, 1, 10, 0))
                                .build()
                ))
                .build();

        when(alistamientoService.obtenerAlistamientoPorOrden(eq(1L))).thenReturn(dto);

        mockMvc.perform(get("/api/produccion/ordenes/{id}/alistamiento", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoOrden").value("OP-123"))
                .andExpect(jsonPath("$.estadoAlistamiento").value("EN_PROCESO"))
                .andExpect(jsonPath("$.solicitudes[0].solicitudId").value(5L));
    }
}
