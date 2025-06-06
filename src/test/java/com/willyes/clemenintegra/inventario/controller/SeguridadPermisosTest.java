package com.willyes.clemenintegra.inventario.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
public class SeguridadPermisosTest {

    @Autowired private MockMvc mockMvc;


    @Test
    @WithMockUser(username = "almacenista", roles = {"ALMACENISTA"})
    void almacenistaPuedeRegistrarMovimiento() throws Exception {
        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                           "cantidad": 5.0,
                           "tipoMovimiento": "RECEPCION_COMPRA",
                           "productoId": 1,
                           "loteProductoId": 1,
                           "almacenId": 1,
                           "proveedorId": 1,
                           "ordenCompraId": 1,
                           "ordenCompraDetalleId": 1,
                           "motivoMovimientoId": 1,
                           "tipoMovimientoDetalleId": 1,
                           "usuarioId": 1
                         }
                        """))
                        .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "almacenista", roles = {"ROL_ALMACENISTA"})
    void almacenistaNoPuedeEditarOrdenCompra() throws Exception {
        String payload = """
        {
            "proveedorId": 1,
            "observaciones": "Intento de modificación",
            "detalles": []
        }
        """;

        mockMvc.perform(put("/api/ordenes-compra/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden()); // ✅ Esperado
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"JEFE_CALIDAD"})
    void jefeCalidadPuedeConsultarProductos() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"JEFE_CALIDAD"})
    void jefeCalidadPuedeVerProductosRetenidos() throws Exception {
        mockMvc.perform(get("/api/productos/con-lotes")
                        .param("estado", "RETENIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoLote").value("RETENIDO"));
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"JEFE_CALIDAD"})
    void obtenerProductosConLotesRetenidos_debeRetornar200() throws Exception {
        mockMvc.perform(get("/api/productos/con-lotes")
                        .param("estado", "RETENIDO"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"JEFE_CALIDAD"})
    void jefeCalidadPuedeVerProductosAgrupadosPorLotesRetenidos() throws Exception {
        mockMvc.perform(get("/api/productos/agrupado-por-lotes")
                        .param("estado", "RETENIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lotes[0].estado").value("RETENIDO"));
    }

    @Test
    @WithMockUser(username = "usuarioInactivo", roles = {"ALMACENISTA"})
    void usuarioInactivoNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized()); // o .isForbidden() si así lo configuras
    }

}
