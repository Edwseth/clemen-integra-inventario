package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraRequestDTO;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class OrdenCompraControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Autowired
    private OrdenCompraRepository ordenCompraRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private AjusteInventarioRepository ajusteInventarioRepository;
    @Autowired
    private FormulaProductoRepository formulaProductoRepository;
    @Autowired
    private DetalleFormulaRepository detalleFormulaRepository;
    @Autowired
    private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long proveedorId;
    private Long producto1Id;
    private Long producto2Id;

    @BeforeEach
    void setup() {
        // No eliminamos datos manualmente porque se reinicializan automáticamente con data.sql
        proveedorId = 1L;      // Corresponde al proveedor de prueba en data.sql
        producto1Id = 1L;      // Producto con SKU001
        producto2Id = 2L;      // Producto con SKU-BEBIDA-001 o insumo adicional
    }

    @Test
    void crearOrdenCompraConDetalles_debeRetornarCreated() throws Exception {
        OrdenCompraDetalleRequestDTO detalle1 = new OrdenCompraDetalleRequestDTO();
        detalle1.setProductoId(producto1Id);
        detalle1.setCantidad(new BigDecimal("10"));
        detalle1.setValorUnitario(new BigDecimal("2000"));
        detalle1.setIva(new BigDecimal("19"));

        OrdenCompraDetalleRequestDTO detalle2 = new OrdenCompraDetalleRequestDTO();
        detalle2.setProductoId(producto2Id);
        detalle2.setCantidad(new BigDecimal("5"));
        detalle2.setValorUnitario(new BigDecimal("5000"));
        detalle2.setIva(new BigDecimal("19"));

        OrdenCompraRequestDTO orden = new OrdenCompraRequestDTO(
                proveedorId,
                "Compra de prueba",
                List.of(detalle1, detalle2)
        );

        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orden)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void registrarOrdenCompraConDetalles_debeRetornarCreated() throws Exception {
        // Usamos producto y proveedor predefinidos en data.sql
        String payload = """
                {
                  "proveedorId": 1,
                  "observaciones": "Compra de prueba desde JSON",
                  "detalles": [
                    {
                      "productoId": 1,
                      "cantidad": 8.0,
                      "valorUnitario": 1200.0,
                      "iva": 19.0
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }
}