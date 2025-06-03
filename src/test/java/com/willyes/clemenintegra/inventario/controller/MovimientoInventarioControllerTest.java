package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.willyes.clemenintegra.util.TestUtil.asJsonString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class MovimientoInventarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private LoteProductoRepository loteProductoRepository;
    @Autowired private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManager entityManager;

    private Producto prepararProductoConEntrada() throws Exception {
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();
        UnidadMedida unidad = unidadMedidaRepository.findById(1L).orElseThrow();
        CategoriaProducto categoria = categoriaProductoRepository.findById(1L).orElseThrow();

        Producto producto = Producto.builder()
                .codigoSku("PRD-IN-001")
                .nombre("Producto Entrada")
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .stockActual(BigDecimal.valueOf(5))
                .stockMinimo(BigDecimal.valueOf(1))
                .activo(true)
                .requiereInspeccion(false)
                .creadoPor(usuario)
                .build();
        producto = productoRepository.saveAndFlush(producto);

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
                .build());

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
                .build());

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

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();

        return producto;
    }

    @Test
    @Transactional
    public void validarIncrementoStockEntrada() throws Exception {
        Producto producto = prepararProductoConEntrada();
        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(15, actualizado.getStockActual());
    }

    @Test
    @Transactional
    public void impedirCambioUnidadMedidaConMovimientosRegistrados_DebeRetornarError() throws Exception {
        Producto producto = prepararProductoConEntrada();

        String payload = """
        {
          "nombre": "Kilogramo Modificado",
          "simbolo": "kgm"
        }
        """;

        mockMvc.perform(put("/api/productos/{id}/unidad-medida", producto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede modificar la unidad de medida: existen movimientos asociados"));
    }

    @Test
    @Transactional
    public void registrarMovimientoConLoteRetenidoOEnCuarentena_DebeFallar() throws Exception {
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();
        Producto producto = crearProductoConStock(5, usuario);
        Almacen almacen = crearAlmacen();
        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-CUA-001")
                .producto(producto)
                .almacen(almacen)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(1))
                .fechaVencimiento(LocalDate.now().plusDays(5))
                .estado(EstadoLote.EN_CUARENTENA)
                .build());

        MovimientoInventarioDTO dto = buildSalidaDTO(producto, lote, almacen, usuario);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No se puede mover: el lote está en cuarentena o retenido"));
    }

    @Test
    @Transactional
    public void registrarMovimientoConLoteVencido_DebeFallar() throws Exception {
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();
        Producto producto = crearProductoConStock(5, usuario);
        Almacen almacen = crearAlmacen();
        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-VEN-001")
                .producto(producto)
                .almacen(almacen)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(30))
                .fechaVencimiento(LocalDate.now().minusDays(1))
                .estado(EstadoLote.VENCIDO)
                .build());

        MovimientoInventarioDTO dto = buildSalidaDTO(producto, lote, almacen, usuario);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No se puede mover: el lote está vencido"));
    }


    private Producto crearProductoConStock(int cantidad, Usuario usuario) {
        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder().nombre("Unidad Test").simbolo("UT").build());
        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder().nombre("Categoria Test").tipo(TipoCategoria.MATERIA_PRIMA).build());

        Producto producto = Producto.builder()
                .codigoSku("SKU-TEST-" + System.currentTimeMillis())
                .nombre("Producto Test " + System.currentTimeMillis())
                .descripcionProducto("Producto de prueba")
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .stockActual(BigDecimal.valueOf(cantidad))
                .stockMinimo(BigDecimal.valueOf(5))
                .stockMinimoProveedor(BigDecimal.valueOf(10))
                .activo(true)
                .requiereInspeccion(true)
                .fechaCreacion(LocalDateTime.now())
                .creadoPor(usuario)
                .build();

        return productoRepository.save(producto);
    }

    private Almacen crearAlmacen() {
        return almacenRepository.save(Almacen.builder()
                .nombre("Almacen Test " + System.currentTimeMillis())
                .ubicacion("Ubicación test")
                .categoria(TipoCategoria.SUMINISTROS)
                .tipo(TipoAlmacen.SATELITE)
                .build());
    }

    private MovimientoInventarioDTO buildSalidaDTO(Producto producto, LoteProducto lote, Almacen almacen, Usuario usuario) {
        return new MovimientoInventarioDTO(
                BigDecimal.valueOf(2),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-PRUEBA-" + System.currentTimeMillis(),
                producto.getId(),
                lote.getId(),
                almacen.getId(),
                1L,
                1L,
                1L,
                1L,
                usuario.getId()
        );
    }
}



