package com.willyes.clemenintegra.inventario.web;

import com.willyes.clemenintegra.inventario.controller.ReporteInventarioController;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class ReporteInventarioControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteInventarioService reporteInventarioService;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private LoteProductoService loteProductoService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("GET /api/reportes/alta-rotacion responde 200 y cabecera de adjunto")
    void altaRotacion_devuelveExcel() throws Exception {
        when(reporteInventarioService.generarReporteAltaRotacion(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new XSSFWorkbook());

        mockMvc.perform(get("/api/reportes/alta-rotacion")
                        .param("fechaInicio", "2024-01-01")
                        .param("fechaFin", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=alta_rotacion.xlsx"))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    @DisplayName("GET /api/reportes/alta-rotacion responde 200 para comprador")
    void altaRotacion_permitidoParaComprador() throws Exception {
        when(reporteInventarioService.generarReporteAltaRotacion(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new XSSFWorkbook());

        mockMvc.perform(get("/api/reportes/alta-rotacion")
                        .param("fechaInicio", "2024-01-01")
                        .param("fechaFin", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("GET /api/reportes/productos-por-vencer responde 200 con adjunto")
    void productosPorVencer_devuelveExcel() throws Exception {
        when(loteProductoService.generarReporteLotesPorVencerExcel(any(), any()))
                .thenReturn(new XSSFWorkbook());

        mockMvc.perform(get("/api/reportes/productos-por-vencer")
                        .param("fechaInicio", "2024-02-01")
                        .param("fechaFin", "2024-02-10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=lotes_por_vencer.xlsx"))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
