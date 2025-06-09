package com.willyes.clemenintegra.inventario.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MovimientoReporteExcelTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "jefeAlmacenes", roles = {"ROL_JEFE_ALMACENES"})
    void jefeAlmacenesPuedeExportarReporteExcel() throws Exception {
        mockMvc.perform(get("/api/movimientos/reporte-excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte_movimientos.xlsx\""))
                .andExpect(content().contentType("application/octet-stream"));
    }

    @Test
    @WithMockUser(username = "analistaCalidad", roles = {"ROL_ANALISTA_CALIDAD"})
    void analistaCalidadNoPuedeExportarReporteExcel() throws Exception {
        mockMvc.perform(get("/api/movimientos/reporte-excel"))
                .andExpect(status().isForbidden());
    }
}

