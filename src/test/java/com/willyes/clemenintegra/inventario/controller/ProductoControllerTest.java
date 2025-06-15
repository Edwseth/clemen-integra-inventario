package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class ProductoControllerTest {

    private static final Logger log = LoggerFactory.getLogger(ProductoControllerTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private LoteProductoRepository loteProductoRepository;
    @Autowired private AjusteInventarioRepository ajusteInventarioRepository;
    @Autowired private DetalleFormulaRepository detalleFormulaRepository;
    @Autowired private FormulaProductoRepository formulaProductoRepository;
    @Autowired private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired private OrdenProduccionRepository ordenProduccionRepository;

    private Long unidadId;
    private Long categoriaId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        ordenProduccionRepository.deleteAll();
        ajusteInventarioRepository.deleteAll();
        movimientoInventarioRepository.deleteAll();
        loteProductoRepository.deleteAll();
        detalleFormulaRepository.deleteAll();
        formulaProductoRepository.deleteAll();
        ordenCompraDetalleRepository.deleteAll();
        ordenCompraRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        categoriaProductoRepository.deleteAll();

        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("Kilogramo");
        unidad.setSimbolo("kg");
        unidad = unidadMedidaRepository.save(unidad);
        unidadId = unidad.getId();

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setNombre("Materia Prima");
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        categoria = categoriaProductoRepository.save(categoria);
        categoriaId = categoria.getId();

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("admin");
        usuario.setNombreCompleto("Administrador");
        usuario.setCorreo("admin@erp.com");
        usuario.setRol(RolUsuario.ROL_ALMACENISTA);
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        usuario.setClave("1234");
        usuario = usuarioRepository.save(usuario);
        usuarioId = usuario.getId();
    }

    @Test
    void crearProducto_SkuDuplicado_RetornaConflict() throws Exception {
        ProductoRequestDTO producto1 = new ProductoRequestDTO();
        producto1.setCodigoSku("MP-001");
        producto1.setNombre("Producto 1");
        producto1.setDescripcionProducto("Desc 1");
        producto1.setUnidadMedidaId(unidadId);
        producto1.setCategoriaProductoId(categoriaId);
        producto1.setStockActual(BigDecimal.valueOf(100));
        producto1.setStockMinimo(BigDecimal.valueOf(20));
        producto1.setStockMinimoProveedor(BigDecimal.valueOf(50));
        producto1.setActivo(true);
        producto1.setRequiereInspeccion(false);
        producto1.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto1)))
                .andExpect(status().isCreated());

        ProductoRequestDTO producto2 = new ProductoRequestDTO();
        producto2.setCodigoSku("MP-001"); // duplicado
        producto2.setNombre("Producto duplicado");
        producto2.setDescripcionProducto("Otro");
        producto2.setUnidadMedidaId(unidadId);
        producto2.setCategoriaProductoId(categoriaId);
        producto2.setStockActual(BigDecimal.valueOf(30));
        producto2.setStockMinimo(BigDecimal.valueOf(5));
        producto2.setStockMinimoProveedor(BigDecimal.valueOf(10));
        producto2.setActivo(true);
        producto2.setRequiereInspeccion(false);
        producto2.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto2)))
                .andDo(result -> log.info("SEGUNDO POST - BODY: {}", result.getResponse().getContentAsString()))
                .andExpect(status().isConflict());
    }

    @Test
    void crearProducto_RequiereInspeccion_True_RetornaCreated() throws Exception {
        ProductoRequestDTO producto = new ProductoRequestDTO();
        producto.setCodigoSku("MP-002");
        producto.setNombre("Producto Inspeccionado");
        producto.setDescripcionProducto("Producto con calidad");
        producto.setUnidadMedidaId(unidadId);
        producto.setCategoriaProductoId(categoriaId);
        producto.setStockActual(BigDecimal.valueOf(50));
        producto.setStockMinimo(BigDecimal.valueOf(10));
        producto.setStockMinimoProveedor(BigDecimal.valueOf(20));
        producto.setActivo(true);
        producto.setRequiereInspeccion(true);
        producto.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiereInspeccion").value(true));
    }

    @Test
    void consultarProductosPorCategoria_RetornaLista() throws Exception {
        mockMvc.perform(get("/api/productos?categoria=MATERIA_PRIMA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "jefeAlmacen", roles = {"ROL_JEFE_ALMACENES"})
    void exportarStockActual_UsuarioAutorizado_RetornaExcel() throws Exception {
        mockMvc.perform(get("/api/productos/reporte-stock"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "analista", roles = {"ROL_ANALISTA_CALIDAD"})
    void exportarStockActual_UsuarioNoAutorizado_RetornaForbidden() throws Exception {
        mockMvc.perform(get("/api/productos/reporte-stock"))
                .andExpect(status().isForbidden());
    }
}
