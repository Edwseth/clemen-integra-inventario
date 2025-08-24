package com.willyes.clemenintegra.inventario.controller;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;

import java.io.ByteArrayOutputStream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteInventarioController.class)
@EnableMethodSecurity
class ReporteInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteInventarioService service;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private LoteProductoService loteProductoService;
    @MockBean
    private MovimientoInventarioService movimientoService;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void stockActualEndpoint() throws Exception {
        Workbook wb = new XSSFWorkbook();
        when(productoService.generarReporteStockActualExcel()).thenReturn(wb);

        mockMvc.perform(get("/api/reportes/stock-actual"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_actual.xlsx"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void productosPorVencerEndpoint() throws Exception {
        Workbook wb = new XSSFWorkbook();
        when(loteProductoService.generarReporteLotesPorVencerExcel()).thenReturn(wb);

        mockMvc.perform(get("/api/reportes/productos-por-vencer"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotes_por_vencer.xlsx"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void alertasInventarioEndpoint() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(1);
        when(loteProductoService.generarReporteAlertasActivasExcel()).thenReturn(bos);

        mockMvc.perform(get("/api/reportes/alertas-inventario"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alertas_activas.xlsx"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void movimientosEndpoint() throws Exception {
        Workbook wb = new XSSFWorkbook();
        when(movimientoService.generarReporteMovimientosExcel()).thenReturn(wb);

        mockMvc.perform(get("/api/reportes/movimientos"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_movimientos.xlsx"));
    }
}
