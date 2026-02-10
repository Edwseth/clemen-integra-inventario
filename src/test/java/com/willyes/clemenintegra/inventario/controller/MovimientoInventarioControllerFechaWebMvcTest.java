package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MovimientoInventarioController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class MovimientoInventarioControllerFechaWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private LoteProductoRepository loteProductoRepository;
    @MockBean
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @MockBean
    private StockQueryService stockQueryService;
    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("/filtrar parsea fechas y devuelve resultados ordenados por fechaIngreso desc")
    void filtrarDebeRetornarOrdenDescPorFechaIngreso() throws Exception {
        MovimientoInventarioResponseDTO masReciente = MovimientoInventarioResponseDTO.builder()
                .id(1L)
                .fechaIngreso(LocalDateTime.of(2025, 12, 16, 10, 15))
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA.name())
                .cantidad(BigDecimal.TEN)
                .build();

        MovimientoInventarioResponseDTO masAntiguo = MovimientoInventarioResponseDTO.builder()
                .id(2L)
                .fechaIngreso(LocalDateTime.of(2025, 12, 8, 9, 0))
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA.name())
                .cantidad(BigDecimal.ONE)
                .build();

        Page<MovimientoInventarioResponseDTO> page = new PageImpl<>(
                List.of(masReciente, masAntiguo),
                PageRequest.of(0, 20),
                2
        );

        given(movimientoInventarioService.filtrar(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/movimientos/filtrar")
                        .param("fechaInicio", "2025-12-08")
                        .param("fechaFin", "2025-12-16")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fechaIngreso").value("2025-12-16T10:15:00"))
                .andExpect(jsonPath("$.content[1].fechaIngreso").value("2025-12-08T09:00:00"));

        ArgumentCaptor<LocalDateTime> inicioCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> finCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(movimientoInventarioService).filtrar(
                inicioCaptor.capture(),
                finCaptor.capture(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );

        assertThat(inicioCaptor.getValue()).isEqualTo(LocalDateTime.of(2025, 12, 8, 0, 0));
        assertThat(finCaptor.getValue()).isEqualTo(LocalDateTime.of(2025, 12, 16, 23, 59, 59));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("/filtrar propaga productoId al servicio")
    void filtrarDebePropagarProductoId() throws Exception {
        given(movimientoInventarioService.filtrar(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(Page.empty());

        mockMvc.perform(get("/api/movimientos/filtrar")
                        .param("fechaInicio", "2025-12-08")
                        .param("fechaFin", "2025-12-16")
                        .param("productoId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(movimientoInventarioService).filtrar(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(99L),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }
}
