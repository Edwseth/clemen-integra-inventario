package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class MovimientoInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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

    @Test
    void registrarMovimiento_DeberiaRetornar201() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(10.5),                            // 1) cantidad
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,  // 2) tipoMovimiento (enum correcto)
                "DOC-456",                                           // 3) docReferencia
                1L, // 4) productoId
                1L, // 5) loteProductoId
                1L, // 6) almacenId
                1L, // 7) proveedorId
                1L, // 8) ordenCompraId
                1L, // 9) motivoMovimientoId
                1L, // 10) tipoMovimientoDetalleId
                1L  // 11) usuarioRegistroId
        );


        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any())).thenReturn(dto);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value("10.5"))
                .andExpect(jsonPath("$.tipoMovimiento").value("RECEPCION_COMPRA"))
                .andExpect(jsonPath("$.docReferencia").value("DOC-456"));
    }

    @Test
    void registrarMovimiento_ProductoInexistente_DeberiaRetornar404() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-404",
                999L, // producto inexistente
                1L, 1L, 1L, 1L, 1L, 1L, 1L
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
                BigDecimal.valueOf(0), // ❌ Cantidad inválida
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-VAL-001",
                1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L
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
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-LOTE-404",
                1L, 999L, 1L, 1L, 1L, 1L, 1L, 1L
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
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-DUP",
                1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Duplicate entry 'LOTE-001' for key 'codigo_lote'"
                ));

        String json = asJsonString(dto);  // 📌 usar dto, no request
        System.out.println("TEST ▶ JSON enviado: " + json);

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

        Usuario usuario = usuarioRepository.findById(1L).orElseThrow();

        // Paso 1: Crear un producto con stock bajo
        Producto producto = new Producto();
        producto.setCodigoSku("PRD-001");
        producto.setNombre("Producto de prueba");
        producto.setUnidadMedida(unidadMedidaRepository.findById(1L).orElseThrow());
        producto.setCategoriaProducto(categoriaProductoRepository.findById(1L).orElseThrow());
        producto.setStockActual(5);
        producto.setStockMinimo(2);
        producto.setActivo(true);
        producto.setRequiereInspeccion(false);
        producto.setCreadoPor(usuario); // 👈 se asegura de que no sea null

        producto = productoRepository.save(producto);


        Almacen almacen = almacenRepository.save(new Almacen(null, "Almacén Central", "Ubicación A",
                TipoCategoria.MATERIA_PRIMA, TipoAlmacen.PRINCIPAL));

        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-NEG-001")
                .producto(producto)
                .almacen(almacen)
                .stockLote(BigDecimal.valueOf(5))
                .fechaFabricacion(LocalDate.now().minusDays(5))
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .estado(EstadoLote.DISPONIBLE)
                .build());

        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository
                .findByDescripcion("SALIDA_PRODUCCION")
                .orElseGet(() -> tipoMovimientoDetalleRepository.save(new TipoMovimientoDetalle(null, "SALIDA_PRODUCCION")));


        // Proveedor de prueba
        Proveedor proveedor = proveedorRepository.findByIdentificacion("123456789")
                .orElseGet(() -> proveedorRepository.save(new Proveedor(
                        null, "Proveedor Test", "123456789", "Dirección", "3000000000",
                        "proveedor@test.com", null, "Contacto", true
                )));


        // Orden de compra de prueba
        OrdenCompra ordenCompra = ordenCompraRepository.save(OrdenCompra.builder()
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDate.now())
                .proveedor(proveedor)
                .build());

        // Motivo de movimiento de prueba sin duplicar
        MotivoMovimiento motivoMovimiento = motivoMovimientoRepository
                .findByMotivo(TipoMovimiento.RECEPCION_COMPRA)
                .orElseGet(() -> motivoMovimientoRepository.save(
                        new MotivoMovimiento(null, "Motivo Test", TipoMovimiento.RECEPCION_COMPRA, TipoMovimiento.RECEPCION_COMPRA)
                ));



        System.out.println("Producto ID: " + producto.getId());
        System.out.println("Stock actual: " + producto.getStockActual());
        System.out.println("Cantidad solicitada: 10");
        System.out.println("Tipo movimiento: " + ClasificacionMovimientoInventario.SALIDA_PRODUCCION);



        // DTO corregido
        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "PRUEBA-STOCK",
                producto.getId(),
                lote.getId(),
                almacen.getId(),
                proveedor.getId(),
                ordenCompra.getId(),
                motivoMovimiento.getId(),
                detalle.getId(),
                1L
        );

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


    @Test
    void registrarEntradaDebeIncrementarStock() throws Exception {
        // TODO: Verificar incremento en stock_actual tras registrar entrada
    }

    @Test
    void impedirCambioUnidadMedidaConMovimientosRegistrados_DebeRetornarError() throws Exception {
        // TODO: Crear movimiento previo, luego intentar modificar unidad de medida
    }

    @Test
    void registrarMovimientoConLoteRetenidoOEnCuarentena_DebeFallar() throws Exception {
        // TODO: Preparar lote con estado CUARENTENA o RETENIDO y enviar salida
    }

    @Test
    void registrarMovimientoConLoteVencido_DebeFallar() throws Exception {
        // TODO: Crear lote con fecha de vencimiento pasada y enviar salida
    }


}


