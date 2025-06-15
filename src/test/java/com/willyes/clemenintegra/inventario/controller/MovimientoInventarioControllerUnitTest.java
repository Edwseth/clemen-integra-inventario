package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class MovimientoInventarioControllerUnitTest {

    private static final Logger log = LoggerFactory.getLogger(MovimientoInventarioControllerUnitTest.class);

    @Autowired private MockMvc mockMvc;
    @MockBean private MovimientoInventarioService movimientoInventarioService;
    @Autowired private ObjectMapper objectMapper;
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
    @Autowired private OrdenCompraDetalleRepository ordenCompraDetalleRepository;

    @Test
    void registrarMovimiento_DeberiaRetornar201() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(10.5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-456",
                1L, 1L, 1L, 1L, 1L, 1L,
                1L, 1L, 1L
        );

        // ✅ Respuesta esperada simulada
        MovimientoInventarioResponseDTO respuesta = new MovimientoInventarioResponseDTO(
                100L,
                BigDecimal.valueOf(10.5),
                1L,
                "RECEPCION_COMPRA",
                "Producto A",
                "LOTE-001",
                "Almacén Central"
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value("10.5"))
                .andExpect(jsonPath("$.tipoMovimiento").value("RECEPCION_COMPRA"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto A"))
                .andExpect(jsonPath("$.nombreLote").value("LOTE-001"))
                .andExpect(jsonPath("$.nombreAlmacen").value("Almacén Central"));
    }


    @Test
    void registrarMovimiento_ProductoInexistente_DeberiaRetornar404() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-404",
                999L, // producto inexistente
                1L, 1L, 1L, 1L, 1L,
                1L, 1L,1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new EntityNotFoundException("Producto no encontrado"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado"))
                .andExpect(jsonPath("$.status").value(404));

    }

    @Test
    void registrarMovimiento_CantidadInvalida_DeberiaRetornar400() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(0), // ❌ Cantidad inválida
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-VAL-001",
                1L, 1L, 1L, 1L, 1L, 1L,
                1L, 1L, 1L
        );

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.errors.cantidad").value("La cantidad debe ser mayor a cero"));
    }

    @Test
    void registrarMovimiento_LoteInexistente_DeberiaRetornar404() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-LOTE-404",
                1L, 999L, 1L, 1L, 1L, 1L,
                1L, 1L, 1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new IllegalArgumentException("Lote no encontrado"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lote no encontrado"));
    }

    @Test
    void registrarMovimiento_CodigoLoteDuplicado_DeberiaRetornar409() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-DUP",
                1L, 1L, 1L, 1L, 1L, 1L,
                1L, 1L, 1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Duplicate entry 'LOTE-001' for key 'codigo_lote'"
                ));

        String json = asJsonString(dto);  // 📌 usar dto, no request
        log.info("TEST ▶ JSON enviado: {}", json);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                // 📌 el GlobalExceptionHandler traduce el mensaje a:
                .andExpect(jsonPath("$.message")
                        .value("Ya existe un lote con el código ingresado."));
    }

    @Test
    void registrarSalidaConStockNegativo_DebeRetornarError() throws Exception {
        // 1) Usuario y producto con stock bajo
        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();
        Producto producto = new Producto();
        producto.setCodigoSku("PRD-001");
        producto.setNombre("Producto de prueba");
        producto.setUnidadMedida(unidadMedidaRepository.findById(1L).orElseThrow());
        producto.setCategoriaProducto(categoriaProductoRepository.findById(1L).orElseThrow());
        producto.setStockActual(BigDecimal.valueOf(5));
        producto.setStockMinimo(BigDecimal.valueOf(2));
        producto.setActivo(true);
        producto.setRequiereInspeccion(false);
        producto.setCreadoPor(usuario);
        producto = productoRepository.save(producto);

        // 2) Almacén y lote
        Almacen almacen = almacenRepository.save(
                new Almacen(null, "Almacén Central", "Ubicación A", TipoCategoria.MATERIA_PRIMA, TipoAlmacen.PRINCIPAL)
        );
        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-NEG-001")
                .producto(producto)
                .almacen(almacen)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(5))
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .estado(EstadoLote.DISPONIBLE)
                .build()
        );

        // 3) Detalle, proveedor y orden de compra
        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository
                .findByDescripcion("SALIDA_PRODUCCION")
                .orElseGet(() -> tipoMovimientoDetalleRepository.save(
                        new TipoMovimientoDetalle(null, "SALIDA_PRODUCCION")
                ));

        Proveedor proveedor = proveedorRepository.findByIdentificacion("123456789")
                .orElseGet(() -> proveedorRepository.save(
                        new Proveedor(
                                null,
                                "Proveedor Test",
                                "123456789",
                                "Dirección",
                                "3000000000",
                                "proveedor@test.com",
                                null,
                                "Contacto",
                                true
                        )
                ));

        OrdenCompra ordenCompra = ordenCompraRepository.save(OrdenCompra.builder()
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDate.now())
                .proveedor(proveedor)
                .build()
        );

        // 4) Ya existe en data.sql el motivo con ID=1 para RECEPCION_COMPRA
        MotivoMovimiento motivoMovimiento = motivoMovimientoRepository.findById(1L)
                .orElseThrow(() -> new AssertionError("Se esperaba motivo con ID=1"));
        OrdenCompraDetalle ordenCompraDetalle = ordenCompraDetalleRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Detalle de orden de compra no encontrado"));


        // 5) Construye el DTO usando ese motivo
        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "PRUEBA-STOCK",
                producto.getId(),
                lote.getId(),
                almacen.getId(),
                proveedor.getId(),
                ordenCompra.getId(),
                motivoMovimiento.getId(), // == 1L
                detalle.getId(),
                usuario.getId(),
                ordenCompraDetalle.getId()
        );

        // 6) Ejecuta y espera conflicto por stock insuficiente
        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay suficiente stock disponible"));
    }

    private static String asJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
