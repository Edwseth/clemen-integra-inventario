package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovimientoInventarioControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MovimientoInventarioService service;

    @MockBean
    ProductoRepository productoRepository;

    @MockBean
    LoteProductoRepository loteProductoRepository;

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void registrarConClasificacionInvalida_devuelve400() throws Exception {
        String json = """
            {
                "cantidad": 1,
                "tipoMovimiento": "SALIDA",
                "clasificacionMovimientoInventario": "SALIDAD_PRODUCCION",
                "productoId": 1,
                "loteProductoId": 1,
                "almacenOrigenId": 1,
                "tipoMovimientoDetalleId": 1
            }
            """;

        mockMvc.perform(post("/api/movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }
}
