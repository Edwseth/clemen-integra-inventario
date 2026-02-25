package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.reportes.InventarioValorizadoRowDTO;
import com.willyes.clemenintegra.inventario.service.InventarioValorizadoReportService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventarioValorizadoReporteController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventarioValorizadoReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventarioValorizadoReportService inventarioValorizadoReportService;

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void listarInventarioValorizadoRetorna200() throws Exception {
        InventarioValorizadoRowDTO fila = new InventarioValorizadoRowDTO(
                "SKU-1",
                "Producto 1",
                "KG",
                "LOT-1",
                LocalDate.now(),
                "A1 / Principal",
                new BigDecimal("10.000000"),
                new BigDecimal("2.000000"),
                new BigDecimal("20.000000")
        );
        when(inventarioValorizadoReportService.listar(any()))
                .thenReturn(new PageImpl<>(List.of(fila), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/reportes/inventario-valorizado?page=0&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void exportarExcelRetorna200ConContentTypeXlsx() throws Exception {
        when(inventarioValorizadoReportService.generarExcel()).thenReturn(new XSSFWorkbook());

        mockMvc.perform(get("/api/inventario/reportes/inventario-valorizado/excel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
