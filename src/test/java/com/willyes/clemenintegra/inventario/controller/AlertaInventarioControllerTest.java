package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.AlertaInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioSeveridad;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioTipo;
import com.willyes.clemenintegra.inventario.service.AlertaInventarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertaInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertaInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertaInventarioService alertaInventarioService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void devuelveAlertasActivasConUmbralPersonalizado() throws Exception {
        AlertaInventarioResponseDTO dto = AlertaInventarioResponseDTO.builder()
                .tipo(AlertaInventarioTipo.STOCK_MINIMO)
                .severidad(AlertaInventarioSeveridad.ADVERTENCIA)
                .productoId(1L)
                .nombreProducto("Producto A")
                .codigoSku("SKU-A")
                .almacenId(10L)
                .nombreAlmacen("Almacén 1")
                .stockActual(BigDecimal.ONE)
                .umbral(BigDecimal.TEN)
                .fechaVencimiento(LocalDateTime.parse("2025-01-10T00:00:00"))
                .mensaje("Stock actual 1 por debajo del mínimo 10")
                .build();
        when(alertaInventarioService.obtenerAlertasInventario(45)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/inventario/alertas")
                        .param("diasVencimiento", "45")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("STOCK_MINIMO"))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].almacenId").value(10))
                .andExpect(jsonPath("$[0].umbral").value(10));

        ArgumentCaptor<Integer> diasCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(alertaInventarioService).obtenerAlertasInventario(diasCaptor.capture());
        assertThat(diasCaptor.getValue()).isEqualTo(45);
    }
}
