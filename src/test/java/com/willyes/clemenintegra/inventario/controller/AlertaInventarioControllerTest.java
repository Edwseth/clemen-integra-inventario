package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class AlertaInventarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void obtenerProductosConStockBajo_DebeRetornarListaConAlerta() throws Exception {
        mockMvc.perform(get("/api/inventario/alertas/stock-bajo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto Test"))
                .andExpect(jsonPath("$[0].stockActual").value(100))
                .andExpect(jsonPath("$[0].stockMinimo").value(150)); // Asegúrate que en data.sql esto se cumpla
    }

    @Test
    void obtenerLotesVencidos_DebeRetornarLoteVencido() throws Exception {
        mockMvc.perform(get("/api/inventario/alertas/productos-vencidos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].codigoLote").value("LOTE-VENCIDO-001"))
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto Test"))
                .andExpect(jsonPath("$[0].nombreAlmacen").value("Almacen Central"));
    }

    @Test
    void obtenerLotesEnCuarentenaProlongada_DebeRetornarLote() throws Exception {
        mockMvc.perform(get("/api/inventario/alertas/lotes-retenidos-prolongados")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].codigoLote").value("LOTE-EN_CUARENTENA-001"))
                .andExpect(jsonPath("$[0].estado").value("EN_CUARENTENA"))
                .andExpect(jsonPath("$[0].nombreProducto").value("Producto Test"))
                .andExpect(jsonPath("$[0].diasEnEstado").value(org.hamcrest.Matchers.greaterThanOrEqualTo(15)));
    }

}

