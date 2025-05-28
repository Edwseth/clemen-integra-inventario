package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.willyes.clemenintegra.util.TestUtil.asJsonString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class MovimientoInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;


    @Test
    @Commit  //Temporal
    @Transactional
    void registrarEntradaDebeIncrementarStock() throws Exception {
        // 1) Usuario existente y valores base
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();

        UnidadMedida unidad = unidadMedidaRepository.findById(1L).orElseThrow();
        CategoriaProducto categoria = categoriaProductoRepository.findById(1L).orElseThrow();

        Producto producto = new Producto();
        producto.setCodigoSku("PRD-IN-001");
        producto.setNombre("Producto Entrada");
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);
        producto.setStockActual(5); // ← stock inicial
        producto.setStockMinimo(1);
        producto.setActivo(true);
        producto.setRequiereInspeccion(false);
        producto.setCreadoPor(usuario);
        producto = productoRepository.saveAndFlush(producto); // flush garantiza persistencia inmediata

        Almacen almacen = almacenRepository.saveAndFlush(
                new Almacen(null, "A1", "Ubicación X", TipoCategoria.MATERIA_PRIMA, TipoAlmacen.PRINCIPAL)
        );

        LoteProducto lote = loteProductoRepository.saveAndFlush(LoteProducto.builder()
                .codigoLote("LOTE-IN-001")
                .producto(producto)
                .almacen(almacen)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(1))
                .fechaVencimiento(LocalDate.now().plusDays(10))
                .estado(EstadoLote.DISPONIBLE)
                .build()
        );

        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository
                .findByDescripcion("ENTRADA_PRODUCCION")
                .orElseGet(() -> tipoMovimientoDetalleRepository.save(new TipoMovimientoDetalle(null, "ENTRADA_PRODUCCION")));

        Proveedor proveedor = proveedorRepository.findByIdentificacion("111111111")
                .orElseGet(() -> proveedorRepository.save(new Proveedor(
                        null, "Prov Entrada", "111111111", "Dir", "3000000001",
                        "prov@e.com", null, "Contacto", true
                )));

        OrdenCompra orden = ordenCompraRepository.saveAndFlush(OrdenCompra.builder()
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDate.now())
                .proveedor(proveedor)
                .build()
        );

        MotivoMovimiento motivo = motivoMovimientoRepository.saveAndFlush(
                new MotivoMovimiento(null, "Motivo Entrada", TipoMovimiento.ENTRADA_PRODUCCION, TipoMovimiento.ENTRADA_PRODUCCION)
        );

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.ENTRADA_PRODUCCION,
                "DOC-ENTRADA",
                producto.getId(),
                lote.getId(),
                almacen.getId(),
                proveedor.getId(),
                orden.getId(),
                motivo.getId(),
                detalle.getId(),
                usuario.getId()
        );

        // 6) Ejecuta la petición y valida status 201
        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isCreated());

        // 7) Forzar sincronización y limpiar caché
        entityManager.flush();
        entityManager.clear();

        // 8) Verifica el incremento de stock directamente
        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(15, actualizado.getStockActual());
    }

    /*@Test
    void impedirCambioUnidadMedidaConMovimientosRegistrados_DebeRetornarError() throws Exception {
        // 1) Crea un producto y registra al menos un movimiento sobre él
        // (puedes reutilizar el test de entrada o salida para dejar datos)
        registrarEntradaDebeIncrementarStock(); // deja un movimiento en BD

        // 2) Intenta cambiar la unidad de medida del mismo producto
        Long productoId = 1L; // el ID del producto que ya tiene movimientos
        String payload = """
        {
          "nombre": "Kilogramo Modificado",
          "simbolo": "kg_mod"
        }
        """;

        mockMvc.perform(put("/api/productos/{id}/unidad-medida", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede modificar la unidad de medida: existen movimientos asociados"));
    }


    /*@Test
    void registrarMovimientoConLoteRetenidoOEnCuarentena_DebeFallar() throws Exception {
        // 1) Prepara producto/lote pero marcándolo en CUARENTENA
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();
        Producto p = crearProductoConStock(5, usuario); // helper
        Almacen a = crearAlmacen();
        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-CUA-001")
                .producto(p)
                .almacen(a)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(1))
                .fechaVencimiento(LocalDate.now().plusDays(5))
                .estado(EstadoLote.CUARENTENA)
                .build());
        // resto de DTO igual al test de salida
        MovimientoInventarioDTO dto = buildSalidaDTO(p, lote, a, usuario);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No se puede mover: el lote está en cuarentena o retenido"));
    }


    @Test
    void registrarMovimientoConLoteVencido_DebeFallar() throws Exception {
        // 1) Prepara lote vencido
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();
        Producto p = crearProductoConStock(5, usuario);
        Almacen a = crearAlmacen();
        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-VEN-001")
                .producto(p)
                .almacen(a)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(30))
                .fechaVencimiento(LocalDate.now().minusDays(1))
                .estado(EstadoLote.VENCIDO)
                .build());
        // DTO de salida
        MovimientoInventarioDTO dto = buildSalidaDTO(p, lote, a, usuario);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No se puede mover: el lote está vencido"));
    }*/


}


